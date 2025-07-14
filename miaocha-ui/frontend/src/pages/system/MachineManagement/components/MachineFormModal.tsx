import { Modal, Form, Input, InputNumber, Button, Row, Col } from 'antd';
import type { CreateMachineParams } from '@/types/machineTypes';
import styles from '../MachineManagement.module.less';

interface MachineFormModalProps {
  title: string;
  open: boolean;
  form: any;
  loading: boolean;
  testingConnection: boolean;
  onCancel: () => void;
  onFinish: (values: CreateMachineParams) => void;
  onTestConnection: () => void;
}

const MachineFormModal: React.FC<MachineFormModalProps> = ({
  title,
  open,
  form,
  loading,
  testingConnection,
  onCancel,
  onFinish,
  onTestConnection,
}) => {
  return (
    <Modal
      title={title}
      open={open}
      onCancel={onCancel}
      footer={[
        <Button key="test" loading={testingConnection} onClick={onTestConnection}>
          测试连接
        </Button>,
        <Button key="submit" type="primary" loading={loading} onClick={() => form.submit()}>
          确定
        </Button>,
      ]}
      maskClosable={false}
    >
      <Form form={form} layout="vertical" onFinish={onFinish}>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              name="name"
              label="名称"
              rules={[
                { required: true, message: '请输入机器名称' },
                { max: 128, message: '机器名称不能超过128个字符' },
              ]}
            >
              <Input placeholder="测试服务器" maxLength={128} />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              name="ip"
              label="IP地址"
              rules={[
                { required: true, message: '请输入IP地址' },
                { max: 128, message: 'IP地址不能超过128个字符' },
              ]}
            >
              <Input placeholder="IP地址" maxLength={128} />
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="port" label="端口" rules={[{ required: true, message: '请输入端口号' }]}>
              <InputNumber min={1} max={65535} className={styles.fullWidth} />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              name="username"
              label="用户名"
              rules={[
                { required: true, message: '请输入用户名' },
                { max: 128, message: '用户名不能超过128个字符' },
              ]}
            >
              <Input placeholder="root" maxLength={128} />
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col span={24}>
            <Form.Item name="password" label="密码" rules={[{ max: 128, message: '密码不能超过128个字符' }]}>
              <Input.Password placeholder="可选" maxLength={128} />
            </Form.Item>
          </Col>
          <Col span={24}>
            <Form.Item name="sshKey" label="SSH密钥">
              <Input.TextArea placeholder="可选" rows={4} showCount />
            </Form.Item>
          </Col>
        </Row>
      </Form>
    </Modal>
  );
};

export default MachineFormModal;
