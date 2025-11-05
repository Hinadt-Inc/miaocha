import { useState, useRef, useEffect, useCallback } from 'react';
import { useRequest } from 'ahooks';
import * as api from '@/api/logs';
import {
  getFavoriteModule,
  toggleFavoriteModule,
  removeLocalActiveColumns,
  clearSearchConditionsKeepFields,
  generateQueryConditionsKey,
  cleanInvalidFieldsFromStorage,
} from '../utils';

/**
 * 收藏模块管理Hook
 */
export const useFavoriteModule = () => {
  const [favoriteModule, setFavoriteModule] = useState<string>('');

  useEffect(() => {
    const favorite = getFavoriteModule();
    setFavoriteModule(favorite);
  }, []);

  const toggleFavorite = useCallback(
    (module: string) => {
      const newFavorite = toggleFavoriteModule(module, favoriteModule);
      setFavoriteModule(newFavorite);
    },
    [favoriteModule],
  );

  return {
    favoriteModule,
    toggleFavorite,
  };
};

/**
 * 列数据管理Hook
 */
export const useColumns = (
  moduleQueryConfig: any,
  onChangeColumns?: (params: ILogColumnsResponse[]) => void,
  onCommonColumnsChange?: (commonColumns: string[]) => void,
  onColumnsLoaded?: (loaded: boolean) => void,
  externalActiveColumns?: string[], // 新增：外部传入的活跃字段
) => {
  const [columns, setColumns] = useState<ILogColumnsResponse[]>([]);
  // 添加一个ref来记录上一次处理的外部activeColumns
  const lastExternalActiveColumnsRef = useRef<string>('');

  const getColumns = useRequest(api.fetchColumns, {
    manual: true,
    onSuccess: (res) => {
      if (!res || res.length === 0) {
        console.warn('未获取到任何字段数据');
        setColumns([]);
        return;
      }

      // 动态确定时间字段的逻辑
      const determineTimeField = (availableColumns: ILogColumnsResponse[]): string => {
        const availableFieldNames = availableColumns.map((col) => col.columnName).filter(Boolean) as string[];
        // console.log('可用字段列表:', availableFieldNames);

        // 优先使用配置的时间字段
        if (moduleQueryConfig?.timeField) {
          // console.log('检查配置的时间字段:', moduleQueryConfig.timeField);
          if (availableFieldNames.includes(moduleQueryConfig.timeField)) {
            // console.log('✅ 使用配置的时间字段:', moduleQueryConfig.timeField);
            return moduleQueryConfig.timeField;
          } else {
            // console.warn(`❌ 配置的时间字段 "${moduleQueryConfig.timeField}" 在可用字段中不存在`);
          }
        }

        // 如果配置的时间字段不存在，按优先级查找常见时间字段
        const commonTimeFields = ['logs_timestamp', 'log_time', 'timestamp', 'time', '@timestamp'];
        for (const timeField of commonTimeFields) {
          if (availableFieldNames.includes(timeField)) {
            console.log('🔍 找到常见时间字段:', timeField);
            return timeField;
          }
        }

        // 如果都没找到，尝试查找包含time关键字的字段
        const timeRelatedField = availableFieldNames.find(
          (field) => field?.toLowerCase().includes('time') || field?.toLowerCase().includes('timestamp'),
        );
        if (timeRelatedField) {
          console.log('🔍 找到时间相关字段:', timeRelatedField);
          return timeRelatedField;
        }

        // 最后兜底：返回第一个字段
        if (availableFieldNames.length > 0 && availableFieldNames[0]) {
          return availableFieldNames[0];
        }

        return '';
      };

      // 确定实际使用的时间字段
      const actualTimeField = determineTimeField(res);

      const processedColumns = res?.map((column) => {
        // 确定时间字段应该始终被选中
        const isTimeField = column.columnName === actualTimeField;

        // 如果有外部传入的activeColumns，优先使用它来决定字段的选中状态
        if (externalActiveColumns && externalActiveColumns.length > 0) {
          const isInActiveColumns = externalActiveColumns.includes(column.columnName || '');
          // 时间字段或在activeColumns中的字段都应该被选中
          const shouldBeSelected = isTimeField || isInActiveColumns;

          console.log('字段选中判断:', column.columnName, {
            isTimeField,
            isInActiveColumns,
            shouldBeSelected,
          });

          if (shouldBeSelected) {
            // console.log('✅ 设置字段为选中:', column.columnName, isTimeField ? '(时间字段)' : '(外部字段)');
            return { ...column, selected: true, _createTime: new Date().getTime() };
          }
          return { ...column, selected: false };
        }

        // 如果没有外部activeColumns，则使用默认逻辑（只选中时间字段）
        if (isTimeField) {
          // console.log('设置时间字段为选中:', column.columnName);
          return { ...column, selected: true, _createTime: new Date().getTime() };
        }
        return column;
      });

      setColumns(processedColumns);

      const commonColumns = processedColumns
        .map((item: any) => item.columnName)
        ?.filter((item: any) => !item.includes('.'));

      // 清理本地存储中的无效字段
      const allFieldNames = (processedColumns?.map((col) => col.columnName).filter(Boolean) as string[]) || [];
      cleanInvalidFieldsFromStorage(allFieldNames);

      // 通知父组件commonColumns变化
      if (onCommonColumnsChange) {
        onCommonColumnsChange(commonColumns || []);
      }

      // 初始加载时也通知父组件列变化
      if (onChangeColumns) {
        // const selectedColumns = processedColumns.filter((col) => col.selected);
        // console.log(
        //   'useColumns onSuccess - 选中的字段:',
        //   selectedColumns.map((col) => col.columnName),
        // );
        // console.log(
        //   'useColumns onSuccess - 即将调用onChangeColumns，传递的processedColumns:',
        //   processedColumns.map((col) => ({
        //     name: col.columnName,
        //     selected: col.selected,
        //   })),
        // );
        onChangeColumns(processedColumns);
      }

      // 通知父组件columns已加载完成
      if (onColumnsLoaded) {
        onColumnsLoaded(true);
      }
    },
    onError: () => {
      setColumns([]);
      if (onCommonColumnsChange) {
        onCommonColumnsChange([]);
      }
    },
  });

  const toggleColumn = useCallback(
    (data: ILogColumnsResponse) => {
      const index = columns.findIndex((item: ILogColumnsResponse) => item.columnName === data.columnName);

      // 动态确定时间字段
      const determineCurrentTimeField = (): string => {
        const availableFieldNames = columns.map((col) => col.columnName).filter(Boolean) as string[];

        // 优先使用配置的时间字段
        if (moduleQueryConfig?.timeField && availableFieldNames.includes(moduleQueryConfig.timeField)) {
          return moduleQueryConfig.timeField;
        }

        // 查找当前选中的时间相关字段
        const selectedTimeField = columns.find(
          (col) =>
            col.selected &&
            col.columnName &&
            (col.columnName.toLowerCase().includes('time') ||
              col.columnName.toLowerCase().includes('timestamp') ||
              col.columnName === 'logs_timestamp' ||
              col.columnName === 'log_time'),
        );

        if (selectedTimeField?.columnName) {
          return selectedTimeField.columnName;
        }

        // 兜底逻辑
        const commonTimeFields = ['logs_timestamp', 'log_time', 'timestamp', 'time', '@timestamp'];
        for (const timeField of commonTimeFields) {
          if (availableFieldNames.includes(timeField)) {
            return timeField;
          }
        }

        return '';
      };

      const currentTimeField = determineCurrentTimeField();

      // 如果是时间字段或固定字段，不允许取消选择
      if (columns[index].columnName === currentTimeField || columns[index].isFixed) {
        console.log('时间字段或固定字段不允许取消选择:', columns[index].columnName);
        return;
      }

      // 切换选中状态
      if (!columns[index].selected) {
        columns[index].selected = true;
        // 为新添加的字段设置 _createTime，确保按添加顺序排列
        columns[index]._createTime = new Date().getTime();
      } else {
        columns[index].selected = false;
        // 保持 _createTime，以便字段重新添加时能回到正确位置
      }

      // 计算新的激活字段列表
      const newActiveColumns = columns
        .filter((item: ILogColumnsResponse) => item.selected)
        .map((item: ILogColumnsResponse) => item.columnName)
        .filter(Boolean) as string[];

      // 更新本地搜索参数
      clearSearchConditionsKeepFields(newActiveColumns);

      // 排序：log_time 始终第一位，其他字段按添加顺序
      const sortedColumns = [...columns].sort((a: ILogColumnsResponse, b: ILogColumnsResponse) => {
        const nameA = a.columnName || '';
        const nameB = b.columnName || '';

        // log_time 始终排在第一位
        if (nameA === 'log_time') return -1;
        if (nameB === 'log_time') return 1;

        // 其他字段按照 _createTime 排序（添加顺序）
        return (a._createTime || 0) - (b._createTime || 0);
      });
      const updatedColumns = sortedColumns;
      setColumns(updatedColumns);

      // 通知父组件列变化
      if (onChangeColumns) {
        onChangeColumns(updatedColumns);
      }

      return newActiveColumns;
    },
    [columns, moduleQueryConfig, onChangeColumns],
  );

  // 监听外部activeColumns变化，同步更新columns的selected状态
  useEffect(() => {
    if (externalActiveColumns && columns.length > 0) {
      // 使用字符串比较来检查是否发生变化，避免引用比较问题
      const currentExternalColumnsStr = JSON.stringify(
        externalActiveColumns.slice().sort((a, b) => a.localeCompare(b)),
      );

      if (lastExternalActiveColumnsRef.current !== currentExternalColumnsStr) {
        // console.log('useColumns: 监听到外部activeColumns变化:', externalActiveColumns);
        lastExternalActiveColumnsRef.current = currentExternalColumnsStr;

        // 动态确定时间字段
        const determineTimeField = (): string => {
          const availableFieldNames = columns.map((col) => col.columnName).filter(Boolean) as string[];

          // 优先使用配置的时间字段
          if (moduleQueryConfig?.timeField && availableFieldNames.includes(moduleQueryConfig.timeField)) {
            return moduleQueryConfig.timeField;
          }

          // 查找常见时间字段
          const commonTimeFields = ['logs_timestamp', 'log_time', 'timestamp', 'time', '@timestamp'];
          for (const timeField of commonTimeFields) {
            if (availableFieldNames.includes(timeField)) {
              return timeField;
            }
          }

          return '';
        };

        const timeField = determineTimeField();

        // 检查是否需要更新
        const currentActiveColumns = columns
          .filter((col) => col.selected)
          .map((col) => col.columnName)
          .filter(Boolean) as string[];

        // 比较当前选中字段和外部传入的字段，如果不同则需要更新
        const needsUpdate =
          currentActiveColumns.length !== externalActiveColumns.length ||
          !currentActiveColumns.every((col) => externalActiveColumns.includes(col)) ||
          !externalActiveColumns.every((col) => currentActiveColumns.includes(col));

        if (needsUpdate) {
          // console.log('useColumns: 需要同步字段选中状态');
          // console.log('useColumns: 当前选中字段:', currentActiveColumns);
          // console.log('useColumns: 外部字段:', externalActiveColumns);

          // 更新columns的selected状态
          const updatedColumns = columns.map((col) => {
            const shouldBeSelected =
              col.columnName === timeField || // 时间字段应该始终选中
              (col.columnName && externalActiveColumns.includes(col.columnName));

            return {
              ...col,
              selected: !!shouldBeSelected, // 确保是boolean类型
              _createTime: shouldBeSelected && !col._createTime ? new Date().getTime() : col._createTime,
            } as ILogColumnsResponse;
          });

          // console.log(
          //   'useColumns: 更新后的字段选中状态:',
          //   updatedColumns.filter((col) => col.selected).map((col) => col.columnName),
          // );

          setColumns(updatedColumns);

          // 通知父组件columns变化
          if (onChangeColumns) {
            onChangeColumns(updatedColumns);
          }
        }
      }
    }
  }, [externalActiveColumns, columns.length]);

  return {
    columns,
    setColumns,
    getColumns,
    toggleColumn,
  };
};

