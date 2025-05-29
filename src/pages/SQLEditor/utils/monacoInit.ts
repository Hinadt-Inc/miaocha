import { loader } from '@monaco-editor/react';
import * as monaco from 'monaco-editor';
import editorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker';
import jsonWorker from 'monaco-editor/esm/vs/language/json/json.worker?worker';
import cssWorker from 'monaco-editor/esm/vs/language/css/css.worker?worker';
import htmlWorker from 'monaco-editor/esm/vs/language/html/html.worker?worker';
import tsWorker from 'monaco-editor/esm/vs/language/typescript/ts.worker?worker';

// 主题配置常量
const THEME_CONFIG: monaco.editor.IStandaloneThemeData = {
  base: 'vs' as monaco.editor.BuiltinTheme,
  inherit: true,
  rules: [
    { token: 'keyword', foreground: '0000ff', fontStyle: 'bold' },
    { token: 'string', foreground: 'a31515' },
    { token: 'identifier', foreground: '001080' },
    { token: 'comment', foreground: '008000' },
  ],
  colors: {
    'editor.foreground': '#000000',
    'editor.background': '#F5F5F5',
    'editorCursor.foreground': '#8B0000',
    'editor.lineHighlightBackground': '#F8F8F8',
    'editorLineNumber.foreground': '#2B91AF',
    'editor.selectionBackground': '#ADD6FF',
    'editor.inactiveSelectionBackground': '#E5EBF1',
  },
};

// Worker配置常量
const WORKER_CONFIG = {
  json: jsonWorker,
  css: cssWorker,
  scss: cssWorker,
  less: cssWorker,
  html: htmlWorker,
  handlebars: htmlWorker,
  razor: htmlWorker,
  typescript: tsWorker,
  javascript: tsWorker,
  default: editorWorker,
};

/**
 * 初始化 Monaco 编辑器
 * 设置必要的配置并加载本地编辑器资源
 */
const initMonacoEditor = (): void => {
  // 获取对应语言的worker
  const getWorker = (label: string): Worker => {
    const WorkerClass = WORKER_CONFIG[label as keyof typeof WORKER_CONFIG] || WORKER_CONFIG.default;
    return new WorkerClass();
  };

  // 本地化配置 Monaco workers
  self.MonacoEnvironment = { getWorker };

  // 配置 Monaco 加载设置
  loader.config({
    monaco,
    paths: {
      vs: '/monaco-editor/min/vs', // 本地化路径
    },
    'vs/nls': {
      availableLanguages: {
        '*': 'zh-cn',
      },
    },
  });

  // 设置超时来处理加载时间过长的情况
  const timeoutPromise = new Promise((_, reject) =>
    setTimeout(() => reject(new Error('Monaco editor 加载超时')), 30000),
  );

  // 初始化编辑器
  Promise.race([loader.init(), timeoutPromise])
    .then((monaco) => {
      console.log('Monaco editor 本地加载成功');

      // 为 SQL 设置自定义主题
      if (monaco) {
        const monacoInstance = monaco as typeof import('monaco-editor');
        monacoInstance.editor.defineTheme('sqlTheme', THEME_CONFIG);
      }
    })
    .catch((error) => {
      console.error('Monaco editor 初始化失败:', error);
      // 即使初始化失败，也确保 MonacoEnvironment 被全局设置
      window.MonacoEnvironment = self.MonacoEnvironment;
    });
};

export default initMonacoEditor;
