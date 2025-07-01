import { useRef, useState } from 'react';
import { useDataSources } from './useDataSources';
import { useQueryExecution } from './useQueryExecution';
import { useEditorSettings } from './useEditorSettings';
import { useQueryHistory } from './useQueryHistory';
import { useDatabaseSchema } from './useDatabaseSchema';
import { ChartType } from '../types';
import * as monaco from 'monaco-editor';

/**
 * SQL编辑器主状态管理Hook
 * 整合所有子状态，提供统一的状态接口
 */
export const useSQLEditorState = () => {
  // 编辑器引用
  const editorRef = useRef<monaco.editor.IStandaloneCodeEditor | null>(null);
  const monacoRef = useRef<typeof monaco | null>(null);

  // 原有的hooks
  const { dataSources, selectedSource, setSelectedSource, loading: loadingDataSources } = useDataSources();
  const { databaseSchema: rawDatabaseSchema, loadingSchema, fetchDatabaseSchema } = useDatabaseSchema(selectedSource);

  // 处理数据库schema的错误状态，确保类型安全
  const databaseSchema = rawDatabaseSchema && 'tables' in rawDatabaseSchema ? rawDatabaseSchema : null;

  const {
    queryResults,
    sqlQuery,
    setSqlQuery,
    loading: loadingResults,
    executeQuery: executeQueryOriginal,
    setQueryResults,
  } = useQueryExecution(selectedSource);
  const { settings: editorSettings, saveSettings } = useEditorSettings();
  const { history: queryHistory, pagination, handlePaginationChange } = useQueryHistory(selectedSource);

  // 本地UI状态
  const [activeTab, setActiveTab] = useState<string>('results');
  const [chartType, setChartType] = useState<ChartType>(ChartType.Bar);
  const [xField, setXField] = useState<string>('');
  const [yField, setYField] = useState<string>('');
  const [fullscreen, setFullscreen] = useState<boolean>(false);
  const [historyDrawerVisible, setHistoryDrawerVisible] = useState(false);
  const [settingsDrawerVisible, setSettingsDrawerVisible] = useState(false);

  return {
    // 编辑器引用
    editorRef,
    monacoRef,

    // 数据源相关
    dataSources,
    selectedSource,
    setSelectedSource,
    loadingDataSources,

    // 数据库结构相关
    databaseSchema,
    loadingSchema,
    fetchDatabaseSchema,

    // 查询相关
    queryResults,
    sqlQuery,
    setSqlQuery,
    loadingResults,
    executeQueryOriginal,
    setQueryResults,

    // 编辑器设置
    editorSettings,
    saveSettings,

    // 查询历史
    queryHistory,
    pagination,
    handlePaginationChange,

    // UI状态
    activeTab,
    setActiveTab,
    chartType,
    setChartType,
    xField,
    setXField,
    yField,
    setYField,
    fullscreen,
    setFullscreen,
    historyDrawerVisible,
    setHistoryDrawerVisible,
    settingsDrawerVisible,
    setSettingsDrawerVisible,
  };
};
