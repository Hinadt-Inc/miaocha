export {};
declare global {
  interface IStatus {
    value?: string;
    label?: string;
    [key: string]: any;
  }
}
