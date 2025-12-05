import React, { useCallback, useMemo } from 'react';

import { PlayCircleOutlined, PauseCircleOutlined, ReloadOutlined } from '@ant-design/icons';
import { Button, Dropdown, Space, Tooltip } from 'antd';

import { useHomeContext } from '../context';
import { useDataInit } from '../hooks/useDataInit';

import { REFRESH_INTERVALS } from './constants';
import styles from './index.module.less';
import { AutoRefreshProps } from './types';
import { useAutoRefresh } from './useAutoRefresh';
import { generateTooltipContent } from './utils';

/**
 * 自动刷新组件
 */
const AutoRefresh: React.FC<AutoRefreshProps> = ({ disabled = false }) => {
  const { loading, searchParams, distributions, distributionLoading, updateSearchParams } = useHomeContext();
  const { fetchData, refreshFieldDistributions } = useDataInit();

  const onRefresh = useCallback(() => {
    // 刷新数据，重新计算时间
    const newParams = updateSearchParams({});
    fetchData({ searchParams: newParams });
    refreshFieldDistributions();
  }, [searchParams, distributions, distributionLoading]);

  // 使用自定义Hook管理状态
  const { isAutoRefreshing, refreshInterval, remainingTime, toggleAutoRefresh, setRefreshInterval } = useAutoRefresh(
    onRefresh,
    loading,
  );

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
    return generateTooltipContent(isAutoRefreshing, refreshInterval, loading, remainingTime);
  }, [isAutoRefreshing, refreshInterval, loading, remainingTime]);

  return (
    <div className={styles.autoRefresh}>
      <Space size={4}>
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
              </Space>
            </Button>
          </Dropdown>
        </div>
      </Space>
    </div>
  );
};

export default AutoRefresh;
