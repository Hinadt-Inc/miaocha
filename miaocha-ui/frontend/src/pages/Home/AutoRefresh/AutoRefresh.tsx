import React, { useCallback, useMemo } from 'react';

import { PlayCircleOutlined, PauseCircleOutlined, ReloadOutlined } from '@ant-design/icons';
import { Button, Dropdown, Space, Tooltip } from 'antd';

import { useHomeContext } from '../context';
import { useDataInit } from '../hooks/useDataInit';

import { REFRESH_INTERVALS } from './constants';
import styles from './index.module.less';
import { AutoRefreshProps } from './types';
import { useAutoRefresh } from './useAutoRefresh';
import { calculateProgressPercent, generateTooltipContent } from './utils';

/**
 * 自动刷新组件
 */
const AutoRefresh: React.FC<AutoRefreshProps> = ({ disabled = false }) => {
  const { loading, searchParams, distributions, distributionLoading } = useHomeContext();
  const { fetchData, refreshFieldDistributions } = useDataInit();

  const onRefresh = useCallback(() => {
    fetchData({ searchParams });
    refreshFieldDistributions();
  }, [searchParams, distributions, distributionLoading]);

  // 使用自定义Hook管理状态
  const { isAutoRefreshing, refreshInterval, remainingTime, lastRefreshTime, toggleAutoRefresh, setRefreshInterval } =
    useAutoRefresh(onRefresh, loading);

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
    const label = REFRESH_INTERVALS.find((item) => item.value === refreshInterval)?.label || '关闭';
    if (label === '关闭') return '自动刷新';
    return label;
  }, [refreshInterval]);

  // 构建tooltip内容
  const tooltipContent = useMemo(() => {
    return generateTooltipContent(
      isAutoRefreshing,
      refreshInterval,
      currentIntervalLabel,
      loading,
      remainingTime,
      lastRefreshTime,
    );
  }, [isAutoRefreshing, refreshInterval, currentIntervalLabel, loading, remainingTime, lastRefreshTime]);

  return (
    <div className={styles.autoRefresh}>
      <Space size={4}>
        {/* 手动刷新按钮 */}
        {/* <Tooltip title="刷新">
          <Button
            className={styles.refreshButton}
            disabled={disabled}
            icon={<ReloadOutlined />}
            loading={loading}
            size="small"
            type="text"
            onClick={handleManualRefresh}
          />
        </Tooltip> */}

        {/* 自动刷新控制区域 */}
        <div className={styles.autoRefreshControl}>
          {/* 暂停/继续按钮，仅在refreshInterval不为0时显示 */}
          {refreshInterval !== 0 && (
            <Tooltip title={tooltipContent}>
              {isAutoRefreshing ? (
                <PauseCircleOutlined className={`${styles.refreshButton}`} onClick={toggleAutoRefresh} />
              ) : (
                <PlayCircleOutlined className={`${styles.refreshButton}`} onClick={toggleAutoRefresh} />
              )}
            </Tooltip>
          )}

          {/* 刷新间隔选择 */}
          <Dropdown
            disabled={disabled}
            menu={{ items: menuItems, selectedKeys: [refreshInterval.toString()] }}
            placement="bottomRight"
            trigger={['click']}
          >
            <Button className={styles.intervalButton} size="small" type="link">
              <Space size={2}>
                {refreshInterval === 0 && <ReloadOutlined />}
                <span>{currentIntervalLabel}</span>
                {/* <CaretDownOutlined style={{ fontSize: '10px' }} /> */}
              </Space>
            </Button>
          </Dropdown>
        </div>

        {/* 刷新状态显示 */}
        {/* {isAutoRefreshing && refreshInterval > 0 && (
          <div className={styles.refreshStatus}>
            <div className={styles.progressContainer}>
              <Progress
                className={styles.progress}
                percent={progressPercent}
                showInfo={false}
                size="small"
                strokeColor={loading ? '#d9d9d9' : '#1890ff'}
              />
              <span className={styles.timeText}>{formatRemainingTime(remainingTime, loading)}</span>
            </div>
          </div>
        )} */}
      </Space>
    </div>
  );
};

export default AutoRefresh;
