import React, { useState } from 'react';

import { HistoryOutlined } from '@ant-design/icons';
import { Button, Tooltip } from 'antd';

import { useDataInit } from '../hooks/useDataInit';
import SavedSearchesModal from '../SavedSearchesModal';

import styles from './index.module.less';

interface SavedSearchesButtonProps {
  size?: 'small' | 'middle' | 'large';
  type?: 'default' | 'primary' | 'text' | 'link';
  variant?: 'outlined' | 'filled' | 'text' | 'link';
  className?: string;
}

const SavedSearchesButton: React.FC<SavedSearchesButtonProps> = ({
  size = 'small',
  type = 'link',
  variant = 'link',
  className,
}) => {
  const [modalVisible, setModalVisible] = useState(false);

  const handleOpen = () => {
    setModalVisible(true);
  };

  const handleCloseModal = () => {
    setModalVisible(false);
  };

  return (
    <>
      <Tooltip title="查看已保存的搜索">
        <Button
          className={`${styles.savedSearchesButton} ${className || ''}`}
          // icon={<HistoryOutlined />}
          size={size}
          type={type}
          variant={variant}
          onClick={handleOpen}
        >
          检索书签
        </Button>
      </Tooltip>

      <SavedSearchesModal visible={modalVisible} onClose={handleCloseModal} />
    </>
  );
};

export default SavedSearchesButton;
