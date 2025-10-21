import React from 'react';
import { Result, Button } from 'antd';
import { Link } from 'react-router-dom';

const ForbiddenPage: React.FC = () => (
  <Result
    extra={
      <Button type="primary">
        <Link to="/">返回首页</Link>
      </Button>
    }
    status="403"
    subTitle="抱歉，你没有权限访问此页面"
    title="403"
  />
);

export default ForbiddenPage;
