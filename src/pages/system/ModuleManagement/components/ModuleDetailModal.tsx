import { Modal, Descriptions } from 'antd';
import dayjs from 'dayjs';
import type { Module } from '../types';
import styles from '../ModuleManagement.module.less';

interface ModuleDetailModalProps {
  visible: boolean;
  moduleDetail: Module | null;
  onCancel: () => void;
}

const ModuleDetailModal: React.FC<ModuleDetailModalProps> = ({ visible, moduleDetail, onCancel }) => {
  return (
    <Modal title="模块详情" open={visible} onCancel={onCancel} footer={null} width={800}>
      {moduleDetail && (
        <Descriptions bordered column={2}>
          <Descriptions.Item label="模块名称">{moduleDetail.name}</Descriptions.Item>
          <Descriptions.Item label="数据源名称">{moduleDetail.datasourceName}</Descriptions.Item>
          <Descriptions.Item label="表名">{moduleDetail.tableName}</Descriptions.Item>
          <Descriptions.Item label="创建时间">
            {dayjs(moduleDetail.createTime).format('YYYY-MM-DD HH:mm:ss')}
          </Descriptions.Item>
          <Descriptions.Item label="更新时间">
            {dayjs(moduleDetail.updateTime).format('YYYY-MM-DD HH:mm:ss')}
          </Descriptions.Item>
          <Descriptions.Item label="创建人">{moduleDetail.createUserName || moduleDetail.createUser}</Descriptions.Item>
          <Descriptions.Item label="更新人">{moduleDetail.updateUserName || moduleDetail.updateUser}</Descriptions.Item>
          <Descriptions.Item label="Doris SQL" span={2}>
            <div className={styles.sqlContainer}>
              <pre className={styles.sqlPre}>{moduleDetail.dorisSql || '暂无SQL语句'}</pre>
            </div>
          </Descriptions.Item>
        </Descriptions>
      )}
    </Modal>
  );
};

export default ModuleDetailModal;
