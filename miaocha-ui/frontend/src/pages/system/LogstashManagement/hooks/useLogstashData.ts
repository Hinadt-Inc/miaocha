import { useState, useEffect } from 'react';
import { message } from 'antd';
import { getLogstashProcesses } from '@/api/logstash';
import type { LogstashProcess } from '@/types/logstashTypes';

export const useLogstashData = () => {
  const [data, setData] = useState<LogstashProcess[]>([]);
  const [loading, setLoading] = useState(false);
  const [messageApi, contextHolder] = message.useMessage();

  const fetchData = async () => {
    setLoading(true);
    try {
      const processes = await getLogstashProcesses();
      setData(processes);
    } catch (err) {
      console.error('获取Logstash进程列表失败:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleReload = () => {
    fetchData();
  };

  useEffect(() => {
    fetchData().catch((err) => {
      console.error('组件加载失败:', err);
    });
  }, []);

  return {
    data,
    setData,
    loading,
    fetchData,
    handleReload,
    messageApi,
    contextHolder,
  };
};
