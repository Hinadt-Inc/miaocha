import type { Module as BaseModule } from '@/api/modules';

export interface Module extends BaseModule {
  createUser: string;
  updateUser: string;
}

export interface ModuleData extends Module {
  key: string;
  dorisSql: string;
}

export interface ModuleFormData {
  name: string;
  datasourceId: number;
  tableName: string;
}

export interface DeleteModalState {
  visible: boolean;
  record: ModuleData | null;
  deleteDorisTable: boolean;
}

export interface SqlExecuteModalState {
  visible: boolean;
  record: ModuleData | null;
  sql: string;
  executing: boolean;
  readOnly: boolean;
}
