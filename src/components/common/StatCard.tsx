import { isValidElement } from 'react';
import { Statistic, Card } from 'antd';
import { useTheme } from '../../providers/ThemeProvider';
import { withPureComponent } from '../../utils/withPureComponent';
import './StatCard.less';

interface StatCardProps {
  title: string;
  value: number | string | React.ReactNode;
  prefix?: React.ReactNode;
  suffix?: React.ReactNode;
  icon?: React.ReactNode;
  loading?: boolean;
  trend?: 'up' | 'down' | 'stable';
  precision?: number;
  valueStyle?: React.CSSProperties;
  onClick?: () => void;
}

const StatCard: React.FC<StatCardProps> = ({
  title,
  value,
  prefix,
  suffix,
  icon,
  loading = false,
  trend,
  precision = 0,
  valueStyle,
  onClick,
}) => {
  const { isDarkMode } = useTheme();

  // 根据趋势设置图标颜色
  const getTrendColor = () => {
    if (trend === 'up') return '#52c41a';
    if (trend === 'down') return '#f5222d';
    return '#faad14';
  };

  // 处理值的渲染方式
  const renderValue = () => {
    // 如果值是 React 元素，直接渲染它
    if (isValidElement(value)) {
      return (
        <div
          style={{
            fontSize: '24px',
            fontWeight: 600,
            color: trend ? getTrendColor() : 'rgba(0, 0, 0, 0.85)',
            ...valueStyle,
          }}
        >
          {value}
        </div>
      );
    }

    // 否则使用 Statistic 组件
    return (
      <Statistic
        value={value as number | string}
        precision={precision}
        valueStyle={{
          fontSize: '24px',
          color: trend ? getTrendColor() : undefined,
          ...valueStyle,
        }}
        prefix={prefix}
        suffix={suffix}
      />
    );
  };

  return (
    <Card
      className={`stat-card ${isDarkMode ? 'dark' : ''}`}
      loading={loading}
      onClick={onClick}
      hoverable={!!onClick}
    >
      <div className="stat-card-content">
        {icon && <div className="stat-card-icon">{icon}</div>}
        <div className="stat-card-data">
          <div className="stat-card-title">{title}</div>
          {renderValue()}
          {trend && (
            <div className={`stat-card-trend stat-card-trend-${trend}`}>
              {trend === 'up' ? '↑' : trend === 'down' ? '↓' : '→'}
              <span className="trend-text">
                {trend === 'up' ? '上升' : trend === 'down' ? '下降' : '稳定'}
              </span>
            </div>
          )}
        </div>
      </div>
    </Card>
  );
};

// 使用纯组件包装器提高性能
export default withPureComponent(StatCard);
