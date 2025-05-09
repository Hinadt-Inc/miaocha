import React from 'react';
import { Spin } from 'antd';
import './loading.less';

interface LoadingProps {
  fullScreen?: boolean;
  tip?: string;
  size?: 'small' | 'default' | 'large';
  delay?: number;
  className?: string;
  style?: React.CSSProperties;
}

// 创建统一的加载组件
const Loading: React.FC<LoadingProps> = ({
  fullScreen = false,
  tip = '',
  size = 'large',
  delay = 200,
  className = '',
  style = {},
}) => {
  return (
    <div
      className={`loading-container ${fullScreen ? 'fullscreen' : ''} ${className}`}
      style={style}
    >
      <Spin size={size} tip={tip} delay={delay} />
    </div>
  );
};

export default Loading;
