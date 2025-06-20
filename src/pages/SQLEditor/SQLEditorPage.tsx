import SQLEditorImpl from './index';

/**
 * SQL编辑器页面实现
 * 这是一个中间组件，用于将路由路径与实际组件实现连接起来
 */
const SQLEditorPage: React.FC = () => {
  return <SQLEditorImpl />;
};

export default SQLEditorPage;