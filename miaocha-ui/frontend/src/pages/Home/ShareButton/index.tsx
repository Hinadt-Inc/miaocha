import React, { useState } from 'react';
import { Button, Tooltip } from 'antd';
import { ShareAltOutlined } from '@ant-design/icons';
import ShareModal from '../ShareModal';
import styles from './index.module.less';

interface ShareButtonProps {
  searchParams: any;
  size?: 'small' | 'middle' | 'large';
  type?: 'default' | 'primary' | 'text' | 'link';
  variant?: 'outlined' | 'filled' | 'text' | 'link';
  className?: string;
}

const ShareButton: React.FC<ShareButtonProps> = ({
  searchParams,
  size = 'small',
  type = 'link',
  variant = 'link',
  className,
}) => {
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
          icon={<ShareAltOutlined />}
          onClick={handleShare}
          size={size}
          type={type}
          variant={variant}
          className={`${styles.shareButton} ${className || ''}`}
        >
          分享
        </Button>
      </Tooltip>
      
      <ShareModal
        visible={modalVisible}
        onClose={handleCloseModal}
        searchParams={searchParams}
      />
    </>
  );
};

export default ShareButton;
