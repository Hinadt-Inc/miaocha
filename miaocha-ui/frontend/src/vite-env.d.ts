/// <reference types="vite/client" />
/// <reference types="vite-plugin-monaco-editor/client" />

// Type declarations for CSS modules
declare module '*.module.less' {
  const classes: Readonly<Record<string, string>>
  export default classes
}

declare module '*.less' { const classes: Readonly<Record<string, string>>; export default classes; } 

declare module 'vite-plugin-static-copy' {
  import { PluginOption } from 'vite';
  interface Target {
    src: string;
    dest: string;
    rename?: string;
  }
  interface Options {
    targets: Target[];
  }
  export default function viteStaticCopy(options: Options): PluginOption;
}
