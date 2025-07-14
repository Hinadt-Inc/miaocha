/**
 * 通用轮询工具
 * @param task 轮询任务函数
 * @param options 配置选项
 * @returns 包含promise和cancel方法的对象
 */
export interface PollingOptions<T> {
  interval: number;
  maxRetries?: number;
  fetchFn: () => Promise<T>;
  onSuccess: (data: T) => void;
  onError?: (error: Error) => void;
}

export function startPolling<T>(options: PollingOptions<T>): () => void {
  const { interval, maxRetries = 3, fetchFn, onSuccess, onError } = options;
  let retryCount = 0;
  let pollingActive = true;

  const executePoll = async () => {
    try {
      const data = await fetchFn();
      onSuccess(data);
      retryCount = 0;
    } catch (error) {
      retryCount++;
      if (retryCount >= maxRetries) {
        pollingActive = false;
        onError?.(error as Error);
        return;
      }
    }

    if (pollingActive) {
      setTimeout(executePoll, interval);
    }
  };

  executePoll();

  return () => {
    pollingActive = false;
  };
}

export function pollTask<T>(
  task: () => Promise<T>,
  options: {
    interval?: number;
    maxAttempts?: number;
    shouldContinue: (result: T) => boolean;
    onProgress?: (result: T) => void;
  },
): { promise: Promise<T>; cancel: () => void } {
  const { interval = 2000, maxAttempts = 10, shouldContinue, onProgress } = options;

  let cancelled = false;
  let timeoutId: NodeJS.Timeout;

  const cancel = () => {
    cancelled = true;
    if (timeoutId) {
      clearTimeout(timeoutId);
    }
  };

  const promise = new Promise<T>((resolve, reject) => {
    const attempt = (remainingAttempts: number) => {
      if (cancelled) {
        reject(new Error('Polling cancelled'));
        return;
      }

      if (remainingAttempts <= 0) {
        reject(new Error('Max polling attempts reached'));
        return;
      }

      task()
        .then((result) => {
          if (onProgress) {
            onProgress(result);
          }

          if (!shouldContinue(result)) {
            resolve(result);
            return;
          }

          timeoutId = setTimeout(() => {
            attempt(remainingAttempts - 1);
          }, interval);
        })
        .catch((err) => {
          reject(err);
        });
    };

    attempt(maxAttempts);
  });

  return { promise, cancel };
}
