import React, { useState } from 'react';
import { Modal, Input, Button, message } from 'antd';
import { CopyOutlined } from '@ant-design/icons';
import styles from './index.module.less';

interface ShareModalProps {
  visible: boolean;
  onClose: () => void;
  searchParams: any;
}

const ShareModal: React.FC<ShareModalProps> = ({
  visible,
  onClose,
  searchParams,
}) => {
  const [copying, setCopying] = useState(false);

  // 生成分享链接
  const generateShareUrl = () => {
    const baseUrl = window.location.origin + window.location.pathname;
    const params = new URLSearchParams();
    
    // 添加搜索参数到URL
    if (searchParams.keywords?.length > 0) {
      params.set('keywords', JSON.stringify(searchParams.keywords));
    }
    if (searchParams.whereSqls?.length > 0) {
      params.set('whereSqls', JSON.stringify(searchParams.whereSqls));
    }
    
    // 时间范围处理：优先使用相对时间范围，避免绝对时间固化
    if (searchParams.timeRange) {
      params.set('timeRange', searchParams.timeRange);
      // 对于相对时间范围，不传递绝对时间，让接收方根据当前时间重新计算
    } else if (searchParams.startTime && searchParams.endTime) {
      // 只有在没有相对时间范围时，才使用绝对时间
      params.set('startTime', searchParams.startTime);
      params.set('endTime', searchParams.endTime);
    }
    
    if (searchParams.module) {
      params.set('module', searchParams.module);
    }
    if (searchParams.timeGrouping) {
      params.set('timeGrouping', searchParams.timeGrouping);
    }

    return `${baseUrl}?${params.toString()}`;
  };

  const shareUrl = generateShareUrl();

  // 复制链接
  const handleCopyLink = async () => {
    try {
      setCopying(true);
      await navigator.clipboard.writeText(shareUrl);
      message.success('链接已复制到剪贴板');
    } catch (error) {
      console.error('复制失败:', error);
      message.error('复制失败，请手动复制');
    } finally {
      setCopying(false);
    }
  };

  return (
    <Modal
      title="分享当前查询"
      open={visible}
      onCancel={onClose}
      footer={null}
      width={500}
      className={styles.shareModal}
    >
      <div className={styles.linkContainer}>
        <div className={styles.linkDescription}>
          复制下面的链接分享给其他人，他们打开链接后将自动应用当前的搜索条件：
        </div>
        <Input.TextArea
          value={shareUrl}
          readOnly
          rows={3}
          className={styles.linkInput}
        />
        <Button
          type="primary"
          icon={<CopyOutlined />}
          loading={copying}
          onClick={handleCopyLink}
          className={styles.copyButton}
        >
          复制链接
        </Button>
      </div>
    </Modal>
  );
};

export default ShareModal;
