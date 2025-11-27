import React, { useState, useCallback, memo, useMemo } from 'react';

import { Collapse, Tag, Button, Progress, Tooltip, Typography, Empty, Spin } from 'antd';

import { getFieldTypeColor } from '@/utils/logDataHelpers';

import { useHomeContext } from '../../context';
import { useDataInit } from '../../hooks/useDataInit';
import { IModuleQueryConfig } from '../../types';
import { deduplicateAndDeleteWhereSqls, formatSqlKey } from '../../utils';
import styles from '../styles/FieldListItem.module.less';
import { FieldListItemProps } from '../types';
import { sumArrayCount } from '../utils';

/**
 * 字段列表项组件
 * 显示字段信息和分布数据
 */
const FieldListItem: React.FC<FieldListItemProps> = memo(
  ({ key, isSelected, column, columnIndex }) => {
    const {
      searchParams,
      moduleQueryConfig,
      distributions,
      distributionLoading,
      logTableColumns,
      setLogTableColumns,
      setSearchParams,
      updateSearchParams,
    } = useHomeContext();
    const { fetchFieldDistribution, fetchData } = useDataInit();

    const [activeKey, setActiveKey] = useState<string[]>([]);

    const whereSqlsMap = useMemo(() => {
      const seen = new Map<string, string>();
      const sqls = searchParams.whereSqls || [];
      sqls.forEach((sql) => {
        // 去掉所有空格作为key
        const normalizedKey = formatSqlKey(sql);
        // 如果还没有这个key，则保留原始值（包含空格的版本）
        seen.set(normalizedKey, sql);
      });
      return seen;
    }, [searchParams.whereSqls]);

    // 获取分布数据
    const dist = distributions[column.columnName as string];
    const hasData = useMemo(() => {
      return !!(
        dist &&
        ((dist.nonNullCount || 0) > 0 || (dist.totalCount || 0) > 0 || (dist.valueDistributions?.length || 0) > 0)
      );
    }, [distributions, column]);
    const isLoading = distributionLoading[column.columnName as string];

    // 动态获取时间字段名
    const determineTimeField = (): string => {
      // 优先使用配置的时间字段
      if (moduleQueryConfig?.timeField) {
        return moduleQueryConfig.timeField;
      }

      const currentFields = searchParams.fields || [];
      // 如果没有配置，从字段数据中智能查找
      const commonTimeFields = ['logs_timestamp', 'log_time', 'timestamp', 'time', '@timestamp'];
      // 查找包含time关键字的字段
      const timeRelatedField = currentFields.find(
        (field) =>
          commonTimeFields.includes(field as string) ||
          field?.toLowerCase().includes('time') ||
          field?.toLowerCase().includes('timestamp'),
      );

      return timeRelatedField || 'logs_timestamp'; // 兜底默认值
    };

    const timeField = determineTimeField();
    const isTimeField = isSelected && column.columnName === timeField;

    // 切换折叠面板
    const handleCollapseChange = useCallback(
      (key: string[]) => {
        const { columnName = '' } = column;
        // 只有当折叠面板状态变化时才更新activeColumns
        if (key.length > 0 && !distributions[columnName]) {
          fetchFieldDistribution(columnName);
        }

        setActiveKey(key);
      },
      [column.columnName, distributions, key, isSelected],
    );

    // 切换字段选中状态的回调
    const handleToggle = useCallback(
      (column: ILogColumnsResponse) => {
        const idx = logTableColumns.findIndex((item) => item.columnName === column.columnName);
        if (idx > -1) {
          const newColumns = [...logTableColumns];
          newColumns[idx].selected = !newColumns[idx].selected;
          setLogTableColumns(newColumns);

          const newFields = [...(searchParams.fields || [])];
          if (newColumns[idx].selected) {
            newFields.push(column.columnName as string);
          } else if (column.columnName && column.columnName.indexOf('.') > -1) {
            newFields.splice(newFields.indexOf(column.columnName as string), 1);
          }

          const paramsWidthFields = updateSearchParams({
            fields: newFields,
          });
          fetchData({
            moduleQueryConfig: moduleQueryConfig as IModuleQueryConfig,
            searchParams: paramsWidthFields,
          });
        }
      },
      [column, logTableColumns],
    );

    // 点击查询的回调
    const handleQuery = useCallback(
      (flag: '=' | '!=', son: IValueDistributions) => {
        const needRemoveSql = `${column.columnName} ${flag === '=' ? '!=' : '='} '${son.value}'`;
        const newSql = `${column.columnName} ${flag} '${son.value}'`;
        const newWhereSqls = deduplicateAndDeleteWhereSqls(
          [...(searchParams.whereSqls || []), newSql] as string[],
          needRemoveSql,
        );
        setSearchParams({
          ...searchParams,
          whereSqls: newWhereSqls,
        });
      },
      [searchParams, column],
    );

    // 如果是固定字段，不显示
    if (column.isFixed) {
      return null;
    }

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
                        disabled={whereSqlsMap.has(formatSqlKey(`${column.columnName} = '${sub.value}'`))}
                        variant="link"
                        onClick={() => handleQuery('=', sub)}
                      >
                        <i className="iconfont icon-fangda" />
                      </Button>
                      <Button
                        color="primary"
                        disabled={whereSqlsMap.has(formatSqlKey(`${column.columnName} != '${sub.value}'`))}
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
                    onClick={(e) => {
                      e.stopPropagation();
                      handleToggle(column);
                    }}
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
    return prevProps.key === nextProps.key;
  },
);

FieldListItem.displayName = 'FieldListItem';

export default FieldListItem;
