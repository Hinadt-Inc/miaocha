import { message } from 'antd';
import * as monaco from 'monaco-editor';
import dayjs from 'dayjs';

/**
 * 在编辑器中的当前位置或选定位置插入文本
 * @param editor Monaco编辑器实例
 * @param text 要插入的文本
 */
export const insertTextToEditor = (
  editor: monaco.editor.IStandaloneCodeEditor,
  text: string
): void => {
  const selection = editor.getSelection();
  const id = { major: 1, minor: 1 };
  const model = editor.getModel();
  
  // 确保 range 是有效的 IRange 对象
  const range = selection 
    ?? (model ? {
      startLineNumber: model.getLineCount(),
      startColumn: model.getLineMaxColumn(model.getLineCount()),
      endLineNumber: model.getLineCount(),
      endColumn: model.getLineMaxColumn(model.getLineCount())
    } : {
      startLineNumber: 1,
      startColumn: 1,
      endLineNumber: 1,
      endColumn: 1
    });
  
  const op = { identifier: id, range, text, forceMoveMarkers: true };
  editor.executeEdits('insert-text', [op]);
  editor.focus();
};

/**
 * 复制文本到剪贴板
 * @param text 要复制的文本
 */
export const copyToClipboard = (text: string) => {
  navigator.clipboard.writeText(text)
    .then(() => message.success('已复制到剪贴板'))
    .catch(err => {
      console.error('复制失败:', err);
      message.error('复制失败');
    });
};

/**
 * 下载查询结果
 * @param content 要下载的内容
 * @param fileName 文件名
 * @param type MIME类型
 */
export const downloadResults = (
  content: string,
  fileName: string,
  type = 'text/csv;charset=utf-8;'
) => {
  const blob = new Blob([content], { type });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
};

/**
 * 将查询结果下载为CSV文件
 * @param rows 查询结果行数据
 * @param columns 查询结果列名
 */
export const downloadAsCSV = (
  rows: Record<string, any>[],
  columns: string[]
): void => {
  // 构造 CSV 内容
  const header = columns.join(',');
  const csvRows = rows.map(row => {
    return columns.map(col => {
      const value = row[col];
      if (value === null || value === undefined) return '';
      if (typeof value === 'string') return `"${value.replace(/"/g, '""')}"`;
      if (typeof value === 'object') return `"${JSON.stringify(value).replace(/"/g, '""')}"`;
      return String(value);
    }).join(',');
  });
  
  const csvContent = [header, ...csvRows].join('\n');
  
  // 创建 Blob 对象
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  
  // 创建下载链接
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', `query_results_${dayjs().format('YYYYMMDD_HHmmss')}.csv`);
  document.body.appendChild(link);
  
  // 触发下载
  link.click();
  
  // 清理
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
};

/**
 * 验证SQL查询语句的有效性
 * @param query SQL查询语句
 * @returns 是否是有效的SQL查询
 */
export const validateSQL = (query: string): boolean => {
  if (!query.trim()) {
    return false;
  }
  
  // 这里可以添加更复杂的SQL验证逻辑
  // 例如检查是否包含基本的SQL关键字：SELECT, FROM等
  const hasSqlKeywords = /\b(SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP)\b/i.test(query);
  
  return hasSqlKeywords;
};

/**
 * 保存历史记录到本地存储
 * @param key 存储键名
 * @param data 要保存的数据
 * @param maxCount 最大保存数量
 */
export const saveToLocalStorage = <T,>(
  key: string,
  data: T[],
  maxCount: number
) => {
  try {
    const toSave = data.slice(0, maxCount);
    localStorage.setItem(key, JSON.stringify(toSave));
  } catch (error) {
    console.error('保存到本地存储失败:', error);
  }
};
