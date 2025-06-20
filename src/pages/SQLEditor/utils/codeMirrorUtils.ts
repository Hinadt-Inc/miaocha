import { EditorView } from '@codemirror/view';
import dayjs from 'dayjs';
import { message } from 'antd';
import { executeSQL, downloadSqlResult } from '@/api/sql';
import { CSVRowData } from './editorUtils';

/**
 * 在 CodeMirror 编辑器中的当前位置或选定位置插入文本
 * @param editor CodeMirror EditorView 实例
 * @param text 要插入的文本
 */
export const insertTextToCodeMirror = (editor: EditorView, text: string): void => {
  if (!editor || !editor.state) return;

  const selection = editor.state.selection;
  const changes = selection.ranges.map((range) => {
    return {
      from: range.from,
      to: range.to,
      insert: text,
    };
  });

  if (changes.length === 0) {
    // 如果没有选定范围，在当前光标位置插入
    const cursorPos = editor.state.selection.main.head;
    changes.push({
      from: cursorPos,
      to: cursorPos,
      insert: text,
    });
  }

  editor.dispatch({ changes });
  editor.focus();
};

/**
 * 在 CodeMirror 编辑器中的当前位置或选定位置插入格式化的 SQL
 * @param editor CodeMirror EditorView 实例
 * @param text 要插入的文本
 * @param options 格式化选项
 */
export const insertFormattedSQL = (
  editor: EditorView,
  text: string,
  options?: {
    useTabs?: boolean;
    indentSize?: number;
  },
): void => {
  // 简单实现，实际上可以使用 SQL 格式化库
  insertTextToCodeMirror(editor, text);
};

/**
 * 获取当前 SQL 上下文（当前光标所在的 SQL 语句）
 * @param editor CodeMirror EditorView 实例
 * @returns 当前 SQL 语句
 */
export const getSQLContext = (editor: EditorView): string => {
  if (!editor || !editor.state) return '';

  const doc = editor.state.doc;
  const cursorPos = editor.state.selection.main.head;

  // 找到当前光标所在的行
  const line = doc.lineAt(cursorPos);

  // 简单实现：返回当前行
  return line.text;
};

/**
 * 根据列名列表生成逗号分隔的列名字符串
 * @param columns 列名数组
 * @returns 逗号分隔的列名字符串
 */
export const generateColumnList = (columns: string[]): string => {
  if (!columns || columns.length === 0) return '*';
  return columns.join(', ');
};

/**
 * 将查询结果下载为CSV文件
 * @param data 表格数据
 * @param filename 文件名
 */
export const downloadAsCSV = (data: CSVRowData[], filename?: string): void => {
  if (!data || data.length === 0) {
    message.error('没有数据可供下载');
    return;
  }

  try {
    // 获取所有可能的列
    const allColumns = new Set<string>();
    data.forEach((row) => {
      Object.keys(row).forEach((key) => allColumns.add(key));
    });

    // 转换为列数组
    const columns = Array.from(allColumns);

    // 创建CSV内容
    let csvContent = columns.join(',') + '\n';

    // 添加每一行数据
    data.forEach((row) => {
      const rowData = columns.map((column) => {
        const value = row[column];
        // 处理不同类型的值
        if (value === null || value === undefined) {
          return '';
        } else if (typeof value === 'object') {
          return `"${JSON.stringify(value).replace(/"/g, '""')}"`;
        } else {
          return `"${String(value).replace(/"/g, '""')}"`;
        }
      });
      csvContent += rowData.join(',') + '\n';
    });

    // 创建Blob并下载
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    const defaultFilename = `query_results_${dayjs().format('YYYY-MM-DD_HH-mm-ss')}.csv`;

    link.setAttribute('href', url);
    link.setAttribute('download', filename || defaultFilename);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  } catch (error) {
    console.error('下载CSV文件时出错:', error);
    message.error('下载CSV文件时出错');
  }
};
