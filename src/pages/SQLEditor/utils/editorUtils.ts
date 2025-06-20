// filepath: /Users/zhangyongjian/project/log-manage-web/src/pages/SQLEditor/utils/editorUtils.ts
import { message } from 'antd';
import { EditorView } from '@codemirror/view';
import dayjs from 'dayjs';
import { executeSQL, downloadSqlResult } from '@/api/sql';
import { insertTextToCodeMirror, getSQLContext as getCodeMirrorSQLContext } from './codeMirrorUtils';

export type CSVRowData = Record<string, string | number | boolean | null | undefined | object>;

/**
 * 在编辑器中的当前位置或选定位置插入文本
 * @param editor CodeMirror编辑器实例
 * @param text 要插入的文本
 */
export const insertTextToEditor = (editor: EditorView, text: string): void => {
  insertTextToCodeMirror(editor, text);
};

/**
 * 在编辑器中的当前位置或选定位置插入文本，支持高级SQL格式
 * @param editor CodeMirror编辑器实例
 * @param text 要插入的文本
 * @param options 格式化选项
 */
export const insertFormattedSQL = (
  editor: EditorView,
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
 * 获取编辑器当前上下文环境
 * @param editor 编辑器实例
 * @returns 光标所在位置的上下文
 */
export const getSQLContext = (editor: EditorView): string => {
  return getCodeMirrorSQLContext(editor);
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
 * 检查是否为完整的SQL语句
 * @param sql SQL语句
 * @returns 是否是完整语句
 */
export const isCompleteSQLStatement = (sql: string): boolean => {
  const trimmed = sql.trim();
  // 简单检查：以分号结尾或包含完整SELECT/FROM结构
  return (
    trimmed.endsWith(';') ||
    (/SELECT\s+.+\s+FROM\s+.+/i.test(trimmed) && !/SELECT\s+.+\s+FROM\s+.+\s+WHERE\s*$/i.test(trimmed))
  );
};

/**
 * 从编辑器中提取选中的SQL语句
 * @param editor CodeMirror编辑器实例
 * @returns 选中的SQL语句或当前光标所在语句
 */
export const getSelectedSQL = (editor: EditorView): string => {
  if (!editor || !editor.state) return '';

  let text = '';
  const selection = editor.state.selection;

  if (!selection.main.empty) {
    const from = selection.main.from;
    const to = selection.main.to;
    text = editor.state.doc.sliceString(from, to);
  }

  return text;
};

/**
 * 从编辑器中提取选中的SQL语句或光标所在语句，支持多条SQL语句中提取单条
 * @param editor CodeMirror编辑器实例
 * @returns 选中的SQL语句或当前光标所在完整语句
 */
export const getSelectedSQLStatement = (editor: EditorView): string => {
  const selectedText = getSelectedSQL(editor);

  if (selectedText) {
    // 如果选中文本包含完整SQL语句(有分号或完整SELECT/FROM结构)，则直接返回
    if (isCompleteSQLStatement(selectedText)) {
      return selectedText;
    }
  }

  // 如果没有选中文本或选中文本不是完整语句，则获取当前光标位置的完整SQL语句
  return getCodeMirrorSQLContext(editor);
};

/**
 * 将查询结果下载为CSV文件
 * @param data 表格数据
 * @param filename 文件名(可选)
 */
export const downloadAsCSV = async (
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
      const res = await downloadSqlResult(response.downloadUrl);
      const result = res.data;
      console.log('下载结果:', result, result instanceof Blob);
      if (exportFormat === 'csv') {
        const arrayBuffer = result instanceof Blob ? await result.arrayBuffer() : result;
        const csvContent = new TextDecoder().decode(arrayBuffer);
        await downloadResults(csvContent, `query_result_${dayjs().format('YYYYMMDD_HHmmss')}.csv`);
      } else if (exportFormat === 'xlsx') {
        const fileName = `query_result_${dayjs().format('YYYYMMDD_HHmmss')}.xlsx`;
        if (result instanceof Blob) {
          // 直接使用返回的Blob，不要再次封装
          await downloadResults(result, fileName, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
        } else {
          // 如果不是Blob，是ArrayBuffer，则需要创建新的Blob
          await downloadResults(
            new Blob([result]),
            fileName,
            'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
          );
        }
      }
    }
  } catch (error) {
    console.error('导出失败:', error);
    message.error('导出失败');
    throw error;
  }
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
