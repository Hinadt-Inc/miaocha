import React from 'react';

import { useRequest } from 'ahooks';
import { Modal, Form, Input, message } from 'antd';

import * as logsApi from '@/api/logs';

import { useHomeContext } from '../context';

import styles from './index.module.less';

interface SaveSearchModalProps {
  visible: boolean;
  onClose: () => void;
}

const SaveSearchModal: React.FC<SaveSearchModalProps> = ({ visible, onClose }) => {
  const [form] = Form.useForm();
  const [messageApi, contextHolder] = message.useMessage();
  const { searchParams } = useHomeContext();

  // 时间范围转译
  const getTimeRangeLabel = (timeRange: string) => {
    const timeRangeMap: Record<string, string> = {
      auto: '自动',
      last_5m: '最近5分钟',
      last_15m: '最近15分钟',
      last_30m: '最近30分钟',
      last_1h: '最近1小时',
      last_8h: '最近8小时',
      last_24h: '最近24小时',
      last_7d: '最近7天',
      last_2week: '最近2周',
      today: '今天',
      yesterday: '昨天',
      this_week: '本周',
      last_week: '上周',
      custom: '自定义时间范围',
    };
    return timeRangeMap[timeRange] || timeRange;
  };

  // 时间分组转译
  const getTimeGroupingLabel = (timeGrouping: string) => {
    const timeGroupingMap: Record<string, string> = {
      second: '按秒分组',
      minute: '按分钟分组',
      hour: '按小时分组',
      day: '按天分组',
      auto: '自动分组',
    };
    return timeGroupingMap[timeGrouping] || timeGrouping;
  };

  // 保存搜索条件
  const { loading, run: saveSearchCondition } = useRequest(logsApi.saveSearchConditionWithCache, {
    manual: true,
    onSuccess: (response) => {
      messageApi.success(`搜索条件已保存，缓存键: ${response}`);
      form.resetFields();
      onClose();
    },
    onError: (error) => {
      console.error('保存搜索失败:', error);
      messageApi.error('保存失败，请重试');
    },
  });

  const handleSave = async () => {
    try {
      const values = await form.validateFields();

      // 构建保存参数
      const saveParams: ISaveSearchConditionWithCacheParams = {
        ...searchParams,
        name: values.name,
        description: values.description,
        targetBuckets: 50,
        pageSize: 50,
        offset: 0,
      } as ISaveSearchConditionWithCacheParams;

      saveSearchCondition(saveParams);
    } catch (error) {
      // 表单验证错误会被 antd 自动处理，此处捕获但不需要额外处理
      console.error('Form validation error:', error);
    }
  };

  const handleCancel = () => {
    form.resetFields();
    onClose();
  };

  return (
    <>
      {contextHolder}
      <Modal
        cancelText="取消"
        confirmLoading={loading}
        okText="保存"
        open={visible}
        title="保存搜索条件"
        width={500}
        onCancel={handleCancel}
        onOk={handleSave}
      >
        <Form form={form} layout="vertical" requiredMark={false}>
          <Form.Item
            label="搜索名称"
            name="name"
            rules={[
              { required: true, message: '请输入搜索名称' },
              { max: 50, message: '搜索名称不能超过50个字符' },
            ]}
          >
            <Input placeholder="请输入搜索名称，如：错误日志查询" />
          </Form.Item>

          <Form.Item label="描述" name="description" rules={[{ max: 200, message: '描述不能超过200个字符' }]}>
            <Input.TextArea maxLength={200} placeholder="请输入搜索描述（可选）" rows={3} showCount />
          </Form.Item>
        </Form>

        <div className={styles.previewContainer}>
          <div className={styles.previewTitle}>当前搜索条件预览</div>
          <div className={styles.previewContent}>
            {searchParams?.keywords && searchParams.keywords.length > 0 && (
              <div>
                <strong>关键词:</strong>
                <span>{searchParams.keywords.join(', ')}</span>
              </div>
            )}
            {searchParams?.whereSqls && searchParams.whereSqls.length > 0 && (
              <div>
                <strong>SQL条件:</strong>
                <span>{searchParams.whereSqls.join(' AND ')}</span>
              </div>
            )}
            {searchParams?.timeRange && (
              <div>
                <strong>时间范围:</strong>
                <span>{getTimeRangeLabel(searchParams.timeRange)}</span>
              </div>
            )}
            {searchParams?.timeGrouping && (
              <div>
                <strong>时间分组:</strong>
                <span>{getTimeGroupingLabel(searchParams.timeGrouping)}</span>
              </div>
            )}
            {searchParams?.module && (
              <div>
                <strong>模块:</strong>
                <span>{searchParams.module}</span>
              </div>
            )}
            {searchParams?.sortFields && searchParams.sortFields.length > 0 && (
              <div>
                <strong>排序:</strong>
                <span>
                  {searchParams.sortFields
                    .map((item: any) => `${item.fieldName} (${item.direction === 'ASC' ? '升序' : '降序'})`)
                    .join(', ')}
                </span>
              </div>
            )}
          </div>
        </div>
      </Modal>
    </>
  );
};

export default SaveSearchModal;
