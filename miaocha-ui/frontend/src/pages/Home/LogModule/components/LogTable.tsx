/**
 * 日志表格组件
 * 封装虚拟表格的展示逻辑
 */

import React from 'react';

import VirtualTable from '../../VirtualTable';
import styles from '../styles/LogTable.module.less';

const LogTable: React.FC = () => {
  return (
    <div className={styles.tableContainer}>
      <VirtualTable />
    </div>
  );
};

export default LogTable;
