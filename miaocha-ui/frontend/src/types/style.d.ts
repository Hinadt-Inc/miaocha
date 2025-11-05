declare module '*.less' {
  const classes: Readonly<Record<string, string>>;
  export default classes;
}

declare module '*.css' {
  const classes: Readonly<Record<string, string>>;
  export default classes;
}

declare module '*.scss' {
  const classes: Readonly<Record<string, string>>;
  export default classes;
}

// 添加对 CSS 模块的支持
declare module '*.module.less' {
  const classes: Readonly<Record<string, string>>;
  export default classes;
}

declare module '*.module.css' {
  const classes: Readonly<Record<string, string>>;
  export default classes;
}

declare module '*.module.scss' {
  const classes: Readonly<Record<string, string>>;
  export default classes;
}
