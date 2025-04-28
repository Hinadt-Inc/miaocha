import React from 'react';
import { Typography } from 'antd';

const { Text } = Typography;

/**
 * 格式化表格单元格内容
 * @param value 单元格值
 * @returns 格式化后的React节点
 */
export const formatTableCell = (value: unknown): React.ReactNode => {
  if (value === null) return <Text type="secondary">(null)</Text>;
  if (value === undefined) return <Text type="secondary">(undefined)</Text>;
  if (typeof value === 'object') return <Text code>{JSON.stringify(value)}</Text>;
  if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
    return <Text>{String(value)}</Text>;
  }
  return <Text type="secondary">(unknown)</Text>;
};

export default formatTableCell;
