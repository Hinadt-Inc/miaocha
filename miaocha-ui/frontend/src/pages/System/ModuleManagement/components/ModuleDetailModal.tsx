import { Modal, Descriptions, Tag, Empty } from 'antd';
import dayjs from 'dayjs';
import type { Module } from '../types';
import type { QueryConfig } from '@/api/modules';
import styles from '../ModuleManagement.module.less';
import modalStyles from './ModuleDetailModal.module.less';

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
      <div className={modalStyles.queryConfigContainer}>
        <div className={modalStyles.configRow}>
          <span className={modalStyles.configLabel}>时间字段：</span>
          <Tag color="blue">{queryConfig.timeField || '未设置'}</Tag>
        </div>
        <div className={`${modalStyles.configRow} ${modalStyles.fieldsRow}`}>
          <span className={modalStyles.configLabel}>关键词检索字段：</span>
          {queryConfig.keywordFields && queryConfig.keywordFields.length > 0 ? (
            <div className={modalStyles.keywordFieldsInline}>
              {queryConfig.keywordFields.map((field, index) => (
                <span key={`${field.fieldName}-${field.searchMethod}-${index}`} className={modalStyles.fieldTag}>
                  <Tag color="green">{field.fieldName}</Tag>
                  <span className={modalStyles.searchMethod}>({field.searchMethod})</span>
                </span>
              ))}
            </div>
          ) : (
            <span className={modalStyles.noConfigText}>暂无配置</span>
          )}
        </div>
        <div className={`${modalStyles.configRow} ${modalStyles.fieldsRow}`}>
          <span className={modalStyles.configLabel}>排除字段：</span>
          {queryConfig.excludeFields && queryConfig.excludeFields.length > 0 ? (
            <div className={modalStyles.keywordFieldsInline}>
              {queryConfig.excludeFields.map((field) => (
                <Tag color="orange">{field}</Tag>
              ))}
            </div>
          ) : (
            <span className={modalStyles.noConfigText}>暂无排除配置</span>
          )}
        </div>
      </div>
    );
  };

  return (
    <Modal footer={null} open={visible} title="模块详情" width={1000} onCancel={onCancel}>
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
