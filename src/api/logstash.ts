import { request } from './request';
import type { LogstashProcess, LogstashTaskStatus, LogstashTaskSummary, MachineTask } from '../types/logstashTypes';

export function getLogstashProcesses(): Promise<LogstashProcess[]> {
  return request({
    url: '/api/logstash/processes',
    method: 'GET',
  });
}

export function getLogstashProcess(instanceId: number): Promise<LogstashProcess> {
  return request({
    url: `/api/logstash/processes/instances/${instanceId}`,
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

export function startLogstashMachine(instanceId: number): Promise<void> {
  return request({
    url: `/api/logstash/processes/instances/${instanceId}/start`,
    method: 'POST',
  });
}

export function stopLogstashMachine(instanceId: number): Promise<void> {
  return request({
    url: `/api/logstash/processes/instances/${instanceId}/stop`,
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
  logstashMachineId: number,
  data: {
    logstashMachineIds?: number[];
    configContent?: string;
    jvmOptions?: string;
    logstashYml?: string;
  },
): Promise<void> {
  return request({
    url: `/api/logstash/processes/${processId}/config`,
    method: 'PUT',
    data: {
      ...data,
      logstashMachineIds: [logstashMachineId],
    },
  });
}

export function reinitializeFailedMachines(processId: number): Promise<void> {
  return request({
    url: `/api/logstash/processes/${processId}/reinitialize`,
    method: 'POST',
  });
}

export function reinitializeMachine(instanceId: number): Promise<void> {
  return request({
    url: `/api/logstash/processes/instances/${instanceId}/reinitialize`,
    method: 'POST',
  });
}

export function getLogstashMachineDetail(processId: number, machineId: number): Promise<LogstashProcess> {
  return request({
    url: `/api/logstash/processes/${processId}/machines/${machineId}/detail`,
    method: 'GET',
  });
}

export function forceStopLogstashMachine(processId: number, machineId: number): Promise<void> {
  return request({
    url: `/api/logstash/processes/${processId}/machines/${machineId}/force-stop`,
    method: 'POST',
  });
}

export function forceStopLogstashProcess(id: number): Promise<void> {
  return request({
    url: `/api/logstash/processes/${id}/force-stop`,
    method: 'POST',
  });
}

export function getLogstashInstanceTasks(instanceId: string): Promise<LogstashTaskStatus[]> {
  return request({
    url: `/api/logstash/processes/instances/${instanceId}/tasks`,
    method: 'GET',
  });
}

export function updateLogstashProcessMetadata(
  id: number,
  data: {
    name: string;
    moduleId: number;
    updateUser?: string;
  },
): Promise<void> {
  return request({
    url: `/api/logstash/processes/${id}/metadata`,
    method: 'PUT',
    data,
  });
}

export async function scaleProcess(
  id: number,
  params: {
    addMachineIds: number[];
    removeLogstashMachineIds: number[];
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

export async function startLogTail(logstashMachineId: number) {
  const accessToken = localStorage.getItem('accessToken') || '';
  const eventSource = new EventSource(
    `/api/logstash/log-tail/stream/${logstashMachineId}?token=${encodeURIComponent(accessToken)}`,
    {
      withCredentials: true,
    },
  );
  eventSource.close = () => {
    stopLogTail(logstashMachineId);
  };
  return eventSource;
}

export function stopLogTail(logstashMachineId: number): Promise<void> {
  return request({
    url: `/api/logstash/log-tail/stop/${logstashMachineId}`,
    method: 'DELETE',
  });
}

export function getLogTailContent(lastLogId?: string): Promise<{ logs: string[]; lastLogId: string }> {
  return request({
    url: '/api/logstash/log-tail/content',
    method: 'GET',
    params: { lastLogId },
  });
}

export function createLogTailTask(logstashMachineId: number, tailLines: number = 100): Promise<{ taskId: string }> {
  return request({
    url: '/api/logstash/log-tail/create',
    method: 'POST',
    data: { logstashMachineId, tailLines },
  });
}
