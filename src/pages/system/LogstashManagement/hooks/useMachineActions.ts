import { useState } from 'react';
import { useErrorContext } from '@/providers/ErrorProvider';
import {
  startLogstashMachine,
  stopLogstashMachine,
  refreshLogstashConfig,
  reinitializeMachine,
  forceStopLogstashMachine,
  scaleProcess,
  getLogstashProcess,
  getLogstashInstanceTasks,
} from '@/api/logstash';
import type { LogstashTaskStatus } from '@/types/logstashTypes';

interface UseMachineActionsProps {
  fetchData: () => Promise<void>;
}

export const useMachineActions = ({ fetchData }: UseMachineActionsProps) => {
  const { showSuccess, showWarning } = useErrorContext();
  const [machineTasks, setMachineTasks] = useState<LogstashTaskStatus[]>([]);
  const [machineTasksModalVisible, setMachineTasksModalVisible] = useState(false);
  const [machineTasksLoading, setMachineTasksLoading] = useState(false);
  const [currentMachine, setCurrentMachine] = useState<{
    configContent?: string;
    jvmOptions?: string;
    logstashYml?: string;
    logstashMachineId?: number;
    processId?: number;
  } | null>(null);
  const [machineConfigModalVisible, setMachineConfigModalVisible] = useState(false);
  const [machineDetailModalVisible, setMachineDetailModalVisible] = useState(false);
  const [currentMachineDetail, setCurrentMachineDetail] = useState<any>();
  const [logTailModalVisible, setLogTailModalVisible] = useState(false);
  const [bottomLogTailModalVisible, setBottomLogTailModalVisible] = useState(false);
  const [currentLogTailMachineId, setCurrentLogTailMachineId] = useState<number>();

  const handleStartMachine = async (machineId: number) => {
    try {
      await startLogstashMachine(machineId);
      showSuccess('启动命令已发送');
      await fetchData();
    } catch {
      // API 错误已由全局错误处理器处理
    }
  };

  const handleStopMachine = async (machineId: number) => {
    try {
      await stopLogstashMachine(machineId);
      showSuccess('停止命令已发送');
      await fetchData();
    } catch {
      // API 错误已由全局错误处理器处理
    }
  };

  const handleRefreshConfig = async (processId: number, machineId: number) => {
    try {
      await refreshLogstashConfig(processId, {
        logstashMachineIds: [machineId],
      });
      showSuccess('配置刷新命令已发送');
      await fetchData();
    } catch {
      // API 错误已由全局错误处理器处理
    }
  };

  const handleReinitializeMachine = async (machineId: number) => {
    try {
      await reinitializeMachine(machineId);
      showSuccess('重新初始化命令已发送');
      await fetchData();
    } catch {
      // API 错误已由全局错误处理器处理
    }
  };

  const handleForceStopMachine = async (processId: number, machineId: number) => {
    try {
      await forceStopLogstashMachine(processId, machineId);
      showSuccess('强制停止命令已发送');
      await fetchData();
    } catch {
      // API 错误已由全局错误处理器处理
    }
  };

  const handleDeleteMachine = async (processId: number, logstashMachineId: number) => {
    try {
      await scaleProcess(processId, {
        addMachineIds: [],
        removeLogstashMachineIds: [logstashMachineId],
        customDeployPath: '',
        forceScale: false,
      });
      showSuccess('机器删除成功');
      await fetchData();
    } catch {
      // API 错误已由全局错误处理器处理
    }
  };

  const showMachineTasks = async (processId: number, logstashMachineId: number) => {
    setCurrentMachine({ logstashMachineId, processId });
    setMachineTasksModalVisible(true);
    setMachineTasksLoading(true);

    try {
      const tasks = await getLogstashInstanceTasks(logstashMachineId.toString());
      setMachineTasks(tasks);
    } catch (err) {
      console.error('获取实例任务失败:', err);
    } finally {
      setMachineTasksLoading(false);
    }
  };

  const handleEditMachineConfig = async (machineId: number, processId: number, data: any[]) => {
    try {
      const machineDetail = await getLogstashProcess(machineId);
      setCurrentMachine({
        logstashMachineId: machineId,
        processId: processId,
        configContent: machineDetail.configContent,
        jvmOptions: machineDetail.jvmOptions,
        logstashYml: machineDetail.logstashYml,
      });
      setMachineConfigModalVisible(true);
    } catch {
      showWarning('获取机器配置失败，使用进程级别配置');
      const process = data.find((p: any) => p.id === processId);
      setCurrentMachine({
        logstashMachineId: machineId,
        processId: processId,
        configContent: process?.configContent,
        jvmOptions: process?.jvmOptions,
        logstashYml: process?.logstashYml,
      });
      setMachineConfigModalVisible(true);
    }
  };

  const handleShowMachineDetail = async (machineId: number) => {
    try {
      const detail = await getLogstashProcess(machineId);
      setCurrentMachineDetail(detail);
      setMachineDetailModalVisible(true);
    } catch {
      // API 错误已由全局错误处理器处理
    }
  };

  const handleShowLog = (machineId: number, isBottom = false) => {
    setCurrentLogTailMachineId(machineId);
    if (isBottom) {
      setBottomLogTailModalVisible(true);
    } else {
      setLogTailModalVisible(true);
    }
  };

  return {
    // 状态
    machineTasks,
    machineTasksModalVisible,
    setMachineTasksModalVisible,
    machineTasksLoading,
    currentMachine,
    setCurrentMachine,
    machineConfigModalVisible,
    setMachineConfigModalVisible,
    machineDetailModalVisible,
    setMachineDetailModalVisible,
    currentMachineDetail,
    setCurrentMachineDetail,
    logTailModalVisible,
    setLogTailModalVisible,
    bottomLogTailModalVisible,
    setBottomLogTailModalVisible,
    currentLogTailMachineId,

    // 动作
    handleStartMachine,
    handleStopMachine,
    handleRefreshConfig,
    handleReinitializeMachine,
    handleForceStopMachine,
    handleDeleteMachine,
    showMachineTasks,
    handleEditMachineConfig,
    handleShowMachineDetail,
    handleShowLog,
  };
};
