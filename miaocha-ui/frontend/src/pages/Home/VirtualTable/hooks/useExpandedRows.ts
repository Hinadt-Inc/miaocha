import { useState, useRef, useEffect } from 'react';

import { generateRecordHash } from '../utils/dataUtils';

/**
 * 管理表格展开行状态的Hook
 */
export const useExpandedRows = (data: any[], searchParams: any, moduleQueryConfig: any) => {
  const [expandedRowKeys, setExpandedRowKeys] = useState<React.Key[]>([]);
  const expandedRecordsRef = useRef<Map<React.Key, any>>(new Map());
  const isUserExpandActionRef = useRef(false);

  // 监听数据变化，保持展开行状态
  useEffect(() => {
    // 如果是用户主动操作，跳过这次检查
    if (isUserExpandActionRef.current) {
      isUserExpandActionRef.current = false;
      return;
    }

    // 添加防抖，避免频繁触发
    const timeoutId = setTimeout(() => {
      if (expandedRowKeys.length > 0 && data && data.length > 0) {
        const timeField = moduleQueryConfig?.timeField || 'log_time';

        // 为当前数据中的每条记录生成hash映射
        const dataHashToKey = new Map<string, React.Key>();

        data.forEach((record) => {
          const hash = generateRecordHash(record, timeField);
          dataHashToKey.set(hash, record._key);
        });

        // 检查当前展开的keys是否还在新数据中存在
        const stillValidKeys = expandedRowKeys.filter((key) => {
          const currentRecord = data.find((item) => item._key === key);
          return currentRecord !== undefined;
        });

        // 如果当前展开的keys在新数据中仍然存在，直接保持
        if (stillValidKeys.length === expandedRowKeys.length) {
          return; // 不需要更新
        }

        // 否则，尝试通过内容匹配来恢复展开状态
        const newExpandedKeys: React.Key[] = [];
        const newExpandedRecords = new Map<React.Key, any>();

        expandedRowKeys.forEach((oldKey) => {
          // 首先检查这个key是否还存在
          if (stillValidKeys.includes(oldKey)) {
            newExpandedKeys.push(oldKey);
            const record = data.find((item) => item._key === oldKey);
            if (record) {
              newExpandedRecords.set(oldKey, record);
            }
          } else {
            // key不存在，尝试通过内容匹配
            const expandedRecord = expandedRecordsRef.current.get(oldKey);
            if (expandedRecord) {
              const recordHash = generateRecordHash(expandedRecord, timeField);
              const newKey = dataHashToKey.get(recordHash);
              if (newKey && !newExpandedKeys.includes(newKey)) {
                // 找到匹配的记录，使用新的key
                newExpandedKeys.push(newKey);
                const newRecord = data.find((item) => item._key === newKey);
                if (newRecord) {
                  newExpandedRecords.set(newKey, newRecord);
                }
              }
            }
          }
        });

        // 更新展开状态，但只有在真正发生变化时才更新
        if (
          newExpandedKeys.length !== expandedRowKeys.length ||
          !newExpandedKeys.every((key) => expandedRowKeys.includes(key))
        ) {
          // 清理旧的引用
          expandedRecordsRef.current.clear();
          // 设置新的引用
          newExpandedRecords.forEach((record, key) => {
            expandedRecordsRef.current.set(key, record);
          });

          setExpandedRowKeys(newExpandedKeys);
        }
      }
    }, 100); // 100ms防抖

    return () => clearTimeout(timeoutId);
  }, [data, expandedRowKeys, moduleQueryConfig]);

  // 监听搜索参数变化，在特定情况下清空展开状态
  const prevSearchParamsRef = useRef(searchParams);
  useEffect(() => {
    const prev = prevSearchParamsRef.current;
    const current = searchParams;

    // 如果是重要的搜索条件发生了变化，则清空展开状态
    // 但如果只是字段列表(fields)变化，则保持展开状态
    const importantParamsChanged =
      prev.startTime !== current.startTime ||
      prev.endTime !== current.endTime ||
      prev.module !== current.module ||
      prev.datasourceId !== current.datasourceId ||
      JSON.stringify(prev.whereSqls) !== JSON.stringify(current.whereSqls) ||
      JSON.stringify(prev.keywords) !== JSON.stringify(current.keywords) ||
      prev.timeRange !== current.timeRange;

    // 检查是否是新的搜索请求（offset回到0）但不是因为字段变化导致的
    const isNewSearchNotFieldChange =
      prev.offset !== 0 && current.offset === 0 && JSON.stringify(prev.fields) === JSON.stringify(current.fields);

    if (importantParamsChanged || isNewSearchNotFieldChange) {
      setExpandedRowKeys([]);
      expandedRecordsRef.current.clear(); // 清空展开记录的引用
    }

    prevSearchParamsRef.current = current;
  }, [searchParams]);

  const handleExpand = (expanded: boolean, record: any) => {
    const key = record._key;

    // 立即更新状态，避免延迟
    if (expanded) {
      // 展开行
      const newExpandedKeys = [...expandedRowKeys, key];
      setExpandedRowKeys(newExpandedKeys);
      // 记录展开的记录内容
      expandedRecordsRef.current.set(key, record);
    } else {
      // 收起行
      const newExpandedKeys = expandedRowKeys.filter((k) => k !== key);
      setExpandedRowKeys(newExpandedKeys);
      // 从ref中移除记录
      expandedRecordsRef.current.delete(key);
    }
  };

  return {
    expandedRowKeys,
    handleExpand,
  };
};
