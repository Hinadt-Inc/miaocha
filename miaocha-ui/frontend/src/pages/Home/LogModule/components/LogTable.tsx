/**
 * 日志表格组件
 * 封装虚拟表格的展示逻辑
 */

import React from 'react';

import VirtualTable from '../../VirtualTable';
import styles from '../styles/LogTable.module.less';
import { ILogTableProps } from '../types';

const LogTable: React.FC<ILogTableProps> = (props) => {
  return (
    <div className={styles.tableContainer}>
      <VirtualTable {...(props as any)} />
    </div>
  );
};

export default LogTable;
