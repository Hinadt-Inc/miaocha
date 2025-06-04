import { request } from './request';
import type { LogstashProcess, LogstashTaskStatus, LogstashTaskSummary, MachineTask } from '../types/logstashTypes';

export function getLogstashProcesses(): Promise<LogstashProcess[]> {
  return request({
    url: '/api/logstash/processes',
    method: 'GET',
  });
}

export function getLogstashProcess(id: number): Promise<LogstashProcess> {
  return request({
    url: `/api/logstash/processes/${id}`,
    method: 'GET',
  });
}

export function createLogstashProcess(data: Partial<LogstashProcess>): Promise<LogstashProcess> {
  return request({
    url: '/api/logstash/processes',
    method: 'POST',
    data,
  });
}

export function updateLogstashProcess(id: number, data: Partial<LogstashProcess>): Promise<LogstashProcess> {
  return request({
    url: `/api/logstash/processes/${id}`,
    method: 'PUT',
    data,
  });
}

export function deleteLogstashProcess(id: number): Promise<void> {
  return request({
    url: `/api/logstash/processes/${id}`,
    method: 'DELETE',
  });
}

export function startLogstashProcess(id: number): Promise<void> {
  return request({
    url: `/api/logstash/processes/${id}/start`,
    method: 'POST',
  });
}

export function startLogstashMachine(processId: number, machineId: number): Promise<void> {
  return request({
    url: `/api/logstash/processes/${processId}/machines/${machineId}/start`,
    method: 'POST',
  });
}

export function stopLogstashMachine(processId: number, machineId: number): Promise<void> {
  return request({
    url: `/api/logstash/processes/${processId}/machines/${machineId}/stop`,
    method: 'POST',
  });
}

export function refreshLogstashMachineConfig(processId: number, machineId: number): Promise<void> {
  return request({
    url: `/api/logstash/processes/${processId}/machines/${machineId}/config/refresh`,
    method: 'POST',
  });
}

export function refreshLogstashConfig(
  processId: number,
  data: {
    machineIds?: number[];
    configContent?: string;
    jvmOptions?: string;
    logstashYml?: string;
  },
): Promise<void> {
  return request({
    url: `/api/logstash/processes/${processId}/config/refresh`,
    method: 'POST',
    data,
  });
}

export function executeLogstashSQL(processId: number, sql: string): Promise<void> {
  return request({
    url: `/api/logstash/processes/${processId}/execute-sql`,
    method: 'POST',
    data: { sql },
  });
}

export function stopLogstashProcess(id: number): Promise<void> {
  return request({
    url: `/api/logstash/processes/${id}/stop`,
    method: 'POST',
  });
}

export function updateLogstashConfig(
  id: number,
  data: {
    machineIds?: number[];
    configContent?: string;
    jvmOptions?: string;
    logstashYml?: string;
  },
): Promise<void> {
  return request({
    url: `/api/logstash/processes/${id}/config`,
    method: 'PUT',
    data,
  });
}

export function getLogstashTaskStatus(id: number): Promise<LogstashTaskStatus> {
  return request({
    url: `/api/logstash/processes/${id}/task-status`,
    method: 'GET',
  });
}

export function getLogstashTaskSummaries(processId: number): Promise<LogstashTaskSummary[]> {
  return request({
    url: `/api/logstash/processes/${processId}/tasks`,
    method: 'GET',
  });
}

export function getMachineTasks(processId: number, machineId: number): Promise<MachineTask[]> {
  return request({
    url: `/api/logstash/processes/${processId}/machines/${machineId}/tasks`,
    method: 'GET',
  });
}

export function updateLogstashMachineConfig(
  processId: number,
  machineId: number,
  data: {
    configContent?: string;
    jvmOptions?: string;
    logstashYml?: string;
  },
): Promise<void> {
  return request({
    url: `/api/logstash/processes/${processId}/machines/${machineId}/config`,
    method: 'PUT',
    data: {
      ...data,
      machineIds: [machineId],
    },
  });
}

export function reinitializeFailedMachines(processId: number): Promise<void> {
  return request({
    url: `/api/logstash/processes/${processId}/reinitialize`,
    method: 'POST',
  });
}

export function reinitializeMachine(processId: number, machineId: number): Promise<void> {
  return request({
    url: `/api/logstash/processes/${processId}/machines/${machineId}/reinitialize`,
    method: 'POST',
  });
}

export function getLogstashMachineDetail(processId: number, machineId: number): Promise<LogstashProcess> {
  return request({
    url: `/api/logstash/processes/${processId}/machines/${machineId}/detail`,
    method: 'GET',
  });
}

export async function scaleProcess(
  id: number,
  params: {
    addMachineIds: number[];
    removeMachineIds: number[];
    customDeployPath?: string;
    forceScale?: boolean;
  },
): Promise<void> {
  return request({
    url: `/api/logstash/processes/${id}/scale`,
    method: 'POST',
    data: params,
  });
}
