import { useState } from 'react';
import { useErrorContext } from '@/providers/ErrorProvider';
import {
  deleteLogstashProcess,
  startLogstashProcess,
  stopLogstashProcess,
  createLogstashProcess,
  updateLogstashConfig,
  getLogstashTaskSummaries,
  reinitializeFailedMachines,
  forceStopLogstashProcess,
  refreshLogstashConfig,
  updateLogstashAlertRecipients,
  startLogstashInstances,
  stopLogstashInstances,
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
  const [alertModalVisible, setAlertModalVisible] = useState(false);
  
  // Batch operation states
  const [selectedInstanceIds, setSelectedInstanceIds] = useState<number[]>([]);
  const [batchLoading, setBatchLoading] = useState(false);

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

  const handleShowAlert = (record: LogstashProcess) => {
    setCurrentProcess(record);
    setAlertModalVisible(true);
  };

  const handleSubmitAlert = async (values: Partial<LogstashProcess>) => {
    try {
      if (values.id && values.alertRecipients) {
        // 调用告警邮箱更新 API
        await updateLogstashAlertRecipients(values.id, {
          alertRecipients: values.alertRecipients,
        });
        showSuccess('告警邮箱设置成功');
        await fetchData(); // 刷新数据
      }
    } catch (error) {
      console.error('设置告警邮箱失败:', error);
      // 错误已由全局错误处理器处理
    }
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
        const configData: {
          name: string;
          moduleId: number;
          configContent?: string;
          jvmOptions?: string;
          logstashYml?: string;
        } = {
          name: values.name || currentProcess.name,
          moduleId: values.moduleId || currentProcess.moduleId,
        };

        // 注意：编辑模式下不更新 machineIds，因为部署机器不可编辑
        if (values.configContent) configData.configContent = values.configContent;
        if (values.jvmOptions) configData.jvmOptions = values.jvmOptions;
        if (values.logstashYml) configData.logstashYml = values.logstashYml;
        await updateLogstashConfig(currentProcess.id, configData);

        showSuccess('更新成功');
      } else {
        await createLogstashProcess(values);
        showSuccess('创建成功');
      }
      setEditModalVisible(false);
      await fetchData();
    } catch {
      // API 错误已由全局错误处理器处理
    }
  };

  // ==================== Batch operations ====================

  const handleInstanceSelectionChange = (selectedIds: number[]) => {
    setSelectedInstanceIds(selectedIds);
  };

  const handleBatchStart = async () => {
    if (selectedInstanceIds.length === 0) {
      return;
    }

    try {
      setBatchLoading(true);
      await startLogstashInstances(selectedInstanceIds);
      showSuccess(`批量启动命令已发送，共${selectedInstanceIds.length}个实例`);
      setSelectedInstanceIds([]); // Clear selection after operation
      await fetchData();
    } catch {
      // API 错误已由全局错误处理器处理
    } finally {
      setBatchLoading(false);
    }
  };

  const handleBatchStop = async () => {
    if (selectedInstanceIds.length === 0) {
      return;
    }

    try {
      setBatchLoading(true);
      await stopLogstashInstances(selectedInstanceIds);
      showSuccess(`批量停止命令已发送，共${selectedInstanceIds.length}个实例`);
      setSelectedInstanceIds([]); // Clear selection after operation
      await fetchData();
    } catch {
      // API 错误已由全局错误处理器处理
    } finally {
      setBatchLoading(false);
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
    alertModalVisible,
    setAlertModalVisible,

    // Batch operation states
    selectedInstanceIds,
    batchLoading,

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
    handleShowAlert,
    handleSubmitAlert,

    // Batch operations
    handleInstanceSelectionChange,
    handleBatchStart,
    handleBatchStop,
  };
};
