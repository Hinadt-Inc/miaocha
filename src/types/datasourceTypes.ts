// 数据源基本类型
export interface DataSource {
  id: string;
  name: string;
  type: 'mysql' | 'postgresql' | 'sqlserver' | 'oracle' | 'mongodb' | 'redis' | 'elasticsearch' | 'clickhouse' | 'hive';
  host: string;
  port: number;
  username: string;
  password?: string;
  database: string;
  description?: string;
  status?: 'active' | 'inactive';
  createdAt: string;
  updatedAt: string;
}

// 创建数据源请求参数
export interface CreateDataSourceParams {
  name: string;
  type: 'mysql' | 'postgresql' | 'sqlserver' | 'oracle' | 'mongodb' | 'redis' | 'elasticsearch';
  host: string;
  port: number;
  username: string;
  password: string;
  database: string;
  description?: string;
}

// 更新数据源请求参数
export interface UpdateDataSourceParams extends Partial<CreateDataSourceParams> {
  id: string;
}

// 测试连接请求参数
export interface TestConnectionParams {
  type: 'mysql' | 'postgresql' | 'sqlserver' | 'oracle' | 'mongodb' | 'redis' | 'elasticsearch';
  host: string;
  port: number;
  username: string;
  password: string;
  database: string;
}
