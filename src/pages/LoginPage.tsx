import { Button, Checkbox, Form, Input, Alert, message, Typography } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { useState, useEffect } from 'react';
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

  // 创建浮动粒子效果
  useEffect(() => {
    // 创建随机粒子
    const createParticles = () => {
      const particleContainer = document.querySelector('.login-page');
      if (!particleContainer) return;
      
      // 清除之前的粒子
      const existingParticles = document.querySelectorAll('.particle');
      existingParticles.forEach(p => p.remove());
      
      // 创建新粒子
      for (let i = 0; i < 25; i++) {
        const particle = document.createElement('div');
        particle.className = 'particle';
        
        // 随机位置
        particle.style.left = `${Math.random() * 100}%`;
        particle.style.top = `${Math.random() * 100}%`;
        
        // 随机大小 (已在CSS中设置基础大小和变化)
        
        // 随机延迟
        particle.style.animationDelay = `${Math.random() * 8}s`;
        
        particleContainer.appendChild(particle);
      }
    };
    
    createParticles();
    
    // 组件卸载时移除粒子
    return () => {
      const particles = document.querySelectorAll('.particle');
      particles.forEach(p => p.remove());
    };
  }, []);

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
      {/* 背景元素 - 粒子将由useEffect动态创建 */}
      
      <div className="login-form-container fade-in">
        <div className="login-banner">
          <div className="login-banner-text">
            <Title level={2} style={{ color: '#fff', marginBottom: 16, textShadow: '0 2px 4px rgba(0,0,0,0.1)' }}>
              欢迎使用
            </Title>
            <Title level={1} style={{ color: '#fff', margin: 0, textShadow: '0 2px 6px rgba(0,0,0,0.15)' }}>
              日志查询平台
            </Title>
            <Text style={{ color: 'rgba(255, 255, 255, 0.9)', fontSize: 16, marginTop: 24, display: 'block' }}>
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
