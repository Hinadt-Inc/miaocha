import { loader } from '@monaco-editor/react';

// 初始化 Monaco 编辑器
export const initMonacoEditor = (): void => {
  // 修改为更可靠的CDN源
  loader.config({
    paths: {
      vs: 'https://cdn.jsdelivr.net/npm/monaco-editor@0.44.0/min/vs'
    },
    // 添加monaco-editor的CSP标头支持
    'vs/nls': {
      availableLanguages: {
        '*': 'zh-cn'
      }
    },
    // 设置一个超时时间
    'vs/editor/editor.main': {
      timeout: 30000 // 30秒超时
    }
  });

  // 确保编辑器能够正确初始化
  try {
    window.MonacoEnvironment = {
      getWorkerUrl: function (_moduleId, label) {
        if (label === 'sql' || label === 'mysql') {
          return '/monaco-editor/sql.worker.js';
        }
        return '/monaco-editor/editor.worker.js';
      }
    };
    
    loader.init()
      .then(monaco => {
        console.log('Monaco editor 加载成功');
        // 定义自定义 SQL 主题
        monaco.editor.defineTheme('sqlTheme', {
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
            'editor.inactiveSelectionBackground': '#E5EBF1'
          }
        });
      })
      .catch(error => {
        console.error('Monaco editor 初始化失败:', error);
        // 尝试备用CDN
        loader.config({
          paths: {
            vs: 'https://unpkg.com/monaco-editor@0.44.0/min/vs'
          }
        });
        return loader.init();
      })
      .catch(error => {
        console.error('Monaco editor 备用CDN初始化也失败:', error);
      });
  } catch (error) {
    console.error('Monaco editor 加载失败:', error);
  }
};

export default initMonacoEditor;