import { Modal, Descriptions, Tag, Divider, Button, message } from 'antd';
import { CopyOutlined } from '@ant-design/icons';
import type { LogstashProcess } from '../../../types/logstashTypes';
import { startLogstashMachine, stopLogstashMachine, refreshLogstashMachineConfig } from '../../../api/logstash';

interface LogstashMachineDetailModalProps {
  visible: boolean;
  onCancel: () => void;
  detail?: LogstashProcess;
}

export default function LogstashMachineDetailModal({ visible, onCancel, detail }: LogstashMachineDetailModalProps) {
  if (!detail) return null;

  const handleRefresh = async () => {
    try {
      await refreshLogstashMachineConfig(detail.id, detail.machineId);
      message.success('配置刷新成功');
    } catch (error) {
      message.error('配置刷新失败');
    }
  };

  const handleStart = async () => {
    try {
      await startLogstashMachine(detail.id, detail.machineId);
      message.success('机器启动成功');
    } catch (error) {
      message.error('机器启动失败');
    }
  };

  const handleStop = async () => {
    try {
      await stopLogstashMachine(detail.id, detail.machineId);
      message.success('机器停止成功');
    } catch (error) {
      message.error('机器停止失败');
    }
  };

  const handleCopy = (text: string | undefined) => {
    if (!text) {
      message.warning('没有内容可复制');
      return;
    }
    try {
      navigator.clipboard.writeText(text);
      message.success('复制成功');
    } catch (error) {
      message.error('复制失败');
    }
  };

  return (
    <Modal
      title={`机器详情 - ${detail.machineName || detail.machineId}`}
      open={visible}
      onCancel={onCancel}
      footer={[
        <Button key="refresh" onClick={handleRefresh}>
          刷新
        </Button>,
        <Button key="start" type="primary" onClick={handleStart} disabled={detail.state === 'RUNNING'}>
          启动
        </Button>,
        <Button key="stop" danger onClick={handleStop} disabled={detail.state !== 'RUNNING'}>
          停止
        </Button>,
      ]}
      width={1000}
    >
      <Descriptions bordered column={2}>
        <Descriptions.Item label="机器ID">{detail.machineId}</Descriptions.Item>
        <Descriptions.Item label="机器名称">{detail.machineName}</Descriptions.Item>
        <Descriptions.Item label="IP地址">{detail.machineIp}</Descriptions.Item>
        <Descriptions.Item label="端口">{detail.machinePort}</Descriptions.Item>
        <Descriptions.Item label="用户名">{detail.machineUsername}</Descriptions.Item>
        <Descriptions.Item label="状态">
          <Tag color={detail.state === 'RUNNING' ? 'green' : detail.state === 'STOPPED' ? 'red' : 'orange'}>
            {detail.state}
          </Tag>
        </Descriptions.Item>
        <Descriptions.Item label="状态描述" span={2}>
          {detail.stateDescription || '无'}
        </Descriptions.Item>
        <Descriptions.Item label="进程PID">{detail.processPid || '无'}</Descriptions.Item>
        <Descriptions.Item label="部署路径">{detail.deployPath}</Descriptions.Item>
        <Descriptions.Item label="创建时间">{detail.createTime}</Descriptions.Item>
        <Descriptions.Item label="更新时间">{detail.updateTime}</Descriptions.Item>
      </Descriptions>

      <Divider orientation="left">配置信息</Divider>
      <div style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <h4>JVM参数</h4>
          <Button icon={<CopyOutlined />} onClick={() => handleCopy(detail.jvmOptions || '')} size="small" />
        </div>
        <pre style={{ background: '#f5f5f5', padding: 12, borderRadius: 4, overflow: 'auto' }}>{detail.jvmOptions}</pre>
      </div>

      <div style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <h4>Logstash配置</h4>
          <Button icon={<CopyOutlined />} onClick={() => handleCopy(detail.logstashYml || '')} size="small" />
        </div>
        <pre style={{ background: '#f5f5f5', padding: 12, borderRadius: 4, overflow: 'auto' }}>
          {detail.logstashYml}
        </pre>
      </div>

      <div style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <h4>配置内容</h4>
          <Button icon={<CopyOutlined />} onClick={() => handleCopy(detail.configContent)} size="small" />
        </div>
        <pre style={{ background: '#f5f5f5', padding: 12, borderRadius: 4, overflow: 'auto' }}>
          {detail.configContent}
        </pre>
      </div>
    </Modal>
  );
}
