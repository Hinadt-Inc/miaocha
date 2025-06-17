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
  duration: number;
  totalSteps: number;
  successCount: number;
  failedCount: number;
  skippedCount: number;
  progressPercentage: number;
  errorMessage: string;
  instanceSteps: {
    [key: string]: LogstashTaskStep[];
  };
  instanceProgressPercentages: {
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
  instanceSteps: {
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
  instanceProgressPercentages: {
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
  instanceSteps: {
    [key: string]: LogstashTaskStep[];
  };
  instanceProgressPercentages: {
    [key: string]: number;
  };
}
