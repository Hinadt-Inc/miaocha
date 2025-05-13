import { useEffect } from 'react';
import { Table, Button, Skeleton, Layout } from 'antd';
import PageContainer from '@/components/common/PageContainer';
import MainLayout from '@/layouts/MainLayout';
import type { ColumnType } from 'antd/es/table';
import { useRequest } from 'ahooks';
import { searchLogs, getTableColumns } from '@/api/logs';
import type { SearchLogsParams, SearchLogsResult } from '@/types/logDataTypes';
import styles from './index.module.less';

const { Content } = Layout;

const LogAnalysisPage = () => {
  // 从API获取字段定义
  const { data: columnDefinitions } = useRequest(() => getTableColumns('1', 'logAnalysis'));

  // 转换字段定义为表格列配置
  const tableColumns: ColumnType<Record<string, unknown>>[] = (columnDefinitions || []).map(
    (col) => ({
      title: col.columnName,
      dataIndex: col.columnName,
      key: col.columnName,
      sorter: true,
    }),
  );

  const { data, loading, run } = useRequest<SearchLogsResult, [SearchLogsParams]>((params) =>
    searchLogs(params),
  );

  // 初始搜索参数
  const searchParams: SearchLogsParams = {
    datasourceId: 1, // 默认数据源ID
    tableName: 'log_table', // 默认日志表名
    pageSize: 50,
    offset: 0,
  };

  // 初始化加载数据
  useEffect(() => {
    run(searchParams);
  }, []);

  return (
    <Layout className={styles.layout}>
      <Content className={styles.content}>
        <div className={styles.header}>
          <h2>日志分析</h2>
          <Button type="primary" onClick={() => run(searchParams)}>
            刷新数据
          </Button>
        </div>

        {loading ? (
          <Skeleton active />
        ) : (
          <Table
            columns={tableColumns}
            dataSource={data?.rows || []}
            rowKey="log_time"
            pagination={{ pageSize: 50 }}
            scroll={{ y: 600 }}
          />
        )}
      </Content>
    </Layout>
  );
};

export default LogAnalysisPage;
