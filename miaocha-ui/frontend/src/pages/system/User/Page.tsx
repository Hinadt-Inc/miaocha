import { useCallback, useMemo } from 'react';
import { Table } from 'antd';
import { UserPageHeader, UserFormModal, PasswordModal, ModulePermissionModal } from './components';
import { useUserData, useTableConfig, useUserActions } from './hooks';
import styles from './Page.module.less';

const UserManagementPage = () => {
  const { data, loading, searchText, moduleList, originalDataRef, handleSearch, handleReload, fetchUsers } =
    useUserData();

  const actions = useUserActions({
    moduleList,
    fetchUsers,
  });

  const { columns, pagination, handleTableChange } = useTableConfig({
    onEdit: actions.handleAddEdit,
    onDelete: actions.handleDelete,
    onChangePassword: actions.handleChangePassword,
    onOpenModuleDrawer: actions.handleOpenModuleDrawer,
  });

  // 固化与模态框相关的回调与派生数据，避免无关渲染时引用变化
  const { setIsModalVisible, setModuleDrawerVisible } = actions;
  const onCloseModuleDrawer = useCallback(() => setModuleDrawerVisible(false), [setModuleDrawerVisible]);
  const handleModuleRefresh = useCallback(async () => {
    await fetchUsers();
    if (actions.selectedUserForDrawer) {
      const latest = originalDataRef.current.find((u) => u.id === actions.selectedUserForDrawer?.id);
      if (latest) actions.setSelectedUserForDrawer(latest);
    }
  }, [fetchUsers, actions, originalDataRef]);
  const userModulePermissions = useMemo(
    () => actions.selectedUserForDrawer?.modulePermissions?.map((m) => ({ moduleName: m.module })) || [],
    [actions.selectedUserForDrawer],
  );

  const total = data.length;

  return (
    <div className={styles.container}>
      {actions.messageContextHolder}
      <div className={styles.header}>
        <UserPageHeader
          loading={loading}
          searchText={searchText}
          onAdd={actions.handleAddEdit}
          onReload={handleReload}
          onSearch={handleSearch}
        />
      </div>

      <Table
        bordered
        columns={columns}
        dataSource={data}
        pagination={{ ...pagination, total }}
        rowKey="id"
        size="small"
        onChange={handleTableChange}
      />

      {actions.isModalVisible && (
        <UserFormModal
          confirmLoading={actions.submitLoading}
          form={actions.form}
          selectedRecord={actions.selectedRecord}
          visible={actions.isModalVisible}
          onCancel={() => setIsModalVisible(false)}
          onSubmit={actions.handleSubmit}
        />
      )}

      {/* 密码修改模态框：仅在可见时挂载，且使用 memo 包装与稳定回调 */}
      {actions.isPasswordModalVisible && (
        <PasswordModal
          confirmLoading={actions.passwordSubmitting}
          form={actions.passwordForm}
          selectedRecord={actions.selectedRecord}
          visible={actions.isPasswordModalVisible}
          onCancel={() => actions.setIsPasswordModalVisible(false)}
          onSubmit={actions.handlePasswordSubmit}
        />
      )}

      {/* 模块权限模态框：仅在可见时挂载，memo 包装与稳定 props */}
      {actions.moduleDrawerVisible && (
        <ModulePermissionModal
          allModules={moduleList}
          email={actions.selectedUserForDrawer?.email || ''}
          modules={actions.selectedUserForDrawer?.modulePermissions || []}
          nickname={actions.selectedUserForDrawer?.nickname || ''}
          open={actions.moduleDrawerVisible}
          userId={actions.selectedUserForDrawer?.id || 0}
          userModulePermissions={userModulePermissions}
          onClose={onCloseModuleDrawer}
          onRefresh={handleModuleRefresh}
          onSave={actions.handleSaveModules}
        />
      )}
    </div>
  );
};

export default UserManagementPage;
