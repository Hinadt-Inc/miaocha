import { useState, useEffect } from 'react';
import { message } from 'antd';
import type { EditorSettings } from '../types';
import { SETTINGS_STORAGE_KEY } from '../types';

const DEFAULT_SETTINGS: EditorSettings = {
  fontSize: 14,
  theme: 'vs',
  wordWrap: true,
  autoComplete: true,
  tabSize: 2,
  minimap: false,
};

export const useEditorSettings = () => {
  const [settings, setSettings] = useState<EditorSettings>(DEFAULT_SETTINGS);
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);

  useEffect(() => {
    const savedSettings = localStorage.getItem(SETTINGS_STORAGE_KEY);
    if (savedSettings) {
      try {
        const parsed = JSON.parse(savedSettings) as unknown;
        if (isValidEditorSettings(parsed)) {
          setSettings(parsed);
        } else {
          console.warn('无效的编辑器设置格式，使用默认设置');
        }
      } catch (error) {
        console.error('解析编辑器设置失败:', error);
        message.error('加载编辑器设置失败');
      }
    }
  }, []);

  const isValidEditorSettings = (obj: unknown): obj is EditorSettings => {
    return (
      !!obj &&
      typeof obj === 'object' &&
      'fontSize' in obj &&
      typeof obj.fontSize === 'number' &&
      'theme' in obj &&
      typeof obj.theme === 'string' &&
      'wordWrap' in obj &&
      typeof obj.wordWrap === 'boolean' &&
      'autoComplete' in obj &&
      typeof obj.autoComplete === 'boolean' &&
      'tabSize' in obj &&
      typeof obj.tabSize === 'number' &&
      'minimap' in obj &&
      typeof obj.minimap === 'boolean'
    );
  };

  const saveSettings = (partialSettings: Partial<EditorSettings>) => {
    const newSettings = { ...settings, ...partialSettings };
    setSettings(newSettings);
    localStorage.setItem(SETTINGS_STORAGE_KEY, JSON.stringify(newSettings));
    message.success('编辑器设置已保存');
  };

  const toggleSettings = () => {
    setIsSettingsOpen((prev) => !prev);
  };

  return {
    settings,
    setSettings,
    saveSettings,
    isSettingsOpen,
    toggleSettings,
  };
};
