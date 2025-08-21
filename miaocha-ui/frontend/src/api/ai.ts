import { aiGet } from './request';

// AI会话接口参数
export interface IAISessionParams {
  message: string;
  context?: {
    currentSearchParams?: any;
    logData?: any;
    moduleOptions?: any[];
  };
}

// AI会话接口响应
export interface IAISessionResponse {
  response?: string;
  message?: string;
  success: boolean;
}

// AI会话接口
export const aiSession = (params: IAISessionParams) => {
  return aiGet<IAISessionResponse>('/api/ai/session', params);
};
