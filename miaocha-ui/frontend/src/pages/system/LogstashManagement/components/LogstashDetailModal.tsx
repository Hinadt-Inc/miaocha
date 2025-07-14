import { Modal, Descriptions, Button, Tooltip, Table, Tag } from 'antd';
import { CopyOutlined } from '@ant-design/icons';
import type { LogstashProcess } from '@/types/logstashTypes';
import { safeCopy } from '@/utils/clipboard';
import { useErrorContext } from '@/providers/ErrorProvider';
import dayjs from 'dayjs';

interface LogstashDetailModalProps {
  visible: boolean;
  onClose: () => void;
  detail: LogstashProcess | null;
  styles?: {
    configSection?: string;
    configHeader?: string;
    configContent?: string;
    machineStatusSection?: string;
  };
}

const LogstashDetailModal = ({ visible, onClose, detail, styles: customStyles }: LogstashDetailModalProps) => {
  const { showSuccess } = useErrorContext();

  const handleCopy = async (text: string, label: string) => {
    const success = await safeCopy(text);
    if (success) {
      showSuccess(`${label}已复制到剪贴板`);
    }
    // 错误提示不处理，因为已有全局处理
  };

  if (!detail) return null;

  const machineColumns = [
    {
      title: '机器名称',
      dataIndex: 'machineName',
      fixed: 'left' as const,
      width: 150,
    },
    {
      title: 'IP',
      dataIndex: 'machineIp',
      width: 120,
    },
    {
      title: '状态',
      dataIndex: 'state',
      width: 120,
      render: (state: string) => {
        let color = 'orange';
        if (state === 'RUNNING') color = 'green';
        else if (state === 'STOPPED') color = 'red';

        return <Tag color={color}>{state}</Tag>;
      },
    },
    {
      title: '状态描述',
      dataIndex: 'stateDescription',
      width: 200,
      ellipsis: true,
    },
  ];

  return (
    <Modal
      title={`Logstash进程详情 - ${detail.name || ''}`}
      open={visible}
      onCancel={onClose}
      footer={null}
      width={1000}
    >
      <div>
        <Descriptions bordered column={2} size="small">
          <Descriptions.Item label="ID" span={1}>
            {detail.id}
          </Descriptions.Item>
          <Descriptions.Item label="模块" span={1}>
            {detail.moduleName}
          </Descriptions.Item>
          <Descriptions.Item label="数据源" span={1}>
            {detail.datasourceName || '未设置'}
          </Descriptions.Item>
          <Descriptions.Item label="表名" span={1}>
            {detail.tableName || '未设置'}
          </Descriptions.Item>
          <Descriptions.Item label="创建时间" span={1}>
            {detail.createTime ? dayjs(detail.createTime).format('YYYY-MM-DD HH:mm:ss') : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="更新时间" span={1}>
            {detail.updateTime ? dayjs(detail.updateTime).format('YYYY-MM-DD HH:mm:ss') : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="创建人" span={1}>
            {detail.createUserName || '未知'}
          </Descriptions.Item>
          <Descriptions.Item label="更新人" span={1}>
            {detail.updateUserName || '未知'}
          </Descriptions.Item>
          <Descriptions.Item label="描述" span={2}>
            {detail.description || '无描述'}
          </Descriptions.Item>
          <Descriptions.Item label="自定义包路径" span={2}>
            {detail.customPackagePath || '未设置'}
          </Descriptions.Item>
        </Descriptions>
        <div className={customStyles?.configSection || 'config-section'}>
          <div className={customStyles?.configHeader || 'config-header'}>
            <h4>配置内容</h4>
            <Tooltip title="复制">
              <Button
                type="text"
                icon={<CopyOutlined />}
                onClick={() => handleCopy(detail.configContent || '', '配置内容')}
              />
            </Tooltip>
          </div>
          <pre className={customStyles?.configContent || 'config-content'}>{detail.configContent}</pre>
        </div>

        <div className={customStyles?.configSection || 'config-section'}>
          <div className={customStyles?.configHeader || 'config-header'}>
            <h4>Logstash配置</h4>
            <Tooltip title="复制">
              <Button
                type="text"
                icon={<CopyOutlined />}
                onClick={() => handleCopy(detail.logstashYml || '', 'Logstash配置')}
              />
            </Tooltip>
          </div>
          <pre className={customStyles?.configContent || 'config-content'}>{detail.logstashYml}</pre>
        </div>

        <div className={customStyles?.configSection || 'config-section'}>
          <div className={customStyles?.configHeader || 'config-header'}>
            <h4>JVM参数</h4>
            <Tooltip title="复制">
              <Button
                type="text"
                icon={<CopyOutlined />}
                onClick={() => handleCopy(detail.jvmOptions || '', 'JVM参数')}
              />
            </Tooltip>
          </div>
          <pre className={customStyles?.configContent || 'config-content'}>{detail.jvmOptions}</pre>
        </div>

        <div className={customStyles?.machineStatusSection || 'machine-status-section'}>
          <h4>机器状态</h4>
          <Table
            size="small"
            bordered
            dataSource={detail.logstashMachineStatusInfo}
            rowKey="machineId"
            pagination={false}
            scroll={{ x: true }}
            columns={machineColumns}
          />
        </div>
      </div>
    </Modal>
  );
};

export default LogstashDetailModal;
