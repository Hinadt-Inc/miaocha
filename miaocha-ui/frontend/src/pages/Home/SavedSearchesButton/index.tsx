import React, { useState } from 'react';
import { Button, Tooltip } from 'antd';
import { HistoryOutlined } from '@ant-design/icons';
import SavedSearchesModal from '../SavedSearchesModal';
import styles from './index.module.less';

interface SavedSearchesButtonProps {
  onLoadSearch: (searchParams: any) => void;
  size?: 'small' | 'middle' | 'large';
  type?: 'default' | 'primary' | 'text' | 'link';
  variant?: 'outlined' | 'filled' | 'text' | 'link';
  className?: string;
}

const SavedSearchesButton: React.FC<SavedSearchesButtonProps> = ({
  onLoadSearch,
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

  const handleLoadSearch = (searchParams: any) => {
    onLoadSearch(searchParams);
    setModalVisible(false);
  };

  return (
    <>
      <Tooltip title="查看已保存的搜索">
        <Button
          icon={<HistoryOutlined />}
          onClick={handleOpen}
          size={size}
          type={type}
          variant={variant}
          className={`${styles.savedSearchesButton} ${className || ''}`}
        >
          检索书签
        </Button>
      </Tooltip>
      
      <SavedSearchesModal
        visible={modalVisible}
        onClose={handleCloseModal}
        onLoadSearch={handleLoadSearch}
      />
    </>
  );
};

export default SavedSearchesButton;
