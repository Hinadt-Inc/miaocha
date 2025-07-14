import editorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker';
import jsonWorker from 'monaco-editor/esm/vs/language/json/json.worker?worker';
import cssWorker from 'monaco-editor/esm/vs/language/css/css.worker?worker';
import htmlWorker from 'monaco-editor/esm/vs/language/html/html.worker?worker';
import tsWorker from 'monaco-editor/esm/vs/language/typescript/ts.worker?worker';

/**
 * 配置 Monaco Editor Workers
 * 本地加载 Monaco Editor 的 web workers 而非使用CDN
 */
export function setupMonacoWorkers() {
  // 本地化配置 Monaco workers
  self.MonacoEnvironment = {
    getWorker(_, label) {
      if (label === 'json') {
        return new jsonWorker();
      }
      if (label === 'css' || label === 'scss' || label === 'less') {
        return new cssWorker();
      }
      if (label === 'html' || label === 'handlebars' || label === 'razor') {
        return new htmlWorker();
      }
      if (label === 'typescript' || label === 'javascript') {
        return new tsWorker();
      }
      if (label === 'sql' || label === 'mysql') {
        // SQL 语言使用通用编辑器worker
        return new editorWorker();
      }
      // 默认的编辑器worker
      return new editorWorker();
    },
  };

  // 确保在全局 window 对象上也设置 MonacoEnvironment
  window.MonacoEnvironment = self.MonacoEnvironment;
}

export default setupMonacoWorkers;
