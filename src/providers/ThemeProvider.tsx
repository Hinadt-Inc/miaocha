import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { ConfigProvider, theme as antdTheme } from 'antd';

// 主题模式类型
export type ThemeMode = 'light' | 'dark';

// 主题上下文类型
interface ThemeContextType {
  mode: ThemeMode;
  toggleTheme: () => void;
  isDarkMode: boolean;
}

// 创建主题上下文
const ThemeContext = createContext<ThemeContextType>({
  mode: 'light',
  toggleTheme: () => {},
  isDarkMode: false,
});

// 主题提供者属性
interface ThemeProviderProps {
  children: ReactNode;
  defaultMode?: ThemeMode;
}

// 主题配置
const themeConfig = {
  light: {
    token: {
      colorPrimary: '#1677ff',
      borderRadius: 4,
      colorBgContainer: '#ffffff',
      colorBgElevated: '#ffffff',
      colorText: 'rgba(0, 0, 0, 0.85)',
      colorTextSecondary: 'rgba(0, 0, 0, 0.45)',
    },
    components: {
      Button: {
        colorPrimaryHover: '#4096ff',
      },
      Card: {
        colorBgContainer: '#ffffff',
        boxShadow: '0 1px 2px rgba(0, 0, 0, 0.05)',
      },
    },
  },
  dark: {
    token: {
      colorPrimary: '#1668dc',
      borderRadius: 4,
      colorBgContainer: '#1f1f1f',
      colorBgElevated: '#1f1f1f',
      colorText: 'rgba(255, 255, 255, 0.85)',
      colorTextSecondary: 'rgba(255, 255, 255, 0.45)',
    },
    components: {
      Button: {
        colorPrimaryHover: '#3c89e8',
      },
      Card: {
        colorBgContainer: '#1f1f1f',
        boxShadow: '0 1px 2px rgba(0, 0, 0, 0.2)',
      },
    },
  },
};

// 主题提供者组件
export const ThemeProvider: React.FC<ThemeProviderProps> = ({ 
  children, 
  defaultMode = 'light' 
}) => {
  // 优先从本地存储中获取主题模式
  const [mode, setMode] = useState<ThemeMode>(() => {
    const savedMode = localStorage.getItem('theme-mode') as ThemeMode | null;
    return savedMode || defaultMode;
  });

  // 切换主题
  const toggleTheme = () => {
    setMode((prevMode) => {
      const newMode = prevMode === 'light' ? 'dark' : 'light';
      localStorage.setItem('theme-mode', newMode);
      return newMode;
    });
  };

  // 当主题改变时更新 body 的类名，以便可以在全局 CSS 中使用
  useEffect(() => {
    document.body.classList.remove('light-mode', 'dark-mode');
    document.body.classList.add(`${mode}-mode`);
    
    // 设置 CSS 变量
    document.documentElement.style.setProperty(
      '--card-bg', 
      mode === 'dark' ? '#1f1f1f' : '#ffffff'
    );
    document.documentElement.style.setProperty(
      '--text-primary', 
      mode === 'dark' ? 'rgba(255, 255, 255, 0.85)' : 'rgba(0, 0, 0, 0.85)'
    );
    document.documentElement.style.setProperty(
      '--text-secondary', 
      mode === 'dark' ? 'rgba(255, 255, 255, 0.45)' : 'rgba(0, 0, 0, 0.45)'
    );
  }, [mode]);

  // 创建 Ant Design 主题配置
  const antTheme = {
    algorithm: mode === 'light' ? antdTheme.defaultAlgorithm : antdTheme.darkAlgorithm,
    ...themeConfig[mode],
  };

  return (
    <ThemeContext.Provider value={{ 
      mode, 
      toggleTheme, 
      isDarkMode: mode === 'dark' 
    }}>
      <ConfigProvider theme={antTheme}>
        {children}
      </ConfigProvider>
    </ThemeContext.Provider>
  );
};

// 使用主题的钩子
export const useTheme = () => useContext(ThemeContext);