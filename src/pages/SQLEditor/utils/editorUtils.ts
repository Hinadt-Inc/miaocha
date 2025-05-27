import { message } from 'antd';
import * as monaco from 'monaco-editor';
import dayjs from 'dayjs';
import { executeSQL, downloadSqlResult } from '@/api/sql';

export type CSVRowData = Record<string, string | number | boolean | null | undefined | object>;

/**
 * 在编辑器中的当前位置或选定位置插入文本
 * @param editor Monaco编辑器实例
 * @param text 要插入的文本
 */
export const insertTextToEditor = (editor: monaco.editor.IStandaloneCodeEditor, text: string): void => {
  const selection = editor.getSelection();
  const id = { major: 1, minor: 1 };
  const model = editor.getModel();

  // 确保 range 是有效的 IRange 对象
  const range =
    selection ??
    (model
      ? {
          startLineNumber: model.getLineCount(),
          startColumn: model.getLineMaxColumn(model.getLineCount()),
          endLineNumber: model.getLineCount(),
          endColumn: model.getLineMaxColumn(model.getLineCount()),
        }
      : {
          startLineNumber: 1,
          startColumn: 1,
          endLineNumber: 1,
          endColumn: 1,
        });

  const op = { identifier: id, range, text, forceMoveMarkers: true };
  editor.executeEdits('insert-text', [op]);
  editor.focus();
};

/**
 * 在编辑器中的当前位置或选定位置插入文本，支持高级SQL格式
 * @param editor Monaco编辑器实例
 * @param text 要插入的文本
 * @param options 格式化选项
 */
export const insertFormattedSQL = (
  editor: monaco.editor.IStandaloneCodeEditor,
  text: string,
  options?: {
    useTabs?: boolean; // 使用Tab而不是空格
    indentSize?: number; // 缩进大小
    addComma?: boolean; // 是否添加逗号
    addNewLine?: boolean; // 是否添加新行
  },
): void => {
  // 处理默认选项
  const { addComma = false, addNewLine = false } = options ?? {};

  // 准备缩进字符和文本
  let formattedText = text;

  // 如果需要添加逗号
  if (addComma) {
    formattedText = formattedText + ',';
  }

  // 如果需要添加新行
  if (addNewLine) {
    formattedText = formattedText + '\n';
  }

  // 使用基础方法插入
  insertTextToEditor(editor, formattedText);
};

/**
 * 复制文本到剪贴板
 * @param text 要复制的文本
 */
export const copyToClipboard = (text: string) => {
  navigator.clipboard
    .writeText(text)
    .then(() => message.success('已复制到剪贴板'))
    .catch((err) => {
      console.error('复制失败:', err);
      message.error('复制失败');
    });
};

/**
 * 下载查询结果
 * @param content 要下载的内容(字符串或Blob对象)
 * @param fileName 文件名
 * @param type MIME类型(仅当content为字符串时使用)
 */
