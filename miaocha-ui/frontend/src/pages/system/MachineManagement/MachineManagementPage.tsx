import { Table } from 'antd';
import { MachinePageHeader, MachineFormModal } from './components';
import { useMachineData, useMachineActions, useTableConfig } from './hooks';
import withAdminAuth from '@/utils/withAdminAuth';
import Loading from '@/components/Loading';
import styles from './MachineManagement.module.less';

const MachineManagementPage = () => {
  const { data, loading, fetchMachines } = useMachineData();

  const actions = useMachineActions({
    fetchMachines,
  });

  const { columns, pagination, handleTableChange } = useTableConfig({
    onEdit: actions.handleOpenEdit,
    onDelete: actions.handleDelete,
  });

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <MachinePageHeader loading={loading} onAdd={actions.handleOpenCreate} />
      </div>

      <div className={styles.tableContainer}>
        <div style={{ position: 'relative' }}>
          <Table
            dataSource={data}
            columns={columns}
            size="small"
            rowKey="id"
            pagination={{
              ...pagination,
              total: data.length,
            }}
            onChange={handleTableChange}
            bordered
          />
          {loading && (
            <Loading
              fullScreen={false}
              size="large"
              tip="加载机器数据..."
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
            />
          )}
        </div>
      </div>

      {/* 新增机器模态框 */}
      <MachineFormModal
        title="新增机器"
        open={actions.createModalVisible}
        form={actions.form}
        loading={actions.loading}
        testingConnection={actions.testingConnection}
        onCancel={actions.handleCloseCreate}
        onFinish={actions.handleCreate}
        onTestConnection={actions.handleTestConnection}
      />

      {/* 编辑机器模态框 */}
      <MachineFormModal
        title="编辑机器"
        open={actions.editModalVisible}
        form={actions.form}
        loading={actions.loading}
        testingConnection={actions.testingConnection}
        onCancel={actions.handleCloseEdit}
        onFinish={actions.handleEdit}
        onTestConnection={actions.handleTestConnection}
      />
    </div>
  );
};

export default withAdminAuth(MachineManagementPage);
