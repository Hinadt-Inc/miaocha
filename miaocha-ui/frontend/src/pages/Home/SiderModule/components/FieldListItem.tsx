import React, { useState, useCallback, memo } from 'react';

import { Collapse, Tag, Button, Progress, Tooltip, Typography, Empty, Spin } from 'antd';

import { getFieldTypeColor } from '@/utils/logDataHelpers';

import styles from '../styles/FieldListItem.module.less';
import { FieldListItemProps } from '../types';
import { sumArrayCount, hasDistributionData } from '../utils';

/**
 * 字段列表项组件
 * 显示字段信息和分布数据
 */
const FieldListItem: React.FC<FieldListItemProps> = memo(
  ({ isSelected, column, columnIndex, fieldData, moduleQueryConfig }) => {
    const {
      distributions = {},
      distributionLoading = {},
      activeColumns = [],
      onActiveColumns,
      searchParams,
      onDistribution,
      onToggle,
      setWhereSqlsFromSider,
    } = fieldData;

    const [activeKey, setActiveKey] = useState<string[]>([]);

    // 切换折叠面板
    const handleCollapseChange = useCallback(
      (key: string[]) => {
        const { columnName = '' } = column;

        // 防止重复触发相同操作
        if (key.length > 0 && activeKey.includes(key[0])) {
          return;
        }
        if (key.length === 0 && activeKey.length === 0) {
          return;
        }

        // 只有当折叠面板状态变化时才更新activeColumns
        if (key.length > 0) {
          // 展开时，无论字段是否已在activeColumns中，都要触发分布数据查询
          if (!activeColumns.includes(columnName)) {
            const newActiveColumns = [...activeColumns, columnName];
            onActiveColumns(newActiveColumns);
            onDistribution(columnName, newActiveColumns, '');
          } else {
            // 字段已在activeColumns中，但仍需要触发分布数据查询以显示loading和数据
            onDistribution(columnName, activeColumns, '');
          }
          const localActiveColumns = JSON.parse(localStorage.getItem('activeColumns') || '[]');
          localStorage.setItem('activeColumns', JSON.stringify([...new Set([...localActiveColumns, ...key])]));
        } else {
          // 移除
          const newActiveColumns = activeColumns.filter((item) => item !== columnName);
          onActiveColumns(newActiveColumns);
          onDistribution(columnName, newActiveColumns, '');
          const localActiveColumns = JSON.parse(localStorage.getItem('activeColumns') || '[]');
          const newLocalActiveColumns = localActiveColumns.filter((item: string) => item !== columnName);
          localStorage.setItem('activeColumns', JSON.stringify([...new Set([...newLocalActiveColumns, ...key])]));
        }

        setActiveKey(key);
      },
      [activeColumns, column.columnName, onActiveColumns, onDistribution, activeKey],
    );

    // 切换字段选中状态的回调
    const handleToggle = useCallback(
      (e: React.MouseEvent) => {
        e.stopPropagation();
        onToggle(column);
      },
      [onToggle, column],
    );

    // 点击查询的回调
    const handleQuery = useCallback(
      (flag: '=' | '!=', son: IValueDistributions) => {
        const { columnName = '' } = column;
        const { value } = son;

        // 更新主要的搜索条件，这会触发主要数据请求（日志列表、时间分布图等）
        // 同时也会同步更新localStorage中的searchBarParams
        // Sider组件会通过useEffect自动监听searchParams变化并重新获取分布数据，无需手动调用
        setWhereSqlsFromSider(flag, columnName, value);
      },
      [setWhereSqlsFromSider, column],
    );

    // 如果是固定字段，不显示
    if (column.isFixed) {
      return null;
    }

    // 动态获取时间字段名
    const determineTimeField = (): string => {
      // 优先使用配置的时间字段
      if (moduleQueryConfig?.timeField) {
        return moduleQueryConfig.timeField;
      }

      // 如果没有配置，从字段数据中智能查找
      const availableFields = fieldData.activeColumns || [];
      const commonTimeFields = ['logs_timestamp', 'log_time', 'timestamp', 'time', '@timestamp'];

      for (const timeField of commonTimeFields) {
        if (availableFields.includes(timeField)) {
          return timeField;
        }
      }

      // 查找包含time关键字的字段
      const timeRelatedField = availableFields.find(
        (field) => field.toLowerCase().includes('time') || field.toLowerCase().includes('timestamp'),
      );

      return timeRelatedField || 'logs_timestamp'; // 兜底默认值
    };

    const timeField = determineTimeField();
    const isTimeField = isSelected && column.columnName === timeField;

    // 获取分布数据
    const dist = distributions[column.columnName as string];
    const isLoading = distributionLoading[column.columnName as string];
    const hasData = hasDistributionData(dist);

    // 渲染内容
    const renderContent = () => {
      if (isLoading) {
        return (
          <div className={styles.loadingContainer}>
            <Spin spinning={true}>
              <div className={styles.spinPlaceholder} />
            </Spin>
          </div>
        );
      }

      if (hasData) {
        return (
          <>
            <div className={styles.header}>
              <b>TOP5 </b>
              {sumArrayCount(dist?.valueDistributions)} / {dist?.sampleSize || 0} 记录
            </div>
            <div className={styles.ul}>
              {dist?.valueDistributions?.map((sub: IValueDistributions, i: number) => (
                <div key={`list${columnIndex}${column.columnName}${i}`} className={styles.li}>
                  <div className={styles.one}>
                    <div className={styles.left}>
                      <Typography.Paragraph
                        ellipsis={{
                          rows: 1,
                          tooltip: true,
                        }}
                        type="secondary"
                      >
                        {sub.value}
                      </Typography.Paragraph>
                    </div>
                    <div className={styles.right}>
                      <Button
                        color="primary"
                        disabled={searchParams?.whereSqls?.includes(`${column.columnName} = '${sub.value}'`)}
                        variant="link"
                        onClick={() => handleQuery('=', sub)}
                      >
                        <i className="iconfont icon-fangda" />
                      </Button>
                      <Button
                        color="primary"
                        disabled={searchParams?.whereSqls?.includes(`${column.columnName} != '${sub.value}'`)}
                        variant="link"
                        onClick={() => handleQuery('!=', sub)}
                      >
                        <i className="iconfont icon-suoxiao1" />
                      </Button>
                    </div>
                  </div>
                  <div className={styles.two}>
                    <Tooltip placement="right" title={sub.count}>
                      <Progress percent={sub.percentage} percentPosition={{ align: 'center', type: 'inner' }} />
                    </Tooltip>
                  </div>
                </div>
              ))}
            </div>
          </>
        );
      }

      return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />;
    };

    return (
      <Collapse
        key={column.columnName}
        activeKey={activeKey}
        className={styles.item}
        expandIcon={() => null}
        items={[
          {
            key: `${column.columnName}`,
            label: (
              <div className={styles.bar}>
                <Tooltip arrow={false} placement="topLeft" title={column.dataType}>
                  <Tag color={getFieldTypeColor(column.dataType)}>
                    {column.dataType?.substring(0, 1)?.toUpperCase()}
                  </Tag>
                </Tooltip>
                <Tooltip arrow={false} placement="topLeft" title={column.columnName}>
                  <span className={styles.columnName}>{column.columnName}</span>
                </Tooltip>
                {!isTimeField && (
                  <Button
                    className={styles.footBtn}
                    color={isSelected ? 'danger' : 'primary'}
                    variant="link"
                    onClick={handleToggle}
                  >
                    {isSelected ? '移除' : '添加'}
                  </Button>
                )}
              </div>
            ),
            children: <div className={styles.record}>{renderContent()}</div>,
          },
        ]}
        size="small"
        onChange={handleCollapseChange}
      />
    );
  },
  (prevProps, nextProps) => {
    // 自定义比较函数，只在关键属性变化时才重新渲染
    return (
      prevProps.isSelected === nextProps.isSelected &&
      prevProps.column.columnName === nextProps.column.columnName &&
      prevProps.column.selected === nextProps.column.selected &&
      prevProps.fieldData.distributions[prevProps.column.columnName as string] ===
        nextProps.fieldData.distributions[nextProps.column.columnName as string] &&
      prevProps.fieldData.distributionLoading[prevProps.column.columnName as string] ===
        nextProps.fieldData.distributionLoading[nextProps.column.columnName as string] &&
      prevProps.fieldData.searchParams === nextProps.fieldData.searchParams
    );
  },
);

FieldListItem.displayName = 'FieldListItem';

export default FieldListItem;
