import React from 'react';

import { StarOutlined, StarFilled } from '@ant-design/icons';
import { Select } from 'antd';

import { ModuleSelectOption } from '../types';

import styles from './ModuleSelector.module.less';

interface IProps {
  modules: IStatus[];
  selectedModule: string;
  favoriteModule: string;
  onModuleChange: (value: string) => void;
  onToggleFavorite: (module: string, e: React.MouseEvent) => void;
}

/**
 * 模块选择器组件
 */
const ModuleSelector: React.FC<IProps> = ({
  modules,
  selectedModule,
  favoriteModule,
  onModuleChange,
  onToggleFavorite,
}) => {
  const options: ModuleSelectOption[] = modules?.map((item: any) => ({
    ...item,
    title: item.label,
    label: (
      <div className={styles.moduleOption}>
        <span className={styles.moduleLabel}>{item.label}</span>
        <button
          className={`${styles.favoriteIcon} ${favoriteModule === item.module ? styles.active : styles.inactive}`}
          type="button"
          onClick={(e) => onToggleFavorite(item.module, e)}
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
      onChange={onModuleChange}
    />
  );
};

export default ModuleSelector;
