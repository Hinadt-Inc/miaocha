import { Modal, Descriptions, Tag, Empty } from 'antd';
import dayjs from 'dayjs';
import type { Module } from '../types';
import type { QueryConfig } from '@/api/modules';
import styles from '../ModuleManagement.module.less';

interface ModuleDetailModalProps {
  visible: boolean;
  moduleDetail: Module | null;
  onCancel: () => void;
}

const ModuleDetailModal: React.FC<ModuleDetailModalProps> = ({ visible, moduleDetail, onCancel }) => {
  // 渲染查询配置信息
  const renderQueryConfig = (queryConfig?: QueryConfig) => {
    if (!queryConfig) {
      return <Empty description="暂无查询配置" image={Empty.PRESENTED_IMAGE_SIMPLE} />;
    }

    return (
      <div className={styles.queryConfigContainer}>
        <div className={styles.timeFieldSection}>
          <strong>时间字段：</strong>
          <Tag color="blue">{queryConfig.timeField || '未设置'}</Tag>
        </div>
        <div className={styles.keywordFieldsSection}>
          <strong>关键词检索字段：</strong>
          {queryConfig.keywordFields && queryConfig.keywordFields.length > 0 ? (
            <div className={styles.keywordFieldsList}>
              {queryConfig.keywordFields.map((field, index) => (
                <div key={`${field.fieldName}-${field.searchMethod}-${index}`} className={styles.keywordFieldItem}>
                  <Tag color="green">{field.fieldName}</Tag>
                  <span className={styles.searchMethodLabel}>({field.searchMethod})</span>
                </div>
              ))}
            </div>
          ) : (
            <span className={styles.noConfigText}>暂无配置</span>
          )}
        </div>
      </div>
    );
  };

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
          <Descriptions.Item label="查询配置" span={2}>
            {renderQueryConfig(moduleDetail.queryConfig)}
          </Descriptions.Item>
        </Descriptions>
      )}
    </Modal>
  );
};

export default ModuleDetailModal;
