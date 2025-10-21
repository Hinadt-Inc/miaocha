import React from 'react';
import HistoryDrawer from './HistoryDrawer';

export interface SQLHistoryDrawerProps {
  visible: boolean;
  onClose: () => void;
  queryHistory: any[];
  onLoadFromHistory: (sql: string) => void;
  onCopyToClipboard: (text: string) => void;
  pagination: any;
  onPaginationChange: (page: number, pageSize: number) => void;
}

/**
 * SQL历史抽屉组件
 * 封装原有的HistoryDrawer组件，提供统一的接口
 */
export const SQLHistoryDrawer: React.FC<SQLHistoryDrawerProps> = ({
  visible,
  onClose,
  queryHistory,
  onLoadFromHistory,
  onCopyToClipboard,
  pagination,
  onPaginationChange,
}) => {
  return (
    <HistoryDrawer
      copyToClipboard={onCopyToClipboard}
      loadFromHistory={onLoadFromHistory}
      pagination={pagination}
      queryHistory={queryHistory}
      visible={visible}
      onClose={onClose}
      onPaginationChange={onPaginationChange}
    />
  );
};
