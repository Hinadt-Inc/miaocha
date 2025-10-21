import { ProTable } from '@ant-design/pro-components';
import { DataSourcePageHeader, DataSourceFormModal, DataSourceToolBar } from './components';
import { useDataSourceData, useTableConfig, useDataSourceActions } from './hooks';
import Loading from '@/components/Loading';
import styles from './DataSourceManagement.module.less';
import withAdminAuth from '@/utils/withAdminAuth';

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
          actionRef={actions.actionRef}
          bordered
          cardProps={{ bodyStyle: { padding: '0px' } }}
          className={styles.tableContainer}
          columns={columns}
          defaultData={[]}
          headerTitle={<DataSourcePageHeader />}
          loading={false}
          options={false}
          pagination={{
            current: pagination.current,
            pageSize: pagination.pageSize,
            onChange: handlePageChange,
            showSizeChanger: true,
            responsive: true,
            showTotal: (total) => `共 ${total} 条`,
            pageSizeOptions: ['10', '20', '50', '100'],
          }}
          request={fetchDataSources}
          rowKey="id"
          scroll={{ x: 'max-content' }}
          search={false}
          size="small"
          toolBarRender={() => [
            <DataSourceToolBar
              key="toolbar"
              searchKeyword={searchKeyword}
              onAdd={actions.openCreateModal}
              onSearchChange={(value) => {
                handleSearchChange(value);
                // 当输入变化时立即搜索，提供更即时的反馈
                actions.actionRef.current?.reload();
              }}
            />,
          ]}
        />
        {loading.table && (
          <Loading
            fullScreen={false}
            size="large"
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
            tip="加载数据源..."
          />
        )}
      </div>

      <DataSourceFormModal
        currentDataSource={actions.currentDataSource}
        submitLoading={loading.submit}
        testLoading={loading.test}
        visible={actions.modalVisible}
        onCancel={() => actions.setModalVisible(false)}
        onSubmit={actions.handleFormSubmit}
        onTestConnection={actions.handleTestConnection}
      />
    </div>
  );
};

export default withAdminAuth(DataSourceManagementPage);
