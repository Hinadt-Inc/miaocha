import { useState } from 'react';
import { useErrorContext } from '@/providers/ErrorProvider';
import {
  deleteLogstashProcess,
  startLogstashProcess,
  stopLogstashProcess,
  createLogstashProcess,
  updateLogstashProcessMetadata,
  updateLogstashConfig,
  getLogstashTaskSummaries,
  reinitializeFailedMachines,
  forceStopLogstashProcess,
  refreshLogstashConfig,
} from '@/api/logstash';
import type { LogstashProcess, LogstashTaskSummary } from '@/types/logstashTypes';

interface UseLogstashActionsProps {
  fetchData: () => Promise<void>;
}

export const useLogstashActions = ({ fetchData }: UseLogstashActionsProps) => {
  const { showSuccess } = useErrorContext();
  const [editModalVisible, setEditModalVisible] = useState(false);
  const [currentProcess, setCurrentProcess] = useState<LogstashProcess | null>(null);
  const [taskSummaries, setTaskSummaries] = useState<LogstashTaskSummary[]>([]);
  const [summaryModalVisible, setSummaryModalVisible] = useState(false);
  const [scaleModalVisible, setScaleModalVisible] = useState(false);
  const [detailModalVisible, setDetailModalVisible] = useState<Record<string, boolean>>({});
  const [currentDetail, setCurrentDetail] = useState<LogstashProcess>();

  const handleAdd = () => {
    setCurrentProcess(null);
    setEditModalVisible(true);
  };

  const handleEdit = (record: LogstashProcess) => {
    const editValues = {
      ...record,
      datasourceId: record.datasourceId,
      tableName: record.tableName,
    };
    setCurrentProcess(editValues);
    setEditModalVisible(true);
  };

  const handleDelete = async (id: number) => {
    try {
      await deleteLogstashProcess(id);
      showSuccess('删除成功');
      await fetchData();
    } catch {
      // API 错误已由全局错误处理器处理
    }
  };

  const handleStart = async (id: number) => {
    try {
      await startLogstashProcess(id);
      showSuccess('启动命令已发送');
      await fetchData();
    } catch {
      // API 错误已由全局错误处理器处理
    }
  };

  const handleStop = async (id: number) => {
    try {
      await stopLogstashProcess(id);
      showSuccess('停止命令已发送');
      await fetchData();
    } catch {
      // API 错误已由全局错误处理器处理
    }
  };

  const handleShowHistory = async (id: number) => {
    try {
      const summaries = await getLogstashTaskSummaries(id);
      setTaskSummaries(summaries);
      setSummaryModalVisible(true);
    } catch (err) {
      console.error('获取任务历史失败:', err);
    }
  };

  const handleScale = (record: LogstashProcess) => {
    setCurrentProcess(record);
    setScaleModalVisible(true);
  };

  const handleRefreshAllConfig = async (record: LogstashProcess) => {
    try {
      const logstashMachineIds = record.logstashMachineStatusInfo
        .filter((machine) => machine.state !== 'RUNNING')
        .map((machine) => machine.logstashMachineId);
      await refreshLogstashConfig(record.id, {
        logstashMachineIds,
      });
      showSuccess('配置刷新命令已发送');
      await fetchData();
    } catch {
      // API 错误已由全局错误处理器处理
    }
  };

  const handleReinitializeFailedMachines = async (processId: number) => {
    try {
      await reinitializeFailedMachines(processId);
      showSuccess('重新初始化命令已发送');
      await fetchData();
    } catch {
      // API 错误已由全局错误处理器处理
    }
  };

  const handleForceStopProcess = async (id: number) => {
    try {
      await forceStopLogstashProcess(id);
      showSuccess('全局强制停止命令已发送');
      await fetchData();
    } catch {
      // API 错误已由全局错误处理器处理
    }
  };

  const handleShowDetail = (record: LogstashProcess) => {
    setCurrentDetail(record);
    setDetailModalVisible({ ...detailModalVisible, [record.id]: true });
  };

  const handleSubmit = async (values: Partial<LogstashProcess>) => {
    try {
      if (currentProcess) {
        await updateLogstashProcessMetadata(currentProcess.id, {
          name: values.name || currentProcess.name,
          moduleId: values.moduleId || currentProcess.moduleId,
        });
        showSuccess('更新成功');
      } else {
        const process = await createLogstashProcess(values);
        showSuccess('创建成功');

        if (!values.jvmOptions || !values.logstashYml) {
          setTimeout(async () => {
            try {
              await updateLogstashConfig(process.id, {
                jvmOptions: values.jvmOptions || '默认JVM参数',
                logstashYml: values.logstashYml || '默认Logstash配置',
              });
              await fetchData();
            } catch (err) {
              console.error('异步更新配置失败:', err);
            }
          }, 3000);
        }
      }
      setEditModalVisible(false);
      await fetchData();
    } catch {
      // API 错误已由全局错误处理器处理
    }
  };

  return {
    // 状态
    editModalVisible,
    setEditModalVisible,
    currentProcess,
    setCurrentProcess,
    taskSummaries,
    summaryModalVisible,
    setSummaryModalVisible,
    scaleModalVisible,
    setScaleModalVisible,
    detailModalVisible,
    setDetailModalVisible,
    currentDetail,
    setCurrentDetail,

    // 动作
    handleAdd,
    handleEdit,
    handleDelete,
    handleStart,
    handleStop,
    handleShowHistory,
    handleScale,
    handleRefreshAllConfig,
    handleReinitializeFailedMachines,
    handleForceStopProcess,
    handleShowDetail,
    handleSubmit,
  };
};
