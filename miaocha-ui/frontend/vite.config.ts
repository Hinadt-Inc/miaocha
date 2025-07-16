import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { viteStaticCopy } from 'vite-plugin-static-copy';
import { resolve } from 'path';
import dynamicImport from 'vite-plugin-dynamic-import';

// https://vite.dev/config/
export default defineConfig({
  base: '/',
  plugins: [
    dynamicImport(),
    react({
      babel: {
        plugins: [
          ['@babel/plugin-transform-react-jsx', { runtime: 'automatic' }],
          '@babel/plugin-syntax-dynamic-import',
        ],
      },
    }),
  ],
  optimizeDeps: {
    include: ['monaco-editor', 'monaco-editor/esm/vs/basic-languages/sql/sql', 'monaco-sql-languages'],
    exclude: [
      // 这些worker文件需要作为Web Worker加载，不应该被优化
      'monaco-editor/esm/vs/editor/editor.worker',
      'monaco-editor/esm/vs/language/json/json.worker',
      'monaco-editor/esm/vs/language/css/css.worker',
      'monaco-editor/esm/vs/language/html/html.worker',
      'monaco-editor/esm/vs/language/typescript/ts.worker',
    ],
  },
  worker: {
    format: 'es',
    plugins: () => [
      viteStaticCopy({
        targets: [
          {
            src: 'node_modules/monaco-editor/esm/vs/**/*.worker.js',
            dest: 'monaco-workers',
          },
        ],
      }),
    ],
  },
  resolve: {
    alias: {
      '@': resolve(__dirname, './src'),
    },
    extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json'],
  },
  build: {
    // 输出目录
    outDir: 'dist',
    // 生成静态资源的存放路径
    assetsDir: 'assets',
    // 小于此阈值的导入或引用资源将内联为base64编码
    assetsInlineLimit: 4096,
    // 启用/禁用CSS代码拆分
    cssCodeSplit: true,
    // 构建后是否生成source map文件
    sourcemap: false,
    // 自定义底层的Rollup打包配置
    rollupOptions: {
      output: {
        // 入口文件配置 - 统一放在assets目录下
        entryFileNames: 'assets/js/[name].[hash].js',
        // 块文件配置 - 统一放在assets目录下
        chunkFileNames: 'assets/js/[name].[hash].js',
        // 资源文件配置 - 统一放在assets目录下
        assetFileNames: 'assets/[ext]/[name].[hash].[ext]',
        // 优化分块策略
        manualChunks: {
          'react-core': ['react', 'react-dom'],
          'react-router': ['react-router-dom'],
          'ant-design-core': ['antd'],
          'ant-design-icons': ['@ant-design/icons'],
          'ant-design-pro': ['@ant-design/pro-components'],
          'echarts-core': ['echarts'],
          'echarts-react': ['echarts-for-react'],
          animation: ['@react-spring/web'],
          utils: ['lodash', 'dayjs', 'axios'],
        },
      },
    },
    // 避免超大的chunk警告限制
    chunkSizeWarningLimit: 2000,
    // 启用/禁用 gzip 压缩大小报告
    reportCompressedSize: false,
    // 打包压缩配置
    minify: 'esbuild',
    // 传递给 esbuild 的配置
    terserOptions: {
      compress: {
        drop_console: true, // 生产环境去除console
        drop_debugger: true, // 生产环境去除debugger
      },
    },
  },
  css: {
    preprocessorOptions: {
      less: {
        javascriptEnabled: true,
      },
    },
    // CSS 模块化配置
    modules: {
      localsConvention: 'camelCase',
      generateScopedName: '[name]__[local]___[hash:base64:5]',
    },
  },
  server: {
    host: '0.0.0.0',
    open: true,
    proxy: {
      '/api': {
        target: 'http://10.254.133.210:32088',
        // target: 'https://miaocha.hinadt.com', // 生产环境
        changeOrigin: true,
      },
    },
  },
  esbuild: {
    jsxInject: `import React from 'react'`,
    // 处理JSX
    jsx: 'automatic',
    // 去除生产环境console
    drop: process.env.NODE_ENV === 'production' ? ['console', 'debugger'] : [],
  },
});
