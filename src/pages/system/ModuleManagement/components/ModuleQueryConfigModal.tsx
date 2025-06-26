import React, { useState, useEffect } from 'react';
import { Modal, Form, Input, Button, Select, Space, message } from 'antd';
import { PlusOutlined, MinusCircleOutlined } from '@ant-design/icons';
import { updateModuleQueryConfig, getModuleQueryConfig } from '@/api/modules';
import type { QueryConfigKeywordField } from '@/api/modules';
import styles from './ModuleQueryConfigModal.module.less';

interface ModuleQueryConfigModalProps {
  visible: boolean;
  moduleId: number | null;
  moduleName: string;
  onCancel: () => void;
  onSuccess: () => void;
}

const { Option } = Select;

const searchMethodOptions = [
  { value: 'LIKE', label: '模糊匹配' },
  { value: 'MATCH_ALL', label: '全匹配' },
  { value: 'EXACT', label: '精确匹配' },
];

const ModuleQueryConfigModal: React.FC<ModuleQueryConfigModalProps> = ({
  visible,
  moduleId,
  moduleName,
  onCancel,
  onSuccess,
}) => {
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (visible && moduleId) {
      fetchQueryConfig();
    }
  }, [visible, moduleId]);

  const fetchQueryConfig = async () => {
    if (!moduleId) return;

    try {
      const config = await getModuleQueryConfig(moduleId);
      form.setFieldsValue({
        timeField: config.timeField || '',
        keywordFields: config.keywordFields || [{ fieldName: '', searchMethod: 'LIKE' }],
      });
    } catch (error) {
      console.error('获取查询配置失败:', error);
      // 如果获取失败，设置默认值
      form.setFieldsValue({
        timeField: '',
        keywordFields: [{ fieldName: '', searchMethod: 'LIKE' }],
      });
    }
  };

  const handleSubmit = async () => {
    if (!moduleId) return;

    try {
      const values = await form.validateFields();
      setSubmitting(true);

      const params = {
        moduleId,
        queryConfig: {
          timeField: values.timeField,
          keywordFields: values.keywordFields.filter((field: QueryConfigKeywordField) => field.fieldName.trim()),
        },
      };

      await updateModuleQueryConfig(params);
      message.success('查询配置保存成功');
      onSuccess();
      onCancel();
    } catch (error) {
      console.error('保存查询配置失败:', error);
      message.error('保存查询配置失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handleCancel = () => {
    form.resetFields();
    onCancel();
  };

  return (
    <Modal
      title={`配置查询设置 - ${moduleName}`}
      open={visible}
      onCancel={handleCancel}
      footer={[
        <Button key="cancel" onClick={handleCancel}>
          取消
        </Button>,
        <Button key="submit" type="primary" loading={submitting} onClick={handleSubmit}>
          保存
        </Button>,
      ]}
      width={600}
    >
      <div className={styles.container}>
        <Form
          form={form}
          layout="vertical"
          initialValues={{
            timeField: '',
            keywordFields: [{ fieldName: '', searchMethod: 'LIKE' }],
          }}
        >
          <Form.Item label="时间字段" name="timeField" rules={[{ required: true, message: '请输入时间字段名称' }]}>
            <Input placeholder="请输入时间字段名称，如：log_time" />
          </Form.Item>

          <Form.Item label="关键词检索字段">
            <Form.List name="keywordFields">
              {(fields, { add, remove }) => (
                <>
                  {fields.map(({ key, name, ...restField }) => (
                    <Space key={key} className={styles.keywordFieldRow} align="baseline">
                      <Form.Item
                        {...restField}
                        name={[name, 'fieldName']}
                        rules={[{ required: true, message: '请输入字段名称' }]}
                        className={styles.fieldNameInput}
                      >
                        <Input placeholder="字段名称，如：message" />
                      </Form.Item>
                      <Form.Item
                        {...restField}
                        name={[name, 'searchMethod']}
                        rules={[{ required: true, message: '请选择检索方法' }]}
                        className={styles.searchMethodSelect}
                      >
                        <Select placeholder="检索方法">
                          {searchMethodOptions.map((option) => (
                            <Option key={option.value} value={option.value}>
                              {option.label}
                            </Option>
                          ))}
                        </Select>
                      </Form.Item>
                      {fields.length > 1 && (
                        <MinusCircleOutlined className={styles.removeButton} onClick={() => remove(name)} />
                      )}
                    </Space>
                  ))}
                  <Form.Item>
                    <Button
                      type="dashed"
                      onClick={() => add({ fieldName: '', searchMethod: 'LIKE' })}
                      block
                      icon={<PlusOutlined />}
                    >
                      添加关键词字段
                    </Button>
                  </Form.Item>
                </>
              )}
            </Form.List>
          </Form.Item>
        </Form>
      </div>
    </Modal>
  );
};

export default ModuleQueryConfigModal;
