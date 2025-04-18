import { Button, Checkbox, Form, Input, Alert, message } from 'antd';
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { login } from '../store/userSlice';
import './LoginPage.less';
import reactLogo from '../assets/react.svg';

const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

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

  const onFinishFailed = (errorInfo: { 
    values: Record<string, unknown>;
    errorFields: Array<{ 
      name: string[]; 
      errors: string[] 
    }> 
  }) => {
    console.log('Failed:', errorInfo);
  };
  
  const dispatch = useDispatch();
  const savedUsername = '';

  return (
    <div className="login-page">
      <div className="login-container fade-in">
        <div className="login-logo">
          <img src={reactLogo} alt="Logo" />
          <h1 className="login-title">Hina Cloud BI</h1>
        </div>
        
        {error && <Alert message={error} type="error" showIcon closable />}
        
        <Form
          name="basic"
          initialValues={{ 
            remember: true,
            username: savedUsername
          }}
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
            <Input size="large" placeholder="请输入用户名" />
          </Form.Item>

          <Form.Item
            label="密码"
            name="password"
            rules={[
              { required: true, message: '请输入密码!' },
              { min: 6, message: '密码长度不能少于6个字符' }
            ]}
          >
            <Input.Password size="large" placeholder="请输入密码" />
          </Form.Item>

          <div className="remember-forgot">
            <Form.Item name="remember" valuePropName="checked" noStyle>
              <Checkbox>记住我</Checkbox>
            </Form.Item>
            <a className="forgot-password" href="#/reset-password">忘记密码？</a>
          </div>

          <div className="google-verify">
            <Checkbox>我不是机器人</Checkbox>
          </div>

          <Form.Item className="login-button">
            <Button type="primary" htmlType="submit" block size="large" loading={loading}>
              登录
            </Button>
          </Form.Item>
        </Form>
      </div>
    </div>
  );
};

export default LoginPage;
