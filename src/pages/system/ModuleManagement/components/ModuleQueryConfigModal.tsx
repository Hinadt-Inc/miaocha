import React, { useState, useEffect } from 'react';
import { Modal, Form, Button, Select, message, AutoComplete } from 'antd';
import { PlusOutlined, MinusCircleOutlined } from '@ant-design/icons';
import { updateModuleQueryConfig, getModuleFieldNames } from '@/api/modules';
import type { QueryConfigKeywordField, QueryConfig } from '@/api/modules';
import styles from './ModuleQueryConfigModal.module.less';

interface ModuleQueryConfigModalProps {
  visible: boolean;
  moduleId: number | null;
  moduleName: string;
  queryConfig?: QueryConfig;
  onCancel: () => void;
  onSuccess: () => void;
}

const { Option } = Select;

const searchMethodOptions = [
  { value: 'LIKE', label: '模糊匹配' },
  { value: 'MATCH_ALL', label: '全匹配' },
  { value: 'MATCH_ANY', label: '任意匹配' },
  { value: 'MATCH_PHRASE', label: '短语匹配' },
];

const ModuleQueryConfigModal: React.FC<ModuleQueryConfigModalProps> = ({
  visible,
  moduleId,
  moduleName,
  queryConfig,
  onCancel,
  onSuccess,
}) => {
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);
  const [fieldNames, setFieldNames] = useState<string[]>([]);
  const [loadingFields, setLoadingFields] = useState(false);

  useEffect(() => {
    if (visible && moduleId) {
      initializeForm();
      fetchFieldNames();
    }
  }, [visible, moduleId, queryConfig]);

  const fetchFieldNames = async () => {
    if (!moduleId) return;

    setLoadingFields(true);
    try {
      const fields = await getModuleFieldNames(moduleId);
      setFieldNames(fields);
    } catch (error) {
      console.error('获取字段名失败:', error);
      message.warning('获取字段名失败，请手动输入');
    } finally {
      setLoadingFields(false);
    }
  };

  const initializeForm = () => {
    // 使用传入的 queryConfig 或默认值初始化表单
    const config = queryConfig || {
      timeField: '',
      keywordFields: [{ fieldName: '', searchMethod: 'LIKE' }],
    };

    const formData = {
      timeField: config.timeField || '',
      keywordFields:
        config.keywordFields && config.keywordFields.length > 0
          ? config.keywordFields
          : [{ fieldName: '', searchMethod: 'LIKE' }],
    };

    // 先重置表单，再设置值
    form.resetFields();
    form.setFieldsValue(formData);
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
            <AutoComplete
              placeholder="请输入时间字段名称，如：log_time"
              options={fieldNames.map((field) => ({
                value: field,
                label: field,
              }))}
              filterOption={(inputValue, option) =>
                option?.value.toString().toLowerCase().includes(inputValue.toLowerCase()) || false
              }
              notFoundContent={loadingFields ? '加载中...' : '无匹配字段'}
              allowClear
            />
          </Form.Item>

          <Form.Item label="关键词检索字段">
            <Form.List name="keywordFields">
              {(fields, { add, remove }) => (
                <>
                  {fields.map(({ key, name, ...restField }, index) => (
                    <div key={key} className={styles.keywordFieldRow}>
                      <Form.Item
                        {...restField}
                        name={[name, 'fieldName']}
                        rules={[{ required: true, message: '请输入字段名称' }]}
                        className={styles.fieldNameInput}
                      >
                        <AutoComplete
                          placeholder="字段名称，如：message"
                          options={fieldNames.map((field) => ({
                            value: field,
                            label: field,
                          }))}
                          filterOption={(inputValue, option) =>
                            option?.value.toString().toLowerCase().includes(inputValue.toLowerCase()) || false
                          }
                          notFoundContent={loadingFields ? '加载中...' : '无匹配字段'}
                          allowClear
                          onSelect={(value) => {
                            // 确保选择的值能正确设置到表单中
                            const currentValues = form.getFieldsValue();
                            const newKeywordFields = [...(currentValues.keywordFields || [])];
                            if (newKeywordFields[index]) {
                              newKeywordFields[index].fieldName = value;
                              form.setFieldsValue({
                                ...currentValues,
                                keywordFields: newKeywordFields,
                              });
                            }
                          }}
                        />
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
                    </div>
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
