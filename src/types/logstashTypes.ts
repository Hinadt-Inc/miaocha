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
  processId: number;
  name: string;
  description: string;
  status: string;
  operationType: string;
  startTime: string;
  endTime: string;
  totalSteps: number;
  completedSteps: number;
  failedSteps: number;
  pendingSteps: number;
  runningSteps: number;
  skippedSteps: number;
  progressPercentage: number;
  errorMessage: string;
}

export interface TaskStepDetail {
  stepId: string;
  stepName: string;
  completedCount: number;
  failedCount: number;
  pendingCount: number;
  runningCount: number;
  skippedCount: number;
  totalCount: number;
  machineSteps: {
    machineId: number;
    machineName: string;
    machineIp: string;
    status: string;
    startTime: string;
    endTime: string;
    errorMessage: string;
  }[];
}

export interface TaskStepsResponse {
  taskId: string;
  taskName: string;
  taskStatus: string;
  steps: TaskStepDetail[];
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

export interface LogstashProcess {
  id: number;
  name: string;
  module: string;
  configJson: string;
  dorisSql: string;
  datasourceId: number;
  datasourceName: string;
  state: LogstashProcessState;
  stateDescription: string;
  machineIds?: number[];
  machines: LogstashMachine[];
  createTime: string;
  updateTime: string;
}
