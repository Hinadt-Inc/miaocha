import { MachineTable } from '.';
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
}

const ExpandedRowRenderer = ({ record, data, machineActions }: ExpandedRowRendererProps) => {
  return (
    <MachineTable
      data={data}
      record={record}
      onDeleteMachine={machineActions.handleDeleteMachine}
      onEditMachineConfig={machineActions.handleEditMachineConfig}
      onForceStopMachine={machineActions.handleForceStopMachine}
      onRefreshConfig={machineActions.handleRefreshConfig}
      onReinitializeMachine={machineActions.handleReinitializeMachine}
      onShowLog={machineActions.handleShowLog}
      onShowMachineDetail={machineActions.handleShowMachineDetail}
      onShowMachineTasks={machineActions.showMachineTasks}
      onStartMachine={machineActions.handleStartMachine}
      onStopMachine={machineActions.handleStopMachine}
    />
  );
};

export default ExpandedRowRenderer;
