import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { resolve } from 'path'
import * as monacoEditorPlugin from 'vite-plugin-monaco-editor'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    monacoEditorPlugin.default({
      languageWorkers: ['editorWorkerService', 'typescript', 'json', 'html'],
      customWorkers: [
        {
          label: 'sql',
          entry: 'monaco-sql-languages/out/esm/sql/sql.worker.js'
        }
      ]
    })
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, './src')
    }
  },
  build: {
    // 优化分块策略
    rollupOptions: {
      output: {
        manualChunks: {
          'react-vendor': ['react', 'react-dom', 'react-router-dom'],
          'ant-design': ['antd', '@ant-design/icons', '@ant-design/pro-components'],
          'echarts': ['echarts', 'echarts-for-react', '@react-spring/web']
        }
      }
    },
    // 避免超大的 chunk
    chunkSizeWarningLimit: 1500
  },
  css: {
    preprocessorOptions: {
      less: {
        javascriptEnabled: true,
      },
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://10.254.133.210:32088',
        changeOrigin: true,
      }
    }
  },
  // 添加动态导入优化
  optimizeDeps: {
    include: [
      'react', 
      'react-dom', 
      'react-router-dom',
      'antd',
      '@ant-design/icons',
      'echarts',
      'echarts-for-react',
      '@react-spring/web',
      'lodash'
    ]
  },
  esbuild: {
    jsxInject: `import React from 'react'`,
  }
})
