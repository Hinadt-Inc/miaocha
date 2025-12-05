/**
 * SQL搜索输入组件
 */

import React, { useRef } from 'react';

import { DownOutlined } from '@ant-design/icons';
import { AutoComplete, Button, Dropdown, Space, Tag } from 'antd';
import type { MenuProps } from 'antd';

import { ILogColumnsResponse } from '../../types';
import { PLACEHOLDERS } from '../constants';

interface ISqlInputProps {
  value: string;
  onChange: (value: string) => void;
  columns?: ILogColumnsResponse[];
}

// SQL操作符配置（带值模板）
interface SqlOperator {
  key: string;
  label: string;
  sql: string;
  valueTemplate?: string; // 可选的值模板
  cursorOffset?: number; // 光标相对于valueTemplate末尾的偏移量（负数表示从末尾往前）
}

const SQL_OPERATORS: SqlOperator[] = [
  { key: 'eq', label: '=', sql: '=', valueTemplate: "''", cursorOffset: -1 }, // 光标在 '' 之间
  { key: 'neq', label: '!=', sql: '!=', valueTemplate: "''", cursorOffset: -1 }, // 光标在 '' 之间
  { key: 'like', label: 'LIKE', sql: 'LIKE', valueTemplate: "'%%'", cursorOffset: -2 }, // 光标在 %% 之间
  { key: 'notlike', label: 'NOT LIKE', sql: 'NOT LIKE', valueTemplate: "'%%'", cursorOffset: -2 }, // 光标在 %% 之间
];

// 更多操作符配置
const MORE_OPERATORS: SqlOperator[] = [
  { key: 'gt', label: '>', sql: '>', valueTemplate: "''", cursorOffset: -1 },
  { key: 'gte', label: '>=', sql: '>=', valueTemplate: "''", cursorOffset: -1 },
  { key: 'lt', label: '<', sql: '<', valueTemplate: "''", cursorOffset: -1 },
  { key: 'lte', label: '<=', sql: '<=', valueTemplate: "''", cursorOffset: -1 },
  { key: 'in', label: 'IN', sql: 'IN', valueTemplate: "('')", cursorOffset: -2 }, // 光标在 ('') 的引号之间
  { key: 'notin', label: 'NOT IN', sql: 'NOT IN', valueTemplate: "('')", cursorOffset: -2 },
  { key: 'isnull', label: 'IS NULL', sql: 'IS NULL' }, // 无需值模板
  { key: 'isnotnull', label: 'IS NOT NULL', sql: 'IS NOT NULL' }, // 无需值模板
];

