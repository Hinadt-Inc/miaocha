import { message } from 'antd';

export function useMessage() {
  const [messageApi] = message.useMessage();
  const showMessage = (type: 'success' | 'error' | 'info', content: string) => {
    messageApi[type]({
      content,
      duration: 2,
    });
  };
  return {
    success: (content: string) => showMessage('success', content),
    error: (content: string) => showMessage('error', content),
    info: (content: string) => showMessage('info', content),
  };
}
