import { Modal, Radio } from 'antd';
import type { ModuleData } from '../types';
import styles from '../ModuleManagement.module.less';

interface DeleteConfirmModalProps {
  visible: boolean;
  deleteRecord: ModuleData | null;
  deleteDorisTable: boolean;
  onDeleteDorisTableChange: (value: boolean) => void;
  onConfirm: () => Promise<void>;
  onCancel: () => void;
}

const DeleteConfirmModal: React.FC<DeleteConfirmModalProps> = ({
  visible,
  deleteRecord,
  deleteDorisTable,
  onDeleteDorisTableChange,
  onConfirm,
  onCancel,
}) => {
  return (
    <Modal
      title="删除模块"
      open={visible}
      onOk={onConfirm}
      onCancel={onCancel}
      okText="确定删除"
      cancelText="取消"
      okButtonProps={{ danger: true }}
    >
      <div>
        <p>
          确定要删除模块 <strong>{deleteRecord?.name}</strong> 吗？
        </p>
        <Radio.Group value={deleteDorisTable} onChange={(e) => onDeleteDorisTableChange(e.target.value)}>
          <Radio value={false}>仅删除模块，保留Doris表数据</Radio>
          <Radio value={true}>同时删除底层Doris表数据</Radio>
        </Radio.Group>
        <div className={styles.warningText}>
          <strong>警告：</strong>此操作不可撤销，请谨慎选择！
        </div>
      </div>
    </Modal>
  );
};

export default DeleteConfirmModal;