const SqlInput: React.FC<ISqlInputProps> = ({ value, onChange, columns = [] }) => {
  const inputRef = useRef<any>(null);

  // 设置光标位置的辅助函数
  const setCursorPosition = (newValue: string, cursorOffset?: number) => {
    setTimeout(() => {
      // 尝试多种可能的路径来获取 input 元素
      const input: HTMLInputElement | null = inputRef.current?.nativeElement?.querySelector('input');
      if (!input) return;
      // 如果没有定义光标偏移，默认定位到末尾
      let cursorPos = newValue.length;
      if (cursorOffset !== undefined) {
        // 负数表示从末尾往前偏移
        cursorPos = newValue.length + cursorOffset;
      }
      input.setSelectionRange(cursorPos, cursorPos);
      input.focus();
    }, 0);
  };

  // 获取当前正在输入的词汇，用于字段匹配
  const getCurrentInputWord = (inputValue: string) => {
    if (!inputValue) return '';

    // 按空格分割，获取最后一个词汇
    const words = inputValue.split(/\s+/);
    const lastWord = words[words.length - 1] || '';

    // 如果最后一个词汇包含操作符，提取字段名部分
    const operatorMatch = lastWord.match(/^([a-zA-Z_][a-zA-Z0-9_.]*)/);
    return operatorMatch ? operatorMatch[1] : lastWord;
  };

  // 处理操作符点击
  const handleOperatorClick = (
    columnName: string,
    operator: string,
    valueTemplate: string | undefined,
    cursorOffset: number | undefined,
    e: React.MouseEvent,
  ) => {
    e.stopPropagation();
    const newValue = buildSqlWithOperator(columnName, operator, valueTemplate);
    onChange(newValue);
    // 设置光标位置
    setCursorPosition(newValue, cursorOffset);
  };

  // 构建带操作符的SQL语句
  const buildSqlWithOperator = (columnName: string, operator: string, valueTemplate?: string): string => {
    if (!columnName) return value || '';

    // 构建SQL片段：字段名 + 操作符 + 值模板（如果有）
    const sqlFragment = valueTemplate ? `${columnName} ${operator} ${valueTemplate}` : `${columnName} ${operator}`;

    // 如果当前输入为空，直接返回SQL片段
    if (!value) return sqlFragment;

    // 如果当前有匹配的词汇，说明用户正在输入字段名，需要替换
    if (currentWord) {
      const words = value.split(/\s+/);
      words[words.length - 1] = sqlFragment;
      return words.join(' ');
    }

    // 如果没有匹配词汇但输入以空格结尾，直接拼接
    if (value.endsWith(' ')) {
      return `${value}${sqlFragment}`;
    }

    // 否则在当前输入和字段名之间添加空格拼接
    return `${value} ${sqlFragment}`;
  };

  // 根据当前输入的词汇筛选字段
  const currentWord = getCurrentInputWord(value);
  const filteredColumns = currentWord
    ? columns.filter(
        (column) => column.columnName && column.columnName.toLowerCase().includes(currentWord.toLowerCase()),
      )
    : columns;

  // AND/OR 逻辑连接符选项（不需要操作按钮）
  const logicalOperators = [
    { key: 'and', label: 'AND', value: 'AND' },
    { key: 'or', label: 'OR', value: 'OR' },
  ];

  // 构建逻辑连接符选项
  const logicalOptions = logicalOperators.map((op) => ({
    value: (() => {
      if (!value) return op.value;
      if (value.endsWith(' ')) return value + op.value;
      return value + ' ' + op.value;
    })(),
    label: (
      <div
        style={{
          padding: '2px 0',
        }}
      >
        <span style={{ fontWeight: 500, color: '#1890ff' }}>{op.label}</span>
      </div>
    ),
  }));

  const options = [
    ...logicalOptions, // AND/OR 选项放在最前面
    ...filteredColumns.map((item: ILogColumnsResponse) => {
      const columnName = item.columnName || '';

      // 更多操作的下拉菜单配置
      const moreMenuItems: MenuProps['items'] = MORE_OPERATORS.map((op) => ({
        key: op.key,
        label: op.label,
        onClick: ({ domEvent }) => {
          domEvent.stopPropagation();
          const newValue = buildSqlWithOperator(columnName, op.sql, op.valueTemplate);
          onChange(newValue);
          // 设置光标位置
          setCursorPosition(newValue, op.cursorOffset);
        },
      }));

      return {
        value: (() => {
          // 智能替换/拼接逻辑
          if (!columnName) return value || '';

          // 如果当前输入为空，直接返回字段名
          if (!value) return columnName;
          // 如果当前有匹配的词汇，说明用户正在输入字段名，需要替换
          if (currentWord) {
            const words = value.split(/\s+/);
            words[words.length - 1] = columnName;
            return words.join(' ');
          }

          // 如果没有匹配词汇但输入以空格结尾，直接拼接
          if (value.endsWith(' ')) {
            return value + columnName;
          }

          // 否则在当前输入和字段名之间添加空格拼接
          return value + ' ' + columnName;
        })(),
        label: (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              gap: '12px',
              padding: '2px 0',
            }}
          >
            <span
              style={{
                flex: 1,
                minWidth: 0,
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
              }}
            >
              {columnName}
            </span>
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '2px',
                flexShrink: 0,
              }}
              onClick={(e) => e.stopPropagation()}
            >
              {SQL_OPERATORS.map((op) => (
                <Tag
                  key={op.key}
                  color="processing"
                  onClick={(e) => handleOperatorClick(columnName, op.sql, op.valueTemplate, op.cursorOffset, e)}
                >
                  {op.label}
                </Tag>
              ))}
              <Dropdown menu={{ items: moreMenuItems }} placement="bottomRight" trigger={['click']}>
                <Tag color="#0038ff" onClick={(e) => e.stopPropagation()}>
                  <DownOutlined />
                </Tag>
              </Dropdown>
            </div>
          </div>
        ),
      };
    }),
  ];

  return (
    <Space.Compact style={{ width: '100%' }}>
      <AutoComplete
        ref={inputRef}
        allowClear
        options={options}
        placeholder={PLACEHOLDERS.SQL}
        style={{ width: '100%' }}
        value={value}
        onChange={onChange}
      />
    </Space.Compact>
  );
};

export default SqlInput;
