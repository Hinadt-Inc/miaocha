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
) => {
  const [columns, setColumns] = useState<ILogColumnsResponse[]>([]);

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
        console.log('可用字段列表:', availableFieldNames);

        // 优先使用配置的时间字段
        if (moduleQueryConfig?.timeField) {
          console.log('检查配置的时间字段:', moduleQueryConfig.timeField);
          if (availableFieldNames.includes(moduleQueryConfig.timeField)) {
            console.log('✅ 使用配置的时间字段:', moduleQueryConfig.timeField);
            return moduleQueryConfig.timeField;
          } else {
            console.warn(`❌ 配置的时间字段 "${moduleQueryConfig.timeField}" 在可用字段中不存在`);
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
          console.log('⚠️ 未找到时间字段，使用第一个字段作为默认:', availableFieldNames[0]);
          return availableFieldNames[0];
        }

        return '';
      };

      // 确定实际使用的时间字段
      const actualTimeField = determineTimeField(res);
      console.log('✅ 最终确定的时间字段:', actualTimeField);

      const processedColumns = res?.map((column) => {
        if (column.columnName === actualTimeField) {
          console.log('设置时间字段为选中:', column.columnName);
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
        columns[index]._createTime = new Date().getTime();
      } else {
        columns[index].selected = false;
        delete columns[index]._createTime;
      }

      // 计算新的激活字段列表
      const newActiveColumns = columns
        .filter((item: ILogColumnsResponse) => item.selected)
        .map((item: ILogColumnsResponse) => item.columnName)
        .filter(Boolean) as string[];

      // 更新本地搜索参数
      clearSearchConditionsKeepFields(newActiveColumns);

      // 排序
      const sortedColumns = [...columns].sort(
        (a: ILogColumnsResponse, b: ILogColumnsResponse) => (a._createTime || 0) - (b._createTime || 0),
      );
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

  const getDistributionWithSearchBar = useCallback(() => {
    const _searchParams = JSON.parse(localStorage.getItem('searchBarParams') || '{}');
    const fields = _searchParams?.fields;

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

      const params = { ...searchParams, fields: validFields };
      queryDistribution.run({ ...params, signal: abortRef.current.signal });
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

      // 验证字段有效性
      const validFields = validateFields(newActiveColumns);
      if (validFields.length === 0) {
        console.warn('没有有效的字段可供查询分布数据');
        setDistributionLoading((prev) => ({ ...prev, [columnName]: false }));
        return;
      }

      setDistributionLoading((prev) => ({ ...prev, [columnName]: true }));

      const params: ILogSearchParams = {
        ...searchParams,
        fields: validFields,
        offset: 0,
      };

      if (sql) {
        params.whereSqls = [...(searchParams?.whereSqls || []), sql];
      }

      const currentConditions = generateQueryConditionsKey({
        columnName: columnName,
        whereSqls: params.whereSqls || [],
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
          queryDistribution.run({ ...params, signal: abortRef.current.signal });
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
    getDistributionWithSearchBar,
    getDistribution,
  };
};