/**
 * 模块选择管理Hook
 */
export const useModuleSelection = (
  modules: IStatus[],
  externalSelectedModule?: string,
  onSelectedModuleChange?: (selectedModule: string, datasourceId?: number) => void,
) => {
  const [selectedModule, setSelectedModule] = useState<string>('');
  const lastModuleRef = useRef<string>('');

  // 同步外部传入的selectedModule
  useEffect(() => {
    if (externalSelectedModule && externalSelectedModule !== selectedModule) {
      setSelectedModule(externalSelectedModule);
      lastModuleRef.current = externalSelectedModule;
    }
  }, [externalSelectedModule, selectedModule]);

  const changeModules = useCallback(
    (value: string) => {
      removeLocalActiveColumns();
      clearSearchConditionsKeepFields([]);

      if (!value) {
        setSelectedModule('');
        if (onSelectedModuleChange) {
          onSelectedModuleChange('');
        }
        return;
      }

      const datasourceId = modules.find((item: IStatus) => item.value === value)?.datasourceId;
      setSelectedModule(value);
      lastModuleRef.current = value;

      if (onSelectedModuleChange) {
        onSelectedModuleChange(value, Number(datasourceId));
      }
      const savedSearchParams = localStorage.getItem('searchBarParams');
      if (savedSearchParams) {
        try {
          const params = JSON.parse(savedSearchParams);
          const updatedParams = {
            ...params,
            module: value,
          };
          localStorage.setItem('searchBarParams', JSON.stringify(updatedParams));
        } catch (error) {
          console.error('更新localStorage中的searchBarParams失败:', error);
        }
      }
    },
    [modules, onSelectedModuleChange],
  );

  return {
    selectedModule,
    setSelectedModule,
    lastModuleRef,
    changeModules,
  };
};

