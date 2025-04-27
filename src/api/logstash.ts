import { request } from './request';
import type { LogstashProcess, LogstashTaskStatus, LogstashTaskSummary, TaskStepsResponse } from '../types/logstashTypes';

export function getLogstashProcesses(): Promise<LogstashProcess[]> {
  return request({
    url: '/api/logstash/processes',
    method: 'GET'
  });
}

export function createLogstashProcess(data: Partial<LogstashProcess>): Promise<LogstashProcess> {
  return request({
    url: '/api/logstash/processes',
    method: 'POST',
    data
  });
}

export function updateLogstashProcess(id: number, data: Partial<LogstashProcess>): Promise<LogstashProcess> {
  return request({
    url: `/api/logstash/processes/${id}`,
    method: 'PUT',
    data
  });
}

export function deleteLogstashProcess(id: number): Promise<void> {
  return request({
    url: `/api/logstash/processes/${id}`,
    method: 'DELETE'
  });
}

export function startLogstashProcess(id: number): Promise<void> {
  return request({
    url: `/api/logstash/processes/${id}/start`,
    method: 'POST'
  });
}

export function stopLogstashProcess(id: number): Promise<void> {
  return request({
    url: `/api/logstash/processes/${id}/stop`,
    method: 'POST'
  });
}

export function getLogstashTaskStatus(id: number): Promise<LogstashTaskStatus> {
  return request({
    url: `/api/logstash/processes/${id}/task-status`, 
    method: 'GET'
  });
}

export function getLogstashTaskSummaries(processId: number): Promise<LogstashTaskSummary[]> {
  return request({
    url: `/api/logstash/processes/${processId}/tasks`,
    method: 'GET'
  });
}

export function getTaskSteps(taskId: string): Promise<TaskStepsResponse> {
  return request({
    url: `/api/logstash/processes/tasks/${taskId}/steps`,
    method: 'GET'
  });
}
