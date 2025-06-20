import * as monaco from 'monaco-editor';
import editorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker';
import jsonWorker from 'monaco-editor/esm/vs/language/json/json.worker?worker';
import cssWorker from 'monaco-editor/esm/vs/language/css/css.worker?worker';
import htmlWorker from 'monaco-editor/esm/vs/language/html/html.worker?worker';
import tsWorker from 'monaco-editor/esm/vs/language/typescript/ts.worker?worker';

// 主题配置
export const THEME_CONFIG: monaco.editor.IStandaloneThemeData = {
  base: 'vs',
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

// Worker 配置
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
 * 初始化 Monaco 编辑器，确保使用本地资源
 * @returns Monaco 实例
 */
const initMonacoEditor = async (): Promise<typeof monaco> => {
  // 设置 MonacoEnvironment 用于本地 Worker
  self.MonacoEnvironment = {
    getWorker: (_moduleId: string, label: string): Worker => {
      const WorkerClass = WORKER_CONFIG[label as keyof typeof WORKER_CONFIG] || WORKER_CONFIG.default;
      return new WorkerClass();
    },
    // 强制指定本地路径，覆盖默认 CDN
    baseUrl: '/monaco-editor/min/vs',
  };

  // 注册 SQL 语言支持（避免加载 basic-languages/sql/sql.js）
  monaco.languages.register({
    id: 'sql',
    extensions: ['.sql'],
    aliases: ['SQL', 'sql'],
  });

  // 定义 SQL 语言的简单 tokenizer（可选，防止请求 sql.js）
  monaco.languages.setMonarchTokensProvider('sql', {
    tokenizer: {
      root: [
        [/\b(SELECT|FROM|WHERE|INSERT|UPDATE|DELETE|INTO|VALUES|AND|OR|NOT)\b/i, 'keyword'],
        [/"[^"]*"|'[^']*'/, 'string'],
        [/--.*$/, 'comment'],
        [/[a-zA-Z_][a-zA-Z0-9_]*/, 'identifier'],
      ],
    },
  });

  // 定义自定义主题
  monaco.editor.defineTheme('sqlTheme', THEME_CONFIG);

  // 加载 CSS（使用URL方式加载public目录下的资源）
  const cssUrl = '/monaco-editor/min/vs/editor/editor.main.css';
  const loaderUrl = '/monaco-editor/min/vs/load.js';
  const response = await fetch(cssUrl);
  const loaderResponse = await fetch(loaderUrl);
  if (response.ok) {
    const cssText = await response.text();
    const style = document.createElement('style');
    style.textContent = cssText;
    document.head.appendChild(style);
  }

  return monaco;
};

export default initMonacoEditor;
