import { Button, Checkbox, Form, Input, Alert, message, Typography, Divider, Space } from 'antd';
import { UserOutlined, LockOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { login } from '../store/userSlice';
import './LoginPage.less';
import reactLogo from '../assets/react.svg';

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
      // 这里模拟登录请求
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      // 简单的用户名和密码验证，实际项目中应该调用API进行验证
      if (values.username === 'admin' && values.password === 'password') {
        dispatch(login(values.username));
        
        message.success('登录成功！');
        navigate('/dashboard'); // 登录成功后跳转到仪表盘页面
      } else {
        setError('用户名或密码错误，请重试！');
      }
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
              云 BI 平台
            </Title>
            <Text style={{ color: 'rgba(255, 255, 255, 0.85)', fontSize: 16, marginTop: 24, display: 'block' }}>
              智能大数据分析 · 数据可视化 · 商业智能
            </Text>
          </div>
        </div>
        
        <div className="login-form">
          <div className="login-logo">
            <img src={reactLogo} alt="Logo" />
            <Title level={3} className="brand-name">Hina Cloud BI</Title>
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

            <Form.Item className="verification-code">
              <Space style={{ width: '100%', justifyContent: 'space-between' }}>
                <Input 
                  prefix={<SafetyCertificateOutlined className="site-form-item-icon" />} 
                  placeholder="验证码" 
                  style={{ width: '60%' }} 
                />
                <Button style={{ width: '35%' }} type="default">
                  获取验证码
                </Button>
              </Space>
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
            
            <Divider plain>其他登录方式</Divider>
            
            <div className="other-login-methods">
              <Button type="link">企业微信</Button>
              <Button type="link">钉钉</Button>
              <Button type="link">飞书</Button>
            </div>
          </Form>
        </div>
      </div>
      
      <div className="login-footer">
        <Text type="secondary">© 2025 Hina Cloud BI 平台 - 版权所有</Text>
      </div>
    </div>
  );
};

export default LoginPage;
