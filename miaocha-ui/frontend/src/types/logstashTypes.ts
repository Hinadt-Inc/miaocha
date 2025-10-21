export interface LogstashProcessMetadataUpdate {
  name: string;
  moduleId: number;
  updateUser: string;
}

export interface LogstashMachine {
  id: number;
  name: string;
  ip: string;
  port: number;
  username: string;
  createTime: string;
  updateTime: string;
}

export interface LogstashTaskStep {
  stepId: string;
  stepName: string;
  status: string;
  startTime: string;
  endTime: string;
  duration: number;
  errorMessage: string;
}

export interface LogstashTaskStatus {
  taskId: string;
  businessId: number;
  name: string;
  description: string;
  status: string;
  operationType: string;
  startTime: string;
  endTime: string;
  createTime: string; // 添加创建时间字段
  duration: number;
  totalSteps: number;
  successCount: number;
  failedCount: number;
  skippedCount: number;
  progressPercentage: number;
  errorMessage: string;
  machineId: number; // 添加机器ID字段
  machineName: string; // 添加机器名称字段
  machineIp: string; // 添加机器IP字段
  processName: string; // 添加进程名称字段
  instanceSteps: Record<string, LogstashTaskStep[]>;
  instanceProgressPercentages: Record<string, number>;
}

export interface LogstashTaskSummary {
  taskId: string;
  businessId: number;
  name: string;
  description: string;
  status: string;
  operationType: string;
  startTime: string;
  endTime: string;
  duration: number;
  totalSteps: number;
  successCount: number;
  failedCount: number;
  skippedCount: number;
  progressPercentage: number;
  errorMessage: string;
  instanceSteps: Record<string, {
      stepId: string;
      stepName: string;
      status: string;
      startTime: string;
      endTime: string;
      duration: number;
      errorMessage: string;
    }[]>;
  instanceProgressPercentages: Record<string, number>;
}

export type LogstashProcessState =
  | 'NOT_STARTED'
  | 'RUNNING'
  | 'STOPPED'
  | 'ERROR'
  | 'STARTING'
  | 'STOPPING'
  | 'RESTARTING'
  | 'UNKNOWN'
  | 'STOP_FAILED';

export interface MachineTask {
  taskId: string;
  businessId: number;
  name: string;
  description: string;
  status: string;
  operationType: string;
  startTime: string;
  endTime: string;
  duration: number;
  totalSteps: number;
  successCount: number;
  failedCount: number;
  skippedCount: number;
  progressPercentage: number;
  errorMessage: string;
  machineSteps: Record<string, {
      stepId: string;
      stepName: string;
      status: string;
      startTime: string;
      endTime: string;
      duration: number;
      errorMessage: string;
    }[]>;
  machineProgressPercentages: Record<string, number>;
}

export interface LogstashProcess {
  id: number;
  name: string;
  moduleName: string;
  moduleId: number;
  description?: string;
  configContent: string;
  jvmOptions?: string;
  logstashYml?: string;
  customPackagePath?: string;
  customDeployPath?: string;
  datasourceId: number;
  datasourceName: string;
  tableName: string;
  state: LogstashProcessState;
  stateDescription: string;
  machineIds?: number[];
  machines: LogstashMachine[];
  logstashMachineStatusInfo: {
    machineId: number;
    machineName: string;
    machineIp: string;
    state: string;
    stateDescription: string;
    logstashMachineId: number;
  }[];
  createTime: string;
  updateTime: string;
  createUser?: string;
  createUserName?: string;
  updateUser?: string;
  updateUserName?: string;
  logstashProcessId: number;
  logstashProcessName: string;
  logstashProcessModule: string;
  logstashProcessDescription: string;
  machineId: number;
  machineName: string;
  machineIp: string;
  machinePort: number;
  machineUsername: string;
  processPid: string;
  deployPath: string;
  processCreateTime: string;
  processUpdateTime: string;
  dorisSql: string;
  alertRecipients?: string[];
}

export interface LogstashTaskStatusV2 {
  taskId: string;
  businessId: number;
  name: string;
  description: string;
  status: string;
  operationType: string;
  startTime: string;
  endTime: string;
  duration: number;
  totalSteps: number;
  successCount: number;
  failedCount: number;
  skippedCount: number;
  progressPercentage: number;
  errorMessage: string;
  instanceSteps: Record<string, LogstashTaskStep[]>;
  instanceProgressPercentages: Record<string, number>;
}
