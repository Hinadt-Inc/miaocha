import { loader } from '@monaco-editor/react';
import * as monaco from 'monaco-editor';

// 声明全局monaco类型
declare global {
  interface Window {
    monaco: typeof import('monaco-editor');
  }

  // 声明全局MonacoEnvironment接口
  interface MonacoEnvironment {
    getWorker(moduleId: string, label: string): Worker;
  }
}
import editorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker';
import jsonWorker from 'monaco-editor/esm/vs/language/json/json.worker?worker';
import cssWorker from 'monaco-editor/esm/vs/language/css/css.worker?worker';
import htmlWorker from 'monaco-editor/esm/vs/language/html/html.worker?worker';
import tsWorker from 'monaco-editor/esm/vs/language/typescript/ts.worker?worker';

// 主题配置常量
export const THEME_CONFIG: monaco.editor.IStandaloneThemeData = {
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
let retryCount = 0;
const MAX_RETRIES = 3;

const initMonacoEditor = async (): Promise<typeof monaco | undefined> => {
  // 如果已经初始化过，直接返回
  if (window.monaco) {
    console.log('Monaco editor 已经初始化，直接返回');
    return window.monaco;
  }

  // 获取对应语言的worker
  const getWorker = (_moduleId: string, label: string): Worker => {
    const WorkerClass = WORKER_CONFIG[label as keyof typeof WORKER_CONFIG] || WORKER_CONFIG.default;
    return new WorkerClass();
  };

  // 本地化配置 Monaco workers
  self.MonacoEnvironment = { getWorker };

  try {
    // 强制禁用CDN，只使用本地资源
    loader.config({
      paths: {
        vs: '/monaco-editor/min/vs',
      },
    });

    // 直接使用本地monaco实例，跳过loader的CDN加载
    window.monaco = monaco;
    console.log('Monaco editor 直接本地加载成功');

    // 为 SQL 设置自定义主题
    if (window.monaco) {
      window.monaco.editor.defineTheme('sqlTheme', THEME_CONFIG);
    }

    retryCount = 0; // 重置计数器
    return window.monaco;
  } catch (error) {
    console.error('Monaco editor 初始化失败:', error);
    // 添加重试机制
    if (retryCount < MAX_RETRIES) {
      console.log(`正在重试Monaco初始化(第${retryCount + 1}次)...`);
      retryCount++;
      return initMonacoEditor();
    } else {
      retryCount = 0; // 重置计数器
    }
    throw error;
  }
};

export default initMonacoEditor;
