import { Modal, Button, message } from 'antd';
import { useEffect, useState, useRef } from 'react';
import { startLogTail, stopLogTail, createLogTailTask } from '@/api/logstash';
import styles from './LogstashLogTailModal.module.less';

interface LogTailModalProps {
  visible: boolean;
  logstashMachineId: number;
  onCancel: () => void;
  style?: React.CSSProperties;
  bodyStyle?: React.CSSProperties;
}

export default function LogTailModal({ visible, logstashMachineId, onCancel, style, bodyStyle }: LogTailModalProps) {
  interface LogEntry {
    id: string;
    timestamp: number;
    content: string;
    groupId?: string;
  }

  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [isTailing, setIsTailing] = useState(false);
  const [isPaused, setIsPaused] = useState(false);
  const [status, setStatus] = useState<any>(null);
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

  // 自定义弹窗关闭处理函数，会自动停止日志跟踪
  const handleModalCancel = async () => {
    // 如果正在跟踪日志，自动停止
    if (isTailing) {
      await stopTail();
    }
    // 调用父组件传入的onCancel
    onCancel();
  };

  const startTail = async () => {
    try {
      // 首先创建日志跟踪任务
      await createLogTailTask(logstashMachineId);
      // 调用异步startLogTail函数并等待返回EventSource对象
      const eventSource = startLogTail(logstashMachineId);
      if (!eventSource) {
        throw new Error('无法创建EventSource连接');
      }
      // 监听EventSource的消息事件
      (
        await // 监听EventSource的消息事件
        eventSource
      ).onopen = () => {
        console.log('日志跟踪连接已打开');
      };

      eventSourceRef.current = await eventSource;
      eventSourceRef.current.addEventListener('log-data', (event) => {
        try {
          const data = JSON.parse(event.data);
          if (!isPaused) {
            setLogs((prev) => {
              const newEntries = (data.logLines || [data]).map((log: string, i: number) => ({
                id: `${Date.now()}-${i}`,
                key: `${Date.now()}-${i}`, // 添加key属性供VirtualList使用
                timestamp: Date.now(),
                content: typeof log === 'string' ? log : JSON.stringify(log),
              }));
              const newLogs = [...prev, ...newEntries];

              // 限制日志数量，避免内存溢出和性能问题
              if (newLogs.length > maxLogs) {
                return newLogs.slice(-maxLogs); // 保留最新的maxLogs条记录
              }

              return newLogs;
            });
          }
        } catch (error) {
          console.error('日志解析错误:', error);
        }
      });

      (await eventSource).addEventListener('heartbeat', (event) => {
        try {
          const data = JSON.parse(event.data);
          setStatus(data.status);
        } catch (error) {
          console.error('心跳数据解析错误:', error);
        }
      });

      eventSourceRef.current.onerror = (err) => {
        console.error('EventSource错误:', err);
        setIsTailing(false);
        eventSourceRef.current?.close();
        eventSourceRef.current = null;
      };

      setIsTailing(true);
      setLogs([]);
      messageApi.success('日志跟踪已启动');
    } catch (error: unknown) {
      console.error('启动日志跟踪失败:', error);
      setIsTailing(false);
    }
  };

  const stopTail = async () => {
    try {
      await stopLogTail(logstashMachineId);
      setIsTailing(false);
      messageApi.success('日志跟踪已停止');
    } catch (error) {
      console.error('停止日志跟踪失败:', error);
    }
  };

  // 组件卸载时确保关闭连接
  useEffect(() => {
    return () => {
      // 组件卸载时确保关闭连接
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
      }
    };
  }, []);

  useEffect(() => {
    if (!visible) {
      // 如果弹窗不可见且正在跟踪，自动停止跟踪
      if (isTailing) {
        stopTail().catch((error) => {
          console.error('组件卸载时自动停止跟踪失败:', error);
        });
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
        title={`日志跟踪 (实例ID: ${logstashMachineId})`}
        open={visible}
        width="100%"
        style={{
          top: 0,
          maxWidth: '100vw',
          margin: 0,
        }}
        onCancel={handleModalCancel}
        // 点击遮罩层不会关闭弹窗
        maskClosable={false}
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
      >
        <div className={styles.statusContainer}>
          <h4>跟踪状态: {isTailing ? '运行中' : '已停止'}</h4>
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
