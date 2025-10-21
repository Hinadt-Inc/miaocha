import { Form, Input, Modal, Tag, Spin, Button, Space, Tooltip, message, theme } from 'antd';
import type { InputRef } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useEffect, useState, useRef } from 'react';
import type { LogstashProcess } from '@/types/logstashTypes';
import { getModules } from '@/api/modules';
import { getMachines } from '@/api/machine';
import type { Module } from '@/api/modules';
import styles from './LogstashAlertModal.module.less';

interface LogstashAlertModalProps {
  readonly visible: boolean;
  readonly onCancel: () => void;
  readonly onOk: (values: Partial<LogstashProcess>) => Promise<void>;
  readonly initialValues?: LogstashProcess | null;
}

export default function LogstashAlertModal({ visible, onCancel, onOk, initialValues }: LogstashAlertModalProps) {
  const [form] = Form.useForm();
  const [confirmLoading, setConfirmLoading] = useState(false);
  const [loading, setLoading] = useState(false);
  const [moduleData, setModuleData] = useState<Module[]>([]);
  const [messageApi, contextHolder] = message.useMessage();

  const { token } = theme.useToken();
  const [emails, setEmails] = useState<string[]>([]);
  const [inputValue, setInputValue] = useState('');

  // 智能解析邮箱输入
  const parseEmails = (input: string): string[] => {
    if (!input.trim()) return [];

    // 支持多种分隔符：换行、逗号、分号、空格
    const emailList = input
      .split(/[\n,;\s]+/) // 按换行、逗号、分号、空格分割
      .map((email) => email.trim()) // 去除首尾空格
      .filter((email) => email.length > 0); // 过滤空字符串

    return emailList;
  };

  // 验证邮箱格式
  const validateEmail = (email: string): boolean => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  };

  // 验证邮箱长度
  const validateEmailLength = (email: string): boolean => {
    return email.length <= 60;
  };

  // 处理智能输入
  const handleSmartInput = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const value = e.target.value;
    setInputValue(value);

    // 实时解析并验证邮箱
    const parsedEmails = parseEmails(value);
    const validEmails = parsedEmails.filter(validateEmail); // 只过滤格式，不过滤长度

    // 去重
    const uniqueEmails = [...new Set(validEmails)];
    setEmails(uniqueEmails);
  };

  // 移除单个邮箱
  const handleRemoveEmail = (emailToRemove: string) => {
    const newEmails = emails.filter((email) => email !== emailToRemove);
    setEmails(newEmails);

    // 同时从输入框中移除对应的文本
    const lines = inputValue.split('\n');
    const filteredLines = lines.filter((line) => {
      const lineEmails = parseEmails(line);
      return !lineEmails.includes(emailToRemove);
    });
    setInputValue(filteredLines.join('\n'));
  };

  // 清空所有邮箱
  const handleClearAll = () => {
    setEmails([]);
    setInputValue('');
  };

  // 获取无效邮箱列表
  const getInvalidEmails = (): string[] => {
    const allEmails = parseEmails(inputValue);
    return allEmails.filter((email) => !validateEmail(email));
  };

  // 获取超长邮箱列表
  const getOversizedEmails = (): string[] => {
    const allEmails = parseEmails(inputValue);
    return allEmails.filter((email) => !validateEmailLength(email));
  };

  // 获取重复邮箱列表
  const getDuplicateEmails = (): string[] => {
    const allEmails = parseEmails(inputValue);
    const duplicates: string[] = [];
    const seen = new Set<string>();

    allEmails.forEach((email) => {
      if (seen.has(email)) {
        duplicates.push(email);
      } else {
        seen.add(email);
      }
    });

    return duplicates;
  };

  // 将邮箱数组转换为输入框文本
  const emailsToInputText = (emailArray: string[]): string => {
    return emailArray.join('\n');
  };

  // 将输入框文本转换为邮箱数组
  const inputTextToEmails = (text: string): string[] => {
    return parseEmails(text).filter(validateEmail).filter(validateEmailLength);
  };

  // 获取所有邮箱（包括超长的）
  const getAllEmails = (): string[] => {
    return parseEmails(inputValue).filter(validateEmail);
  };

  useEffect(() => {
    const fetchData = async () => {
      if (visible) {
        setLoading(true);
        try {
          // 如果有初始值，回显告警邮箱
          if (initialValues?.alertRecipients) {
            console.log('Initial alert recipients:', initialValues.alertRecipients);
            const emailList = Array.isArray(initialValues.alertRecipients) ? initialValues.alertRecipients : [];

            // 设置输入框的值
            const inputText = emailsToInputText(emailList);
            setInputValue(inputText);

            // 设置邮箱列表状态
            setEmails(emailList);
          } else {
            // 没有初始值，清空状态
            setInputValue('');
            setEmails([]);
          }
        } finally {
          setLoading(false);
        }
      }
    };
    console.log('LogstashAlertModal---initialValues', initialValues);

    fetchData();
  }, [visible, initialValues]);

  const handleOk = async () => {
    try {
      setConfirmLoading(true);

      // 1. 检查是否有任何邮箱输入
      const allEmails = parseEmails(inputValue);

      // 如果没有输入任何邮箱，直接提交空数组（允许不设置告警）
      if (allEmails.length === 0) {
        await onOk({
          id: initialValues?.id,
          alertRecipients: [],
        });
        onCancel();
        return;
      }

      // 2. 检查格式错误
      const invalidEmails = getInvalidEmails();
      if (invalidEmails.length > 0) {
        messageApi.warning(`发现 ${invalidEmails.length} 个无效邮箱格式，请修正后再保存`);
        return;
      }

      // 3. 检查长度错误
      const oversizedEmails = getOversizedEmails();
      if (oversizedEmails.length > 0) {
        messageApi.warning(`发现 ${oversizedEmails.length} 个超长邮箱，请修正后再保存`);
        return;
      }

      // 4. 检查重复
      const duplicateEmails = getDuplicateEmails();
      if (duplicateEmails.length > 0) {
        messageApi.warning(`发现 ${duplicateEmails.length} 个重复邮箱，请修正后再保存`);
        return;
      }

      // 5. 所有校验通过，提交数据
      const currentEmails = inputTextToEmails(inputValue);
      await onOk({
        id: initialValues?.id,
        alertRecipients: currentEmails,
      });

      onCancel();
    } catch (error) {
      console.error('保存告警邮箱失败:', error);
    } finally {
      setConfirmLoading(false);
    }
  };

  const invalidEmails = getInvalidEmails();
  const oversizedEmails = getOversizedEmails();
  const duplicateEmails = getDuplicateEmails();

  return (
    <Spin spinning={loading}>
      {contextHolder}
      <Modal
        className={styles.container}
        confirmLoading={confirmLoading}
        maskClosable={false}
        open={visible}
        title="设置告警邮箱"
        width={600}
        onCancel={onCancel}
        onOk={handleOk}
      >
        <div className={styles.header}>
          <span className={styles.title}>邮箱地址输入</span>
          <span className={styles.subtitle}>支持多种格式：换行、英文逗号、英文分号、空格分隔</span>
        </div>

        <div className={styles.inputArea}>
          <Input.TextArea
            className={styles.textArea}
            placeholder="请输入邮箱地址，支持多种格式：
1. 每行一个邮箱
2. 逗号分隔：email1@test.com,email2@test.com
3. 分号分隔：email1@test.com;email2@test.com
4. 空格分隔：email1@test.com email2@test.com"
            rows={6}
            value={inputValue}
            onChange={handleSmartInput}
          />

          <div className={styles.toolbar}>
            <Space>
              <Button
                className={styles.clearButton}
                disabled={emails.length === 0}
                size="small"
                onClick={handleClearAll}
              >
                清空所有
              </Button>
              <span className={styles.emailCount}>已解析出 {emails.length} 个有效邮箱</span>
            </Space>
          </div>
        </div>

        {/* 有效邮箱展示 */}
        {emails.length > 0 && (
          <div className={styles.emailList}>
            <div className={styles.listTitle}>有效邮箱列表：</div>
            <div className={styles.listContainer}>
              {emails.map((email) => {
                const isOversized = !validateEmailLength(email);
                return (
                  <span key={email} className={styles.emailTag}>
                    <Tag closable color={isOversized ? 'red' : 'green'} onClose={() => handleRemoveEmail(email)}>
                      {email}
                    </Tag>
                  </span>
                );
              })}
            </div>
          </div>
        )}

        {/* 错误提示 */}
        {(invalidEmails.length > 0 || oversizedEmails.length > 0 || duplicateEmails.length > 0) && (
          <div className={styles.errorSection}>
            {invalidEmails.length > 0 && (
              <div className={styles.errorItem}>
                <span className={styles.errorLabel}>发现 {invalidEmails.length} 个无效邮箱格式：</span>
                <div className={styles.errorContent}>{invalidEmails.join(', ')}</div>
              </div>
            )}

            {oversizedEmails.length > 0 && (
              <div className={styles.errorItem}>
                <span className={styles.warningLabel}>发现 {oversizedEmails.length} 个超长邮箱：</span>
                <div className={styles.errorContent}>{oversizedEmails.join(', ')}</div>
              </div>
            )}

            {duplicateEmails.length > 0 && (
              <div className={styles.errorItem}>
                <span className={styles.warningLabel}>发现 {duplicateEmails.length} 个重复邮箱：</span>
                <div className={styles.errorContent}>{duplicateEmails.join(', ')}</div>
              </div>
            )}
          </div>
        )}
      </Modal>
    </Spin>
  );
}
