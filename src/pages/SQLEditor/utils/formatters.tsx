import React from 'react';
import { Typography } from 'antd';
import dayjs from 'dayjs';

const { Text } = Typography;

/**
 * 格式化表格单元格内容
 * @param value 单元格值
 * @param column 列信息对象
 * @returns 格式化后的React节点
 */
export const formatTableCell = (value: unknown, column?: string): React.ReactNode => {
  if (value === null) return <Text type="secondary">(null)</Text>;
  if (value === undefined) return <Text type="secondary">(undefined)</Text>;

  if (typeof value === 'object') return <Text code>{JSON.stringify(value)}</Text>;

  // 检查列类型
  const lowerColumn = column?.toLowerCase() || '';
  const isMessageField = lowerColumn.includes('message');
  const isPathField =
    lowerColumn.includes('path') || lowerColumn.includes('url') || lowerColumn.includes('file');
  const isLogTimeField = lowerColumn === 'log_time' || lowerColumn.includes('timestamp');

  if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
    // 对log_time字段进行日期格式化
    if (isLogTimeField && value) {
      try {
        // 尝试解析日期时间
        const dateTime = dayjs(value);
        if (dateTime.isValid()) {
          return (
            <Text style={{ color: '#1677ff', fontFamily: 'monospace' }}>
              {dateTime.format('YYYY-MM-DD HH:mm:ss.SSS')}
            </Text>
          );
        }
      } catch (err) {
        // 解析失败，使用原始值显示
        console.warn('Failed to parse date:', value);
      }
    }
    
    // 对于message字段，使用自动换行的样式
    if ((isMessageField || isPathField) && typeof value === 'string') {
      return (
        <Text
          style={{
            whiteSpace: 'pre-wrap', // 保留空格并允许自动换行
            wordBreak: 'break-word', // 在单词内部允许换行
            display: 'block', // 使Text组件占满整个单元格
            ...(isPathField ? { fontFamily: 'monospace', fontSize: '13px' } : {}), // 路径使用等宽字体
          }}
        >
          {String(value)}
        </Text>
      );
    }
    return <Text>{String(value)}</Text>;
  }
  return <Text type="secondary">(unknown)</Text>;
};

export default formatTableCell;
