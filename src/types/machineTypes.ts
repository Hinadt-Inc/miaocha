export interface Machine {
  id: number;
  name: string;
  ip: string;
  port: number;
  username: string;
  createTime: string;
  updateTime: string;
  logstashMachineId?: number;
}

export interface CreateMachineParams {
  name: string;
  ip: string;
  port: number;
  username: string;
  password?: string;
  sshKey?: string;
}

export interface UpdateMachineParams extends CreateMachineParams {
  id: number;
}