export const downloadResults = async (
  content: string | ArrayBuffer | Blob,
  fileName: string,
  type = 'text/csv;charset=utf-8;',
) => {
  let blob: Blob;
  if (content instanceof Blob) {
    blob = content;
  } else if (content instanceof ArrayBuffer) {
    blob = new Blob([content], { type });
  } else {
    blob = new Blob([content], { type });
  }
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
export const downloadAsCSV = async (
  rows: CSVRowData[],
  columns: (keyof CSVRowData)[],
  datasourceId: string,
  sqlQuery: string,
  exportFormat: 'csv' | 'xlsx' = 'csv',
  exportResult = true,
): Promise<void> => {
  try {
    const response = await executeSQL({
      datasourceId,
      sql: sqlQuery,
      exportFormat,
      exportResult,
    });

    if (response.downloadUrl) {
      const result = await downloadSqlResult(response.downloadUrl);
      if (exportFormat === 'csv') {
        const arrayBuffer = result instanceof Blob ? await result.arrayBuffer() : result;
        const csvContent = new TextDecoder().decode(arrayBuffer);
        await downloadResults(csvContent, `query_result_${dayjs().format('YYYYMMDD_HHmmss')}.csv`);
      } else if (exportFormat === 'xlsx') {
        const fileName = `query_result_${dayjs().format('YYYYMMDD_HHmmss')}.xlsx`;
        const arrayBuffer = result instanceof Blob ? await result.arrayBuffer() : result;
        await downloadResults(
          new Blob([arrayBuffer]),
          fileName,
          'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        );
      }
    }
  } catch (error) {
    console.error('导出失败:', error);
    message.error('导出失败');
    throw error;
  }
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
 * SQL关键字检测器
 * @param text 要检测的文本
 * @param keyword 关键字
 * @returns 是否包含关键字
 */
export const containsSQLKeyword = (text: string, keyword: string): boolean => {
  const regex = new RegExp(`\\b${keyword}\\b`, 'i');
  return regex.test(text);
};

/**
 * 获取当前SQL查询中的上下文位置
 * @param editor 编辑器实例
 * @returns SQL上下文信息
 */
export const getSQLContext = (
  editor: monaco.editor.IStandaloneCodeEditor,
): {
  isSelectQuery: boolean;
  hasFromClause: boolean;
  hasWhereClause: boolean;
  isInSelectClause: boolean;
  isInFromClause: boolean;
  isInWhereClause: boolean;
  cursorOffsetInSQL: number;
} => {
  const model = editor.getModel();
  if (!model) {
    return {
      isSelectQuery: false,
      hasFromClause: false,
      hasWhereClause: false,
      isInSelectClause: false,
      isInFromClause: false,
      isInWhereClause: false,
      cursorOffsetInSQL: 0,
    };
  }

  const text = model.getValue();
  const selection = editor.getSelection();

  // 获取光标位置的偏移量
  const cursorOffsetInSQL = selection
    ? model.getOffsetAt({
        lineNumber: selection.startLineNumber,
        column: selection.startColumn,
      })
    : 0;

  // 使用正则表达式检测SQL关键字
  const isSelectQuery = /\bSELECT\b/i.test(text);
  const hasFromClause = /\bFROM\b/i.test(text);
  const hasWhereClause = /\bWHERE\b/i.test(text);

  // 获取关键字的位置
  const selectPos = text.toUpperCase().indexOf('SELECT');
  const fromPos = text.toUpperCase().indexOf('FROM');
  const wherePos = text.toUpperCase().indexOf('WHERE');

  // 判断光标是否在特定子句中
  const isInSelectClause =
    isSelectQuery && hasFromClause && cursorOffsetInSQL > selectPos && cursorOffsetInSQL < fromPos;

  const isInFromClause =
    hasFromClause && cursorOffsetInSQL > fromPos && (!hasWhereClause || cursorOffsetInSQL < wherePos);

  const isInWhereClause = hasWhereClause && cursorOffsetInSQL > wherePos;

  return {
    isSelectQuery,
    hasFromClause,
    hasWhereClause,
    isInSelectClause,
    isInFromClause,
    isInWhereClause,
    cursorOffsetInSQL,
  };
};

/**
 * 生成SQL字段列表字符串
 * @param columns 字段信息列表
 * @param options 格式化选项
 * @returns 格式化后的字段列表
 */
export const generateColumnList = (
  columns: {
    columnName: string;
    dataType?: string;
    columnComment?: string;
    isPrimaryKey?: boolean;
    isNullable?: boolean;
  }[],
  options?: {
    addComments?: boolean;
    indentSize?: number;
    multiline?: boolean;
  },
): string => {
  const { addComments = false, indentSize = 4, multiline = true } = options ?? {};

  const indent = ' '.repeat(indentSize);

  if (columns.length === 0) {
    return '*';
  }

  const formattedColumns = columns.map((col) => {
    let field = col.columnName;
    if (addComments && col.columnComment) {
      field += ` /* ${col.columnComment} */`;
    }
    return multiline ? `${indent}${field}` : field;
  });

  if (multiline) {
    return formattedColumns.join(',\n');
  }
  return formattedColumns.join(', ');
};

/**
 * 保存历史记录到本地存储
 * @param key 存储键名
 * @param data 要保存的数据
 * @param maxCount 最大保存数量
 */
export const saveToLocalStorage = <T>(key: string, data: T[], maxCount: number) => {
  try {
    const toSave = data.slice(0, maxCount);
    localStorage.setItem(key, JSON.stringify(toSave));
  } catch (error) {
    console.error('保存到本地存储失败:', error);
  }
};
