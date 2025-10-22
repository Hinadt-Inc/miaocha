import { useCallback, useRef, useEffect, useMemo } from 'react';

import { message } from 'antd';
import copy from 'copy-to-clipboard';
import * as monaco from 'monaco-editor';

import { debounce } from '@/utils/utils';

import { QueryResult } from '../types';
import { downloadAsCSV, insertTextToEditor, getSQLContext, generateColumnList } from '../utils/editorUtils';

import { useSQLCompletion } from './useSQLCompletion';
import type { useSQLEditorState } from './useSQLEditorState';

type SQLEditorState = ReturnType<typeof useSQLEditorState>;

/**
 * SQL编辑器操作管理Hook
 * 管理所有用户操作和业务逻辑
 */
export const useSQLEditorActions = (editorState: SQLEditorState) => {
  const {
    selectedSource,
    sqlQuery,
    setSqlQuery,
    executeQueryOriginal,
    setActiveTab,
    setXField,
    setYField,
    editorRef,
    monacoRef,
    saveSettings,
    fetchDatabaseSchema,
    setQueryResults,
    databaseSchema,
  } = editorState;

  // 初始化 SQL 补全功能
  const { registerCompletionProvider } = useSQLCompletion(databaseSchema);
  const completionProviderRef = useRef<monaco.IDisposable | null>(null);

  // 计算SQL语句数量的辅助函数
  const countSQLStatements = useCallback((text: string): number => {
    if (!text.trim()) return 0;

    // 移除注释和字符串中的分号，避免误判
    let cleanText = text;

    // 移除单行注释
    cleanText = cleanText.replace(/--.*$/gm, '');

    // 移除多行注释
    cleanText = cleanText.replace(/\/\*[\s\S]*?\*\//g, '');

    // 移除字符串中的分号（简单处理）
    cleanText = cleanText.replace(/'[^']*'/g, '');
    cleanText = cleanText.replace(/"[^"]*"/g, '');

    // 按分号分割并计算有效语句
    const statements = cleanText
      .split(';')
      .map((stmt) => stmt.trim())
      .filter((stmt) => stmt.length > 0);

    return statements.length;
  }, []);

  // 检查SQL语法有效性
  const validateSQL = useCallback((query: string): boolean => {
    if (!query.trim()) {
      message.warning('请输入SQL查询语句');
      return false;
    }
    return true;
  }, []);

  // 防抖执行查询的内部函数 - 使用useMemo稳定依赖
  const executeQueryInternal = useCallback(() => {
    try {
      if (!selectedSource) {
        message.warning('请先选择数据源');
        return;
      }

      // 获取要执行的SQL语句
      let queryToExecute = '';

      if (editorRef.current) {
        const selection = editorRef.current.getSelection();
        const model = editorRef.current.getModel();
        if (model) {
          const fullText = model.getValue();

          // 检查是否有选中文本
          if (selection && !selection.isEmpty()) {
            queryToExecute = model.getValueInRange(selection);
          } else {
            const statementCount = countSQLStatements(fullText);

            if (statementCount === 1) {
              queryToExecute = fullText;
            } else if (statementCount > 1) {
              message.warning('检测到多条SQL语句，请选中要执行的语句');
              return;
            } else {
              queryToExecute = fullText;
            }
          }
        }
      }

      // 如果编辑器获取不到内容，则使用状态中的 sqlQuery
      if (!queryToExecute.trim()) {
        queryToExecute = sqlQuery;
      }

      // 验证SQL非空
      if (!validateSQL(queryToExecute)) return;

      // 自动添加分号
      if (queryToExecute.trim() && !queryToExecute.trim().endsWith(';')) {
        queryToExecute = queryToExecute + ';';
      }

      setActiveTab('results');

      executeQueryOriginal({
        datasourceId: selectedSource,
        sql: queryToExecute,
        selectedText: queryToExecute,
        editor: editorRef.current,
      })
        .then((results: QueryResult) => {
          if (results?.rows?.length && results?.columns) {
            setXField(results.columns[0]);
            const numericColumn = results.columns.find((col: string) => {
              const sampleValue = results.rows?.[0][col];
              return typeof sampleValue === 'number';
            });
            setYField(numericColumn ?? results.columns[1] ?? results.columns[0]);
          }
        })
        .catch((error: Error) => {
          console.error('执行查询失败:', error);
          message.error(`执行查询失败: ${error.message}`);
        });
    } catch (error) {
      console.error('执行查询过程中发生未捕获的异常:', error);
      message.error('执行查询时发生未知错误');
    }
  }, [
    selectedSource,
    sqlQuery,
    countSQLStatements,
    validateSQL,
    setActiveTab,
    executeQueryOriginal,
    setXField,
    setYField,
  ]);

  // 使用防抖的查询执行 - 使用useMemo稳定debounce函数
  const debouncedExecuteQuery = useMemo(() => debounce(executeQueryInternal, 300), [executeQueryInternal]);

  // 清理防抖函数
  useEffect(() => {
    return () => {
      debouncedExecuteQuery.cancel();
    };
  }, [debouncedExecuteQuery]);

  const executeQuery = useCallback(() => {
    debouncedExecuteQuery();
  }, [debouncedExecuteQuery]);

  // 下载查询结果为 CSV
  const handleDownloadResults = useCallback(() => {
    const { queryResults } = editorState;
    if (!queryResults?.rows?.length || !queryResults.columns) {
      message.warning('没有可下载的结果');
      return;
    }

    try {
      if (!selectedSource || !sqlQuery) {
        message.warning('缺少必要参数');
        return;
      }

      downloadAsCSV(selectedSource, sqlQuery, 'csv');
      message.success('下载已开始');
    } catch (error) {
      console.error('下载失败:', error);
      message.error('下载失败');
    }
  }, [editorState, selectedSource, sqlQuery]);

  // 从历史记录加载查询
  const loadFromHistory = useCallback(
    (historySql: string) => {
      setSqlQuery(historySql);
      editorState.setHistoryDrawerVisible(false);
    },
    [setSqlQuery, editorState],
  );

  // 复制到剪贴板
  const copyToClipboard = useCallback((text: string) => {
    copy(text);
    message.success('已复制到剪贴板');
  }, []);

  // SQL片段插入
  const insertSnippet = useCallback(
    (snippet: string) => {
      if (editorRef.current) {
        editorRef.current.focus();
        const selection = editorRef.current.getSelection();
        const model = editorRef.current.getModel();

        if (model && selection) {
          editorRef.current.executeEdits('insert-snippet', [{ range: selection, text: snippet }]);
          const position = editorRef.current.getPosition();
          if (position) {
            editorRef.current.setPosition(position);
          }
          setSqlQuery(model.getValue());
        }
      }
    },
    [setSqlQuery],
  );

  // 处理树节点双击
  const handleTreeNodeDoubleClick = useCallback(
    (tableName: string) => {
      if (editorRef.current) {
        insertTextToEditor(editorRef.current, tableName);
        const model = editorRef.current.getModel();
        if (model) {
          setSqlQuery(model.getValue());
        }
      }
    },
    [setSqlQuery],
  );

  // 插入字段
  const handleInsertField = useCallback(
    (fieldName: string) => {
      if (editorRef.current) {
        insertTextToEditor(editorRef.current, fieldName);
        const model = editorRef.current.getModel();
        if (model) {
          setSqlQuery(model.getValue());
        }
      }
    },
    [setSqlQuery],
  );

  // 插入表格
  const handleInsertTable = useCallback(
    (
      tableName: string,
      columns: {
        columnName: string;
        dataType: string;
        columnComment: string;
        isPrimaryKey: boolean;
        isNullable: boolean;
      }[],
    ) => {
      if (!editorRef.current) return;
      const editor = editorRef.current;
      const safeTableName = tableName.replace(/[^\w]/g, '');
      const sqlContext = getSQLContext(editor);

      try {
        if (!sqlContext.isSelectQuery || editor.getModel()?.getValue().trim() === '') {
          if (columns.length > 0) {
            const fieldList = generateColumnList(columns, {
              addComments: true,
              indentSize: 4,
              multiline: true,
            });
            insertTextToEditor(editor, `SELECT\n${fieldList}\nFROM ${safeTableName};`);
          } else {
            insertTextToEditor(editor, `SELECT * FROM ${safeTableName};`);
          }
          return;
        }

        // 其他复杂的SQL上下文处理逻辑...
        insertTextToEditor(editor, safeTableName);
      } catch (error) {
        console.error('SQL插入错误:', error);
        insertTextToEditor(editor, safeTableName);
      } finally {
        const model = editor.getModel();
        if (model) {
          setSqlQuery(model.getValue());
        }
      }
    },
    [setSqlQuery],
  );

  // 包装数据源设置函数，在数据源变化时确保数据库结构自动加载
  const handleSetSelectedSource = useCallback(
    (sourceId: string) => {
      editorState.setSelectedSource(sourceId);
      // 数据源变化后，会在 useDatabaseSchema 的 useEffect 中自动触发 fetchDatabaseSchema
    },
    [editorState],
  );

  // 编辑器挂载事件 - 修复内容监听导致的循环更新
  const handleEditorDidMount = useCallback(
    (editor: monaco.editor.IStandaloneCodeEditor, monacoInstance: typeof monaco) => {
      editorRef.current = editor;
      monacoRef.current = monacoInstance;

      // 添加快捷键：Ctrl+Enter 执行查询
      editor.addCommand(monacoInstance.KeyMod.CtrlCmd | monacoInstance.KeyCode.Enter, executeQuery);

      // 注册 SQL 补全提供器
      try {
        // 清理之前的补全提供器
        if (completionProviderRef.current) {
          completionProviderRef.current.dispose();
        }

        // 注册新的补全提供器
        completionProviderRef.current = registerCompletionProvider();
        console.log('✅ SQL补全提供器已注册');
      } catch (error) {
        console.warn('⚠️ SQL补全提供器注册失败:', error);
      }

      // 监听编辑器内容变化 - 使用防抖避免频繁更新
      const model = editor.getModel();
      if (model) {
        const debouncedContentChange = debounce((content: string) => {
          setSqlQuery(content);
        }, 300);

        const disposable = model.onDidChangeContent(() => {
          const currentValue = model.getValue();
          debouncedContentChange(currentValue);
        });

        // 清理函数
        return () => {
          disposable.dispose();
          debouncedContentChange.cancel();
        };
      }
    },
    [executeQuery, setSqlQuery, registerCompletionProvider],
  );

  // 清理补全提供器
  useEffect(() => {
    return () => {
      if (completionProviderRef.current) {
        completionProviderRef.current.dispose();
        completionProviderRef.current = null;
      }
    };
  }, []);

  return {
    // 数据源操作
    setSelectedSource: handleSetSelectedSource,

    // 查询操作
    setSqlQuery,
    executeQuery,
    setQueryResults,

    // UI操作
    setActiveTab: editorState.setActiveTab,
    setChartType: editorState.setChartType,
    setXField: editorState.setXField,
    setYField: editorState.setYField,
    setHistoryDrawerVisible: editorState.setHistoryDrawerVisible,
    setSettingsDrawerVisible: editorState.setSettingsDrawerVisible,

    // 编辑器操作
    saveEditorSettings: saveSettings,
    handleEditorDidMount,

    // 其他操作
    fetchDatabaseSchema,
    handleDownloadResults,
    loadFromHistory,
    copyToClipboard,
    handlePaginationChange: editorState.handlePaginationChange,
    insertSnippet,
    handleTreeNodeDoubleClick,
    handleInsertField,
    handleInsertTable,
  };
};
