import { Button, Checkbox, Form, Input, Alert, message, Typography } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { login } from '../store/userSlice';
import { login as apiLogin } from '../api/auth';
import './LoginPage.less';

const { Title, Text } = Typography;

const LoginPage = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [form] = Form.useForm();

  const onFinish = async (values: { username: string; password: string; remember: boolean }) => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await apiLogin({
        email: values.username,
        password: values.password,
      });
      console.log('Login response:', response);
      
      dispatch(login({
        userId: response.userId,
        name: response.nickname,
        role: response.role,
        tokens: {
          accessToken: response.token,
          refreshToken: response.refreshToken,
          expiresAt: response.expiresAt,
          refreshExpiresAt: response.refreshExpiresAt
        },
      }));
      
      message.success('登录成功！');
      navigate('/'); 
    } catch {
      setError('登录时发生错误，请稍后再试！');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-form-container fade-in">
        <div className="login-banner">
          <div className="login-banner-text">
            <Title level={2} style={{ color: '#fff', marginBottom: 16 }}>
              欢迎使用
            </Title>
            <Title level={1} style={{ color: '#fff', margin: 0 }}>
              日志查询平台
            </Title>
            <Text style={{ color: 'rgba(255, 255, 255, 0.85)', fontSize: 16, marginTop: 24, display: 'block' }}>
              智能大数据分析 · 数据可视化 · 商业智能
            </Text>
          </div>
        </div>
        
        <div className="login-form">
          <div className="login-logo">
            <img src="/logo.png" alt="Logo" />
            <Title level={3} className="brand-name">日志查询平台</Title>
          </div>
          
          <Title level={4} className="welcome-text">欢迎登录</Title>
          
          {error && (
            <Alert 
              message={error} 
              type="error" 
              showIcon 
              closable 
              style={{ marginBottom: 24 }} 
            />
          )}
          
          <Form
            form={form}
            name="login"
            initialValues={{ remember: true }}
            onFinish={onFinish}
            size="large"
            layout="vertical"
          >
            <Form.Item
              name="username"
              rules={[{ required: true, message: '请输入用户名!' }]}
            >
              <Input 
                prefix={<UserOutlined className="site-form-item-icon" />} 
                placeholder="用户名" 
                allowClear
              />
            </Form.Item>

            <Form.Item
              name="password"
              rules={[
                { required: true, message: '请输入密码!' },
                { min: 6, message: '密码长度不能少于6个字符' }
              ]}
            >
              <Input.Password 
                prefix={<LockOutlined className="site-form-item-icon" />} 
                placeholder="密码"
              />
            </Form.Item>

            <div className="login-form-options">
              <Form.Item name="remember" valuePropName="checked" noStyle>
                <Checkbox>记住我</Checkbox>
              </Form.Item>
              <a className="forgot-password" href="#/reset-password">忘记密码？</a>
            </div>

            <Form.Item>
              <Button
                type="primary" 
                htmlType="submit" 
                block 
                className="login-button" 
                loading={loading}
              >
                登录
              </Button>
            </Form.Item>
          </Form>
        </div>
      </div>
      
      <div className="login-footer">
        <Text type="secondary">© 2025 日志查询系统 - 版权所有</Text>
      </div>
    </div>
  );
};

export default LoginPage;
