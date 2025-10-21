import React from 'react';
import { Result, Button } from 'antd';
import { Link } from "react-router-dom";

const NotFoundPage: React.FC = () => (
  <Result
    extra={
      <Button type="primary">
        <Link to="/">返回首页</Link>
      </Button>
    }
    status="404"
    subTitle="抱歉，您访问的页面不存在"
    title="404"
  />
);

export default NotFoundPage;
