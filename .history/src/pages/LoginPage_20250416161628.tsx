import { Button, Checkbox, Form, Input } from 'antd';
import React from 'react';
import './LoginPage.css';

const LoginPage: React.FC = () => {
  const onFinish = (values: { username: string; password: string }) => {
    console.log('Success:', values);
  };

  const onFinishFailed = (errorInfo: { values: object; errorFields: Array<{ name: string[]; errors: string[] }> }) => {
    console.log('Failed:', errorInfo);
  };

  return (
    <div className="login-container">
      <h1 style={{ textAlign: 'center', marginBottom: 24 }}>登录</h1>
      <Form
        name="basic"
        initialValues={{ remember: true }}
        onFinish={onFinish}
        onFinishFailed={onFinishFailed}
        autoComplete="off"
        layout="vertical"
      >
        <Form.Item
          label="用户名"
          name="username"
          rules={[{ required: true, message: '请输入用户名!' }]}
        >
          <Input />
        </Form.Item>

        <Form.Item
          label="密码"
          name="password"
          rules={[{ required: true, message: '请输入密码!' }]}
        >
          <Input.Password />
        </Form.Item>

        <div className="google-verify">
          <Checkbox>我不是机器人</Checkbox>
        </div>

        <Form.Item>
          <Button type="primary" htmlType="submit" block>
            登录
          </Button>
        </Form.Item>
      </Form>
    </div>
  );
};

export default LoginPage;
