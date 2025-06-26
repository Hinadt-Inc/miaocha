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
  }) as unknown as { columns: any; pagination: any; handleTableChange: any };

  const sqlModalTitle = (
    <div className={styles.sqlModalTitle}>
      <span>
        {actions.isReadOnlyMode ? '查看' : '执行'}Doris SQL - <strong>{actions.currentRecord?.name}</strong>
      </span>
      {!actions.isReadOnlyMode && (
        <Tooltip title="应用模板的Doris SQL语句">
          <Button
            type="text"
            icon={<DatabaseOutlined />}
            className={styles.templateButton}
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
          searchText={searchText}
          loading={loading}
          onSearch={handleSearch}
          onAdd={() => actions.handleAddEdit()}
          onReload={handleReload}
        />
      </div>

      <div className={styles.antTable}>
        <Table
          columns={columns}
          dataSource={data}
          rowKey="key"
          loading={loading}
          scroll={{ x: 1300 }}
          size="small"
          bordered
          pagination={{
            ...pagination,
            total: data.length,
          }}
          onChange={handleTableChange}
        />
      </div>

      {/* 表单模态框 */}
      <ModuleFormModal
        visible={actions.formModalVisible}
        selectedRecord={actions.selectedRecord}
        dataSources={dataSources}
        onSubmit={actions.handleFormSubmit}
        onCancel={() => actions.setFormModalVisible(false)}
      />

      {/* 详情模态框 */}
      <ModuleDetailModal
        visible={actions.detailModalVisible}
        moduleDetail={actions.moduleDetail}
        onCancel={() => actions.setDetailModalVisible(false)}
      />

      {/* 删除确认模态框 */}
      <DeleteConfirmModal
        visible={actions.deleteModalVisible}
        deleteRecord={actions.deleteRecord}
        deleteDorisTable={actions.deleteDorisTable}
        onDeleteDorisTableChange={actions.setDeleteDorisTable}
        onConfirm={actions.handleDeleteConfirm}
        onCancel={() => actions.setDeleteModalVisible(false)}
      />

      {/* SQL执行模态框 */}
      <ExecuteConfirmationModal
        visible={actions.sqlModalVisible}
        sql={actions.executeSql}
        onConfirm={actions.handleSqlExecuteConfirm}
        onCancel={() => actions.setSqlModalVisible(false)}
        onSqlChange={actions.setExecuteSql}
        loading={actions.executing}
        readonly={actions.isReadOnlyMode}
        title={sqlModalTitle}
      />

      {/* 配置模态框 */}
      <ModuleQueryConfigModal
        visible={actions.configModalVisible}
        moduleId={actions.configRecord ? Number(actions.configRecord.key) : null}
        moduleName={actions.configRecord?.name || ''}
        onCancel={() => actions.setConfigModalVisible(false)}
        onSuccess={actions.handleConfigSuccess}
      />
    </div>
  );
};

export default ModuleManagementPage;
