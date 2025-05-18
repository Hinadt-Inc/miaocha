import { useState, useCallback } from 'react';
import { Collapse, Tag, Button, Progress, Tooltip, Typography } from 'antd';
import { getFieldTypeColor } from '@/utils/logDataHelpers';
import styles from './Sider.module.less';

interface IProps {
  item: ILogColumnsResponse; // 字段列表项
  index: number; // 索引
  fieldDistributions: IFieldDistributions[]; // 字段值分布列表
  isSelected: boolean; // 是否选中
  onToggle: (item: ILogColumnsResponse, index: number) => void; // 切换选中状态的回调函数
}

const FieldListItem: React.FC<IProps> = ({ item, index, fieldDistributions, isSelected, onToggle }) => {
  const [activeKey, setActiveKey] = useState<string[]>([]);
  const handleCollapseChange = useCallback((key: string | string[]) => {
    setActiveKey(key as string[]);
  }, []);

  if (item.isFixed) {
    return null;
  }

  return (
    <Collapse
      size="small"
      key={item.columnName}
      activeKey={activeKey}
      className={styles.item}
      onChange={handleCollapseChange}
      items={[
        {
          key: `${item.columnName}`,
          label: (
            <div className={styles.bar}>
              <div>
                <Tag color={getFieldTypeColor(item.dataType)}>{item.dataType?.substr(0, 1)?.toUpperCase()}</Tag>
                {item.columnName}
              </div>
              {!(isSelected && item.columnName === 'log_time') && (
                <Button
                  color={isSelected ? 'danger' : 'primary'}
                  variant="link"
                  className={styles.footBtn}
                  onClick={() => onToggle(item, index)}
                >
                  {isSelected ? '移除' : '添加'}
                </Button>
              )}
            </div>
          ),
          children: (
            <div className={styles.record}>
              {fieldDistributions
                ?.filter((sub) => sub.fieldName === item.columnName)
                .map((sub: IFieldDistributions, i: number) => (
                  <div key={`header${index}${item.columnName}${i}`} className={styles.header}>
                    <b>TOP5 </b>
                    {sub.nonNullCount}/{sub.totalCount} 记录
                  </div>
                ))}
              <div className={styles.ul}>
                {fieldDistributions
                  ?.find((sub) => sub.fieldName === item.columnName)
                  ?.valueDistributions?.map((sub: IValueDistributions, i: number) => (
                    <div className={styles.li} key={`list${index}${item.columnName}${i}`}>
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
                          <Button color="primary" variant="link">
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
                  ))}
              </div>
            </div>
          ),
        },
      ]}
    />
  );
};

export default FieldListItem;
