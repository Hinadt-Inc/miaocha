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
  // todo 待确认
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
          defaultToken: '',
          tokenPostfix: '.sql',
          ignoreCase: true,

          brackets: [
            { open: '(', close: ')', token: 'delimiter.parenthesis' },
            { open: '[', close: ']', token: 'delimiter.square' },
          ],

          keywords: [
            'SELECT',
            'FROM',
            'WHERE',
            'INSERT',
            'UPDATE',
            'DELETE',
            'CREATE',
            'DROP',
            'ALTER',
            'JOIN',
            'INNER',
            'LEFT',
            'RIGHT',
            'FULL',
            'OUTER',
            'ON',
            'AS',
            'AND',
            'OR',
            'NOT',
            'NULL',
            'TRUE',
            'FALSE',
            'DISTINCT',
            'ORDER',
            'BY',
            'GROUP',
            'HAVING',
            'LIMIT',
            'OFFSET',
            'INTO',
            'VALUES',
            'SET',
            'TABLE',
            'INDEX',
            'VIEW',
            'DATABASE',
            'SCHEMA',
            'PRIMARY',
            'KEY',
            'FOREIGN',
            'REFERENCES',
            'UNIQUE',
            'CHECK',
            'DEFAULT',
            'COUNT',
            'SUM',
            'AVG',
            'MIN',
            'MAX',
            'CASE',
            'WHEN',
            'THEN',
            'ELSE',
            'END',
            'UNION',
            'ALL',
            'EXISTS',
            'IN',
            'BETWEEN',
            'LIKE',
            'IS',
            'ISNULL',
          ],

          operators: [
            '=',
            '>',
            '<',
            '!',
            '~',
            '?',
            ':',
            '==',
            '<=',
            '>=',
            '!=',
            '<>',
            '+=',
            '-=',
            '*=',
            '/=',
            '%=',
            '|=',
            '&=',
            '^=',
            '>>=',
            '<<=',
          ],

          builtinFunctions: [
            'COUNT',
            'SUM',
            'AVG',
            'MIN',
            'MAX',
            'CONCAT',
            'SUBSTRING',
            'LENGTH',
            'UPPER',
            'LOWER',
            'TRIM',
            'LTRIM',
            'RTRIM',
            'REPLACE',
            'CAST',
            'CONVERT',
            'COALESCE',
            'NULLIF',
            'DATE',
            'TIME',
            'DATETIME',
            'NOW',
            'CURRENT_TIMESTAMP',
            'YEAR',
            'MONTH',
            'DAY',
          ],

          builtinVariables: [],

          tokenizer: {
            root: [
              { include: '@comments' },
              { include: '@whitespace' },
              { include: '@numbers' },
              { include: '@strings' },
              { include: '@complexIdentifiers' },
              [/[;,.]/, 'delimiter'],
              [/[()]/, '@brackets'],
              [
                /[\w@#$]+/,
                {
                  cases: {
                    '@keywords': 'keyword',
                    '@operators': 'operator',
                    '@builtinFunctions': 'predefined',
                    '@builtinVariables': 'predefined',
                    '@default': 'identifier',
                  },
                },
              ],
              [/[<>=!%&+\-*/|~^]/, 'operator'],
            ],

            whitespace: [[/\s+/, 'white']],

            comments: [
              [/--+.*/, 'comment'],
              [/\/\*/, { token: 'comment.quote', next: '@comment' }],
            ],

            comment: [
              [/[^*/]+/, 'comment'],
              [/\*\//, { token: 'comment.quote', next: '@pop' }],
              [/./, 'comment'],
            ],

            numbers: [
              [/0[xX][0-9a-fA-F]*/, 'number'],
              [/[$][+-]*\d*(\.\d*)?/, 'number'],
              [/((\d+(\.\d*)?)|(\.\d+))([eE][\-+]?\d+)?/, 'number'],
            ],

            strings: [
              [/N?'/, { token: 'string', next: '@string' }],
              [/N?"/, { token: 'string', next: '@stringDouble' }],
            ],

            string: [
              [/[^']+/, 'string'],
              [/''/, 'string'],
              [/'/, { token: 'string', next: '@pop' }],
            ],

            stringDouble: [
              [/[^"]+/, 'string'],
              [/""/, 'string'],
              [/"/, { token: 'string', next: '@pop' }],
            ],

            complexIdentifiers: [
              [/\[/, { token: 'identifier.quote', next: '@bracketedIdentifier' }],
              [/`/, { token: 'identifier.quote', next: '@quotedIdentifier' }],
            ],

            bracketedIdentifier: [
              [/[^\]]+/, 'identifier'],
              [/]]/, 'identifier'],
              [/]/, { token: 'identifier.quote', next: '@pop' }],
            ],

            quotedIdentifier: [
              [/[^`]+/, 'identifier'],
              [/``/, 'identifier'],
              [/`/, { token: 'identifier.quote', next: '@pop' }],
            ],
          },
        });

        // 设置SQL语言配置
        monaco.languages.setLanguageConfiguration('sql', {
          comments: {
            lineComment: '--',
            blockComment: ['/*', '*/'],
          },
          brackets: [
            ['(', ')'],
            ['[', ']'],
          ],
          autoClosingPairs: [
            { open: '(', close: ')' },
            { open: '[', close: ']' },
            { open: "'", close: "'" },
            { open: '"', close: '"' },
          ],
          surroundingPairs: [
            { open: '(', close: ')' },
            { open: '[', close: ']' },
            { open: "'", close: "'" },
            { open: '"', close: '"' },
          ],
        });

        // SQL补全提供器将在组件中通过 useSQLCompletion hook 注册
        // 这样可以访问到数据库结构信息和上下文状态
        console.log('✅ SQL语言支持已注册，等待补全提供器注册');
      } else {
        console.log('✅ SQL语言已存在，跳过注册');
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
