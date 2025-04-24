import { App } from 'antd';

export function useMessage() {
  const { message } = App.useApp();
  return message;
}
