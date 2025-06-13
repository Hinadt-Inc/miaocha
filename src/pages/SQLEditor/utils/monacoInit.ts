import { loader } from '@monaco-editor/react';
import * as monaco from 'monaco-editor';

// 声明全局monaco类型
declare global {
  interface Window {
    monaco: typeof import('monaco-editor');
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

const initMonacoEditor = async (): Promise<void> => {
  // 获取对应语言的worker
  const getWorker = (label: string): Worker => {
    const WorkerClass = WORKER_CONFIG[label as keyof typeof WORKER_CONFIG] || WORKER_CONFIG.default;
    return new WorkerClass();
  };

  // 本地化配置 Monaco workers
  self.MonacoEnvironment = { getWorker };

  // 强制使用本地配置(使用相对路径)
  const config = {
    monaco,
    paths: {
      vs: './public/monaco-editor/min/vs',
      'vs/loader.js': './public/monaco-editor/min/vs/loader.js',
    },
    'vs/nls': {
      availableLanguages: {
        '*': 'zh-cn',
      },
    },
    'vs/loader': {
      usePlainModuleNames: true,
      ignoreDuplicateModules: ['vs/editor/editor.main'],
    },
    // 禁用CDN加载
    useCDN: false,
    // 禁用默认worker加载
    disableWorker: false,
    // 自定义worker路径
    workerPath: '/monaco-workers',
  };
  // 精确类型声明
  loader.config(config as Parameters<typeof loader.config>[0]);

  // 设置超时来处理加载时间过长的情况
  const timeoutPromise = new Promise((_, reject) =>
    setTimeout(() => reject(new Error('Monaco editor 加载超时')), 30000),
  );

  // 初始化编辑器并确保加载顺序
  Promise.race([loader.init(), timeoutPromise])
    .then((monacoInstance) => {
      if (!monacoInstance) {
        throw new Error('Monaco实例未正确加载');
      }
      window.monaco = monacoInstance as typeof import('monaco-editor');
      console.log('Monaco editor 本地加载成功', window.monaco === monacoInstance);

      // 为 SQL 设置自定义主题
      if (window.monaco) {
        window.monaco.editor.defineTheme('sqlTheme', THEME_CONFIG);
      }
    })
    .catch((error) => {
      console.error('Monaco editor 初始化失败:', error);
      // 即使初始化失败，也确保 MonacoEnvironment 被全局设置
      window.MonacoEnvironment = self.MonacoEnvironment;
      // 添加重试机制
      if (retryCount < MAX_RETRIES) {
        console.log(`正在重试Monaco初始化(第${retryCount + 1}次)...`);
        retryCount++;
        return initMonacoEditor();
      } else {
        retryCount = 0; // 重置计数器
      }
      throw error;
    });
};

export default initMonacoEditor;
