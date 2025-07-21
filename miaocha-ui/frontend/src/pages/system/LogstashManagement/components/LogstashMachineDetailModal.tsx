import { Modal, Descriptions, Tag, Divider, Button, message, Tooltip } from 'antd';
import { CopyOutlined } from '@ant-design/icons';
import type { LogstashProcess } from '@/types/logstashTypes';
import { safeCopy } from '@/utils/clipboard';
import dayjs from 'dayjs';

interface LogstashMachineDetailModalProps {
  visible: boolean;
  onCancel: () => void;
  detail?: LogstashProcess;
}

export default function LogstashMachineDetailModal({ visible, onCancel, detail }: LogstashMachineDetailModalProps) {
  const [messageApi, contextHolder] = message.useMessage();
  if (!detail) return null;
  const handleCopy = async (text: string | undefined) => {
    if (!text) {
      messageApi.warning('没有内容可复制');
      return;
    }
    const success = await safeCopy(text);
    if (success) {
      messageApi.success('内容已复制到剪贴板');
    } else {
      messageApi.error('复制失败，请手动复制');
    }
  };

  return (
    <>
      {contextHolder}
      <Modal
        title={`机器详情 - ${detail.machineName || detail.machineId}`}
        open={visible}
        onCancel={onCancel}
        width={1000}
      >
        <Descriptions bordered column={2}>
          <Descriptions.Item label="实例ID">{detail.id}</Descriptions.Item>
          <Descriptions.Item label="模块名称">{detail.moduleName || '未知'}</Descriptions.Item>
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
          <Descriptions.Item label="创建时间">
            {dayjs(detail.createTime).format('YYYY-MM-DD HH:mm:ss')}
          </Descriptions.Item>
          <Descriptions.Item label="更新时间">
            {dayjs(detail.updateTime).format('YYYY-MM-DD HH:mm:ss')}
          </Descriptions.Item>
        </Descriptions>
        <Divider orientation="left">配置信息</Divider>
        <div style={{ marginBottom: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <h4>配置内容</h4>
            <Tooltip title="复制">
              <Button icon={<CopyOutlined />} onClick={() => handleCopy(detail.configContent)} size="small" />
            </Tooltip>
          </div>
          <pre style={{ background: '#f5f5f5', padding: 12, borderRadius: 4, overflow: 'auto', height: 200 }}>
            {detail.configContent}
          </pre>
        </div>

        <div style={{ marginBottom: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <h4>JVM参数</h4>
            <Tooltip title="复制">
              <Button icon={<CopyOutlined />} onClick={() => handleCopy(detail.jvmOptions || '')} size="small" />
            </Tooltip>
          </div>
          <pre style={{ background: '#f5f5f5', padding: 12, borderRadius: 4, overflow: 'auto', height: 200 }}>
            {detail.jvmOptions}
          </pre>
        </div>

        <div style={{ marginBottom: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <h4>Logstash配置</h4>
            <Tooltip title="复制">
              <Button icon={<CopyOutlined />} onClick={() => handleCopy(detail.logstashYml)} size="small" />
            </Tooltip>
          </div>
          <pre style={{ background: '#f5f5f5', padding: 12, borderRadius: 4, overflow: 'auto', height: 200 }}>
            {detail.logstashYml}
          </pre>
        </div>
      </Modal>
    </>
  );
}
