import { Modal, Button, message } from 'antd';
import { useEffect, useState, useRef } from 'react';
import { startLogTail, stopLogTail, createLogTailTask } from '../../../api/logstash';

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
  const [status, setStatus] = useState<any>(null);
  const logsEndRef = useRef<HTMLDivElement>(null);
  const [shouldAutoScroll, setShouldAutoScroll] = useState(true);
  const [messageApi, contextHolder] = message.useMessage();

  const scrollToBottom = () => {
    if (shouldAutoScroll) {
      logsEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  };

  const handleMouseEnter = () => setShouldAutoScroll(false);
  const handleMouseLeave = () => setShouldAutoScroll(true);

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

      console.log('准备调用startLogTail...');
      // 调用异步startLogTail函数并等待返回EventSource对象
      const eventSource = startLogTail(logstashMachineId);
      console.log('startLogTail返回:', eventSource);
      if (!eventSource) {
        throw new Error('无法创建EventSource连接');
      }
      // 监听EventSource的消息事件
      (
        await // 监听EventSource的消息事件
        eventSource
      ).onopen = () => {
        console.log('日志跟踪连接已打开');
        messageApi.success('日志跟踪连接已打开');
      };

      (await eventSource).addEventListener('log-data', (event) => {
        const data = JSON.parse(event.data);
        console.log('收到日志:', data.logLines);
        // 只提取logLines数组内容
        setLogs((prev) => [...prev, ...data.logLines]);
        scrollToBottom();
      });

      (await eventSource).addEventListener('heartbeat', (event) => {
        console.log('收到心跳');
        const data = JSON.parse(event.data);
        setStatus(data.status);
      });
      console.log('日志跟踪已启动', eventSource);
      (await eventSource).onmessage = (event) => {
        console.log('收到日志消息:', event.data);
        const newLog = event.data;
        setLogs((prev) => [...prev, newLog, `---------------分隔线---------------`]);
        scrollToBottom();
      };

      (await eventSource).onerror = async (err) => {
        console.error('EventSource错误:', err);
        messageApi.error('日志跟踪连接出错');
        setIsTailing(false);
        (await eventSource).close();
      };

      // 存储EventSource实例以便之后关闭
      (window as any).currentEventSource = eventSource;

      setIsTailing(true);
      setLogs([]);
      messageApi.success('日志跟踪已启动');
    } catch (error: unknown) {
      console.error('启动日志跟踪失败:', error);
      const errorMessage = error instanceof Error ? error.message : '未知错误';
      messageApi.error(`启动日志跟踪失败: ${errorMessage}`);
      setIsTailing(false);
    }
  };

  const stopTail = async () => {
    try {
      await stopLogTail(logstashMachineId);
      setIsTailing(false);
      messageApi.success('日志跟踪已停止');
    } catch (error) {
      messageApi.error('停止日志跟踪失败');
    }
  };

  // 组件卸载时确保关闭连接
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

  // 监视visible属性，当弹窗关闭时自动停止跟踪
  useEffect(() => {
    // 当弹窗变为不可见且仍在跟踪时，停止跟踪
    if (!visible && isTailing) {
      stopTail().catch((error) => {
        console.error('自动停止跟踪失败:', error);
      });
    }
  }, [visible, isTailing]);

  return (
    <>
      {contextHolder}
      <Modal
        title={`日志跟踪 (机器ID: ${logstashMachineId})`}
        open={visible}
        width={800}
        onCancel={handleModalCancel}
        // 点击遮罩层不会关闭弹窗
        maskClosable={false}
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
            fontSize: 12,
          }}
          onMouseEnter={handleMouseEnter}
          onMouseLeave={handleMouseLeave}
        >
          {logs.length > 0 ? (
            <>
              {logs.map((log, i) => (
                <div key={i} style={{ whiteSpace: 'pre-wrap' }}>
                  {typeof log === 'object' ? JSON.stringify(log) : log}
                </div>
              ))}
              <div ref={logsEndRef} />
            </>
          ) : (
            <div>暂无日志数据</div>
          )}
        </div>
      </Modal>
    </>
  );
}
