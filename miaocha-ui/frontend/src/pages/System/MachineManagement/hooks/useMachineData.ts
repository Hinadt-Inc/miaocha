import { useState, useEffect, useRef } from 'react';
import { getMachines } from '@/api/machine';
import type { Machine } from '@/types/machineTypes';
import { useErrorContext, ErrorType } from '@/providers/ErrorProvider';

// 转换API数据到表格数据
const transformMachineData = (machines: Machine[]) => {
  return machines.map((machine) => ({
    ...machine,
    key: machine.id.toString(),
  }));
};

export const useMachineData = () => {
  const [data, setData] = useState<Machine[]>([]);
  const [loading, setLoading] = useState(false);
  const originalDataRef = useRef<Machine[]>([]);
  const { handleError } = useErrorContext();

  // 获取机器列表
  const fetchMachines = async () => {
    setLoading(true);
    try {
      const res = await getMachines();
      const transformedData = transformMachineData(res);
      setData(transformedData);
      originalDataRef.current = transformedData;
    } catch (error) {
      if (error && typeof error === 'object' && 'name' in error && error.name !== 'CanceledError') {
        handleError(error instanceof Error ? error : new Error('获取机器列表失败'), {
          type: ErrorType.BUSINESS,
          showType: 'notification',
        });
      }
    } finally {
      setLoading(false);
    }
  };

  // 初始化数据
  useEffect(() => {
    const abortController = new AbortController();
    fetchMachines().catch((error: Error) => {
      if (error.name !== 'CanceledError') {
        // API 错误已由全局错误处理器处理，这里不再重复处理
      }
    });

    return () => {
      abortController.abort();
    };
  }, []);

  // 重新加载数据
  const handleReload = () => {
    fetchMachines();
  };

  return {
    data,
    setData,
    loading,
    originalDataRef,
    fetchMachines,
    handleReload,
  };
};
