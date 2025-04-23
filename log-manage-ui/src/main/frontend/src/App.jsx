import React from 'react';
import { Layout, Typography, Card } from 'antd';
import './App.css';

const { Header, Content, Footer } = Layout;
const { Title } = Typography;

function App() {
    return (
        <Layout className="layout">
            <Header className="header">
                <Title level={3} style={{ color: 'white', margin: '10px 0' }}>
                    日志管理系统
                </Title>
            </Header>
            <Content className="content">
                <Card title="欢迎" style={{ width: '100%', maxWidth: 800, margin: '0 auto' }}>
                    <Typography.Title level={2} style={{ textAlign: 'center' }}>
                        Hello World!
                    </Typography.Title>
                    <Typography.Paragraph style={{ textAlign: 'center' }}>
                        欢迎使用日志管理系统前端界面
                    </Typography.Paragraph>
                </Card>
            </Content>
            <Footer style={{ textAlign: 'center' }}>
                Log Management System ©{new Date().getFullYear()} Created by Hina
            </Footer>
        </Layout>
    );
}

export default App; 