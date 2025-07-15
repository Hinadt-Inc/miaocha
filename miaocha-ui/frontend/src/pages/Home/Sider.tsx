// @ts-ignore
import { useState, useEffect, useMemo, forwardRef, useImperativeHandle, useRef, useCallback } from 'react';
import { Collapse, Select, Input } from 'antd';
import { StarOutlined, StarFilled } from '@ant-design/icons';
import { useRequest } from 'ahooks';
import * as api from '@/api/logs';
import styles from './Sider.module.less';

import FieldListItem from './FieldListItem';

interface IProps {
  searchParams: ILogSearchParams; // 搜索参数
  modules: IStatus[]; // 模块名称列表
  onSearch: (params: ILogSearchParams) => void; // 搜索回调函数
  onChangeColumns?: (params: ILogColumnsResponse[]) => void; // 列变化回调函数
  setWhereSqlsFromSider: any; // 设置where条件
  onActiveColumnsChange?: (activeColumns: string[]) => void; // 激活字段变化回调函数
  onSelectedModuleChange?: (selectedModule: string, datasourceId?: number) => void; // 选中模块变化回调函数
  moduleQueryConfig?: any; // 模块查询配置
  onCommonColumnsChange?: (commonColumns: string[]) => void; // 普通字段变化回调函数
}

// 简化的虚拟滚动组件
const VirtualFieldList: React.FC<{
  data: any[];
  itemHeight: number;
  containerHeight: number;
  renderItem: (item: any, index: number) => React.ReactNode;
}> = ({ data, itemHeight, containerHeight, renderItem }) => {
  const [scrollTop, setScrollTop] = useState(0);
  const containerRef = useRef<HTMLDivElement>(null);

  const handleScroll = useCallback((e: React.UIEvent<HTMLDivElement>) => {
    setScrollTop(e.currentTarget.scrollTop);
  }, []);

  // 计算可视范围，增大缓冲区
  const visibleRange = useMemo(() => {
    const actualHeight = containerRef.current?.clientHeight || containerHeight;
    const visibleCount = Math.ceil(actualHeight / itemHeight);
    const bufferSize = Math.max(5, Math.floor(visibleCount / 2)); // 动态缓冲区，至少5个元素

    const start = Math.floor(scrollTop / itemHeight);
    const end = Math.min(data.length, start + visibleCount + bufferSize);

    return {
      start: Math.max(0, start - bufferSize),
      end,
    };
  }, [scrollTop, itemHeight, containerHeight, data.length]);

  const visibleData = data.slice(visibleRange.start, visibleRange.end);

  return (
    <div
      ref={containerRef}
      style={{
        height: 'calc(100vh - 300px)',
        overflow: 'auto',
        borderRadius: '4px',
      }}
      onScroll={handleScroll}
    >
      {/* 总高度占位符 */}
      <div style={{ height: data.length * itemHeight, position: 'relative' }}>
        {/* 可视区域内容 */}
        <div
          style={{
            position: 'absolute',
            top: visibleRange.start * itemHeight,
            left: 0,
            right: 0,
          }}
        >
          {visibleData.map((item, index) => renderItem(item, visibleRange.start + index))}
        </div>
      </div>
    </div>
  );
};

