import React, { useMemo } from 'react';
import { Button, Dropdown, Space, Progress, Tooltip } from 'antd';
import { PlayCircleOutlined, PauseCircleOutlined, ReloadOutlined, ClockCircleOutlined, CaretDownOutlined } from '@ant-design/icons';
import { AutoRefreshProps } from './types';
import { REFRESH_INTERVALS } from './constants';
import { useAutoRefresh } from './useAutoRefresh';
import { formatRemainingTime, calculateProgressPercent, generateTooltipContent } from './utils';
import styles from './index.module.less';

/**
 * 自动刷新组件
 */
const AutoRefresh: React.FC<AutoRefreshProps> = ({ onRefresh, loading = false, disabled = false }) => {
  // 使用自定义Hook管理状态
  const {
    isAutoRefreshing,
    refreshInterval,
    remainingTime,
    lastRefreshTime,
    toggleAutoRefresh,
    setRefreshInterval,
    handleManualRefresh,
  } = useAutoRefresh(onRefresh, loading);

  // 计算进度百分比
  const progressPercent = useMemo(() => {
    return calculateProgressPercent(refreshInterval, remainingTime, isAutoRefreshing, loading);
  }, [refreshInterval, remainingTime, isAutoRefreshing, loading]);

  // 下拉菜单项
  const menuItems = useMemo(() => {
    return REFRESH_INTERVALS.map((item) => ({
      key: item.value,
      label: item.label,
      disabled: item.disabled,
      onClick: () => setRefreshInterval(item.value),
    }));
  }, [setRefreshInterval]);

  // 当前选中的间隔标签
  const currentIntervalLabel = useMemo(() => {
    return REFRESH_INTERVALS.find(item => item.value === refreshInterval)?.label || '关闭';
  }, [refreshInterval]);

  // 构建tooltip内容
  const tooltipContent = useMemo(() => {
    return generateTooltipContent(
      isAutoRefreshing,
      refreshInterval,
      currentIntervalLabel,
      loading,
      remainingTime,
      lastRefreshTime
    );
  }, [isAutoRefreshing, refreshInterval, currentIntervalLabel, loading, remainingTime, lastRefreshTime]);

  return (
    <div className={styles.autoRefresh}>
      <Space size={4}>
        {/* 手动刷新按钮 */}
        <Tooltip title="手动刷新">
          <Button
            size="small"
            type="text"
            icon={<ReloadOutlined />}
            onClick={handleManualRefresh}
            loading={loading}
            disabled={disabled}
            className={styles.refreshButton}
          />
        </Tooltip>

        {/* 自动刷新控制区域 */}
        <div className={styles.autoRefreshControl}>
          <Tooltip title={tooltipContent}>
            <Button
              size="small"
              type="text"
              icon={isAutoRefreshing ? <PauseCircleOutlined /> : <PlayCircleOutlined />}
              onClick={toggleAutoRefresh}
              disabled={disabled || refreshInterval === 0}
              className={`${styles.autoRefreshToggle} ${isAutoRefreshing ? styles.active : ''}`}
            >
              {isAutoRefreshing ? '暂停' : '自动刷新'}
            </Button>
          </Tooltip>

          {/* 刷新间隔选择 */}
          <Dropdown
            menu={{ items: menuItems, selectedKeys: [refreshInterval.toString()] }}
            trigger={['click']}
            disabled={disabled}
            placement="bottomRight"
          >
            <Button size="small" type="text" className={styles.intervalButton}>
              <Space size={2}>
                <ClockCircleOutlined />
                <span>{currentIntervalLabel}</span>
                <CaretDownOutlined style={{ fontSize: '10px' }} />
              </Space>
            </Button>
          </Dropdown>
        </div>

        {/* 刷新状态显示 */}
        {isAutoRefreshing && refreshInterval > 0 && (
          <div className={styles.refreshStatus}>
            <div className={styles.progressContainer}>
              <Progress
                percent={progressPercent}
                size="small"
                showInfo={false}
                strokeColor={loading ? "#d9d9d9" : "#1890ff"}
                className={styles.progress}
              />
              <span className={styles.timeText}>
                {formatRemainingTime(remainingTime, loading)}
              </span>
            </div>
          </div>
        )}
      </Space>
    </div>
  );
};

export default AutoRefresh;
