import React, { useState, useEffect, useMemo, forwardRef, useImperativeHandle, useRef, useCallback } from 'react';
import { Collapse, Input } from 'antd';
import { ISiderProps, ISiderRef, IFieldData } from './types';
import { ModuleSelector, FieldListItem } from './components';
import { useFavoriteModule, useColumns, useModuleSelection, useDistributions } from './hooks';
import { getFavoriteModule, removeLocalActiveColumns, updateSearchParamsInStorage } from './utils';
import styles from './styles/index.module.less';

/**
 * 侧边栏组件 - 模块化重构版本
 * 包含模块选择、字段管理和字段分布查询功能
 */
const Sider = forwardRef<ISiderRef, ISiderProps>((props, ref) => {
  const {
    modules,
    onChangeColumns,
    searchParams,
    setWhereSqlsFromSider,
    onActiveColumnsChange,
    onSelectedModuleChange,
    moduleQueryConfig,
    onCommonColumnsChange,
    selectedModule: externalSelectedModule,
    activeColumns: externalActiveColumns,
    onColumnsLoaded,
  } = props;

  // 状态管理
  const [activeColumns, setActiveColumns] = useState<string[]>([]);
  const [searchText, setSearchText] = useState<string>('');
  const [isFirstLoad, setIsFirstLoad] = useState<boolean>(true);

  // 自定义Hooks
  const { favoriteModule, toggleFavorite } = useFavoriteModule();

  const { columns, getColumns, toggleColumn } = useColumns(
    moduleQueryConfig,
    onChangeColumns,
    onCommonColumnsChange,
    onColumnsLoaded,
    externalActiveColumns, // 传递外部activeColumns
  );
  const { selectedModule, setSelectedModule, lastModuleRef, changeModules } = useModuleSelection(
    modules,
    externalSelectedModule,
    onSelectedModuleChange,
  );
  const { distributions, distributionLoading, getDistributionWithSearchBar, getDistribution } = useDistributions(
    searchParams,
    columns,
  );

  // Refs
  const modulesRef = useRef<IStatus[]>([]);
  const getColumnsTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const [hasCalledGetColumns, setHasCalledGetColumns] = useState<string>('');

  // 同步外部传入的selectedModule
  useEffect(() => {
    if (externalSelectedModule && externalSelectedModule !== selectedModule) {
      setSelectedModule(externalSelectedModule);
      lastModuleRef.current = externalSelectedModule;
      setHasCalledGetColumns('');
    }
  }, [externalSelectedModule, selectedModule, setSelectedModule]);

  // 同步外部传入的activeColumns
  useEffect(() => {
    if (externalActiveColumns && JSON.stringify(externalActiveColumns) !== JSON.stringify(activeColumns)) {
      console.log('Sider同步外部字段:', externalActiveColumns);
      setActiveColumns(externalActiveColumns);
    }
  }, [externalActiveColumns, activeColumns]);

  // 监听searchParams.module变化
  useEffect(() => {
    if (
      searchParams.module &&
      searchParams.module !== selectedModule &&
      searchParams.module !== lastModuleRef.current
    ) {
      lastModuleRef.current = searchParams.module;
      setSelectedModule(searchParams.module);
      setHasCalledGetColumns('');
    }
  }, [searchParams.module, selectedModule, setSelectedModule]);

  // 自动选择模块
  useEffect(() => {
    if (modules?.length > 0 && selectedModule?.length === 0) {
      const favorite = getFavoriteModule();
      let targetModule = null;

      if (favorite) {
        targetModule = modules.find((item: IStatus) => item.module === favorite);
      }

      if (!targetModule) {
        targetModule = modules[0];
      }

      if (targetModule) {
        const module = String(targetModule.module);
        setSelectedModule(module);
        lastModuleRef.current = module;
        setHasCalledGetColumns('');
        if (onSelectedModuleChange) {
          onSelectedModuleChange(module, targetModule.datasourceId);
        }
      }
    }
  }, [modules, favoriteModule, selectedModule, setSelectedModule, onSelectedModuleChange]);

  // 更新modules引用
  useEffect(() => {
    modulesRef.current = modules;
  }, [modules]);

  // 调用getColumns
  useEffect(() => {
    if (getColumnsTimeoutRef.current) {
      clearTimeout(getColumnsTimeoutRef.current);
    }

    if (selectedModule && moduleQueryConfig !== undefined && modulesRef.current.length > 0) {
      const targetModule = modulesRef.current.find((item: IStatus) => item.value === selectedModule);
      if (targetModule) {
        const datasourceId = targetModule.datasourceId;
        const callKey = `${selectedModule}_${datasourceId}_${moduleQueryConfig?.timeField || 'default'}`;

        if (hasCalledGetColumns !== callKey && !getColumns.loading) {
          getColumnsTimeoutRef.current = setTimeout(() => {
            setHasCalledGetColumns(callKey);
            getColumns.run({ module: selectedModule });
          }, 200);
        }
      }
    }

    return () => {
      if (getColumnsTimeoutRef.current) {
        clearTimeout(getColumnsTimeoutRef.current);
      }
    };
  }, [selectedModule, moduleQueryConfig, hasCalledGetColumns, getColumns]);

  // 初始化
  useEffect(() => {
    removeLocalActiveColumns();
  }, []);

  // 监听搜索条件变化，重新获取分布数据
  useEffect(() => {
    // 页面第一次加载时不调用接口
    if (isFirstLoad) {
      setIsFirstLoad(false);
      return;
    }

    // 确保模块已选择且模块配置已加载，避免报错"模块名称不能为空"
    if (!selectedModule || !moduleQueryConfig) {
      return;
    }

    const savedSearchParams = localStorage.getItem('searchBarParams');
    if (savedSearchParams) {
      const parsedParams = JSON.parse(savedSearchParams);
      if (parsedParams?.fields?.length) {
        const timer = setTimeout(() => {
          getDistributionWithSearchBar();
        }, 300);
        return () => clearTimeout(timer);
      }
    } else {
      // 即使没有保存的搜索参数，如果有活跃的字段，也要重新获取分布数据
      const localActiveColumns = JSON.parse(localStorage.getItem('activeColumns') || '[]');
      if (localActiveColumns?.length > 0) {
        const timer = setTimeout(() => {
          getDistributionWithSearchBar();
        }, 300);
        return () => clearTimeout(timer);
      }
    }
  }, [
    searchParams.whereSqls,
    searchParams.keywords,
    searchParams.startTime,
    searchParams.endTime,
    getDistributionWithSearchBar,
    isFirstLoad,
    selectedModule, // 添加selectedModule依赖
    moduleQueryConfig, // 添加moduleQueryConfig依赖
  ]);

  // 处理字段切换
  const handleToggleColumn = useCallback(
    (data: ILogColumnsResponse) => {
      const newActiveColumns = toggleColumn(data);
      if (newActiveColumns && onActiveColumnsChange) {
        onActiveColumnsChange(newActiveColumns);
      }
      updateSearchParamsInStorage(newActiveColumns || []);
    },
    [toggleColumn, onActiveColumnsChange],
  );

  // 当外部activeColumns变化时，检查同步状态（暂时只记录，避免无限循环）
  useEffect(() => {
    if (externalActiveColumns && externalActiveColumns.length > 0 && columns.length > 0) {
      const currentSelectedColumns = columns
        .filter((col) => col.selected)
        .map((col) => col.columnName)
        .filter(Boolean) as string[];

      const hasAllExpected = externalActiveColumns.every((field) => currentSelectedColumns.includes(field));
      const hasOnlyExpected = currentSelectedColumns.every((field) => externalActiveColumns.includes(field));
      const needSync = !hasAllExpected || !hasOnlyExpected;

      if (needSync) {
        console.log('Sider状态不同步 - 外部:', externalActiveColumns, '内部:', currentSelectedColumns);
      }
    }
  }, [externalActiveColumns, columns]);

  // 处理收藏切换
  const handleToggleFavorite = useCallback(
    (module: string, e: React.MouseEvent) => {
      e.stopPropagation();
      toggleFavorite(module);
    },
    [toggleFavorite],
  );

  // 构建字段数据
  const fieldListProps: IFieldData = useMemo(() => {
    return {
      searchParams,
      distributions,
      distributionLoading,
      activeColumns,
      onToggle: handleToggleColumn,
      setWhereSqlsFromSider,
      onActiveColumns: setActiveColumns,
      onDistribution: (columnName: string, newActiveColumns: string[], sql: string) =>
        getDistribution(columnName, newActiveColumns, sql),
    };
  }, [
    searchParams,
    distributions,
    distributionLoading,
    activeColumns,
    handleToggleColumn,
    getDistribution,
    setWhereSqlsFromSider,
  ]);

  // 过滤可用字段
  const filteredAvailableColumns = useMemo(() => {
    const availableColumns = columns?.filter((item: ILogColumnsResponse) => !item.selected);
    if (!searchText.trim()) {
      return availableColumns;
    }
    return availableColumns?.filter((item: ILogColumnsResponse) =>
      item.columnName?.toLowerCase().includes(searchText.toLowerCase()),
    );
  }, [columns, searchText]);

  // 暴露给父组件的方法
  useImperativeHandle(ref, () => ({
    getDistributionWithSearchBar,
  }));

  // 清理工作
  useEffect(() => {
    return () => {
      if (getColumnsTimeoutRef.current) {
        clearTimeout(getColumnsTimeoutRef.current);
      }
    };
  }, []);

  return (
    <div className={styles.layoutSider}>
      {/* 模块选择器 */}
      <ModuleSelector
        modules={modules}
        selectedModule={selectedModule}
        favoriteModule={favoriteModule}
        onModuleChange={changeModules}
        onToggleFavorite={handleToggleFavorite}
      />

      {/* 字段折叠面板 */}
      <Collapse
        ghost
        size="small"
        defaultActiveKey={['selected', 'available']}
        expandIcon={() => null}
        items={[
          {
            key: 'selected',
            label: '已选字段',
            children: columns
              ?.filter((item: ILogColumnsResponse) => item.selected)
              ?.sort((a: ILogColumnsResponse, b: ILogColumnsResponse) => (a._createTime || 0) - (b._createTime || 0))
              ?.map((item: ILogColumnsResponse, index: number) => (
                <FieldListItem
                  key={item.columnName}
                  isSelected
                  column={item}
                  columnIndex={index}
                  fieldData={fieldListProps}
                  moduleQueryConfig={moduleQueryConfig}
                />
              )),
          },
          {
            key: 'available',
            label: `可用字段 (${filteredAvailableColumns?.length || 0})`,
            children: [
              <Input.Search
                key="search"
                placeholder="搜索字段"
                allowClear
                variant="filled"
                className={styles.searchInput}
                value={searchText}
                onChange={(e) => setSearchText(e.target.value)}
              />,
              <div key="field-list-container" className={styles.virtualListWrapper}>
                {filteredAvailableColumns && filteredAvailableColumns.length > 0 ? (
                  <div className={styles.fieldListContainer}>
                    {filteredAvailableColumns.map((item: ILogColumnsResponse, index: number) => (
                      <FieldListItem
                        key={item.columnName}
                        isSelected={false}
                        column={item}
                        columnIndex={index}
                        fieldData={fieldListProps}
                        moduleQueryConfig={moduleQueryConfig}
                      />
                    ))}
                  </div>
                ) : (
                  <div className={styles.emptyState}>{searchText ? '未找到匹配的字段' : '暂无可用字段'}</div>
                )}
              </div>,
            ],
          },
        ]}
      />
    </div>
  );
});

Sider.displayName = 'Sider';

export default Sider;