const Sider = forwardRef<{ getDistributionWithSearchBar: () => void }, IProps>(
  (
    props: {
      modules: any;
      onChangeColumns: any;
      onSearch: any;
      searchParams: any;
      setWhereSqlsFromSider: any;
      onActiveColumnsChange: any;
      onSelectedModuleChange: any;
      moduleQueryConfig: any;
      onCommonColumnsChange: any;
    },
    ref: any,
  ) => {
    const {
      modules,
      onChangeColumns,
      onSearch,
      searchParams,
      setWhereSqlsFromSider,
      onActiveColumnsChange,
      onSelectedModuleChange,
      moduleQueryConfig,
      onCommonColumnsChange,
    } = props;
    const [columns, setColumns] = useState<ILogColumnsResponse[]>([]); // 日志表字段
    const [selectedModule, setSelectedModule] = useState<string>(''); // 已选模块
    const [distributions, setDistributions] = useState<Record<string, IFieldDistributions>>({}); // 字段值分布列表
    const [activeColumns, setActiveColumns] = useState<string[]>([]); // 激活的字段
    const [searchText, setSearchText] = useState<string>(''); // 字段搜索文本
    const [favoriteModule, setFavoriteModule] = useState<string>(''); // 收藏的模块
    const [_searchParams, _setSearchParams] = useState<ILogSearchParams>(searchParams); // 临时查询参数，供searchBar查询调用
    const abortRef = useRef<AbortController | null>(null);

    // 获取日志字段
    const getColumns = useRequest(api.fetchColumns, {
      manual: true,
      onSuccess: (res) => {
        // 确保时间字段默认被选中（使用moduleQueryConfig中的timeField）
        const timeField = moduleQueryConfig?.timeField || 'log_time'; // 如果没有配置则回退到log_time
        const processedColumns = res?.map((column) => {
          if (column.columnName === timeField) {
            return { ...column, selected: true, _createTime: new Date().getTime() };
          }
          return column;
        });

        setColumns(processedColumns);
        const _commonColumns = processedColumns
          .map((item: any) => item.columnName)
          ?.filter((item: any) => !item.includes('.'));

        // 通知父组件commonColumns变化
        if (onCommonColumnsChange) {
          onCommonColumnsChange(_commonColumns || []);
        }

        // 初始加载时也通知父组件列变化
        if (onChangeColumns) {
          onChangeColumns(processedColumns);
        }
      },
      onError: () => {
        setColumns([]);
        if (onCommonColumnsChange) {
          onCommonColumnsChange([]);
        }
      },
    });

    // 获取指定字段的TOP5分布数据
    const queryDistribution = useRequest(
      async (params: ILogSearchParams & { signal?: AbortSignal }) => {
        const requestParams: any = {
          ...params,
        };
        delete requestParams?.datasourceId;
        // 传 signal 给 api
        return api.fetchDistributions(requestParams, { signal: params.signal });
      },
      {
        manual: true,
        onSuccess: (res) => {
          const { fieldDistributions = [], sampleSize } = res;
          const target: any = {};
          fieldDistributions.forEach((item) => {
            const { fieldName } = item;
            target[fieldName] = { ...item, sampleSize };
          });
          setDistributions(target);
        },
        onError: () => {
          setDistributions({});
        },
      },
    );

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
        setHasCalledGetColumns(''); // 重置调用标识
        if (onSelectedModuleChange) {
          onSelectedModuleChange('');
        }
        return;
      }
      const datasourceId = modules.find((item: { value: string }) => item.value === value)?.datasourceId;
      // 解析value：datasourceId-module
      setSelectedModule(value);
      // 重置调用标识，允许新模块重新调用getColumns
      setHasCalledGetColumns('');
      // 通知父组件模块变化，让父组件统一处理搜索参数更新
      if (onSelectedModuleChange) {
        onSelectedModuleChange(value, Number(datasourceId));
      }
      // 移除直接调用onSearch，让父组件统一控制
      // onSearch({
      //   ...searchParams,
      //   datasourceId: Number(datasourceId),
      //   module: value,
      //   offset: 0,
      // });
    };

    // 当 modules 加载完成后，自动选择第一个数据源和第一个模块
    useEffect(() => {
      if (modules?.length > 0 && selectedModule?.length === 0) {
        const favorite = getFavoriteModule();
        let targetModule = null;

        // 优先选择收藏的模块
        if (favorite) {
          targetModule = modules.find((item: { module: string }) => item.module === favorite);
        }

        // 如果没有收藏或收藏的模块不存在，选择第一个
        if (!targetModule) {
          targetModule = modules[0];
        }

        if (targetModule) {
          const datasourceId = String(targetModule.datasourceId);
          const module = String(targetModule.module);
          setSelectedModule(module);
          // 重置调用标识，允许初始化时调用getColumns
          setHasCalledGetColumns('');
          // 通知父组件模块变化
          if (onSelectedModuleChange) {
            onSelectedModuleChange(module, targetModule.datasourceId);
          }
          // 注意：这里不立即调用getColumns，而是等待moduleQueryConfig加载完成
          // getColumns.run({ datasourceId: Number(datasourceId), module });
        }
      }
    }, [modules, favoriteModule]);

    // 添加状态来跟踪是否已经调用过getColumns，避免重复调用
    const [hasCalledGetColumns, setHasCalledGetColumns] = useState<string>('');
    const modulesRef = useRef<IStatus[]>([]);

    // 更新modules的引用
    useEffect(() => {
      modulesRef.current = modules;
    }, [modules]);

    // 当moduleQueryConfig和selectedModule都准备好时，调用getColumns
    useEffect(() => {
      if (selectedModule && moduleQueryConfig !== undefined && modulesRef.current.length > 0) {
        const targetModule = modulesRef.current.find((item: { value: any }) => item.value === selectedModule);
        if (targetModule) {
          const datasourceId = targetModule.datasourceId;
          // 生成唯一标识，避免重复调用
          const callKey = `${selectedModule}_${datasourceId}_${moduleQueryConfig?.timeField || 'default'}`;

          // 只有当标识发生变化时才调用getColumns
          if (hasCalledGetColumns !== callKey) {
            setHasCalledGetColumns(callKey);
            // datasourceId: Number(datasourceId),
            getColumns.run({ module: selectedModule });
          }
        }
      }
    }, [selectedModule, moduleQueryConfig, hasCalledGetColumns]);

    // 切换字段选中状态
    const toggleColumn = (data: ILogColumnsResponse) => {
      const index = columns.findIndex(
        (item: { columnName: string | undefined }) => item.columnName === data.columnName,
      );
      // 如果是时间字段，不允许取消选择（使用moduleQueryConfig中的timeField）
      const timeField = moduleQueryConfig?.timeField || 'log_time';
      if (columns[index].columnName === timeField || columns[index].isFixed) {
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
        .filter((item: { selected: any }) => item.selected)
        .map((item: { columnName: any }) => item.columnName)
        .filter(Boolean) as string[];

      // 更新本地搜索参数
      const _savedSearchParams = localStorage.getItem('searchBarParams');
      if (_savedSearchParams) {
        const savedSearchParams = JSON.parse(_savedSearchParams);
        localStorage.setItem(
          'searchBarParams',
          JSON.stringify({
            ...savedSearchParams,
            fields: newActiveColumns,
          }),
        );
      }

      // 通知父组件激活字段变化;
      if (onActiveColumnsChange) {
        onActiveColumnsChange(newActiveColumns);
      }
      // }

      // 排序
      const sortedColumns = columns.sort(
        (a: { _createTime: any }, b: { _createTime: any }) => (a._createTime || 0) - (b._createTime || 0),
      );
      const updatedColumns = [...sortedColumns];
      setColumns(updatedColumns);

      // 通知父组件列变化
      if (onChangeColumns) {
        onChangeColumns(updatedColumns);
      }
    };

    // 获取字段值分布
    const getDistributionWithSearchBar = () => {
      // 有字段的时候调用
      if (_searchParams?.fields?.length) {
        if (abortRef.current) abortRef.current.abort(); // 取消上一次
        abortRef.current = new AbortController();
        queryDistribution.run({ ..._searchParams, signal: abortRef.current.signal });
      }
    };

    // 获取字段值分布
    const getDistribution = (columnName: string, newActiveColumns: string[], sql: string) => {
      // 折叠的时候重新赋值
      if (!newActiveColumns.includes(columnName)) {
        _setSearchParams({
          ..._searchParams,
          fields: newActiveColumns,
        });
        return;
      }
      const params: ILogSearchParams = {
        ...searchParams,
        fields: newActiveColumns,
        offset: 0,
      };
      if (sql) {
        params.whereSqls = [...(searchParams?.whereSqls || []), sql];
      }
      _setSearchParams(params);

      if (abortRef.current) abortRef.current.abort(); // 取消上一次
      abortRef.current = new AbortController();
      queryDistribution.run({ ...params, signal: abortRef.current.signal });
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
      const availableColumns = columns?.filter((item: { selected: any }) => !item.selected);
      if (!searchText.trim()) {
        return availableColumns;
        // return availableColumns?.sort((a: { columnName: string }, b: { columnName: any }) => {
        //   if (!a.columnName) return -1;
        //   if (!b.columnName) return 1;
        //   return a.columnName.localeCompare(b.columnName);
        // });
      }
      return availableColumns?.filter((item: { columnName: string }) =>
        item.columnName?.toLowerCase().includes(searchText.toLowerCase()),
      );
      // ?.sort((a: { columnName: string }, b: { columnName: any }) => {
      //   if (!a.columnName) return -1;
      //   if (!b.columnName) return 1;
      //   return a.columnName.localeCompare(b.columnName);
      // });
    }, [columns, searchText]);

    // 虚拟列表的渲染函数
    const renderVirtualItem = useCallback(
      (item: ILogColumnsResponse, index: number) => {
        return (
          <FieldListItem
            key={item.columnName}
            isSelected={false}
            column={item}
            columnIndex={index}
            fieldData={fieldListProps}
            moduleQueryConfig={moduleQueryConfig}
          />
        );
      },
      [fieldListProps, moduleQueryConfig],
    );

    useImperativeHandle(ref, () => ({
      getDistributionWithSearchBar,
    }));

    // 组件卸载时取消正在进行的请求
    useEffect(() => {
      return () => {
        if (abortRef.current) {
          abortRef.current.abort();
        }
      };
    }, []);

    // 计算虚拟滚动容器高度
    const virtualContainerHeight = useMemo(() => {
      return 700; // 固定高度700px
    }, []);

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
          optionLabelProp="title"
          options={modules?.map(
            (item: {
              label:
                | string
                | number
                | bigint
                | boolean
                | ReactElement<unknown, string | JSXElementConstructor<any>>
                | Iterable<ReactNode>
                | ReactPortal
                | Promise<
                    | string
                    | number
                    | bigint
                    | boolean
                    | ReactPortal
                    | ReactElement<unknown, string | JSXElementConstructor<any>>
                    | Iterable<ReactNode>
                    | null
                    | undefined
                  >
                | null
                | undefined;
              module: string;
            }) => ({
              ...item,
              title: item.label,
              label: (
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span
                    style={{
                      display: 'inline-block',
                      maxWidth: '120px',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                    }}
                  >
                    {item.label}
                  </span>
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
            }),
          )}
        />

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
                ?.filter((item: { selected: any }) => item.selected)
                ?.sort(
                  (a: { _createTime: any }, b: { _createTime: any }) => (a._createTime || 0) - (b._createTime || 0),
                )
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
                  style={{ width: '100%', marginBottom: 8 }}
                  value={searchText}
                  onChange={(e: { target: { value: any } }) => setSearchText(e.target.value)}
                />,
                <div key="virtual-list-container" style={{ marginTop: '8px' }}>
                  {filteredAvailableColumns && filteredAvailableColumns.length > 0 ? (
                    <VirtualFieldList
                      data={filteredAvailableColumns}
                      itemHeight={35}
                      containerHeight={virtualContainerHeight}
                      renderItem={renderVirtualItem}
                    />
                  ) : (
                    <div
                      style={{
                        textAlign: 'center',
                        color: '#999',
                        padding: '20px',
                        border: '1px solid #f0f0f0',
                        borderRadius: '4px',
                      }}
                    >
                      {searchText ? '未找到匹配的字段' : '暂无可用字段'}
                    </div>
                  )}
                </div>,
              ],
            },
          ]}
        />
      </div>
    );
  },
);

export default Sider;
