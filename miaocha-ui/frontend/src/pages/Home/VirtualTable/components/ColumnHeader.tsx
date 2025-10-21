import React, { useState } from 'react';
import { Button, Tooltip } from 'antd';
import { CloseOutlined, DoubleLeftOutlined, DoubleRightOutlined } from '@ant-design/icons';
import { ColumnHeaderProps } from '../types';
import styles from '../VirtualTable.module.less';

/**
 * 可操作的列标题组件
 */
const ColumnHeader: React.FC<ColumnHeaderProps> = ({
  title,
  colIndex,
  onDelete,
  onMoveLeft,
  onMoveRight,
  showActions,
  columns,
}) => {
  const [hovered, setHovered] = useState(false);
  const isLeftLogTime = colIndex > 0 && columns[colIndex - 1]?.dataIndex === '_source';
  const isLast = colIndex === columns.length - 1;
  
  return (
    <div
      className={styles.columnHeaderContainer}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
    >
      {title}
      {showActions && hovered && (
        <div className={styles.headerActions}>
          <Tooltip title="移除该列">
            <Button color="primary" size="small" variant="link" onClick={() => onDelete(colIndex)}>
              <CloseOutlined />
            </Button>
          </Tooltip>
          {colIndex > 0 && !isLeftLogTime && (
            <Tooltip title="将列左移​">
              <Button color="primary" size="small" variant="link" onClick={() => onMoveLeft(colIndex)}>
                <DoubleLeftOutlined />
              </Button>
            </Tooltip>
          )}
          {!isLast && (
            <Tooltip title="将列右移​">
              <Button color="primary" size="small" variant="link" onClick={() => onMoveRight(colIndex)}>
                <DoubleRightOutlined />
              </Button>
            </Tooltip>
          )}
        </div>
      )}
    </div>
  );
};

export default ColumnHeader;
