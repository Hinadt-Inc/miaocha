import React, { useState } from 'react';

import { CopyOutlined } from '@ant-design/icons';
import { Modal, Input, Button } from 'antd';

import { useHomeContext } from '../context';

import styles from './index.module.less';

interface ShareModalProps {
  visible: boolean;
  onClose: () => void;
}

const ShareModal: React.FC<ShareModalProps> = ({ visible, onClose }) => {
  const { searchParams } = useHomeContext();
  const [copying, setCopying] = useState(false);

  // 生成分享链接
  const generateShareUrl = () => {
    const baseUrl = window.location.origin + window.location.pathname;
    const params = new URLSearchParams();

    // 添加搜索参数到URL
    if (searchParams?.keywords && searchParams.keywords.length > 0) {
      params.set('keywords', JSON.stringify(searchParams.keywords));
    }
    if (searchParams?.whereSqls && searchParams.whereSqls.length > 0) {
      params.set('whereSqls', JSON.stringify(searchParams.whereSqls));
    }

    // 时间范围处理：检查时间类型
    const isRelativeTime = searchParams.timeType === 'relative';

    // 这是一个预定义的相对时间范围（如 last_15m）
    params.set('timeRange', searchParams.timeRange || 'last_15m');
    if (isRelativeTime && searchParams.relativeStartOption && searchParams.relativeEndOption) {
      // 这是一个自定义的相对时间范围（如 120秒前到30秒前）
      params.set('relativeStartOption', JSON.stringify(searchParams.relativeStartOption));
      params.set('relativeEndOption', JSON.stringify(searchParams.relativeEndOption));
    }
    if (searchParams.startTime && searchParams.endTime) {
      // 这是绝对时间或者自定义时间范围，分享绝对时间
      params.set('startTime', searchParams.startTime);
      params.set('endTime', searchParams.endTime);
    }

    if (searchParams.module) {
      params.set('module', searchParams.module);
    }
    if (searchParams.timeGrouping) {
      params.set('timeGrouping', searchParams.timeGrouping);
    }

    // 添加字段信息到分享链接
    if (searchParams?.fields && searchParams.fields.length > 0) {
      params.set('fields', JSON.stringify(searchParams.fields));
    }

    return `${baseUrl}?${params.toString()}`;
  };

  const shareUrl = generateShareUrl();

  // 复制链接
  const handleCopyLink = async () => {
    try {
      setCopying(true);

      // 优先使用现代 Clipboard API（需要 HTTPS 或 localhost）
      if (navigator.clipboard && window.isSecureContext) {
        await navigator.clipboard.writeText(shareUrl);
        window.messageApi.success('链接已复制到剪贴板');
      } else {
        // 降级方案：使用传统的 document.execCommand 方法（兼容 HTTP）
        const textArea = document.createElement('textarea');
        textArea.value = shareUrl;
        textArea.style.position = 'fixed';
        textArea.style.left = '-999999px';
        textArea.style.top = '-999999px';
        document.body.appendChild(textArea);
        textArea.focus();
        textArea.select();

        const successful = document.execCommand('copy');
        document.body.removeChild(textArea);

        if (successful) {
          window.messageApi.success('链接已复制到剪贴板');
        } else {
          throw new Error('复制命令执行失败');
        }
      }
    } catch (error) {
      console.error('复制失败:', error);
      window.messageApi.error('复制失败，请手动复制');
    } finally {
      setCopying(false);
    }
  };

  return (
    <Modal
      className={styles.shareModal}
      footer={null}
      open={visible}
      title="分享当前查询"
      width={500}
      onCancel={onClose}
    >
      <div className={styles.linkContainer}>
        <div className={styles.linkDescription}>
          复制下面的链接分享给其他人，他们打开链接后将自动应用当前的搜索条件：
        </div>
        <Input.TextArea className={styles.linkInput} readOnly rows={3} value={shareUrl} />
        <Button
          className={styles.copyButton}
          icon={<CopyOutlined />}
          loading={copying}
          type="primary"
          onClick={handleCopyLink}
        >
          复制链接
        </Button>
      </div>
    </Modal>
  );
};

export default ShareModal;
