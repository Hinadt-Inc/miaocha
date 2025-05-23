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
  duration: number;
  totalSteps: number;
  successCount: number;
  failedCount: number;
  skippedCount: number;
  progressPercentage: number;
  errorMessage: string;
  machineSteps: {
    [key: string]: LogstashTaskStep[];
  };
  machineProgressPercentages: {
    [key: string]: number;
  };
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
  machineSteps: {
    [key: string]: {
      stepId: string;
      stepName: string;
      status: string;
      startTime: string;
      endTime: string;
      duration: number;
      errorMessage: string;
    }[];
  };
  machineProgressPercentages: {
    [key: string]: number;
  };
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
  machineSteps: {
    [key: string]: {
      stepId: string;
      stepName: string;
      status: string;
      startTime: string;
      endTime: string;
      duration: number;
      errorMessage: string;
    }[];
  };
  machineProgressPercentages: {
    [key: string]: number;
  };
}

export interface LogstashProcess {
  id: number;
  name: string;
  module: string;
  description?: string;
  configContent: string;
  jvmOptions?: string;
  logstashYml?: string;
  customPackagePath?: string;
  datasourceId: number;
  datasourceName: string;
  tableName: string;
  state: LogstashProcessState;
  stateDescription: string;
  machineIds?: number[];
  machines: LogstashMachine[];
  machineStatuses: {
    machineId: number;
    machineName: string;
    machineIp: string;
    state: string;
    stateDescription: string;
  }[];
  createTime: string;
  updateTime: string;
  logstashProcessId: number;
  logstashProcessName: string;
  logstashProcessModule: string;
  logstashProcessDescription: string;
  machinePort: number;
  machineUsername: string;
  processPid: string;
  deployPath: string;
  processCreateTime: string;
  processUpdateTime: string;
}
