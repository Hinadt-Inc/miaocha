import { useState, useEffect, useMemo } from 'react';
import { Collapse, Select, Spin, Input, Space } from 'antd';
import { StarOutlined, StarFilled } from '@ant-design/icons';
import { useRequest } from 'ahooks';
import * as api from '@/api/logs';
import styles from './Sider.module.less';

import FieldListItem from './FieldListItem';

interface IProps {
  searchParams: ILogSearchParams; // 搜索参数
  modules: IStatus[]; // 模块名称列表
  moduleLoading: boolean; // 模块名称是否正在加载
  detailLoading: boolean; // 日志数据是否正在加载
  onSearch: (params: ILogSearchParams) => void; // 搜索回调函数
  onChangeColumns?: (params: ILogColumnsResponse[]) => void; // 列变化回调函数
  setWhereSqlsFromSider: any; // 设置where条件
  onActiveColumnsChange?: (activeColumns: string[]) => void; // 激活字段变化回调函数
}

const Sider: React.FC<IProps> = (props) => {
  const {
    detailLoading,
    modules,
    moduleLoading,
    onChangeColumns,
    onSearch,
    searchParams,
    setWhereSqlsFromSider,
    onActiveColumnsChange,
  } = props;
  const [columns, setColumns] = useState<ILogColumnsResponse[]>([]); // 日志表字段
  const [selectedModule, setSelectedModule] = useState<string>(''); // 已选模块
  const [distributions, setDistributions] = useState<Record<string, IFieldDistributions>>({}); // 字段值分布列表
  const [activeColumns, setActiveColumns] = useState<string[]>([]); // 激活的字段
  const [searchText, setSearchText] = useState<string>(''); // 字段搜索文本
  const [favoriteModule, setFavoriteModule] = useState<string>(''); // 收藏的模块

  // 获取日志字段
  const getColumns = useRequest(api.fetchColumns, {
    manual: true,
    onSuccess: (res) => {
      // 确保log_time和message字段默认被选中
      const processedColumns = res?.map((column) => {
        if (column.columnName === 'log_time') {
          return { ...column, selected: true, _createTime: new Date().getTime() };
        }
        return column;
      });

      setColumns(processedColumns);
      // 初始加载时也通知父组件列变化
      if (onChangeColumns) {
        onChangeColumns(processedColumns);
      }
    },
    onError: () => {
      setColumns([]);
    },
  });

  // 获取指定字段的TOP5分布数据
  const queryDistribution = useRequest(api.fetchDistributions, {
    manual: true,
    onSuccess: (res) => {
      const { fieldDistributions = [] } = res;
      const target: any = {};
      fieldDistributions.forEach((item) => {
        const { fieldName } = item;
        target[fieldName] = item;
      });
      setDistributions(target);
    },
    onError: () => {
      setDistributions({});
    },
  });

  // 获取收藏的模块
  const getFavoriteModule = () => {
    return localStorage.getItem('favoriteModule') || '';
  };

  // 设置收藏的模块
  const setFavoriteModuleStorage = (module: string) => {
    if (module) {
      localStorage.setItem('favoriteModule', module);
    } else {
      localStorage.removeItem('favoriteModule');
    }
    setFavoriteModule(module);
  };

  // 切换收藏状态
  const toggleFavorite = (module: string, e: React.MouseEvent) => {
    e.stopPropagation(); // 防止触发Select的选择
    const currentFavorite = getFavoriteModule();
    if (currentFavorite === module) {
      // 取消收藏
      setFavoriteModuleStorage('');
    } else {
      // 设置收藏
      setFavoriteModuleStorage(module);
    }
  };

  // 初始化收藏状态
  useEffect(() => {
    const favorite = getFavoriteModule();
    setFavoriteModule(favorite);
  }, []);

  // 选择模块时触发，避免重复请求和状态更新
  const changeModules = (value: string) => {
    if (!value) {
      setSelectedModule('');
      return;
    }
    const datasourceId = modules.find((item) => item.value === value)?.datasourceId;
    // 解析value：datasourceId-module
    setSelectedModule(value);
    getColumns.run({ datasourceId: Number(datasourceId), module: value });
    onSearch({
      ...searchParams,
      datasourceId: Number(datasourceId),
      module: value,
      offset: 0,
    });
  };

  // 当 modules 加载完成后，自动选择第一个数据源和第一个模块
  useEffect(() => {
    if (modules?.length > 0 && selectedModule?.length === 0) {
      const favorite = getFavoriteModule();
      let targetModule = null;

      // 优先选择收藏的模块
      if (favorite) {
        targetModule = modules.find((item) => item.module === favorite);
      }

      // 如果没有收藏或收藏的模块不存在，选择第一个
      if (!targetModule) {
        targetModule = modules[0];
      }

      if (targetModule) {
        const datasourceId = String(targetModule.datasourceId);
        const module = String(targetModule.module);
        setSelectedModule(module);
        getColumns.run({ datasourceId: Number(datasourceId), module });
      }
    }
  }, [modules, favoriteModule]);

  // 切换字段选中状态
  const toggleColumn = (data: ILogColumnsResponse) => {
    const index = columns.findIndex((item) => item.columnName === data.columnName);
    // 如果是log_time字段，不允许取消选择
    if (columns[index].columnName === 'log_time' || columns[index].isFixed) {
      return;
    }
    // 添加
    if (!columns[index].selected) {
      columns[index].selected = true;
      columns[index]._createTime = new Date().getTime();
    } else {
      // 删除
      columns[index].selected = false;
      delete columns[index]._createTime;
    }

    // if (data?.columnName?.includes('.')) {
    // 添加或者移除的时候，计算新的激活字段列表
    const newActiveColumns = columns
      .filter((item) => item.selected)
      .map((item) => item.columnName)
      .filter(Boolean) as string[];
    // 通知父组件激活字段变化;
    if (onActiveColumnsChange) {
      onActiveColumnsChange(newActiveColumns);
    }
    // }

    // 排序
    const sortedColumns = columns.sort((a, b) => (a._createTime || 0) - (b._createTime || 0));
    const updatedColumns = [...sortedColumns];
    setColumns(updatedColumns);

    // 通知父组件列变化
    if (onChangeColumns) {
      onChangeColumns(updatedColumns);
    }
  };

  // 获取字段值分布
  const getDistribution = (columnName: string, newActiveColumns: string[], sql: string) => {
    if (!newActiveColumns.includes(columnName)) return;
    // 通知父组件激活字段变化
    // if (onActiveColumnsChange) {
    //   onActiveColumnsChange(newActiveColumns);
    // }

    const params: ILogSearchParams = {
      ...searchParams,
      fields: newActiveColumns,
      offset: 0,
    };
    if (sql) {
      params.whereSqls = [...(searchParams?.whereSqls || []), sql];
    }
    queryDistribution.run(params);
  };

  const fieldListProps = useMemo(() => {
    return {
      searchParams,
      distributions,
      activeColumns,
      onToggle: toggleColumn,
      setWhereSqlsFromSider,
      onActiveColumns: setActiveColumns,
      onDistribution: (columnName: string, newActiveColumns: string[], sql: string) =>
        getDistribution(columnName, newActiveColumns, sql),
    };
  }, [searchParams, distributions, activeColumns, toggleColumn, getDistribution, setWhereSqlsFromSider]);

  // 根据搜索文本过滤可用字段
  const filteredAvailableColumns = useMemo(() => {
    const availableColumns = columns?.filter((item) => !item.selected);
    if (!searchText.trim()) {
      return availableColumns?.sort((a, b) => {
        if (!a.columnName) return -1;
        if (!b.columnName) return 1;
        return a.columnName.localeCompare(b.columnName);
      });
    }
    return availableColumns
      ?.filter((item) => item.columnName?.toLowerCase().includes(searchText.toLowerCase()))
      ?.sort((a, b) => {
        if (!a.columnName) return -1;
        if (!b.columnName) return 1;
        return a.columnName.localeCompare(b.columnName);
      });
  }, [columns, searchText]);

  return (
    <div className={styles.layoutSider}>
      <Select
        showSearch
        allowClear={false}
        variant="filled"
        placeholder="请选择模块"
        style={{ width: '100%' }}
        value={selectedModule}
        onChange={changeModules}
        loading={moduleLoading}
        disabled={moduleLoading || detailLoading}
        optionLabelProp="title"
        options={modules?.map((item) => ({
          ...item,
          title: item.label,
          label: (
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span>{item.label}</span>
              <span
                onClick={(e) => toggleFavorite(item.module, e)}
                style={{
                  cursor: 'pointer',
                  color: favoriteModule === item.module ? '#0038ff' : '#d9d9d9',
                  fontSize: '12px',
                }}
              >
                {favoriteModule === item.module ? <StarFilled /> : <StarOutlined />}
              </span>
            </div>
          ),
        }))}
      />

      <Spin spinning={getColumns.loading || queryDistribution.loading} size="small">
        <Collapse
          ghost
          size="small"
          className={styles.collapse}
          defaultActiveKey={['selected', 'available']}
          items={[
            {
              key: 'selected',
              label: '已选字段',
              children: columns
                ?.filter((item) => item.selected)
                ?.sort((a, b) => (a._createTime || 0) - (b._createTime || 0))
                ?.map((item, index) => (
                  <FieldListItem
                    key={item.columnName}
                    isSelected
                    column={item}
                    columnIndex={index}
                    fieldData={fieldListProps}
                  />
                )),
            },
            {
              key: 'available',
              label: '可用字段',
              children: [
                <Input.Search
                  key="search"
                  placeholder="搜索字段"
                  allowClear
                  variant="filled"
                  style={{ width: '100%', marginBottom: 8 }}
                  value={searchText}
                  onChange={(e) => setSearchText(e.target.value)}
                />,
                ...(filteredAvailableColumns?.map((item, index) => (
                  <FieldListItem
                    key={item.columnName}
                    isSelected={false}
                    column={item}
                    columnIndex={index}
                    fieldData={fieldListProps}
                  />
                )) || []),
              ],
            },
          ]}
        />
      </Spin>
    </div>
  );
};
export default Sider;
