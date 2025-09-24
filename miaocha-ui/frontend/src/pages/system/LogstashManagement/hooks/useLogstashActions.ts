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
        // Call alert recipients update API
        await updateLogstashAlertRecipients(values.id, {
          alertRecipients: values.alertRecipients,
        });
        showSuccess('Alert recipients configured successfully');
        await fetchData(); // Refresh data
      }
    } catch (error) {
      console.error('Failed to configure alert recipients:', error);
      // 错误已由全局错误处理器处理
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await deleteLogstashProcess(id);
      showSuccess('Deleted successfully');
      await fetchData();
    } catch {
      // API errors handled by global error handler
    }
  };

  const handleStart = async (id: number) => {
    try {
      await startLogstashProcess(id);
      showSuccess('Start command sent');
      await fetchData();
    } catch {
      // API errors handled by global error handler
    }
  };

  const handleStop = async (id: number) => {
    try {
      await stopLogstashProcess(id);
      showSuccess('Stop command sent');
      await fetchData();
    } catch {
      // API errors handled by global error handler
    }
  };

  const handleShowHistory = async (id: number) => {
    try {
      const summaries = await getLogstashTaskSummaries(id);
      setTaskSummaries(summaries);
      setSummaryModalVisible(true);
    } catch (err) {
      console.error('Failed to fetch task history:', err);
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
      showSuccess('Config refresh command sent');
      await fetchData();
    } catch {
      // API errors handled by global error handler
    }
  };

  const handleReinitializeFailedMachines = async (processId: number) => {
    try {
      await reinitializeFailedMachines(processId);
      showSuccess('Reinitialize command sent');
      await fetchData();
    } catch {
      // API errors handled by global error handler
    }
  };

  const handleForceStopProcess = async (id: number) => {
    try {
      await forceStopLogstashProcess(id);
      showSuccess('Global force stop command sent');
      await fetchData();
    } catch {
      // API errors handled by global error handler
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

        // Note: Do not update machineIds in edit mode, since deployment machines are not editable
        if (values.configContent) configData.configContent = values.configContent;
        if (values.jvmOptions) configData.jvmOptions = values.jvmOptions;
        if (values.logstashYml) configData.logstashYml = values.logstashYml;
        await updateLogstashConfig(currentProcess.id, configData);

        showSuccess('Updated successfully');
      } else {
        await createLogstashProcess(values);
        showSuccess('Created successfully');
      }
      setEditModalVisible(false);
      await fetchData();
    } catch {
      // API errors handled by global error handler
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
      showSuccess(`Batch start command sent for ${selectedInstanceIds.length} instances`);
      setSelectedInstanceIds([]); // Clear selection after operation
      await fetchData();
    } catch {
      // API errors handled by global error handler
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
      showSuccess(`Batch stop command sent for ${selectedInstanceIds.length} instances`);
      setSelectedInstanceIds([]); // Clear selection after operation
      await fetchData();
    } catch {
      // API errors handled by global error handler
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
