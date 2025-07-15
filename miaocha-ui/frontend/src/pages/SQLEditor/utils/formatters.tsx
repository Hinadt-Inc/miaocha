import React from 'react';
import { Typography } from 'antd';
import dayjs from 'dayjs';

const { Text } = Typography;

// 处理对象类型数据
const formatObject = (value: object): React.ReactNode => {
  try {
    const formatted = JSON.stringify(value, null, 2);
    return (
      <Text code style={{ whiteSpace: 'pre-wrap', display: 'block' }}>
        {formatted}
      </Text>
    );
  } catch {
    return <Text type="warning">无法显示对象内容</Text>;
  }
};

// 处理时间类型数据
const formatDateTime = (value: string | number): React.ReactNode => {
  try {
    const date = dayjs(value);
    if (date.isValid()) {
      return (
        <Text style={{ fontFamily: 'monospace', color: '#1677ff', fontSize: '13px' }}>
          {date.format('YYYY-MM-DD HH:mm:ss')}
        </Text>
      );
    }
  } catch {
    // 如果日期解析失败，返回原始值
    return null;
  }
  return null;
};

// 处理消息字段
const formatMessage = (value: string): React.ReactNode => (
  <Text
    style={{
      whiteSpace: 'pre-wrap',
      wordBreak: 'break-word',
      display: 'block',
      fontSize: '13px',
      lineHeight: '1.5',
      color: '#333',
    }}
  >
    {value}
  </Text>
);

// 处理路径字段
const formatPath = (value: string): React.ReactNode => (
  <Text
    style={{
      fontFamily: 'monospace',
      fontSize: '13px',
      color: '#666',
      wordBreak: 'break-all',
      display: 'block',
    }}
  >
    {value}
  </Text>
);

/**
 * 格式化表格单元格内容
 * @param value 单元格值
 * @param column 列信息对象
 * @returns 格式化后的React节点
 */
export const formatTableCell = (value: unknown, column?: string): React.ReactNode => {
  // 处理空值
  if (value === null) return <Text type="secondary">null</Text>;
  if (value === undefined) return <Text type="secondary">undefined</Text>;

  // 检查列类型
  const lowerColumn = column?.toLowerCase() || '';
  const isMessageField = lowerColumn.includes('message');
  const isPathField = lowerColumn.includes('path') || lowerColumn.includes('url') || lowerColumn.includes('file');
  const isLogTimeField = lowerColumn === 'log_time' || lowerColumn.includes('timestamp');

  // 根据数据类型处理
  if (typeof value === 'object') {
    return formatObject(value);
  }

  if (typeof value === 'boolean') {
    return <Text style={{ color: value ? '#52c41a' : '#ff4d4f', fontWeight: 500 }}>{String(value)}</Text>;
  }

  if (typeof value === 'number') {
    return (
      <Text style={{ color: '#1677ff', fontFamily: 'monospace', fontSize: '13px' }}>{value.toLocaleString()}</Text>
    );
  }

  if (typeof value === 'string') {
    if (isLogTimeField) {
      const formatted = formatDateTime(value);
      if (formatted) return formatted;
    }

    if (isMessageField) return formatMessage(value);
    if (isPathField) return formatPath(value);

    return <Text style={{ color: '#333' }}>{value}</Text>;
  }

  return <Text type="secondary">{JSON.stringify(value)}</Text>;
};

export default formatTableCell;
