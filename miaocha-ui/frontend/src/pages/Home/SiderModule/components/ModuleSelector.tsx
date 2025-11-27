import React, { useCallback, useEffect, useState } from 'react';

import { StarOutlined, StarFilled } from '@ant-design/icons';
import { Select, Tooltip } from 'antd';

import { useHomeContext } from '../../context';
import { useDataInit } from '../../hooks/useDataInit';
import { ModuleSelectOption } from '../types';

import styles from './ModuleSelector.module.less';

/**
 * 模块选择器组件
 */
const ModuleSelector: React.FC = () => {
  const { moduleOptions, searchParams, setSearchParams } = useHomeContext();
  const { handleReloadData } = useDataInit();
  const [selectedModule, setSelectedModule] = useState<string>(searchParams.module || '');
  const [favoriteModule, setFavoriteModule] = useState<string | null>();

  const handleChangeModule = useCallback(
    (value: string) => {
      const moduleItem = moduleOptions.find((item) => item.value === value);
      if (moduleItem) {
        // todo 重置一些状态， distributionData, distributionLoading, logTableColumns
        setSelectedModule(moduleItem?.module || '');
        setSearchParams({
          ...searchParams,
          datasourceId: moduleItem.datasourceId as number,
          module: moduleItem.module,
        });
        handleReloadData({ module: moduleItem.module });
      }
    },
    [moduleOptions, searchParams],
  );

  const handleChangeFavorite = (module: string, isFavorite: boolean) => {
    if (isFavorite) {
      localStorage.setItem('favoriteModule', '');
    }
    setFavoriteModule(module);
    localStorage.setItem('favoriteModule', module);
  };

  useEffect(() => {
    const favorite = localStorage.getItem('favoriteModule');
    setFavoriteModule(favorite);
  }, []);

  const options: ModuleSelectOption[] = moduleOptions?.map((item: any) => ({
    ...item,
    title: item.label,
    label: (
      <div className={styles.moduleOption}>
        <div className={styles.moduleLabel}>
          <Tooltip placement="topLeft" title={item.label}>
            <span className={styles.moduleLabelText}>{item.label}</span>
          </Tooltip>
        </div>
        <button
          className={`${styles.favoriteIcon} ${favoriteModule === item.module ? styles.active : styles.inactive}`}
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            handleChangeFavorite(item.module, favoriteModule === item.module);
          }}
        >
          {favoriteModule === item.module ? <StarFilled /> : <StarOutlined />}
        </button>
      </div>
    ),
  }));

  return (
    <Select
      allowClear={false}
      className={styles.moduleSelector}
      optionLabelProp="title"
      options={options}
      placeholder="请选择模块"
      showSearch
      value={selectedModule}
      variant="filled"
      onChange={handleChangeModule}
    />
  );
};

export default ModuleSelector;
