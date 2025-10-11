import { Table } from 'antd';
import { useState } from 'react';
import { useErrorContext } from '@/providers/ErrorProvider';
import {
  LogstashPageHeader,
  TaskHistoryModal,
  TaskDetailModal,
  LogstashDetailModal,
  MachineTasksModal,
  ExpandedRowRenderer,
  LogstashEditModal,
  LogstashAlertModal,
  LogstashMachineConfigModal,
  LogstashMachineDetailModal,
  LogstashScaleModal,
  LogstashLogTailModal,
} from './components';
import { useLogstashData, useTableConfig, useLogstashActions, useMachineActions } from './hooks';
import { scaleProcess } from '@/api/logstash';
import type { LogstashTaskSummary, LogstashProcess } from '@/types/logstashTypes';
import styles from './LogstashManagement.module.less';
import Loading from '@/components/Loading';

const LogstashManagementPage = () => {
  const { data, loading, fetchData, handleReload, messageApi, contextHolder } = useLogstashData();
  const { showSuccess } = useErrorContext();

  const actions = useLogstashActions({ fetchData });
  const machineActions = useMachineActions({ fetchData });

  // messageApi 暂时保留，供其他未重构的组件使用
  console.log('messageApi available for legacy components:', !!messageApi);

  const [selectedTask, setSelectedTask] = useState<LogstashTaskSummary | null>(null);
  const [stepsModalVisible, setStepsModalVisible] = useState(false);
  const [scaleParams, setScaleParams] = useState({
    addMachineIds: [] as number[],
    removeLogstashMachineIds: [] as number[],
    customDeployPath: '',
    forceScale: false,
  });

  const { columns, pagination, handleTableChange } = useTableConfig({
    onEdit: actions.handleEdit,
    onDelete: (id: number) => {
      actions.handleDelete(id);
    },
    onStart: (id: number) => {
      actions.handleStart(id);
    },
    onStop: (id: number) => {
      actions.handleStop(id);
    },
    onShowHistory: (id: number) => {
      actions.handleShowHistory(id);
    },
    onScale: actions.handleScale,
    onRefreshAllConfig: (record: any) => {
      actions.handleRefreshAllConfig(record);
    },
    onReinitializeFailedMachines: (processId: number) => {
      actions.handleReinitializeFailedMachines(processId);
    },
    onForceStopProcess: (id: number) => {
      actions.handleForceStopProcess(id);
    },
    onShowDetail: actions.handleShowDetail,
    onShowAlert: (record: LogstashProcess) => {
      actions.handleShowAlert(record);
    },
  });

  const showTaskSteps = (task: LogstashTaskSummary) => {
    setSelectedTask(task);
    setStepsModalVisible(true);
  };

  const renderExpandedRow = (record: any) => (
    <ExpandedRowRenderer record={record} data={data} machineActions={machineActions} />
  );

  const handleScale = async (
    processId: number,
    params?: {
      addMachineIds: number[];
      removeLogstashMachineIds: number[];
      customDeployPath: string;
      forceScale: boolean;
    },
  ) => {
    try {
      const scaleParameters = params || scaleParams;
      await scaleProcess(processId, scaleParameters);
      showSuccess('操作成功');
      actions.setScaleModalVisible(false);
      await fetchData();
    } catch {
      // API 错误已由全局错误处理器处理
    }
  };
  return (
    <div className={styles.container}>
      {contextHolder}
      <div className={styles.header}>
        <LogstashPageHeader
          loading={loading}
          onAdd={actions.handleAdd}
          onReload={handleReload}
          className={styles.tableToolbar}
        />
      </div>

      <div className={styles.antTable}>
        <div style={{ position: 'relative' }}>
          <Table
            columns={columns}
            dataSource={data}
            size="small"
            rowKey="id"
            bordered
            scroll={{ x: 'max-content' }}
            pagination={{
              ...pagination,
              total: data.length,
            }}
            onChange={handleTableChange}
            expandable={{
              expandedRowRender: renderExpandedRow,
            }}
          />
          {loading && (
            <Loading
              fullScreen={false}
              size="large"
              tip="加载Logstash数据..."
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

      {/* 告警模态框 */}
      <LogstashAlertModal
        visible={actions.alertModalVisible}
        onCancel={() => actions.setAlertModalVisible(false)}
        onOk={actions.handleSubmitAlert}
        initialValues={actions.currentProcess}
      />

      {/* 编辑模态框 */}
      <LogstashEditModal
        visible={actions.editModalVisible}
        onCancel={() => actions.setEditModalVisible(false)}
        onOk={actions.handleSubmit}
        initialValues={actions.currentProcess}
      />

      {/* 任务历史模态框 */}
      <TaskHistoryModal
        visible={actions.summaryModalVisible}
        onClose={() => actions.setSummaryModalVisible(false)}
        taskSummaries={actions.taskSummaries}
        onShowSteps={showTaskSteps}
      />

      {/* 任务详情模态框 */}
      <TaskDetailModal
        visible={stepsModalVisible}
        onClose={() => setStepsModalVisible(false)}
        selectedTask={selectedTask}
      />

      {/* 进程详情模态框 */}
      <LogstashDetailModal
        visible={actions.currentDetail ? !!actions.detailModalVisible[actions.currentDetail.id] : false}
        onClose={() => {
          if (actions.currentDetail) {
            actions.setDetailModalVisible({ ...actions.detailModalVisible, [actions.currentDetail.id]: false });
          }
        }}
        detail={actions.currentDetail || null}
        styles={{
          configSection: styles.configSection,
          configHeader: styles.configHeader,
          configContent: styles.configContent,
          machineStatusSection: styles.machineStatusSection,
        }}
      />

      {/* 机器任务模态框 */}
      <MachineTasksModal
        visible={machineActions.machineTasksModalVisible}
        onClose={() => machineActions.setMachineTasksModalVisible(false)}
        machineTasks={machineActions.machineTasks}
        loading={machineActions.machineTasksLoading}
        machineId={machineActions.currentMachine?.logstashMachineId || 0}
      />

      {/* 机器配置模态框 */}
      <LogstashMachineConfigModal
        visible={machineActions.machineConfigModalVisible}
        onCancel={() => machineActions.setMachineConfigModalVisible(false)}
        processId={machineActions.currentMachine?.processId || 0}
        logstashMachineId={machineActions.currentMachine?.logstashMachineId || 0}
        moduleName={machineActions.currentMachine?.moduleName}
        processName={machineActions.currentMachine?.processName}
        initialConfig={{
          configContent: machineActions.currentMachine?.configContent,
          jvmOptions: machineActions.currentMachine?.jvmOptions,
          logstashYml: machineActions.currentMachine?.logstashYml,
        }}
        onSuccess={() => {
          fetchData();
        }}
      />

      {/* 机器详情模态框 */}
      <LogstashMachineDetailModal
        visible={machineActions.machineDetailModalVisible}
        onCancel={() => {
          machineActions.setMachineDetailModalVisible(false);
          machineActions.setCurrentMachineDetail(undefined);
        }}
        detail={machineActions.currentMachineDetail}
      />

      {/* 扩容模态框 */}
      <LogstashScaleModal
        visible={actions.scaleModalVisible}
        onCancel={() => actions.setScaleModalVisible(false)}
        onOk={async (params: {
          addMachineIds: number[];
          removeLogstashMachineIds?: number[];
          customDeployPath: string;
          forceScale: boolean;
        }) => {
          if (actions.currentProcess) {
            const validParams = {
              ...params,
              removeLogstashMachineIds: params.removeLogstashMachineIds || [],
            };
            setScaleParams(validParams);
            await handleScale(actions.currentProcess.id, validParams);
          }
        }}
        currentProcess={actions.currentProcess}
        initialParams={scaleParams}
      />

      {/* 日志模态框 */}
      <LogstashLogTailModal
        visible={machineActions.logTailModalVisible}
        logstashMachineId={machineActions.currentLogTailMachineId || 0}
        onCancel={() => machineActions.setLogTailModalVisible(false)}
      />

      {/* 底部日志模态框 */}
      <LogstashLogTailModal
        visible={machineActions.bottomLogTailModalVisible}
        logstashMachineId={machineActions.currentLogTailMachineId || 0}
        onCancel={() => machineActions.setBottomLogTailModalVisible(false)}
      />
    </div>
  );
};

import withAdminAuth from '@/utils/withAdminAuth';
export default withAdminAuth(LogstashManagementPage);
