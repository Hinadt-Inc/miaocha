import { defineConfig } from 'vitest/config';
import { resolve } from 'path';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    setupFiles: ['./setupTests.ts'],
    globals: true,
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
    },
    deps: {
      optimizer: {
        web: {
          include: ['monaco-editor'],
        },
      },
    },
  },

  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
      'monaco-editor': resolve(__dirname, 'node_modules/monaco-editor'),
    },
  },
});
