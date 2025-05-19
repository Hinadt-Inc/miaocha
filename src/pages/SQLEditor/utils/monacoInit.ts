import { loader } from '@monaco-editor/react';

/**
 * 初始化 Monaco 编辑器
 * 设置必要的配置并尝试加载编辑器资源
 */
const initMonacoEditor = (): void => {
  // 使用CDN资源，而不是本地打包，避免"Unexpected token '<'"错误
  // 默认使用jsdelivr CDN，比较稳定可靠
  const cdnBase =
    (window as any).monacoFallbackCdnBase ||
    'https://cdn.jsdelivr.net/npm/monaco-editor@0.44.0/min';

  // 配置 Monaco 加载路径使用CDN
  loader.config({
    paths: {
      vs: `${cdnBase}/vs`,
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

  // 使用 Promise.race 来实现超时处理
  Promise.race([loader.init(), timeoutPromise])
    .then((monaco) => {
      console.log('Monaco editor 加载成功');

      // 为 SQL 设置自定义主题
      if (monaco) {
        const monacoInstance = monaco as typeof import('monaco-editor');
        monacoInstance.editor.defineTheme('sqlTheme', {
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
        });
      }
    })
    .catch((error) => {
      console.error('Monaco editor 初始化失败:', error);

      // 尝试使用备用CDN
      loader.config({
        paths: {
          vs: 'https://cdn.jsdelivr.net/npm/monaco-editor@0.44.0/esm/vs',
        },
      });

      // 备用CDN情况下更新worker配置
      const cdnPath =
        (window as any).monacoFallbackCdnBase ||
        'https://cdn.jsdelivr.net/npm/monaco-editor@0.44.0/min/vs';
      window.MonacoEnvironment = {
        getWorkerUrl: function (_moduleId, label) {
          if (label === 'sql' || label === 'mysql') {
            return `${cdnPath}/language/sql/sql.worker.js`;
          }
          if (label === 'json') {
            return `${cdnPath}/language/json/json.worker.js`;
          }
          if (label === 'css') {
            return `${cdnPath}/language/css/css.worker.js`;
          }
          if (label === 'html') {
            return `${cdnPath}/language/html/html.worker.js`;
          }
          if (label === 'typescript') {
            return `${cdnPath}/language/typescript/ts.worker.js`;
          }
          return `${cdnPath}/editor/editor.worker.js`;
        },
      };

      // 重新尝试加载
      loader
        .init()
        .then(() => {
          console.log('Monaco editor 通过备用CDN加载成功');
        })
        .catch((cdnError) => {
          console.error('Monaco editor 备用CDN初始化也失败:', cdnError);
        });
    });
};

export default initMonacoEditor;
