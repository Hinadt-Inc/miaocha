import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { resolve } from 'path'
import dynamicImport from 'vite-plugin-dynamic-import'
import monacoEditorPlugin from 'vite-plugin-monaco-editor'
// 添加类型断言解决.default的类型问题
const monacoPlugin = monacoEditorPlugin as unknown as { default: typeof monacoEditorPlugin }

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    monacoPlugin.default({
      publicPath: 'node_modules/monaco-editor/min',
      customDistPath: (root: string) => {
        return resolve(root, 'node_modules/monaco-editor/min')
      }
    }),
    dynamicImport(),
    react({
      babel: {
        plugins: [
          ['@babel/plugin-transform-react-jsx', { runtime: 'automatic' }],
          '@babel/plugin-syntax-dynamic-import'
        ]
      }
    }),
  ],
  base: '/',
  resolve: {
    alias: {
      '@': resolve(__dirname, './src')
    }
  },
  build: {
    // 优化分块策略
    rollupOptions: {
      output: {
        chunkFileNames: 'assets/[name]-[hash].js',
        entryFileNames: 'assets/[name]-[hash].js',
        assetFileNames: 'assets/[name]-[hash].[ext]',
        manualChunks: {
          'react-vendor': ['react', 'react-dom', 'react-router-dom'],
          'ant-design': ['antd', '@ant-design/icons', '@ant-design/pro-components'],
          'echarts': ['echarts', 'echarts-for-react', '@react-spring/web'],
          'providers': [
            './src/providers/QueryProvider.tsx',
            './src/providers/ThemeProvider.tsx',
            './src/providers/LoadingProvider.tsx'
          ],
          'store': [
            './src/store/counterSlice.ts',
            './src/store/userSlice.ts',
            './src/store/store.ts'
          ],
          'api': [
            './src/api/request.ts',
            './src/api/auth.ts',
            './src/api/logs.ts',
            './src/api/sql.ts'
          ]
        }
      }
    },
    sourcemap: false,
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
    sourcemapIgnoreList(sourcePath) {
      return sourcePath.includes('node_modules')
    },
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
