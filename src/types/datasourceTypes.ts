// 数据源基本类型
export interface DataSource {
  id: string;
  name: string;
  type: 'mysql' | 'postgresql' | 'sqlserver' | 'oracle' | 'mongodb' | 'redis' | 'elasticsearch' | 'clickhouse' | 'hive';
  ip: string;
  port: number;
  username: string;
  password?: string;
  database: string;
  description?: string;
  status?: 'active' | 'inactive';
  createdAt: string;
  updatedAt: string;
  jdbcParams?: {
    connectTimeout: number;
  };
  createUser?: string; // 创建用户
  updateUser?: string; // 更新用户
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
  jdbcParams?: {
    connectTimeout: number;
  };
}

// 更新数据源请求参数
export interface UpdateDataSourceParams extends Partial<CreateDataSourceParams> {
  id: string;
}

// 测试连接请求参数
export interface TestConnectionParams {
  name?: string; // 添加可选的数据源名称
  type: 'mysql' | 'postgresql' | 'sqlserver' | 'oracle' | 'mongodb' | 'redis' | 'elasticsearch';
  ip: string;
  port: number;
  username: string;
  password: string;
  database: string;
  jdbcParams?: {
    connectTimeout: number;
  };
}
