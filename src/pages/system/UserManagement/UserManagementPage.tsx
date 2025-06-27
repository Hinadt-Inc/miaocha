import { Table } from 'antd';
import { UserPageHeader, UserFormModal, PasswordModal, ModulePermissionModal } from './components';
import { useUserData, useTableConfig, useUserActions } from './hooks';
import styles from './UserManagement.module.less';

const UserManagementPage = () => {
  const { data, setData, loading, searchText, moduleList, originalDataRef, handleSearch, handleReload, fetchUsers } =
    useUserData();

  const actions = useUserActions({
    setData,
    data,
    originalDataRef,
    moduleList,
    fetchUsers,
  });

  const { columns, pagination, handleTableChange } = useTableConfig({
    onEdit: actions.handleAddEdit,
    onDelete: actions.handleDelete,
    onChangePassword: actions.handleChangePassword,
    onOpenModuleDrawer: actions.handleOpenModuleDrawer,
  });

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <UserPageHeader
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
          pagination={{
            ...pagination,
            total: data.length,
          }}
          loading={loading}
          scroll={{ x: 1300 }}
          onChange={handleTableChange}
          size="small"
          bordered
        />
      </div>

      {/* 用户表单模态框 */}
      <UserFormModal
        visible={actions.isModalVisible}
        selectedRecord={actions.selectedRecord}
        onSubmit={actions.handleSubmit}
        onCancel={() => actions.setIsModalVisible(false)}
        form={actions.form}
      />

      {/* 密码修改模态框 */}
      <PasswordModal
        visible={actions.isPasswordModalVisible}
        selectedRecord={actions.selectedRecord}
        onSubmit={actions.handlePasswordSubmit}
        onCancel={() => actions.setIsPasswordModalVisible(false)}
        form={actions.passwordForm}
      />

      {/* 模块权限模态框 */}
      <ModulePermissionModal
        open={actions.moduleDrawerVisible}
        onClose={() => actions.setModuleDrawerVisible(false)}
        userId={actions.selectedUserForDrawer?.key || ''}
        modules={actions.selectedUserForDrawer?.modulePermissions || []}
        userModulePermissions={
          actions.selectedUserForDrawer?.modulePermissions?.map((m) => ({ moduleName: m.module })) || []
        }
        allModules={moduleList}
        onSave={actions.handleSaveModules}
        onRefresh={async () => {
          await fetchUsers();
          // 刷新抽屉用户数据，保证弹窗内容同步
          if (actions.selectedUserForDrawer) {
            const latest = originalDataRef.current.find((u) => u.key === actions.selectedUserForDrawer?.key);
            if (latest) actions.setSelectedUserForDrawer(latest);
          }
        }}
        nickname={actions.selectedUserForDrawer?.nickname || ''}
        email={actions.selectedUserForDrawer?.email || ''}
      />
    </div>
  );
};

import withSystemAccess from '@/utils/withSystemAccess';
export default withSystemAccess(UserManagementPage);
