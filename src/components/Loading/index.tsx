import React from 'react';
import { Spin } from 'antd';

  // 创建统一的加载组件
const Loading: React.FC = () => {
  return (
    <div style={{ 
      display: 'flex', 
      justifyContent: 'center', 
      alignItems: 'center', 
      height: '100%',
      width: '100%',
      position: 'absolute',
      top: 20,
      left: 0
    }}>
      <Spin size="large" />
    </div>
  );
};

export default Loading;
