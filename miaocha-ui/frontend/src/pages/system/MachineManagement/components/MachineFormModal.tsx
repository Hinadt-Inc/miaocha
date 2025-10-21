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
      footer={[
        <Button key="test" loading={testingConnection} onClick={onTestConnection}>
          测试连接
        </Button>,
        <Button key="submit" loading={loading} type="primary" onClick={() => form.submit()}>
          确定
        </Button>,
      ]}
      maskClosable={false}
      open={open}
      title={title}
      onCancel={onCancel}
    >
      <Form form={form} layout="vertical" onFinish={onFinish}>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              label="名称"
              name="name"
              rules={[
                { required: true, message: '请输入机器名称' },
                { max: 128, message: '机器名称不能超过128个字符' },
              ]}
            >
              <Input maxLength={128} placeholder="测试服务器" />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              label="IP地址"
              name="ip"
              rules={[
                { required: true, message: '请输入IP地址' },
                { max: 128, message: 'IP地址不能超过128个字符' },
              ]}
            >
              <Input maxLength={128} placeholder="IP地址" />
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item label="端口" name="port" rules={[{ required: true, message: '请输入端口号' }]}>
              <InputNumber className={styles.fullWidth} max={65535} min={1} />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              label="用户名"
              name="username"
              rules={[
                { required: true, message: '请输入用户名' },
                { max: 128, message: '用户名不能超过128个字符' },
              ]}
            >
              <Input maxLength={128} placeholder="root" />
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col span={24}>
            <Form.Item label="密码" name="password" rules={[{ max: 128, message: '密码不能超过128个字符' }]}>
              <Input.Password maxLength={128} placeholder="可选" />
            </Form.Item>
          </Col>
          <Col span={24}>
            <Form.Item label="SSH密钥" name="sshKey">
              <Input.TextArea placeholder="可选" rows={4} showCount />
            </Form.Item>
          </Col>
        </Row>
      </Form>
    </Modal>
  );
};

export default MachineFormModal;
