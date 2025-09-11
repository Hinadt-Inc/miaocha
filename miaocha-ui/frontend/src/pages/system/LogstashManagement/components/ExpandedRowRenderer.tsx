import { MachineTable } from '../components';
import type { LogstashProcess } from '@/types/logstashTypes';

interface ExpandedRowRendererProps {
  record: LogstashProcess;
  data: LogstashProcess[];
  machineActions: {
    handleStartMachine: (machineId: number) => void;
    handleStopMachine: (machineId: number) => void;
    handleRefreshConfig: (processId: number, machineId: number) => void;
    handleReinitializeMachine: (machineId: number) => void;
    handleForceStopMachine: (processId: number, machineId: number) => void;
    handleEditMachineConfig: (machineId: number, processId: number, data: LogstashProcess[]) => void;
    handleShowMachineDetail: (machineId: number, moduleName?: string) => void;
    showMachineTasks: (processId: number, machineId: number) => void;
    handleShowLog: (machineId: number, isBottom?: boolean) => void;
    handleDeleteMachine: (processId: number, machineId: number) => void;
  };
  // Batch selection props
  selectedInstanceIds?: number[];
  onInstanceSelectionChange?: (selectedIds: number[]) => void;
}

const ExpandedRowRenderer = ({ 
  record, 
  data, 
  machineActions, 
  selectedInstanceIds,
  onInstanceSelectionChange 
}: ExpandedRowRendererProps) => {
  return (
    <MachineTable
      record={record}
      data={data}
      onStartMachine={machineActions.handleStartMachine}
      onStopMachine={machineActions.handleStopMachine}
      onRefreshConfig={machineActions.handleRefreshConfig}
      onReinitializeMachine={machineActions.handleReinitializeMachine}
      onForceStopMachine={machineActions.handleForceStopMachine}
      onEditMachineConfig={machineActions.handleEditMachineConfig}
      onShowMachineDetail={machineActions.handleShowMachineDetail}
      onShowMachineTasks={machineActions.showMachineTasks}
      onShowLog={machineActions.handleShowLog}
      onDeleteMachine={machineActions.handleDeleteMachine}
      selectedInstanceIds={selectedInstanceIds}
      onInstanceSelectionChange={onInstanceSelectionChange}
    />
  );
};

export default ExpandedRowRenderer;
