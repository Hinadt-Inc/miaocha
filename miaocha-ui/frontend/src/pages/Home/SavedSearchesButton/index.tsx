import React, { useState } from 'react';

import { HistoryOutlined } from '@ant-design/icons';
import { Button, Tooltip } from 'antd';

import { useHomeContext } from '../context';
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
  const { searchParams, logTableColumns, moduleQueryConfig, updateSearchParams } = useHomeContext();
  const { handleLoadCacheData } = useDataInit();
  const [modalVisible, setModalVisible] = useState(false);

  const handleOpen = () => {
    setModalVisible(true);
  };

  const handleCloseModal = () => {
    setModalVisible(false);
  };

  const handleLoadSearch = (cacheSearchParams: ILogSearchParams) => {
    handleLoadCacheData(cacheSearchParams);
    setModalVisible(false);
  };

  return (
    <>
      <Tooltip title="查看已保存的搜索">
        <Button
          className={`${styles.savedSearchesButton} ${className || ''}`}
          icon={<HistoryOutlined />}
          size={size}
          type={type}
          variant={variant}
          onClick={handleOpen}
        >
          检索书签
        </Button>
      </Tooltip>

      <SavedSearchesModal visible={modalVisible} onClose={handleCloseModal} onLoadSearch={handleLoadSearch} />
    </>
  );
};

export default SavedSearchesButton;
