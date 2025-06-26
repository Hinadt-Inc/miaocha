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

// 全局初始化标记
let isInitialized = false;
let promiseRejectionHandlerAdded = false;

/**
 * 添加全局Promise rejection处理器来忽略Monaco Editor的取消错误
 */
const addPromiseRejectionHandler = () => {
  if (promiseRejectionHandlerAdded || typeof window === 'undefined') return;

  window.addEventListener('unhandledrejection', (event) => {
    // 检查是否是Monaco Editor相关的取消错误
    const reason = event.reason;
    const reasonString = reason?.toString() || '';
    const reasonStack = reason?.stack || '';
    const reasonMessage = reason?.message || '';

    const isMonacoCancellation =
      reason &&
      // 基本的Canceled错误
      (reasonMessage.includes('Canceled') ||
        reason.name === 'Canceled' ||
        reasonString.includes('Canceled') ||
        // Monaco Editor特定的取消错误
        reasonString.includes('monaco') ||
        reasonStack.includes('monaco') ||
        reasonStack.includes('Delayer.cancel') ||
        reasonStack.includes('chunk-RWT5L') || // Vite打包的Monaco chunk
        // TypeScript服务相关的取消
        reasonStack.includes('typescript') ||
        reasonStack.includes('languageFeatures') ||
        // 其他Monaco内部取消模式
        reasonString.includes('Operation was cancelled') ||
        reasonMessage.includes('Operation was cancelled'));

    if (isMonacoCancellation) {
      // 完全静默处理，不输出任何日志
      event.preventDefault(); // 阻止错误显示在控制台
      return;
    }

    // 对于非Monaco的错误，保持原有行为
  });

  promiseRejectionHandlerAdded = true;
  // 静默添加，不输出日志
};

/**
 * 完全本地化的Monaco Editor初始化
 * 不依赖CDN，完全使用本地资源
 * 修复Promise rejection错误
 */
export const initMonacoEditorLocally = (): typeof monaco => {
  // 添加Promise rejection处理器
  addPromiseRejectionHandler();

  // 如果已经初始化过，直接返回 - 完全静默
  if (isInitialized && window.monaco) {
    return window.monaco;
  }

  try {
    // 配置Web Workers (100%本地) - 更健壮的错误处理
    const monacoEnvironment = {
      getWorker(_moduleId: string, label: string): Worker {
        try {
          const WorkerClass = WORKER_CONFIG[label as keyof typeof WORKER_CONFIG] || WORKER_CONFIG.default;
          return new WorkerClass();
        } catch (error) {
          console.warn(`创建${label} worker失败，使用默认worker:`, error);
          return new WORKER_CONFIG.default();
        }
      },
    };

    // 安全地设置环境变量
    if (typeof self !== 'undefined') {
      self.MonacoEnvironment = monacoEnvironment;
    }
    if (typeof window !== 'undefined') {
      (window as any).MonacoEnvironment = monacoEnvironment;
    }
    if (typeof global !== 'undefined') {
      (global as any).MonacoEnvironment = monacoEnvironment;
    }

    // 直接使用导入的monaco实例
    window.monaco = monaco;

    // 配置SQL语言支持
    try {
      // 注册SQL语言（如果还没有注册）
      const languages = monaco.languages.getLanguages();
      const hasSql = languages.some((lang) => lang.id === 'sql');

      if (!hasSql) {
        // 注册基本的SQL语言
        monaco.languages.register({ id: 'sql' });

        // 设置基本的SQL语法高亮
        monaco.languages.setMonarchTokensProvider('sql', {
          tokenizer: {
            root: [
              [/\b(SELECT|FROM|WHERE|INSERT|UPDATE|DELETE|CREATE|DROP|ALTER)\b/i, 'keyword'],
              [/\b(JOIN|INNER|LEFT|RIGHT|ON|AS|AND|OR|NOT|NULL)\b/i, 'keyword'],
              [/\b(TRUE|FALSE|DISTINCT|ORDER|BY|GROUP|HAVING|LIMIT|OFFSET)\b/i, 'keyword'],
              [/\b(VARCHAR|INT|INTEGER|TEXT|DATE|DATETIME|TIMESTAMP|DECIMAL|FLOAT|BOOLEAN|CHAR)\b/i, 'type'],
              [/'[^']*'/, 'string'],
              [/"[^"]*"/, 'string'],
              [/--.*$/, 'comment'],
              [/\/\*[\s\S]*?\*\//, 'comment'],
              [/\b\d+(\.\d+)?\b/, 'number'],
            ],
          },
        });

        // 设置SQL自动补全
        monaco.languages.registerCompletionItemProvider('sql', {
          provideCompletionItems: (model, position) => {
            const word = model.getWordUntilPosition(position);
            const range = {
              startLineNumber: position.lineNumber,
              endLineNumber: position.lineNumber,
              startColumn: word.startColumn,
              endColumn: word.endColumn,
            };

            const suggestions: monaco.languages.CompletionItem[] = [
              {
                label: 'SELECT',
                kind: monaco.languages.CompletionItemKind.Keyword,
                insertText: 'SELECT ',
                detail: 'SQL SELECT statement',
                range,
              },
              {
                label: 'FROM',
                kind: monaco.languages.CompletionItemKind.Keyword,
                insertText: 'FROM ',
                detail: 'SQL FROM clause',
                range,
              },
              {
                label: 'WHERE',
                kind: monaco.languages.CompletionItemKind.Keyword,
                insertText: 'WHERE ',
                detail: 'SQL WHERE clause',
                range,
              },
              {
                label: 'ORDER BY',
                kind: monaco.languages.CompletionItemKind.Keyword,
                insertText: 'ORDER BY ',
                detail: 'SQL ORDER BY clause',
                range,
              },
              {
                label: 'GROUP BY',
                kind: monaco.languages.CompletionItemKind.Keyword,
                insertText: 'GROUP BY ',
                detail: 'SQL GROUP BY clause',
                range,
              },
            ];

            return { suggestions };
          },
        });

        console.log('✅ SQL语言支持已注册');
      }
    } catch (sqlError) {
      console.warn('SQL语言配置失败:', sqlError);
    }

    // 优化TypeScript配置 - 只针对必要的部分进行配置
    try {
      // 设置更温和的TypeScript配置，不完全禁用
      monaco.languages.typescript.typescriptDefaults.setDiagnosticsOptions({
        noSemanticValidation: false, // 保留语义验证
        noSyntaxValidation: false, // 保留语法验证
        noSuggestionDiagnostics: true, // 禁用建议诊断来减少noise
        diagnosticCodesToIgnore: [1108, 1109, 1005], // 忽略一些常见的非关键错误
      });

      monaco.languages.typescript.javascriptDefaults.setDiagnosticsOptions({
        noSemanticValidation: false,
        noSyntaxValidation: false,
        noSuggestionDiagnostics: true,
        diagnosticCodesToIgnore: [1108, 1109, 1005],
      });
    } catch (langError) {
      console.warn('语言配置设置失败:', langError);
    }

    // 设置自定义主题
    try {
      window.monaco.editor.defineTheme('sqlTheme', THEME_CONFIG);
      console.log('✅ Monaco Editor 本地初始化成功，主题已设置');
    } catch (error) {
      console.warn('⚠️ 设置Monaco主题失败:', error);
    }

    // 标记为已初始化
    isInitialized = true;

    return window.monaco;
  } catch (error) {
    console.error('❌ Monaco Editor 初始化失败:', error);
    throw error;
  }
};

// 声明全局类型
declare global {
  interface Window {
    monaco: typeof monaco;
  }
}

export default initMonacoEditorLocally;
