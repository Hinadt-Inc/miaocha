import React from 'react';
import { Select } from 'antd';
import { StarOutlined, StarFilled } from '@ant-design/icons';
import { IModuleSelectOption } from '../types';
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
  const options: IModuleSelectOption[] = modules?.map((item: any) => ({
    ...item,
    title: item.label,
    label: (
      <div className={styles.moduleOption}>
        <span className={styles.moduleLabel}>{item.label}</span>
        <button
          type="button"
          onClick={(e) => onToggleFavorite(item.module, e)}
          className={`${styles.favoriteIcon} ${favoriteModule === item.module ? styles.active : styles.inactive}`}
        >
          {favoriteModule === item.module ? <StarFilled /> : <StarOutlined />}
        </button>
      </div>
    ),
  }));

  return (
    <Select
      showSearch
      allowClear={false}
      variant="filled"
      placeholder="请选择模块"
      className={styles.moduleSelector}
      value={selectedModule}
      onChange={onModuleChange}
      optionLabelProp="title"
      options={options}
    />
  );
};

export default ModuleSelector;
