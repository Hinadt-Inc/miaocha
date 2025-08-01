import React, { useState } from 'react';
import { Button, Tooltip } from 'antd';
import { SaveOutlined } from '@ant-design/icons';
import SaveSearchModal from '../SaveSearchModal';
import styles from './index.module.less';

interface SaveSearchButtonProps {
  searchParams: any;
  size?: 'small' | 'middle' | 'large';
  type?: 'default' | 'primary' | 'text' | 'link';
  variant?: 'outlined' | 'filled' | 'text' | 'link';
  className?: string;
}

const SaveSearchButton: React.FC<SaveSearchButtonProps> = ({
  searchParams,
  size = 'small',
  type = 'link',
  variant = 'link',
  className,
}) => {
  const [modalVisible, setModalVisible] = useState(false);

  const handleSave = () => {
    setModalVisible(true);
  };

  const handleCloseModal = () => {
    setModalVisible(false);
  };

  return (
    <>
      <Tooltip title="保存当前搜索条件">
        <Button
          icon={<SaveOutlined />}
          onClick={handleSave}
          size={size}
          type={type}
          variant={variant}
          className={`${styles.saveSearchButton} ${className || ''}`}
        >
          保存
        </Button>
      </Tooltip>
      
      <SaveSearchModal
        visible={modalVisible}
        onClose={handleCloseModal}
        searchParams={searchParams}
      />
    </>
  );
};

export default SaveSearchButton;