/**
 * 字段分布数据管理Hook
 */
export const useDistributions = (searchParams: ILogSearchParams, availableColumns: ILogColumnsResponse[] = []) => {
  const [distributions, setDistributions] = useState<Record<string, IFieldDistributions>>({});
  const [distributionLoading, setDistributionLoading] = useState<Record<string, boolean>>({});
  const abortRef = useRef<AbortController | null>(null);
  const lastQueryConditionsRef = useRef<string | Record<string, string>>('');

  // 验证字段是否存在于可用字段列表中
  const validateFields = useCallback(
    (fields: string[]): string[] => {
      if (!availableColumns || availableColumns.length === 0) {
        return fields;
      }
      const availableFieldNames = availableColumns.map((col) => col.columnName);
      const validFields = fields.filter((field) => availableFieldNames.includes(field));

      // 如果有无效字段，输出警告信息
      const invalidFields = fields.filter((field) => !availableFieldNames.includes(field));
      if (invalidFields.length > 0) {
        console.warn('检测到无效字段，已自动过滤:', invalidFields);
      }

      return validFields;
    },
    [availableColumns],
  );

  const queryDistribution = useRequest(
    async (params: ILogSearchParams & { signal?: AbortSignal }) => {
      const localActiveColumns = JSON.parse(localStorage.getItem('activeColumns') || '[]');
      if (localActiveColumns.length > 0) {
        // 验证字段有效性
        const validFields = validateFields(localActiveColumns);
        if (validFields.length === 0) {
          throw new Error('没有有效的字段可供查询');
        }
        params.fields = validFields;
      }
      const requestParams: any = { ...params };
      delete requestParams?.datasourceId;

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

        // 清除loading状态
        const fields = Object.keys(target);
        const newLoadingState: Record<string, boolean> = {};
        fields.forEach((field: string) => {
          newLoadingState[field] = false;
        });
        setDistributionLoading((prev) => ({ ...prev, ...newLoadingState }));
      },
      onError: (error) => {
        // 只有在不是主动取消的情况下才处理错误
        if (error?.name !== 'AbortError') {
          setDistributions({});
          setDistributionLoading({});
        }
      },
    },
  );

  // 刷新字段分布
  const refreshFieldDistributions = useCallback(() => {
    const _searchParams = JSON.parse(localStorage.getItem('searchBarParams') || '{}');
    const fields = _searchParams?.fields;

    // 确保模块已选择，优先从localStorage获取，其次从searchParams获取
    const currentModule = _searchParams?.module || searchParams.module;
    if (!currentModule) {
      console.warn('模块未选择，跳过字段分布查询');
      return;
    }

    if (fields?.length) {
      // 验证字段有效性
      const validFields = validateFields(fields);
      if (validFields.length === 0) {
        console.warn('没有有效的字段可供查询分布数据');
        return;
      }

      const currentConditions = generateQueryConditionsKey({
        whereSqls: searchParams.whereSqls || [],
        keywords: searchParams.keywords || [],
        startTime: searchParams.startTime,
        endTime: searchParams.endTime,
        fields: validFields || [],
      });

      if (currentConditions === lastQueryConditionsRef.current) {
        return;
      }

      lastQueryConditionsRef.current = currentConditions;

      // 设置loading状态（只对有效字段）
      const newLoadingState: Record<string, boolean> = {};
      validFields.forEach((field: string) => {
        newLoadingState[field] = true;
      });
      setDistributionLoading((prev) => ({ ...prev, ...newLoadingState }));

      if (abortRef.current) abortRef.current.abort();
      abortRef.current = new AbortController();

      // 使用localStorage中的参数或当前searchParams，确保模块信息正确
      const effectiveParams = {
        ...searchParams,
        ..._searchParams, // localStorage的参数优先级更高
        fields: validFields,
        module: currentModule, // 确保模块信息正确
      };
      queryDistribution.run({ ...effectiveParams, signal: abortRef.current.signal });
    }
  }, [searchParams, queryDistribution, validateFields]);

  const getDistribution = useCallback(
    (columnName: string, newActiveColumns: string[], sql: string) => {
      if (!newActiveColumns.includes(columnName)) {
        setDistributionLoading((prev) => {
          const newState = { ...prev };
          delete newState[columnName];
          return newState;
        });
        return;
      }

      // 确保模块已选择，优先从localStorage获取，其次从searchParams获取
      const _searchParams = JSON.parse(localStorage.getItem('searchBarParams') || '{}');
      const currentModule = _searchParams?.module || searchParams.module;
      if (!currentModule) {
        console.warn('模块未选择，跳过字段分布查询');
        setDistributionLoading((prev) => ({ ...prev, [columnName]: false }));
        return;
      }

      // 验证字段有效性
      const validFields = validateFields(newActiveColumns);
      if (validFields.length === 0) {
        console.warn('没有有效的字段可供查询分布数据');
        setDistributionLoading((prev) => ({ ...prev, [columnName]: false }));
        return;
      }

      setDistributionLoading((prev) => ({ ...prev, [columnName]: true }));

      // 使用localStorage中的参数或当前searchParams，确保模块信息正确
      const effectiveParams: ILogSearchParams = {
        ...searchParams,
        ..._searchParams, // localStorage的参数优先级更高
        fields: validFields,
        module: currentModule, // 确保模块信息正确
        offset: 0,
      };

      if (sql) {
        effectiveParams.whereSqls = [...(searchParams?.whereSqls || []), sql];
      }

      const currentConditions = generateQueryConditionsKey({
        currentModule,
        columnName: columnName,
        whereSqls: effectiveParams.whereSqls || [],
        keywords: searchParams.keywords || [],
        startTime: searchParams.startTime,
        endTime: searchParams.endTime,
        fields: validFields || [],
      });

      const fieldQueryKey = `${columnName}_query`;
      const lastFieldQuery = (lastQueryConditionsRef.current as any)?.[fieldQueryKey];
      // 恢复重复请求检查，现在localStorage同步更新后应该能正确工作
      if (lastFieldQuery === currentConditions) {
        setDistributionLoading((prev) => ({ ...prev, [columnName]: false }));
        return;
      }

      if (typeof lastQueryConditionsRef.current !== 'object' || lastQueryConditionsRef.current === null) {
        lastQueryConditionsRef.current = {};
      }
      (lastQueryConditionsRef.current as any)[fieldQueryKey] = currentConditions;

      if (abortRef.current) abortRef.current.abort();
      abortRef.current = new AbortController();

      setTimeout(() => {
        if (abortRef.current && !abortRef.current.signal.aborted) {
          queryDistribution.run({ ...effectiveParams, signal: abortRef.current.signal });
        }
      }, 300);
    },
    [searchParams, queryDistribution, validateFields],
  );

  // 组件卸载时取消请求
  useEffect(() => {
    return () => {
      if (abortRef.current) {
        abortRef.current.abort();
      }
    };
  }, []);

  return {
    distributions,
    distributionLoading,
    refreshFieldDistributions,
    getDistribution,
  };
};
