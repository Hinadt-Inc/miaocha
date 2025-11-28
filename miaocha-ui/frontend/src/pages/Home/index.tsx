import { useEffect } from 'react';

import { Splitter } from 'antd';

import { HomeProvider, useHomeContext } from './context';
import { useDataInit } from './hooks/useDataInit';
import styles from './index.module.less';
import Log from './LogModule/index';
import SearchBar from './SearchBar';
import Sider from './SiderModule/index';

/**
 * Home页面内部组件
 * 使用模块化的hooks来组织代码，提供日志查询和分析功能
 */
const HomePageContent = () => {
  const { loading, abortRef } = useHomeContext();
  const state = useHomeContext();
  const { initializeData } = useDataInit();

  // 初始化数据
  useEffect(() => {
    initializeData();
    return () => {
      if (abortRef.current) {
        abortRef.current.abort();
      }
    };
  }, []);

  if (loading) {
    return <div className={styles.layout}>加载中...</div>;
  }

  console.log('state======', state);
  return (
    <div className={styles.layout}>
      <SearchBar />

      <Splitter className={styles.container}>
        <Splitter.Panel collapsible defaultSize={200} max="50%" min={0}>
          <Sider />
        </Splitter.Panel>
        <Splitter.Panel collapsible>
          <div className={styles.right}>
            <Log />
          </div>
        </Splitter.Panel>
      </Splitter>
    </div>
  );
};

/**
 * Home页面组件（带Context Provider）
 */
const HomePage = () => {
  return (
    <HomeProvider>
      <HomePageContent />
    </HomeProvider>
  );
};

export default HomePage;
