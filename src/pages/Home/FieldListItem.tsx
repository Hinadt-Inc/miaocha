import { useState, useCallback } from 'react';
import { Collapse, Tag, Button, Progress, Tooltip, Typography, Empty } from 'antd';
import { getFieldTypeColor } from '@/utils/logDataHelpers';
import styles from './Sider.module.less';

interface IFieldData {
  searchParams: ILogSearchParams; // 搜索参数
  distributions: Record<string, IFieldDistributions>; // 字段分布
  isSelected: boolean; // 是否选中
  onToggle: (column: ILogColumnsResponse, index: number) => void; // 切换选中状态
  onSearch: (params: ILogSearchParams) => void; // 搜索
  onDistribution: (params: string) => void; // 分布
}

interface IProps {
  columnIndex: number; // 字段索引
  column: ILogColumnsResponse; // 字段数据
  fieldData: IFieldData; // 合并后的字段数据
}

const FieldListItem: React.FC<IProps> = ({ column, columnIndex, fieldData }) => {
  const { distributions = {}, isSelected, onSearch, searchParams } = fieldData;
  console.log('【打印日志】fieldData:', fieldData);
  console.log('【打印日志】column:', column);
  const [activeKey, setActiveKey] = useState<string[]>([]);
  const handleCollapseChange = useCallback((key: string | string[]) => {
    setActiveKey(key as string[]);
  }, []);

  console.log('【打印日志】activeKey:', activeKey);

  if (column.isFixed) {
    return null;
  }

  // const add = (parent: ILogColumnsResponse, son: IValueDistributions) => {
  //   const { columnName } = parent;
  //   const { value } = son;
  //   onSearch({
  //     whereSqls: [...(searchParams?.whereSqls || []), `${columnName} = '${value}'`],
  //   });
  // };

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
            <div className={styles.bar} onClick={() => fieldData.onDistribution(activeKey[0])}>
              <div>
                <Tag color={getFieldTypeColor(column.dataType)}>{column.dataType?.substr(0, 1)?.toUpperCase()}</Tag>
                {column.columnName}
              </div>
              {!(isSelected && column.columnName === 'log_time') && (
                <Button
                  color={isSelected ? 'danger' : 'primary'}
                  variant="link"
                  className={styles.footBtn}
                  onClick={(e) => {
                    e.stopPropagation();
                    fieldData.onToggle(column, columnIndex);
                  }}
                >
                  {isSelected ? '移除' : '添加'}
                </Button>
              )}
            </div>
          ),
          children: (
            <div className={styles.record}>
              {!distributions[column.columnName as string] && <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />}
              {distributions[column.columnName as string] && (
                <div className={styles.header}>
                  <b>TOP5 </b>
                  {distributions[column.columnName as string]?.nonNullCount || 0} /{' '}
                  {distributions[column.columnName as string]?.totalCount || 0} 记录
                </div>
              )}
              <div className={styles.ul}>
                {distributions[column.columnName as string]?.valueDistributions?.map(
                  (sub: IValueDistributions, i: number) => (
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
                            color="primary"
                            variant="link"
                            // onClick={() =>
                            //   fieldData.onSearch({
                            //     whereSqls: [
                            //       ...(fieldData.searchParams?.whereSqls || []),
                            //       `${column.columnName} = '${sub.value}'`,
                            //     ],
                            //   })
                            // }
                          >
                            <i className="iconfont icon-fangda"></i>
                          </Button>
                          <Button color="primary" variant="link">
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
                  ),
                )}
              </div>
            </div>
          ),
        },
      ]}
    />
  );
};

export default FieldListItem;
