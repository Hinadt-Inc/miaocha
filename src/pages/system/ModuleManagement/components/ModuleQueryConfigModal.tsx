import React, { useState, useEffect, useCallback } from 'react';
import { Modal, Form, Button, Select, message, AutoComplete, Tooltip } from 'antd';
import { PlusOutlined, MinusCircleOutlined, QuestionCircleOutlined } from '@ant-design/icons';
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
  { value: 'LIKE', label: 'LIKE' },
  { value: 'MATCH_ALL', label: 'MATCH_ALL' },
  { value: 'MATCH_ANY', label: 'MATCH_ANY' },
  { value: 'MATCH_PHRASE', label: 'MATCH_PHRASE' },
];

// 预生成选项JSX元素
const searchMethodOptionElements = searchMethodOptions.map((option) => (
  <Option key={option.value} value={option.value}>
    {option.label}
  </Option>
));

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
  const [excludeFieldNames, setExcludeFieldNames] = useState<string[]>([]);
  const [loadingFields, setLoadingFields] = useState(false);
  const [disabledFields, setDisabledFields] = useState<number[]>([]);
  const [messageApi, contextHolder] = message.useMessage();

  // 检测字段名是否包含特殊格式（点号或方括号）
  const isSpecialField = (fieldName: string): boolean => {
    if (!fieldName) return false;
    // 检测包含点号的格式：message.marker.reqType
    const hasDots = fieldName.includes('.');
    // 检测包含方括号的格式：message[marker][reqType]
    const hasBrackets = /\[.*\]/.test(fieldName);
    return hasDots || hasBrackets;
  };

  // 验证字段名是否重复
  const validateFieldNameUnique = (_: any, value: string) => {
    if (!value) return Promise.resolve();

    const formValues = form.getFieldsValue();
    const keywordFields = formValues.keywordFields || [];

    // 统计当前字段名出现的次数
    const duplicateCount = keywordFields.filter(
      (field: any) => field?.fieldName && field.fieldName.trim() === value.trim(),
    ).length;

    if (duplicateCount > 1) {
      return Promise.reject(new Error('字段名不能重复'));
    }

    return Promise.resolve();
  };

  // 弹框打开时的初始化
  useEffect(() => {
    if (visible && moduleId) {
      // 初始化表单
      const config = queryConfig || {
        timeField: '',
        excludeFields: [],
        keywordFields: [{ fieldName: '', searchMethod: 'LIKE' }],
      };

      const formData = {
        timeField: config.timeField || '',
        excludeFields: config.excludeFields || [],
        keywordFields:
          config.keywordFields && config.keywordFields.length > 0
            ? config.keywordFields
            : [{ fieldName: '', searchMethod: 'LIKE' }],
      };

      // 先重置表单，再设置值
      form.resetFields();
      form.setFieldsValue(formData);

      // 初始化时也需要检查并设置禁用字段状态
      const newDisabledFields: number[] = [];
      formData.keywordFields.forEach((field: any, index: number) => {
        if (field?.fieldName && isSpecialField(field.fieldName)) {
          newDisabledFields.push(index);
        }
      });
      setDisabledFields(newDisabledFields);

      // 获取字段名称列表
      const fetchFields = async () => {
        setLoadingFields(true);
        try {
          const fields = await getModuleFieldNames(moduleId);
          setFieldNames(fields);

          // 获取字段后立即更新排除字段选项
          const currentTimeField = formData.timeField || '';
          if (currentTimeField) {
            setExcludeFieldNames(fields.filter((field) => field !== currentTimeField));
          } else {
            setExcludeFieldNames(fields);
          }
        } catch (error) {
          console.error('获取字段名失败:', error);
          messageApi.warning('获取字段名失败，请手动输入');
        } finally {
          setLoadingFields(false);
        }
      };

      fetchFields();
    }
  }, [visible, moduleId, queryConfig, messageApi]);

  // 时间字段变化时更新排除字段选项
  const updateExcludeFieldNames = (timeField?: string) => {
    const currentTimeField = timeField || form.getFieldValue('timeField') || '';
    if (fieldNames.length > 0) {
      if (currentTimeField) {
        setExcludeFieldNames(fieldNames.filter((field) => field !== currentTimeField));
      } else {
        setExcludeFieldNames(fieldNames);
      }
    }
  };

  const handleSubmit = async () => {
    if (!moduleId) return;

    try {
      // 先验证表单
      const values = await form.validateFields();

      // 验证成功后再设置提交状态
      setSubmitting(true);

      const params = {
        moduleId,
        queryConfig: {
          timeField: values.timeField,
          excludeFields: values.excludeFields,
          keywordFields: values.keywordFields.filter((field: QueryConfigKeywordField) => field.fieldName.trim()),
        },
      };

      await updateModuleQueryConfig(params);
      messageApi.success('查询配置保存成功');
      onSuccess();
      onCancel();
    } catch (error) {
      // 处理验证失败或API调用失败
      if (error && typeof error === 'object' && 'errorFields' in error) {
        // 表单验证失败
        messageApi.error('请检查表单填写是否正确');
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleCancel = () => {
    onCancel();
  };

  return (
    <>
      {contextHolder}
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
        maskClosable={false}
      >
        <div className={styles.container}>
          <Form
            form={form}
            layout="vertical"
            initialValues={{
              timeField: '',
              excludeFields: [],
              keywordFields: [{ fieldName: '', searchMethod: 'LIKE' }],
            }}
            onValuesChange={(changedValues, allValues) => {
              // 当字段名发生变化时，检查并自动设置特殊字段的检索方法为LIKE
              if (changedValues.keywordFields) {
                const newKeywordFields = allValues.keywordFields.map((field: any) => {
                  if (field?.fieldName && isSpecialField(field.fieldName) && field.searchMethod !== 'LIKE') {
                    return { ...field, searchMethod: 'LIKE' };
                  }
                  return field;
                });

                // 更新禁用字段的状态
                const newDisabledFields: number[] = [];
                allValues.keywordFields.forEach((field: any, index: number) => {
                  if (field?.fieldName && isSpecialField(field.fieldName)) {
                    newDisabledFields.push(index);
                  }
                });
                setDisabledFields(newDisabledFields);

                // 如果有变化，更新表单值
                const hasChanges = newKeywordFields.some(
                  (field: any, index: number) => field.searchMethod !== allValues.keywordFields[index]?.searchMethod,
                );

                if (hasChanges) {
                  setTimeout(() => {
                    form.setFieldsValue({
                      ...allValues,
                      keywordFields: newKeywordFields,
                    });
                  }, 0);
                }
              }
            }}
          >
            <Form.Item
              label={
                <span>
                  时间字段
                  <Tooltip title="添加的时间字段毫秒级别检索仅支持 .SSS 格式，例如 yyyy-MM-dd HH:mm:ss.SSS">
                    <QuestionCircleOutlined style={{ marginLeft: 4, color: '#999' }} />
                  </Tooltip>
                </span>
              }
              name="timeField"
              rules={[{ required: true, message: '请选择时间字段' }]}
            >
              <AutoComplete
                placeholder="请选择时间字段"
                options={fieldNames.map((field) => ({
                  value: field,
                  label: field,
                }))}
                filterOption={(inputValue, option) =>
                  option?.value.toString().toLowerCase().includes(inputValue.toLowerCase()) || false
                }
                onBlur={() => updateExcludeFieldNames()}
                onChange={(value) => updateExcludeFieldNames(value)}
                notFoundContent={loadingFields ? '加载中...' : '无匹配字段'}
                allowClear
                maxLength={128}
              />
            </Form.Item>
            <Form.Item
              label={
                <span>
                  排除字段
                  <Tooltip title="查询字段排除列表，定义在日志查询中需要排除展示和查询的字段名">
                    <QuestionCircleOutlined style={{ marginLeft: 4, color: '#999' }} />
                  </Tooltip>
                </span>
              }
              name="excludeFields"
            >
              <Select
                mode="multiple"
                placeholder="请选择需要排除的字段"
                options={excludeFieldNames.map((field) => ({
                  value: field,
                  label: field,
                }))}
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
                          rules={[
                            { required: true, message: '请输入字段名称' },
                            {
                              max: 128,
                              message: '字段名称长度不能超过128个字符',
                            },
                            {
                              pattern: /^[a-zA-Z_][a-zA-Z0-9_.\[\]'"]*$/,
                              message: '字段名必须以字母或下划线开头，只能包含字母、数字、下划线、点号、方括号和引号',
                            },
                            {
                              validator: validateFieldNameUnique,
                            },
                          ]}
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
                                // 如果是特殊字段格式，自动设置为 LIKE
                                if (isSpecialField(value)) {
                                  newKeywordFields[index].searchMethod = 'LIKE';
                                }
                                form.setFieldsValue({
                                  ...currentValues,
                                  keywordFields: newKeywordFields,
                                });
                              }
                            }}
                            onChange={(value) => {
                              // 当用户手动输入时也检查是否为特殊字段
                              if (typeof value === 'string') {
                                const currentValues = form.getFieldsValue();
                                const newKeywordFields = [...(currentValues.keywordFields || [])];
                                if (newKeywordFields[index]) {
                                  newKeywordFields[index].fieldName = value;
                                  // 如果是特殊字段格式，自动设置为 LIKE
                                  if (isSpecialField(value)) {
                                    newKeywordFields[index].searchMethod = 'LIKE';
                                    form.setFieldsValue({
                                      ...currentValues,
                                      keywordFields: newKeywordFields,
                                    });
                                  }
                                }
                              }
                            }}
                            maxLength={128}
                          />
                        </Form.Item>
                        <Form.Item
                          {...restField}
                          name={[name, 'searchMethod']}
                          rules={[{ required: true, message: '请选择检索方法' }]}
                          className={styles.searchMethodSelect}
                        >
                          <Select
                            placeholder="检索方法"
                            disabled={disabledFields.includes(index)}
                            onChange={(value) => {
                              // 手动处理onChange事件，确保表单值正确更新
                              const formValues = form.getFieldsValue();
                              const currentFieldName = formValues?.keywordFields?.[index]?.fieldName || '';

                              // 如果是特殊字段，强制设置为LIKE
                              if (isSpecialField(currentFieldName) && value !== 'LIKE') {
                                form.setFieldValue(['keywordFields', index, 'searchMethod'], 'LIKE');
                                return;
                              }

                              // 正常情况下更新值
                              form.setFieldValue(['keywordFields', index, 'searchMethod'], value);
                            }}
                          >
                            {searchMethodOptionElements}
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
    </>
  );
};

export default ModuleQueryConfigModal;
