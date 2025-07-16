import { useCallback, useRef, useEffect, useMemo } from 'react';
import { message } from 'antd';
import { debounce } from 'lodash';
import copy from 'copy-to-clipboard';
import * as monaco from 'monaco-editor';
import { downloadAsCSV, insertTextToEditor, getSQLContext } from '../utils/editorUtils';
import { QueryResult } from '../types';
import { useSQLCompletion } from './useSQLCompletion';
import type { useOptimizedSQLEditorState } from './useOptimizedSQLEditorState';

type OptimizedSQLEditorState = ReturnType<typeof useOptimizedSQLEditorState>;

/**
 * 优化的SQL编辑器操作管理Hook
 * 支持新的数据库结构管理
 */
export const useOptimizedSQLEditorActions = (editorState: OptimizedSQLEditorState) => {
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
    fetchDatabaseTables,
    fetchTableSchema,
    databaseSchema,
  } = editorState;

  // 初始化 SQL 补全功能，兼容扩展的数据库结构
  const compatibleSchema = useMemo(() => {
    if (!databaseSchema) return null;
    if ('error' in databaseSchema) return null;
    
    // 转换为兼容的格式
    return {
      databaseName: databaseSchema.databaseName,
      tables: databaseSchema.tables
        .filter(table => table.columns && table.columns.length > 0)
        .map(table => ({
          tableName: table.tableName,
          tableComment: table.tableComment,
          columns: table.columns || [],
        })),
    };
  }, [databaseSchema]);

  const { registerCompletionProvider } = useSQLCompletion(compatibleSchema);
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

  // 防抖执行查询的内部函数
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

      // 过滤掉结尾的换行符，避免接口报错
      queryToExecute = queryToExecute.replace(/\n+$/, '');

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

  // 使用防抖的查询执行
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
      if (editorRef.current) {
        insertTextToEditor(editorRef.current, historySql);
      }
      message.success('已加载历史查询');
    },
    [setSqlQuery],
  );

  // 复制到剪贴板
  const copyToClipboard = useCallback((text: string) => {
    if (copy(text)) {
      message.success('已复制到剪贴板');
    } else {
      message.error('复制失败');
    }
  }, []);

  // 处理编辑器挂载
  const handleEditorDidMount = useCallback(
    (editor: monaco.editor.IStandaloneCodeEditor, monaco: typeof import('monaco-editor')) => {
      editorRef.current = editor;
      monacoRef.current = monaco;

      // 注册SQL补全功能
      if (compatibleSchema) {
        if (completionProviderRef.current) {
          completionProviderRef.current.dispose();
        }
        completionProviderRef.current = registerCompletionProvider();
      }
    },
    [compatibleSchema, registerCompletionProvider],
  );

  // 保存编辑器设置
  const saveEditorSettings = useCallback(
    (settings: any) => {
      saveSettings(settings);
      message.success('设置已保存');
    },
    [saveSettings],
  );

  // 插入SQL片段
  const insertSnippet = useCallback((snippet: string) => {
    if (editorRef.current) {
      const position = editorRef.current.getPosition();
      if (position) {
        editorRef.current.executeEdits('insert-snippet', [
          {
            range: new (editorRef.current.getModel()?.constructor as any).Range(
              position.lineNumber,
              position.column,
              position.lineNumber,
              position.column,
            ),
            text: snippet,
            forceMoveMarkers: true,
          },
        ]);
        editorRef.current.focus();
      }
    }
  }, []);

  // 插入字段名
  const handleInsertField = useCallback(
    (fieldName: string) => {
      if (editorRef.current) {
        const position = editorRef.current.getPosition();
        const model = editorRef.current.getModel();
        if (position && model) {
          const context = getSQLContext(editorRef.current);
          let textToInsert = fieldName;

          // 简化的插入逻辑
          if (context.isInSelectClause) {
            textToInsert = fieldName;
          }

          insertTextToEditor(editorRef.current, textToInsert);
          message.success(`已插入字段: ${textToInsert}`);
        }
      }
    },
    [],
  );

  // 兼容的插入表操作 - 支持随时插入表名，无需预先加载列信息
  const handleInsertTable = useCallback(
    (tableName: string, _columns?: {
        columnName: string;
        dataType: string;
        columnComment: string;
        isPrimaryKey: boolean;
        isNullable: boolean;
      }[]) => {
      if (editorRef.current) {
        const context = getSQLContext(editorRef.current);
        let textToInsert = tableName;

        // 根据上下文智能插入
        if (context.isInFromClause) {
          // 在FROM子句中，只插入表名
          textToInsert = tableName;
        } else {
          // 在其他位置，插入完整的SELECT语句
          // 注意：_columns参数保留是为了向后兼容，当前版本统一使用SELECT * FROM
          textToInsert = `SELECT * FROM ${tableName}`;
        }

        insertTextToEditor(editorRef.current, textToInsert);
        message.success(`已插入表: ${tableName}`);
      }
    },
    [],
  );

  // 分页处理
  const handlePaginationChange = useCallback(
    (page: number, pageSize: number) => {
      editorState.handlePaginationChange(page, pageSize);
    },
    [], // 移除editorState依赖，因为handlePaginationChange函数引用应该是稳定的
  );

  return {
    // 基础操作
    executeQuery,
    handleDownloadResults,
    loadFromHistory,
    copyToClipboard,

    // 编辑器操作
    handleEditorDidMount,
    saveEditorSettings,
    insertSnippet,
    handleInsertField,
    handleInsertTable,

    // 数据库操作
    fetchDatabaseSchema,
    fetchDatabaseTables,
    fetchTableSchema,

    // 分页
    handlePaginationChange,

    // 状态设置
    setSelectedSource: editorState.setSelectedSource,
    setSqlQuery,
    setActiveTab,
    setChartType: editorState.setChartType,
    setXField,
    setYField,
    setHistoryDrawerVisible: editorState.setHistoryDrawerVisible,
    setSettingsDrawerVisible: editorState.setSettingsDrawerVisible,
  };
};
