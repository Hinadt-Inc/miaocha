import { useState, useCallback } from 'react';
import { Collapse, Tag, Button, Progress, Tooltip, Typography, Empty } from 'antd';
import { getFieldTypeColor } from '@/utils/logDataHelpers';
import styles from './Sider.module.less';

interface IFieldData {
  activeColumns: string[]; // 选中的列
  searchParams: ILogSearchParams; // 搜索参数
  distributions: Record<string, IFieldDistributions>; // 字段分布
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
}

const FieldListItem: React.FC<IProps> = ({ isSelected, column, columnIndex, fieldData }) => {
  const {
    distributions = {},
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
      // 只有当折叠面板状态变化时才更新activeColumns
      if (key.length > 0) {
        if (!activeColumns.includes(columnName)) {
          const newActiveColumns = [...activeColumns, columnName];
          onActiveColumns(newActiveColumns);
          onDistribution(columnName, newActiveColumns, '');
        }
      } else {
        // 移除
        const newActiveColumns = activeColumns.filter((item) => item !== columnName);
        onActiveColumns(newActiveColumns);
        onDistribution(columnName, newActiveColumns, '');
      }
      setActiveKey(key as string[]);
    },
    [activeColumns, column.columnName, onActiveColumns],
  );

  if (column.isFixed) {
    return null;
  }

  // 点击查询
  const query = (flag: '=' | '!=', parent: ILogColumnsResponse, son: IValueDistributions) => {
    const { columnName = '' } = parent;
    const { value } = son;
    setWhereSqlsFromSider(flag, columnName, value);
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
              <Tag color={getFieldTypeColor(column.dataType)}>{column.dataType?.substr(0, 1)?.toUpperCase()}</Tag>
              <span className={styles.columnName}>{column.columnName}</span>
              {!(isSelected && column.columnName === 'log_time') && (
                <Button
                  color={isSelected ? 'danger' : 'primary'}
                  variant="link"
                  className={styles.footBtn}
                  onClick={(e) => {
                    e.stopPropagation();
                    onToggle(column);
                  }}
                >
                  {isSelected ? '移除' : '添加'}
                </Button>
              )}
            </div>
          ),
          children: (
            <div className={styles.record}>
              {(() => {
                const dist = distributions[column.columnName as string];
                const hasData =
                  !!dist &&
                  ((dist.nonNullCount || 0) > 0 ||
                    (dist.totalCount || 0) > 0 ||
                    (dist.valueDistributions?.length || 0) > 0);
                return (
                  <>
                    {!hasData && <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />}
                    {hasData && (
                      <div className={styles.header}>
                        <b>TOP5 </b>
                        {dist?.nonNullCount || 0} / {dist?.totalCount || 0} 记录
                      </div>
                    )}
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
                                  onEllipsis: () => {},
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
                                onClick={() => query('=', column, sub)}
                              >
                                <i className="iconfont icon-fangda"></i>
                              </Button>
                              <Button
                                disabled={searchParams?.whereSqls?.includes(`${column.columnName} != '${sub.value}'`)}
                                color="primary"
                                variant="link"
                                onClick={() => query('!=', column, sub)}
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
              })()}
            </div>
          ),
        },
      ]}
    />
  );
};

export default FieldListItem;
