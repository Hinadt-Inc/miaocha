import React from 'react';
import SQLEditorPage from './SQLEditor/SQLEditorPage';
import './SQLEditorPage.less';

/**
 * SQL编辑器页面入口
 * 重构后的组件使用模块化结构，所有功能被分散到 SQLEditor 目录中的多个组件
 */
const SQLEditor: React.FC = () => {
  return <SQLEditorPage />;
};

export default SQLEditor;
