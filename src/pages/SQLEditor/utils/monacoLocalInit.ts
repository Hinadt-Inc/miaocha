import * as monaco from 'monaco-editor';

// 导入worker文件
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

// Worker配置映射
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
 * 完全本地化的Monaco Editor初始化
 * 不依赖CDN，完全使用本地资源
 */
export const initMonacoEditorLocally = (): typeof monaco => {
  // 如果已经初始化过，直接返回
  if (window.monaco) {
    console.log('Monaco editor 已经本地初始化，直接返回');
    return window.monaco;
  }

  // 配置Web Workers (100%本地)
  self.MonacoEnvironment = {
    getWorker(_moduleId: string, label: string): Worker {
      const WorkerClass = WORKER_CONFIG[label as keyof typeof WORKER_CONFIG] || WORKER_CONFIG.default;
      return new WorkerClass();
    },
  };

  // 确保在window上也设置MonacoEnvironment
  (window as any).MonacoEnvironment = self.MonacoEnvironment;

  // 直接使用导入的monaco实例，无需任何CDN加载
  window.monaco = monaco;

  // 设置自定义主题
  try {
    window.monaco.editor.defineTheme('sqlTheme', THEME_CONFIG);
    console.log('Monaco Editor 本地初始化成功，主题已设置');
  } catch (error) {
    console.warn('设置Monaco主题失败:', error);
  }

  return window.monaco;
};

// 声明全局类型
declare global {
  interface Window {
    monaco: typeof monaco;
  }
}

export default initMonacoEditorLocally;
