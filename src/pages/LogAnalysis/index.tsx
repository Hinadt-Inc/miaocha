import { useEffect, useState } from 'react';
import { Table, Button, Skeleton, Layout, Alert } from 'antd';
import { FieldSelector } from '@/components/HomePage/FieldSelector';
import { getMyTablePermissions } from '@/api/permission';
import type { ColumnType } from 'antd/es/table';
import { useRequest } from 'ahooks';
import { searchLogs, getTableColumns } from '@/api/logs';
import type { SearchLogsParams, SearchLogsResult } from '@/types/logDataTypes';
import styles from './index.module.less';

const { Content } = Layout;

const LogAnalysisPage = () => {
  const [availableFields, setAvailableFields] = useState<string[]>([]);
  const [selectedFields, setSelectedFields] = useState<string[]>([]);

  // 获取用户权限字段
  const { data: permissionData, loading: permissionLoading } = useRequest(getMyTablePermissions);

  // 当权限数据加载完成时更新可用字段
  useEffect(() => {
    if (permissionData) {
      const fields = permissionData.flatMap((p) => p.tables?.map((table) => table.tableName) || []);
      setAvailableFields(fields);
      setSelectedFields(fields.slice(0, 5)); // 默认选择前5个字段
    }
  }, [permissionData]);

  // 从API获取字段定义
  const { data: columnDefinitions } = useRequest(() => getTableColumns('1', 'hina-cloud-test-env'));

  // 转换字段定义为表格列配置
  // 转换字段定义为表格列配置
  const tableColumns: ColumnType<Record<string, unknown>>[] = (columnDefinitions || []).map(
    (col) => ({
      title: col.columnName,
      dataIndex: col.columnName,
      key: col.columnName,
      sorter: true,
      ellipsis: true,
      width: 'auto',
      render: (text: string) => (
        <div
          style={{
            maxWidth: 300,
            whiteSpace: 'nowrap',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
          }}
        >
          {text}
        </div>
      ),
      onCell: () => ({
        style: {
          minWidth: 120,
          maxWidth: 600,
          cursor: 'pointer',
        },
      }),
    }),
  );

  const { data, loading, run } = useRequest<SearchLogsResult, [SearchLogsParams]>(
    async (params) => {
      const response = await searchLogs(params);
      return {
        success: response.success,
        errorMessage: response.errorMessage,
        executionTimeMs: response.executionTimeMs,
        columns: response.columns,
        rows: response.rows,
        totalCount: response.totalCount,
        distributionData: response.distributionData,
      };
    },
  );

  // 初始搜索参数
  const searchParams: SearchLogsParams = {
    datasourceId: 1, // 默认数据源ID
    module: 'hina-cloud-test-env', // 模块名称
    tableName: 'hina-cloud-test-env', // 新增表名字段
    pageSize: 10,
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
          <div className={styles.controls}>
            {
              <FieldSelector
                availableFields={availableFields.map((fieldName) => ({
                  columnName: fieldName,
                  dataType: 'string',
                }))}
                selectedFields={selectedFields}
                onToggleField={(fieldName) =>
                  setSelectedFields((prev) =>
                    prev.includes(fieldName)
                      ? prev.filter((f) => f !== fieldName)
                      : [...prev, fieldName],
                  )
                }
              />
            }
            <Button
              type="primary"
              onClick={() => run({ ...searchParams, fields: selectedFields })}
              disabled={selectedFields.length === 0}
            >
              分析选定字段
            </Button>
          </div>
        </div>
        {permissionLoading && <Skeleton paragraph={{ rows: 1 }} />}
        {!permissionLoading && selectedFields.length === 0 && (
          <Alert message="请先选择要分析的字段" type="info" showIcon className={styles.alert} />
        )}

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
