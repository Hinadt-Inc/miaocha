import React from 'react';
import SettingsDrawer from './SettingsDrawer';

export interface SQLSettingsDrawerProps {
  visible: boolean;
  onClose: () => void;
  editorSettings: any;
  onUpdateSettings: (settings: any) => void;
}

/**
 * SQL设置抽屉组件
 * 封装原有的SettingsDrawer组件，提供统一的接口
 */
export const SQLSettingsDrawer: React.FC<SQLSettingsDrawerProps> = ({
  visible,
  onClose,
  editorSettings,
  onUpdateSettings,
}) => {
  return (
    <SettingsDrawer
      editorSettings={editorSettings}
      updateEditorSettings={onUpdateSettings}
      visible={visible}
      onClose={onClose}
    />
  );
};
