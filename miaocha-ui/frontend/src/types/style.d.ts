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
