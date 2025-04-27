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
  machineSteps: Record<string, LogstashTaskStep[]>;
  machineProgressPercentages: Record<string, number>;
}

export type LogstashProcessState = 'NOT_STARTED' | 'RUNNING' | 'STOPPED' | 'ERROR';

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
