import { Table, Tooltip, Button } from 'antd';
import { DatabaseOutlined } from '@ant-design/icons';
import ExecuteConfirmationModal from '@/pages/SQLEditor/components/ExecuteConfirmationModal';
import {
  ModuleFormModal,
  ModuleDetailModal,
  DeleteConfirmModal,
  ModulePageHeader,
  ModuleQueryConfigModal,
} from './components';
import { useModuleData, useTableConfig, useModuleActions } from './hooks';
import Loading from '@/components/Loading';
import styles from './ModuleManagement.module.less';

const ModuleManagementPage = () => {
  const {
    data,
    dataSources,
    loading,
    searchText,
    fetchModuleDetail,
    handleSearch,
    handleReload,
    messageApi,
    contextHolder,
  } = useModuleData();

  const actions = useModuleActions({
    messageApi,
    onDataChange: handleReload,
  });

  const handleViewDetail = (record: any) => {
    fetchModuleDetail(Number(record.key))
      .then((detail) => {
        actions.handleViewDetail(detail);
      })
      .catch(() => {
        // Error handling is already done in fetchModuleDetail
      });
  };

  const { columns, pagination, handleTableChange } = useTableConfig({
    onViewDetail: handleViewDetail,
    onEdit: actions.handleAddEdit,
    onExecuteSql: actions.handleExecuteSql,
    onDelete: actions.handleDelete,
    onConfig: actions.handleConfig,
    onStatusToggle: actions.handleStatusToggle,
  }) as unknown as { columns: any; pagination: any; handleTableChange: any };

  const sqlModalTitle = (
    <div className={styles.sqlModalTitle}>
      <span>
        {actions.isReadOnlyMode ? '查看' : '执行'}Doris SQL - <strong>{actions.currentRecord?.name}</strong>
      </span>
      {!actions.isReadOnlyMode && (
        <Tooltip title="应用模板的Doris SQL语句">
          <Button
            className={styles.templateButton}
            icon={<DatabaseOutlined />}
            type="text"
            onClick={actions.handleApplyTemplate}
          />
        </Tooltip>
      )}
    </div>
  );

  return (
    <div className={styles.container}>
      {contextHolder}

      <div className={styles.header}>
        <ModulePageHeader
          loading={loading}
          searchText={searchText}
          onAdd={() => actions.handleAddEdit()}
          onReload={handleReload}
          onSearch={handleSearch}
        />
      </div>

      <div className={styles.antTable}>
        <div style={{ position: 'relative' }}>
          <Table
            bordered
            columns={columns}
            dataSource={data}
            pagination={{
              ...pagination,
              total: data.length,
            }}
            rowKey="key"
            size="small"
            onChange={handleTableChange}
          />
          {loading && (
            <Loading
              fullScreen={false}
              size="large"
              style={{
                position: 'absolute',
                top: 0,
                left: 0,
                right: 0,
                bottom: 0,
                zIndex: 10,
                backgroundColor: 'rgba(255, 255, 255, 0.8)',
                backdropFilter: 'blur(2px)',
              }}
              tip="加载模块数据..."
            />
          )}
        </div>
      </div>

      {/* 表单模态框 */}
      <ModuleFormModal
        dataSources={dataSources}
        selectedRecord={actions.selectedRecord}
        visible={actions.formModalVisible}
        onCancel={() => actions.setFormModalVisible(false)}
        onSubmit={actions.handleFormSubmit}
      />

      {/* 详情模态框 */}
      <ModuleDetailModal
        moduleDetail={actions.moduleDetail}
        visible={actions.detailModalVisible}
        onCancel={() => actions.setDetailModalVisible(false)}
      />

      {/* 删除确认模态框 */}
      <DeleteConfirmModal
        deleteDorisTable={actions.deleteDorisTable}
        deleteRecord={actions.deleteRecord}
        visible={actions.deleteModalVisible}
        onCancel={() => actions.setDeleteModalVisible(false)}
        onConfirm={actions.handleDeleteConfirm}
        onDeleteDorisTableChange={actions.setDeleteDorisTable}
      />

      {/* SQL执行模态框 */}
      <ExecuteConfirmationModal
        loading={actions.executing}
        readonly={actions.isReadOnlyMode}
        sql={actions.executeSql}
        title={sqlModalTitle}
        visible={actions.sqlModalVisible}
        onCancel={() => actions.setSqlModalVisible(false)}
        onConfirm={actions.handleSqlExecuteConfirm}
        onSqlChange={actions.setExecuteSql}
      />

      {/* 配置模态框 */}
      <ModuleQueryConfigModal
        moduleId={actions.configRecord ? Number(actions.configRecord.key) : null}
        moduleName={actions.configRecord?.name || ''}
        queryConfig={actions.configRecord?.queryConfig}
        visible={actions.configModalVisible}
        onCancel={() => actions.setConfigModalVisible(false)}
        onSuccess={actions.handleConfigSuccess}
      />
    </div>
  );
};

export default ModuleManagementPage;
