import { useState, useEffect } from 'react';

import './loading.less';

interface LoadingProps {
  fullScreen?: boolean;
  tip?: string;
  size?: 'small' | 'default' | 'large';
  delay?: number;
  className?: string;
  style?: React.CSSProperties;
  type?: 'spinner' | 'dots' | 'pulse' | 'bars';
}

// 旋转加载器
const SpinnerLoader: React.FC<{ size: string }> = ({ size }) => (
  <div className={`spinner-loader ${size}`}>
    <div className="spinner-ring"></div>
  </div>
);

// 跳动的点 - 优化为5个点的横向排列
const DotsLoader: React.FC<{ size: string }> = ({ size }) => (
  <div className={`dots-loader ${size}`}>
    <div className="dot"></div>
    <div className="dot"></div>
    <div className="dot"></div>
  </div>
);

// 脉冲加载器
const PulseLoader: React.FC<{ size: string }> = ({ size }) => (
  <div className={`pulse-loader ${size}`}>
    <div className="pulse-ring"></div>
    <div className="pulse-ring"></div>
    <div className="pulse-ring"></div>
  </div>
);

// 条形加载器
const BarsLoader: React.FC<{ size: string }> = ({ size }) => (
  <div className={`bars-loader ${size}`}>
    <div className="bar"></div>
    <div className="bar"></div>
    <div className="bar"></div>
    <div className="bar"></div>
    <div className="bar"></div>
  </div>
);

// 创建统一的加载组件
const Loading: React.FC<LoadingProps> = ({
  fullScreen = false,
  tip = '',
  size = 'small',
  delay = 200,
  className = '',
  style = {},
  type = 'dots', // 改为默认使用dots效果
}) => {
  const [visible, setVisible] = useState(delay === 0);

  useEffect(() => {
    let timer: NodeJS.Timeout;
    if (delay > 0) {
      timer = setTimeout(() => {
        setVisible(true);
      }, delay);
    }
    return () => {
      if (timer) {
        clearTimeout(timer);
      }
    };
  }, [delay]);

  if (!visible) return null;

  const renderLoader = () => {
    switch (type) {
      case 'spinner':
        return <SpinnerLoader size={size} />;
      case 'pulse':
        return <PulseLoader size={size} />;
      case 'bars':
        return <BarsLoader size={size} />;
      default:
        return <DotsLoader size={size} />;
    }
  };

  return (
    <div className={`loading-container ${fullScreen ? 'fullscreen' : ''} ${className}`} style={style}>
      <div className="loading-content">
        {renderLoader()}
        {tip && <div className="loading-text">{tip}</div>}
      </div>
    </div>
  );
};

export default Loading;
