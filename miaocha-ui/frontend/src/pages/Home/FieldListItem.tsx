import { useState, useCallback, memo } from 'react';
import { Collapse, Tag, Button, Progress, Tooltip, Typography, Empty, Spin } from 'antd';
import { getFieldTypeColor } from '@/utils/logDataHelpers';
import styles from './Sider.module.less';

interface IFieldData {
  activeColumns: string[]; // 选中的列
  searchParams: ILogSearchParams; // 搜索参数
  distributions: Record<string, IFieldDistributions>; // 字段分布
  distributionLoading: Record<string, boolean>; // 字段分布加载状态
  onToggle: (column: ILogColumnsResponse) => void; // 切换选中状态
  onDistribution: (columnName: string, newActiveColumns: string[], sql: string) => void; // 分布
  onActiveColumns: (params: string[]) => void; // 选中的列
  setWhereSqlsFromSider: any; // 设置where条件
}

interface IProps {
  isSelected: boolean; // 是否选中
  columnIndex: number; // 字段索引
  column: ILogColumnsResponse; // 字段数据
  fieldData: IFieldData; // 合并后的字段数据
  moduleQueryConfig?: any; // 模块查询配置
}

const FieldListItem: React.FC<IProps> = memo(
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
          return; // 如果已经展开，不重复处理
        }
        if (key.length === 0 && activeKey.length === 0) {
          return; // 如果已经折叠，不重复处理
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
        setWhereSqlsFromSider(flag, columnName, value);
      },
      [setWhereSqlsFromSider, column],
    );

    if (column.isFixed) {
      return null;
    }

    // 数组count求和
    const sumArrayCount = (valueDistributions: IValueDistributions[]): number => {
      const counts = valueDistributions.map((item) => item.count);
      return counts.reduce((sum: number, item: string | number): number => {
        const num = typeof item === 'string' ? parseFloat(item) : item;
        return sum + (isNaN(num) ? 0 : num);
      }, 0);
    };

    // 获取时间字段名
    const timeField = moduleQueryConfig?.timeField || 'log_time';
    const isTimeField = isSelected && column.columnName === timeField;

    // 获取分布数据
    const dist = distributions[column.columnName as string];
    const isLoading = distributionLoading[column.columnName as string];
    const hasData =
      !!dist &&
      ((dist.nonNullCount || 0) > 0 || (dist.totalCount || 0) > 0 || (dist.valueDistributions?.length || 0) > 0);

    // 渲染内容
    const renderContent = () => {
      if (isLoading) {
        return (
          <div className={styles.loadingContainer}>
            <Spin size="small" tip="加载中..." />
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
                <div className={styles.li} key={`list${columnIndex}${column.columnName}${i}`}>
                  <div className={styles.one}>
                    <div className={styles.left}>
                      <Typography.Paragraph
                        type="secondary"
                        ellipsis={{
                          rows: 1,
                          tooltip: true,
                          onEllipsis: () => { },
                        }}
                      >
                        {sub.value}
                      </Typography.Paragraph>
                    </div>
                    <div className={styles.right}>
                      <Button
                        disabled={searchParams?.whereSqls?.includes(`${column.columnName} = '${sub.value}'`)}
                        color="primary"
                        variant="link"
                        onClick={() => handleQuery('=', sub)}
                      >
                        <i className="iconfont icon-fangda"></i>
                      </Button>
                      <Button
                        disabled={searchParams?.whereSqls?.includes(`${column.columnName} != '${sub.value}'`)}
                        color="primary"
                        variant="link"
                        onClick={() => handleQuery('!=', sub)}
                      >
                        <i className="iconfont icon-suoxiao1"></i>
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
        size="small"
        key={column.columnName}
        activeKey={activeKey}
        className={styles.item}
        onChange={handleCollapseChange}
        items={[
          {
            key: `${column.columnName}`,
            label: (
              <div className={styles.bar}>
                <Tooltip placement="topLeft" title={column.dataType} arrow={false}>
                  <Tag color={getFieldTypeColor(column.dataType)}>{column.dataType?.substring(0, 1)?.toUpperCase()}</Tag>
                </Tooltip>
                <Tooltip placement="topLeft" title={column.columnName} arrow={false}>
                  <span className={styles.columnName}>{column.columnName}</span>
                </Tooltip>
                {!isTimeField && (
                  <Button
                    color={isSelected ? 'danger' : 'primary'}
                    variant="link"
                    className={styles.footBtn}
                    onClick={handleToggle}
                  >
                    {isSelected ? '移除' : '添加'}
                  </Button>
                )}
              </div>
            ),
            children: (
              <div className={styles.record}>
                {renderContent()}
              </div>
            ),
          },
        ]}
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
