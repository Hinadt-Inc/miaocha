import { ProTable } from '@ant-design/pro-components';
import { DataSourcePageHeader, DataSourceFormModal, DataSourceToolBar } from './components';
import { useDataSourceData, useTableConfig, useDataSourceActions } from './hooks';
import Loading from '@/components/Loading';
import styles from './DataSourceManagement.module.less';
import withSystemAccess from '@/utils/withSystemAccess';

const DataSourceManagementPage = () => {
  const {
    searchKeyword,
    pagination,
    loading,
    fetchDataSources,
    handlePageChange,
    handleSearchChange,
    setSubmitLoading,
    setTestLoading,
    setTestExistingLoading,
  } = useDataSourceData();

  const actions = useDataSourceActions({
    setSubmitLoading,
    setTestLoading,
    setTestExistingLoading,
  });

  const { columns } = useTableConfig({
    testExistingLoading: loading.testExisting,
    onEdit: actions.openEditModal,
    onDelete: (id: string, name: string) => actions.handleDelete(id, name),
    onTestConnection: (id: string, name: string) => actions.handleTestExistingConnection(id, name),
  });

  return (
    <div className={styles.container}>
      <div style={{ position: 'relative', minHeight: '400px' }}>
        <ProTable
          loading={false}
          className={styles.tableContainer}
          bordered
          size="small"
          search={false}
          options={false}
          headerTitle={<DataSourcePageHeader />}
          toolBarRender={() => [
            <DataSourceToolBar
              key="toolbar"
              searchKeyword={searchKeyword}
              onSearchChange={(value) => {
                handleSearchChange(value);
                // 当输入变化时立即搜索，提供更即时的反馈
                actions.actionRef.current?.reload();
              }}
              onAdd={actions.openCreateModal}
            />,
          ]}
          actionRef={actions.actionRef}
          rowKey="id"
          scroll={{ x: 'max-content' }}
          cardProps={{ bodyStyle: { padding: '0px' } }}
          request={fetchDataSources}
          defaultData={[]}
          columns={columns}
          pagination={{
            current: pagination.current,
            pageSize: pagination.pageSize,
            onChange: handlePageChange,
            showSizeChanger: true,
            responsive: true,
            showTotal: (total) => `共 ${total} 条`,
            pageSizeOptions: ['10', '20', '50', '100'],
          }}
        />
        {loading.table && (
          <Loading
            fullScreen={false}
            size="large"
            tip="加载数据源..."
            style={{
              position: 'absolute',
              top: 0,
              left: 0,
              right: 0,
              bottom: 0,
              zIndex: 1000,
              backgroundColor: 'rgba(255, 255, 255, 0.85)',
              backdropFilter: 'blur(2px)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          />
        )}
      </div>

      <DataSourceFormModal
        visible={actions.modalVisible}
        currentDataSource={actions.currentDataSource}
        testLoading={loading.test}
        submitLoading={loading.submit}
        onSubmit={actions.handleFormSubmit}
        onCancel={() => actions.setModalVisible(false)}
        onTestConnection={actions.handleTestConnection}
      />
    </div>
  );
};

export default withSystemAccess(DataSourceManagementPage);
