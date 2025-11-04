// eslint.config.js
// 说明：这是项目的 ESLint 主配置文件，采用新版 "flat config" 形式（typescript-eslint 提供的组合器）。

import js from '@eslint/js'; // 官方 JS 推荐规则集合
import importPlugin from 'eslint-plugin-import'; // Import 排序和规范相关规则
import reactPlugin from 'eslint-plugin-react'; // React 组件与 JSX 相关规则
import reactHooks from 'eslint-plugin-react-hooks'; // React Hooks 相关规则（useEffect 依赖等）
import reactRefresh from 'eslint-plugin-react-refresh'; // 开发模式下的 React Fast Refresh 约束
import globals from 'globals'; // 常用运行时全局变量（browser、node 等）
import tseslint from 'typescript-eslint'; // TypeScript 与 ESLint 的集成（含规则集合与解析器）

export default tseslint.config(
  {
    // ignores 列表越精准，lint 越聚焦真实业务代码
    ignores: ['dist', 'node_modules', 'build', 'coverage', '**/*.md', 'setupTests.ts', 'vitest.config.ts'],
  },

  // TypeScript + React：这是主要规则块，限定只检查 src 下的 TS/TSX 文件，避免误把根目录其他文件按 TS 工程解析
  {
    files: ['src/**/*.{ts,tsx}'], // 仅 lint src 内的 TypeScript/React 源码，防止解析到非工程文件
    extends: [
      js.configs.recommended, // JS 基础推荐规则（合理但不过度）
      ...tseslint.configs.recommended, // TS 推荐规则（非 type-checked 版本，避免过度严格）
      ...tseslint.configs.stylistic, // 风格规则（格式与编码风格相关）
      // 保留 React 插件与规则（不使用兼容层）：React 相关规则通过 plugins+rules 配置
    ],
    languageOptions: {
      ecmaVersion: 'latest', // 解析最新 ECMAScript 语法
      globals: {
        ...globals.browser, // 注入浏览器运行时全局变量（window、document 等）
        ...globals.node, // 注入 Node 运行时全局变量（process、__dirname 等）
      },
      parserOptions: {
        tsconfigRootDir: import.meta.dirname, // 以当前文件目录为 tsconfig 根，保证路径稳定
        ecmaFeatures: { jsx: true }, // 启用 JSX 解析能力
      },
    },
    plugins: {
      'react-hooks': reactHooks, // 启用 React Hooks 插件
      'react-refresh': reactRefresh, // 启用 React Fast Refresh 插件
      react: reactPlugin, // 启用 React 基础规则插件
      import: importPlugin, // 启用 Import 规则插件
    },
    rules: {
      // React Hooks 官方推荐规则：确保 hooks 使用规范（比如 useEffect 依赖项管理）
      ...reactHooks.configs.recommended.rules,

      // 仅在开发环境下提醒导出组件相关约束，降低误报
      'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],

      // 类型相关规则的“降噪”策略：
      // - 允许 any（在实际业务中快速迭代时常用；关键路径再做精细化）
      '@typescript-eslint/no-explicit-any': 'off',
      // - 未使用变量，并允许以下划线开头的“有意忽略”变量
      '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_', varsIgnorePattern: '^_' }],
      // - 函数返回类型/模块边界类型不强制，避免样板代码与噪音
      '@typescript-eslint/explicit-function-return-type': 'off',
      '@typescript-eslint/explicit-module-boundary-types': 'off',
      // - 非空断言设为 warn：提醒但不强制禁止（在确知数据存在时可用）
      '@typescript-eslint/no-non-null-assertion': 'error',
      // - 关闭“多余类型断言”规则，避免在联合类型/控制流复杂时产生噪音
      '@typescript-eslint/no-unnecessary-type-assertion': 'off',

      // React 常见规则调整：
      'react/prop-types': 'off', // TypeScript 项目通常不使用 PropTypes
      'react/react-in-jsx-scope': 'off', // 新版 React JSX 无需显式引入 React

      // Import 排序规则
      'import/order': [
        'error',
        {
          groups: ['builtin', 'external', 'internal', 'parent', 'sibling', 'index'],
          pathGroups: [
            {
              pattern: 'react',
              group: 'external',
              position: 'before',
            },
            {
              pattern: '@/**',
              group: 'internal',
              position: 'after',
            },
          ],
          pathGroupsExcludedImportTypes: ['react'],
          'newlines-between': 'always',
          alphabetize: {
            order: 'asc',
            caseInsensitive: true,
          },
        },
      ],

      // JSX 属性排序：设为 error，以统一属性书写顺序（提升可读性与一致性）
      'react/jsx-sort-props': [
        'error',
        {
          callbacksLast: true, // 回调放最后
          ignoreCase: true, // 忽略大小写排序差异
          noSortAlphabetically: false, // 允许按字母排序
          reservedFirst: true, // 保留属性（如 key）优先
        },
      ],

      // 通用代码风格：
      'no-console': ['warn', { allow: ['warn', 'error'] }], // 控制台输出仅允许 warn/error
      'prefer-const': 'warn', // 能用 const 就用 const，减少可变性

      // 进一步降低不必要提示的规则：
      '@typescript-eslint/require-await': 'off',
      '@typescript-eslint/no-inferrable-types': 'off',
      '@typescript-eslint/consistent-indexed-object-style': 'off', // 不强制 Record 风格，保留索引签名灵活性

      'react-hooks/exhaustive-deps': 'off',
    },
    settings: {
      react: { version: 'detect' }, // 自动检测 React 版本，避免手动维护
      'import/resolver': {
        typescript: {
          alwaysTryTypes: true,
          project: './tsconfig.json',
        },
      },
    },
  },
);
