/// <reference types="vite/client" />
/// <reference types="vite-plugin-monaco-editor/client" />

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
