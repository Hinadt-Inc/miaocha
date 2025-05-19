/**
 * 配置 Monaco Editor Workers
 * 在生产环境和开发环境中正确加载 Monaco Editor 的 web workers
 * 使用CDN方式加载资源，避免打包问题导致的"Unexpected token '<'"错误
 */
export function setupMonacoWorkers() {
  // 使用CDN资源，而不是本地打包
  const cdnPath =
    (window as any).monacoFallbackCdnBase ||
    'https://cdn.jsdelivr.net/npm/monaco-editor@0.44.0/min/vs';

  // Monaco Editor worker 路径配置
  window.MonacoEnvironment = {
    getWorkerUrl: function (_moduleId, label) {
      // 根据语言类型返回对应的worker
      if (label === 'sql' || label === 'mysql') {
        return `${cdnPath}/language/sql/sql.worker.js`;
      }

      if (label === 'json') {
        return `${cdnPath}/language/json/json.worker.js`;
      }

      if (label === 'css' || label === 'scss' || label === 'less') {
        return `${cdnPath}/language/css/css.worker.js`;
      }

      if (label === 'html' || label === 'handlebars' || label === 'razor') {
        return `${cdnPath}/language/html/html.worker.js`;
      }

      if (label === 'typescript' || label === 'javascript') {
        return `${cdnPath}/language/typescript/ts.worker.js`;
      }

      // 默认的编辑器worker
      return `${cdnPath}/editor/editor.worker.js`;
    },
  };
}

export default setupMonacoWorkers;
