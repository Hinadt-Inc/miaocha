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

export async function startLogTail(logstashMachineId: number, tailLines: number = 100): Promise<EventSource> {
  try {
    // 首先发送POST请求启动日志跟踪
    await request({
      url: '/api/logstash/log-tail/start',
      method: 'POST',
      data: { logstashMachineId, tailLines },
    });

    // 然后创建 EventSource 来监听日志流
    console.log('Creating EventSource for machine ID:', logstashMachineId);

    // 构建 EventSource URL，包含认证信息
    const token = localStorage.getItem('accessToken');
    const url = new URL('/api/logstash/log-tail/stream', window.location.origin);
    url.searchParams.append('logstashMachineId', logstashMachineId.toString());
    url.searchParams.append('tailLines', tailLines.toString());
    if (token) {
      url.searchParams.append('token', token);
    }

    // 创建 EventSource 对象
    const eventSource = new EventSource(url.toString(), {
      withCredentials: true,
    });

    // 添加通用的错误处理
    eventSource.onerror = (err) => {
      console.error('EventSource error in startLogTail:', err);
    };

    console.log('Created EventSource:', eventSource);
    return eventSource;
  } catch (error) {
    console.error('Error starting log tail:', error);
    throw error;
  }
}

export function getLogTailStatus(): Promise<any> {
  return request({
    url: '/api/logstash/log-tail/status',
    method: 'GET',
  });
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
