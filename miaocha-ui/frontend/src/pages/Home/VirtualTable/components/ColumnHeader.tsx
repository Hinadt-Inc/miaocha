import React, { useState } from 'react';

import { CloseOutlined, DoubleLeftOutlined, DoubleRightOutlined } from '@ant-design/icons';
import { Button, Tooltip } from 'antd';

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
  canMoveLeft,
  canMoveRight,
}) => {
  const [hovered, setHovered] = useState(false);
  return (
    <div
      className={styles.columnHeaderContainer}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
    >
      {title}
      {hovered && (
        <div className={styles.headerActions}>
          <Tooltip title="移除该列">
            <Button
              color="primary"
              size="small"
              variant="link"
              onClick={(e) => {
                e.stopPropagation();
                onDelete(colIndex);
              }}
            >
              <CloseOutlined />
            </Button>
          </Tooltip>
          {canMoveLeft && (
            <Tooltip title="将列左移​">
              <Button
                color="primary"
                size="small"
                variant="link"
                onClick={(e) => {
                  e.stopPropagation();
                  onMoveLeft(colIndex);
                }}
              >
                <DoubleLeftOutlined />
              </Button>
            </Tooltip>
          )}
          {canMoveRight && (
            <Tooltip title="将列右移​">
              <Button
                color="primary"
                size="small"
                variant="link"
                onClick={(e) => {
                  e.stopPropagation();
                  onMoveRight(colIndex);
                }}
              >
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
