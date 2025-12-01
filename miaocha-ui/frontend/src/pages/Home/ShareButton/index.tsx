import React, { useState } from 'react';

// import { ShareAltOutlined } from '@ant-design/icons';
import { Button, Tooltip } from 'antd';

import ShareModal from '../ShareModal';

import styles from './index.module.less';

interface ShareButtonProps {
  size?: 'small' | 'middle' | 'large';
  type?: 'default' | 'primary' | 'text' | 'link';
  variant?: 'outlined' | 'filled' | 'text' | 'link';
  className?: string;
}

const ShareButton: React.FC<ShareButtonProps> = ({ size = 'small', type = 'link', variant = 'link', className }) => {
  const [modalVisible, setModalVisible] = useState(false);

  const handleShare = () => {
    setModalVisible(true);
  };

  const handleCloseModal = () => {
    setModalVisible(false);
  };

  return (
    <>
      <Tooltip title="分享当前查询">
        <Button
          className={`${styles.shareButton} ${className || ''}`}
          // icon={<ShareAltOutlined />}
          size={size}
          type={type}
          variant={variant}
          onClick={handleShare}
        >
          分享
        </Button>
      </Tooltip>

      <ShareModal visible={modalVisible} onClose={handleCloseModal} />
    </>
  );
};

export default ShareButton;
