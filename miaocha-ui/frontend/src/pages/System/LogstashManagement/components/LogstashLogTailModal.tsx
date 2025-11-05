import { Modal, Button, message } from 'antd';
import { useEffect, useState, useRef } from 'react';
import { startLogTail } from '@/api/logstash';
import styles from './LogstashLogTailModal.module.less';

interface LogTailModalProps {
  visible: boolean;
  logstashMachineId: number;
  onCancel: () => void;
}

export default function LogTailModal({ visible, logstashMachineId, onCancel }: LogTailModalProps) {
  interface LogEntry {
    id: string;
    timestamp: number;
    content: string;
    groupId?: string;
  }

  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [isTailing, setIsTailing] = useState(false);
  const [isPaused] = useState(false);
  const [, setStatus] = useState<any>(null);
  const [maxLogs] = useState(1000);
  const logsEndRef = useRef<HTMLDivElement>(null);
  const eventSourceRef = useRef<EventSource | null>(null);
  const scrollContainerRef = useRef<HTMLDivElement>(null); // 替换 virtualListRef
  const [shouldAutoScroll, setShouldAutoScroll] = useState(true);
  const [messageApi, contextHolder] = message.useMessage();

  const handleMouseEnter = () => {
    setShouldAutoScroll(false);
  };
  const handleMouseLeave = () => {
    setShouldAutoScroll(true);
  };

  // Custom modal close handler with automatic cleanup
  const handleModalCancel = async () => {
    // Stop tailing by closing the connection (automatic cleanup)
    if (isTailing && eventSourceRef.current) {
      eventSourceRef.current.close();
      eventSourceRef.current = null;
      messageApi.success('Log tail stopped');
      setIsTailing(false);
    }
    setLogs([]);
    // Call parent component's onCancel
    onCancel();
  };

  const startTail = async () => {
    try {
      const eventSource = startLogTail(logstashMachineId, 500); // Default 500 lines
      if (!eventSource) {
        // throw new Error('Failed to create EventSource connection');
      }

      const resolvedEventSource = await eventSource;

      // Setup connection handlers
      resolvedEventSource.onopen = () => {
        console.log('Log tail connection opened');
      };

      eventSourceRef.current = resolvedEventSource;
      eventSourceRef.current.addEventListener('log-data', (event) => {
        try {
          const data = JSON.parse(event.data);
          if (!isPaused) {
            setLogs((prev) => {
              const newEntries = (data.logLines || [data]).map((log: string, i: number) => ({
                id: `${Date.now()}-${i}`,
                key: `${Date.now()}-${i}`,
                timestamp: Date.now(),
                content: typeof log === 'string' ? log : JSON.stringify(log),
              }));
              const newLogs = [...prev, ...newEntries];

              // Limit log count to prevent memory issues
              if (newLogs.length > maxLogs) {
                return newLogs.slice(-maxLogs);
              }

              return newLogs;
            });
          }
        } catch (error) {
          console.error('Log parsing error:', error);
        }
      });

      resolvedEventSource.addEventListener('heartbeat', (event) => {
        try {
          const data = JSON.parse(event.data);
          setStatus(data.status);
        } catch (error) {
          console.error('Heartbeat data parsing error:', error);
        }
      });

      eventSourceRef.current.onerror = (err) => {
        setIsTailing(false);
        eventSourceRef.current?.close();
        eventSourceRef.current = null;
      };

      setIsTailing(true);
      setLogs([]);
      messageApi.success('Log tail started');
    } catch (error: unknown) {
      console.error('Failed to start log tail:', error);
      setIsTailing(false);
    }
  };

  const stopTail = async () => {
    try {
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
      }
      setIsTailing(false);
      messageApi.success('Log tail stopped');
    } catch (error) {
      console.error('Failed to stop log tail:', error);
    }
  };

  // Component cleanup - ensure connection is closed
  useEffect(() => {
    return () => {
      // Close connection on component unmount
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
      }
    };
  }, []);

  useEffect(() => {
    if (!visible) {
      // Auto-stop tailing when modal is hidden
      if (isTailing && eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
        setIsTailing(false);
      }
    }
  }, [visible]);

  useEffect(() => {
    if (shouldAutoScroll && scrollContainerRef.current) {
      scrollContainerRef.current.scrollTop = scrollContainerRef.current.scrollHeight;
    }
  }, [logs, shouldAutoScroll]);

  return (
    <>
      {contextHolder}
      <Modal
        bodyStyle={{
          height: 'calc(100vh - 140px)',
          padding: 0,
          display: 'flex',
          flexDirection: 'column',
        }}
        footer={[
          <Button key="stop" danger onClick={stopTail}>
            停止跟踪
          </Button>,
          <Button key="start" type="primary" disabled={isTailing} onClick={startTail}>
            开始跟踪
          </Button>,
        ]}
        open={visible}
        style={{
          top: 0,
          maxWidth: '100vw',
          margin: 0,
        }}
        title={`日志跟踪 (实例ID: ${logstashMachineId})`}
        width="100%"
        onCancel={handleModalCancel}
        // 点击遮罩层不会关闭弹窗
        maskClosable={false}
      >
        <div className={styles.statusContainer}>
          <h4>跟踪状态: {isTailing ? '运行中' : '已停止'} </h4>
        </div>
        <div
          ref={logsEndRef}
          className={styles.logContainer}
          onMouseEnter={handleMouseEnter}
          onMouseLeave={handleMouseLeave}
        >
          {logs.length > 0 ? (
            <div ref={scrollContainerRef} className={styles.scrollableWrapper}>
              {/* 
                直接渲染所有日志项，适合日志显示场景
                优势：
                1. 支持可变高度的日志内容（多行文本、换行等）
                2. 滚动条显示正常
                3. 实现简单，性能足够（通过maxLogs限制数量）
                
                注意：如果需要处理超大量数据（>10000条），
                可以考虑重新启用虚拟列表并修复itemHeight问题
              */}
              {logs.map((log, index) => (
                <div key={log.id || index} className={styles.logItem}>
                  {log.content}
                </div>
              ))}
            </div>
          ) : (
            <div className={styles.noData}>暂无日志数据</div>
          )}
        </div>
      </Modal>
    </>
  );
}
