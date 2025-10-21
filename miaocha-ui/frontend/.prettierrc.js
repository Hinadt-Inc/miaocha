export default {
  singleQuote: true,
  arrowParens: 'always',
  useTabs: false,
  bracketSpacing: true,
  endOfLine: 'auto',
  semi: true,
  tabWidth: 2,
  printWidth: 120,
  trailingComma: 'all',
  bracketSameLine: false,
  jsxSingleQuote: false,
  quoteProps: 'as-needed',
  htmlWhitespaceSensitivity: 'css',
  // 按文件类型设置 parser，避免 JSON 被 typescript 解析
  overrides: [
    {
      files: ['*.ts', '*.tsx', '*.js', '*.jsx'],
      options: { parser: 'typescript' },
    },
    {
      files: ['*.json'],
      options: { parser: 'json' },
    },
  ],
};
