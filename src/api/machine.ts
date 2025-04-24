import { request } from './request'
import type { Machine, CreateMachineParams, UpdateMachineParams } from '../types/machineTypes'

export const getMachines = () => {
  return request<Machine[]>({
    url: '/api/machines',
    method: 'GET'
  })
}

export const createMachine = (params: CreateMachineParams) => {
  return request<Machine>({
    url: '/api/machines',
    method: 'POST',
    data: params
  })
}

export const deleteMachine = (id: string | number) => {
  return request({
    url: `/api/machines/${id}`,
    method: 'DELETE'
  })
}

export const updateMachine = (params: UpdateMachineParams) => {
  return request<Machine>({
    url: `/api/machines/${params.id}`,
    method: 'PUT',
    data: params
  })
}

export const testMachineConnection = (id: string | number) => {
  return request<{ success: boolean; message?: string }>({
    url: `/api/machines/${id}/test-connection`,
    method: 'POST'
  })
}
