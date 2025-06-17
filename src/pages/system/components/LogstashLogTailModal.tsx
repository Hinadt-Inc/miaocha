import { Modal, Button, Space, message } from 'antd';
import { useEffect, useState, useRef } from 'react';
import { startLogTail, stopLogTail, getLogTailStatus } from '../../../api/logstash';

interface LogTailModalProps {
  visible: boolean;
  logstashMachineId: number;
  onCancel: () => void;
  style?: React.CSSProperties;
  bodyStyle?: React.CSSProperties;
}

export default function LogTailModal({ visible, logstashMachineId, onCancel, style, bodyStyle }: LogTailModalProps) {
  const [logs, setLogs] = useState<string[]>([]);
  const [isTailing, setIsTailing] = useState(false);
  const [lastLogId, setLastLogId] = useState<string | undefined>();
  const [status, setStatus] = useState<any>(null);
  const logsEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    logsEndRef.current?.scrollIntoView({ behavior: 'smooth' });
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
      console.log('开始调用startLogTail');
      const status = await getLogTailStatus();
      console.log('日志跟踪状态:', status);
      setStatus(status);

      console.log('准备调用startLogTail...');
      // 调用异步startLogTail函数并等待返回EventSource对象
      const eventSource = await startLogTail(logstashMachineId);
      console.log('startLogTail返回:', eventSource);

      if (!eventSource || !(eventSource instanceof EventSource)) {
        throw new Error('无效的EventSource对象');
      }

      console.log('日志跟踪已启动', eventSource);
      eventSource.onmessage = (event) => {
        console.log('收到日志消息:', event.data);
        const newLog = event.data;
        setLogs((prev) => [...prev, newLog]);
        scrollToBottom();
      };

      eventSource.onerror = (err) => {
        console.error('EventSource错误:', err);
        message.error('日志跟踪连接出错');
        setIsTailing(false);
        eventSource.close();
      };

      // 存储EventSource实例以便之后关闭
      (window as any).currentEventSource = eventSource;

      setIsTailing(true);
      setLogs([]);
      setLastLogId(undefined);
      message.success('日志跟踪已启动');
    } catch (error: unknown) {
      console.error('启动日志跟踪失败:', error);
      const errorMessage = error instanceof Error ? error.message : '未知错误';
      message.error(`启动日志跟踪失败: ${errorMessage}`);
      setIsTailing(false);
    }
  };

  const stopTail = async () => {
    try {
      // 关闭EventSource连接
      if ((window as any).currentEventSource) {
        (window as any).currentEventSource.close();
        (window as any).currentEventSource = null;
      }

      await stopLogTail(logstashMachineId);
      setIsTailing(false);
      message.success('日志跟踪已停止');
    } catch (error) {
      message.error('停止日志跟踪失败');
    }
  };

  // 组件卸载时确保关闭连接
  useEffect(() => {
    return () => {
      if ((window as any).currentEventSource) {
        (window as any).currentEventSource.close();
        (window as any).currentEventSource = null;
      }
    };
  }, []);

  // 监视visible属性，当弹窗关闭时自动停止跟踪
  useEffect(() => {
    // 当弹窗变为不可见且仍在跟踪时，停止跟踪
    if (!visible && isTailing) {
      stopTail().catch((error) => {
        console.error('自动停止跟踪失败:', error);
      });
    }
  }, [visible, isTailing]);

  useEffect(() => {
    const statusInterval = setInterval(async () => {
      if (visible && isTailing) {
        try {
          const status = await getLogTailStatus();
          setStatus(status);
        } catch (error) {
          console.error('获取状态失败', error);
        }
      }
    }, 3000);

    return () => {
      clearInterval(statusInterval);
    };
  }, [visible, isTailing]);

  return (
    <Modal
      title={`日志跟踪 (机器ID: ${logstashMachineId})`}
      open={visible}
      width={800}
      onCancel={handleModalCancel}
      footer={[
        <Button key="stop" danger onClick={stopTail}>
          停止跟踪
        </Button>,
        <Button key="start" type="primary" disabled={isTailing} onClick={startTail}>
          开始跟踪
        </Button>,
      ]}
    >
      <div style={{ marginBottom: 16 }}>
        <h4>跟踪状态: {isTailing ? '运行中' : '已停止'}</h4>
        {status && <pre>{JSON.stringify(status, null, 2)}</pre>}
      </div>
      <div
        style={{
          height: 500,
          overflow: 'auto',
          background: '#000',
          color: '#fff',
          padding: 8,
          fontFamily: 'monospace',
        }}
      >
        {logs.length > 0 ? (
          <>
            {logs.map((log, i) => (
              <div key={i} style={{ whiteSpace: 'pre-wrap' }}>
                {log}
              </div>
            ))}
            <div ref={logsEndRef} />
          </>
        ) : (
          <div>暂无日志数据</div>
        )}
      </div>
    </Modal>
  );
}
