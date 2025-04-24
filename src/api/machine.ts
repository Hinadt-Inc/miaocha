import { request } from './request'
import type { Machine } from '../types/machineTypes'

export const getMachines = () => {
  return request<Machine[]>({
    url: '/api/machines',
    method: 'GET'
  })
}
