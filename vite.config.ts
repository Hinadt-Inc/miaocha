import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { resolve } from 'path'

// https://vite.dev/config/
export default defineConfig({
  base: '/',
  plugins: [
    react(),
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, './src')
    },
    extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json']
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
        // 入口文件配置
        entryFileNames: 'js/[name].[hash].js',
        // 块文件配置
        chunkFileNames: 'js/[name].[hash].js',
        // 资源文件配置
        assetFileNames: 'assets/[name].[hash].[ext]',
        // 优化分块策略
        manualChunks: {
          'react-core': ['react', 'react-dom'],
          'react-router': ['react-router-dom'],
          'ant-design-core': ['antd'],
          'ant-design-icons': ['@ant-design/icons'],
          'ant-design-pro': ['@ant-design/pro-components'],
          'echarts-core': ['echarts'],
          'echarts-react': ['echarts-for-react'],
          'animation': ['@react-spring/web'],
          'utils': ['lodash', 'dayjs', 'axios']
        }
      }
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
        drop_debugger: true // 生产环境去除debugger
      }
    }
  },
  css: {
    preprocessorOptions: {
      less: {
        javascriptEnabled: true,
        // 全局变量和混合
        additionalData: `
          @import "@/styles/variables.less";
          @import "@/styles/mixins.less";
        `,
        // 修改主题变量
        modifyVars: {
          'primary-color': '#1677ff',
          'link-color': '#1677ff',
          'border-radius-base': '6px',
        },
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
        changeOrigin: true,
      }
    }
  },
  // 预构建优化
  optimizeDeps: {
    include: [
      'react', 
      'react-dom', 
      'react-router-dom',
      'antd',
      '@ant-design/icons',
      '@ant-design/pro-components',
      'echarts',
      'echarts-for-react',
      '@react-spring/web',
      'lodash',
      'axios',
      'dayjs'
    ],
    exclude: [] // 可以排除一些不需要预构建的依赖
  },
  esbuild: {
    jsxInject: `import React from 'react'`,
    // 处理JSX
    jsx: 'automatic',
    // 去除生产环境console
    drop: process.env.NODE_ENV === 'production' ? ['console', 'debugger'] : []
  },
})
