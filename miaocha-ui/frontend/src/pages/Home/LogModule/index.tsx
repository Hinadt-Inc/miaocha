/**
 * Log 模块主组件
 * 重构后的模块化版本，提供更好的代码组织和可维护性
 */

import React from 'react';

import { Splitter } from 'antd';

import { LogChart, LogTable } from './components';
import styles from './styles/Log.module.less';

const Log: React.FC = () => {
  return (
    <Splitter className={styles.logContainer} layout="vertical">
      <Splitter.Panel collapsible defaultSize={170} max={170} min={170}>
        <LogChart />
      </Splitter.Panel>
      <Splitter.Panel>
        <LogTable />
      </Splitter.Panel>
    </Splitter>
  );
};

export default Log;
