import React, { useEffect, useRef, useState, useMemo, Fragment, useCallback } from 'react';

import { Table } from 'antd';
import { useSearchParams } from 'react-router-dom';

import * as api from '@/api/logs';
import { highlightText } from '@/utils/highlightText';

import { useHomeContext } from '../context';
import ExpandedRow from '../ExpandedRow/index';
import { useDataInit } from '../hooks/useDataInit';
import { formatTimeString, debounce } from '../utils';

import { ResizableTitle, ColumnHeader } from './components';
import { useScreenWidth } from './hooks';
import {
  extractSqlKeywords,
  formatSearchKeywords,
  processSorterChange,
  getAutoColumnWidth,
  isFieldSortable,
  createColumnSorter,
  createTimeSorter,
} from './utils';
import styles from './VirtualTable.module.less';

/**
 * 虚拟化表格组件
 */
const VirtualTable: React.FC = () => {
  const [urlSearchParams] = useSearchParams();
  const tabId = urlSearchParams.get('tabId');
  const {
    searchParams,
    detailData,
    moduleQueryConfig,
    logTableColumns,
    updateSearchParams,
    setDetailData,
    setLogTableColumns,
    cacheParams,
    updateCacheParams,
  } = useHomeContext();
  const { fetchData } = useDataInit();

  // 自定义hooks
  const screenWidth = useScreenWidth();

  // 混合模式配置: 300条以内不启用虚拟滚动
  const VIRTUAL_THRESHOLD = 10000;
  const MAX_LOAD_COUNT = 10; // 最大加载条数限制
  // 状态管理
  const detailDataLoading = useRef(false); // 防止重复请求
  const containerRef = useRef<HTMLDivElement>(null);
  const tblRef: Parameters<typeof Table>[0]['ref'] = useRef(null);
  const columnsRef = useRef<any[]>([]);
  const prevShouldUseVirtualRef = useRef(false); // 用于追踪虚拟滚动状态变化
  const [containerHeight, setContainerHeight] = useState<number>(0);
  const [headerHeight, setHeaderHeight] = useState<number>(0);
  const [columns, setColumns] = useState<any[]>([]);
  const [scrollX, setScrollX] = useState(1300);
  const [limitNoticeHeight, setLimitNoticeHeight] = useState(0); // 底部提示高度
  const limitNoticeRef = useRef<HTMLDivElement>(null); // 底部提示 ref

  const cacheColumnWidths = useMemo(() => {
    if (tabId) {
      return cacheParams?.[tabId]?.columnWidths || {};
    }
    return {};
  }, [tabId, cacheParams]);
  const shouldUseVirtual = useMemo(() => {
    return (detailData?.rows?.length || 0) > VIRTUAL_THRESHOLD;
  }, [detailData?.rows?.length]);
  // 数据处理
  const sqlFilterValue = useMemo(() => extractSqlKeywords(searchParams.whereSqls || []), [searchParams.whereSqls]);
  const keyWordsFormat = useMemo(() => formatSearchKeywords(searchParams.keywords || []), [searchParams.keywords]);
  const sortFieldsMap = useMemo(() => {
    if (!searchParams.sortFields) return {};
    return searchParams.sortFields?.reduce(
      (acc: Record<string, string>, item: any) => {
        acc[item.fieldName] = item.direction === 'ASC' ? 'ascend' : 'descend';
        return acc;
      },
      {} as Record<string, string>,
    );
  }, [searchParams.sortFields]);
  // 表格列配置Hook（简化版本，仅提取timeField）
  const timeField = useMemo(() => moduleQueryConfig?.timeField || 'log_time', [moduleQueryConfig]);
  const hasMore = useMemo(() => {
    const currentCount = detailData?.rows?.length || 0;
    // 如果已经达到最大加载条数，不再加载
    if (currentCount >= MAX_LOAD_COUNT) {
      return false;
    }
    return detailData?.totalCount ? currentCount < detailData?.totalCount : false;
  }, [searchParams, detailData, moduleQueryConfig, MAX_LOAD_COUNT]);

  const onLoadMore = useCallback(async () => {
    const currentCount = detailData?.rows?.length || 0;
    // 检查是否达到最大加载条数
    if (currentCount >= MAX_LOAD_COUNT) {
      console.log(`已达到最大加载条数 ${MAX_LOAD_COUNT}，停止加载`);
      return;
    }

    const newSearchParams = { ...searchParams, offset: currentCount };
    const res = await api.fetchLogDetails(newSearchParams);
    const { rows } = res;
    const timeField = moduleQueryConfig?.timeField || 'log_time';

    // 为每条记录添加唯一ID并格式化时间字段
    (rows || []).forEach((item, index) => {
      if (item[timeField]) {
        item[timeField] = formatTimeString(item[timeField] as string);
      }
      item._originalSource = { ...item };
      item._key = `${Date.now()}_${index}`;
    });

    // 限制总数不超过 MAX_LOAD_COUNT
    const newRows = [...(detailData?.rows || []), ...rows].slice(0, MAX_LOAD_COUNT);
    const newDetailData = { ...detailData, rows: newRows };
    setDetailData(newDetailData as ILogDetailsResponse);
    detailDataLoading.current = false;
  }, [detailData, moduleQueryConfig, searchParams, setDetailData, MAX_LOAD_COUNT]);

  // 列宽调整处理
  const handleResize = (index: number) => (width: number) => {
    const column = columnsRef.current[index];
    if (!column?.dataIndex || width < 150) return;
    if (tabId) {
      updateCacheParams(tabId, {
        columnWidths: {
          ...cacheColumnWidths,
          [column.dataIndex]: width,
        },
      });
    }
  };

  const handleDeleteColumn = (colIndex: number) => {
    const col = columns[colIndex];
    const newCols = columns.filter((_, idx) => idx !== colIndex);
    setColumns(newCols);

    // 重置左侧选中状态
    const newLogTableColumns = [...logTableColumns];
    const idx = newLogTableColumns.findIndex((c) => c.columnName === col.dataIndex);
    if (idx > -1) {
      newLogTableColumns[idx].selected = false;
    }
    setLogTableColumns(newLogTableColumns);

    let newFields = [...(searchParams.fields || [])];
    if (col.dataIndex.indexOf('.') > -1) {
      newFields = newFields.filter((f) => f !== col.dataIndex);
    }
    const paramsWidthFields = updateSearchParams({
      fields: newFields,
      sortFields: searchParams.sortFields?.filter((e) => e.fieldName !== col.columnName),
    });
    fetchData({
      searchParams: paramsWidthFields,
    });
  };

  const handleMoveLeft = (colIndex: number) => {
    if (colIndex < 2) return;
    const newCols = [...columns];
    [newCols[colIndex - 1], newCols[colIndex]] = [newCols[colIndex], newCols[colIndex - 1]];
    setColumns(newCols);
  };

  const handleMoveRight = (colIndex: number) => {
    if (colIndex >= columns.length - 1) return;
    const newCols = [...columns];
    [newCols[colIndex], newCols[colIndex + 1]] = [newCols[colIndex + 1], newCols[colIndex]];
    setColumns(newCols);
  };

  // 表格变化处理
  const handleTableChange = useCallback(
    (_pagination: any, _filters: any, sorter: any) => {
      const resultSorter = processSorterChange(sorter);
      const newSearchParams = updateSearchParams({
        sortFields: resultSorter,
      });
      fetchData({ searchParams: newSearchParams });
    },
    [updateSearchParams, fetchData],
  );

  // 防抖处理表格变化（300ms）
  const debouncedHandleTableChange = useMemo(() => debounce(handleTableChange, 300), [handleTableChange]);

  useEffect(() => {
    columnsRef.current = columns;
  }, [columns]);

  // 回到顶部功能
  const scrollToTop = useCallback(() => {
    const tableNode = tblRef.current?.nativeElement;
    if (!tableNode) return;

    const scrollElement = shouldUseVirtual
      ? tableNode.querySelector('.ant-table-tbody-virtual-holder')
      : tableNode.querySelector('.ant-table-body');

    if (scrollElement) {
      scrollElement.scrollTo({ top: 0, behavior: 'smooth' });
    }

    // 如果是虚拟滚动，使用 scrollTo 方法
    if (shouldUseVirtual && tblRef.current?.scrollTo) {
      tblRef.current.scrollTo({ index: 0 });
    }
  }, [shouldUseVirtual]);

  // 判断是否达到最大加载条数
  const hasReachedLimit = useMemo(() => {
    return (detailData?.rows?.length || 0) >= MAX_LOAD_COUNT;
  }, [detailData?.rows?.length, MAX_LOAD_COUNT]);

  // 监听底部提示高度变化
  useEffect(() => {
    if (limitNoticeRef.current) {
      const height = limitNoticeRef.current.offsetHeight;
      setLimitNoticeHeight(height);
    } else {
      setLimitNoticeHeight(0);
    }
  }, [hasReachedLimit]);

  // 初始化时重置 loading 标志
  useEffect(() => {
    detailDataLoading.current = false;
  }, [searchParams]);

  // 构建完整的列配置
  useEffect(() => {
    const otherColumns = logTableColumns?.filter((item) => item.selected && item.columnName !== timeField);
    const sourceColumn = [];
    let commonColumns: any = [];
    if (otherColumns.length < 1) {
      sourceColumn.push({
        title: '_source',
        dataIndex: '_source',
        ellipsis: false,
        render: (_: any, record: ILogColumnsResponse) => {
          const sourceData = (record as any)._originalSource || record;
          const entries = Object.entries(sourceData)
            .filter(([key]) => !key.startsWith('_')) // 过滤掉内部字段如 _key, _originalSource
            .map(([key, value]) => {
              let priority = 2;
              const highlightArr: string[] = [...keyWordsFormat, ...sqlFilterValue];
              if (!keyWordsFormat.includes(key)) {
                if (sqlFilterValue.some((kw) => String(value).includes(kw))) {
                  priority = 1;
                }
              }
              return { key, value, priority, highlightArr };
            });
          const sortedEntries = entries.sort((a, b) => a.priority - b.priority);
          return (
            <dl className={styles.source}>
              {sortedEntries.map(({ key, value, highlightArr }) => (
                <Fragment key={key}>
                  <dt>{key}</dt>
                  <dd>{highlightText(String(value), highlightArr)}</dd>
                </Fragment>
              ))}
            </dl>
          );
        },
      });
    } else {
      const isSmallScreen = screenWidth < 1200;
      commonColumns = otherColumns.map((item: any, idx: number) => {
        const isLast = idx === otherColumns.length - 1;
        const { columnName = '' } = item;

        let columnWidth;
        if (cacheColumnWidths?.[columnName]) {
          columnWidth = cacheColumnWidths[columnName];
        } else if (isSmallScreen) {
          const availableWidth = screenWidth - 250;
          const maxWidthPerColumn = Math.floor(availableWidth / Math.max(otherColumns.length, 2));
          columnWidth = Math.min(getAutoColumnWidth(columnName, screenWidth), maxWidthPerColumn);
        } else {
          columnWidth = getAutoColumnWidth(columnName, screenWidth);
        }
        return {
          title: columnName,
          dataIndex: columnName,
          width: isLast ? undefined : columnWidth || 150,
          minWidth: 150,
          onHeaderCell: (item: any) => ({
            width: item.width,
            onResize: handleResize(idx + 1),
          }),
          render: (text: string) => highlightText(text, [...(keyWordsFormat || []), ...(sqlFilterValue || [])]),
          ...(isFieldSortable(item.dataType)
            ? {
                sortOrder: sortFieldsMap[columnName] || null, // 使用受控的 sortOrder
                sorter: {
                  compare: createColumnSorter(columnName, item.dataType),
                  multiple: otherColumns.findIndex((col) => col.columnName === columnName) + 2,
                },
              }
            : {}),
        };
      });
    }

    const result = [
      {
        title: timeField,
        dataIndex: timeField,
        width: 190,
        resizable: false,
        sorter: {
          compare: createTimeSorter(timeField),
          multiple: 1,
        },
        render: (text: any) => {
          if (!text) return '';
          const str = String(text);
          if (str === 'Invalid Date' || str === 'NaN') return '';
          return str.replace('T', ' ');
        },
      },
      ...sourceColumn,
      ...commonColumns,
    ];
    setColumns(result);
  }, [logTableColumns, keyWordsFormat, cacheColumnWidths, sqlFilterValue, screenWidth, timeField, sortFieldsMap]);

  // 容器大小和滚动处理
  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    setContainerHeight(container.clientHeight);

    const resizeObserver = new ResizeObserver((entries) => {
      for (const entry of entries) {
        if (entry.target === container) {
          setContainerHeight(entry.contentRect.height);
        }
      }
    });

    resizeObserver.observe(container);

    const tableNode = tblRef.current?.nativeElement;
    if (tableNode) {
      const header = tableNode.querySelector('.ant-table-thead');
      if (header) {
        setHeaderHeight(header.clientHeight);
      }
    }

    const handleScroll = () => {
      if (!hasMore || detailDataLoading.current) return;

      // 根据是否启用虚拟滚动选择不同的滚动容器
      const scrollElement = shouldUseVirtual
        ? tableNode?.querySelector('.ant-table-tbody-virtual-holder')
        : tableNode?.querySelector('.ant-table-body');

      if (scrollElement) {
        const { scrollHeight, scrollTop, clientHeight } = scrollElement;
        const distanceToBottom = scrollHeight - scrollTop - clientHeight;
        if (distanceToBottom > 0 && distanceToBottom < 600) {
          detailDataLoading.current = true;
          onLoadMore();
        }
      }
    };

    if (tableNode) {
      const scrollElement = shouldUseVirtual
        ? tableNode.querySelector('.ant-table-tbody-virtual-holder')
        : tableNode.querySelector('.ant-table-body');
      if (scrollElement) {
        scrollElement.addEventListener('scroll', handleScroll);
      }
    }

    return () => {
      resizeObserver.disconnect();
      if (tableNode) {
        const scrollElement = shouldUseVirtual
          ? tableNode.querySelector('.ant-table-tbody-virtual-holder')
          : tableNode.querySelector('.ant-table-body');
        if (scrollElement) {
          scrollElement.removeEventListener('scroll', handleScroll);
        }
      }
    };
  }, [containerRef.current, tblRef.current, hasMore, onLoadMore, shouldUseVirtual]);

  // 当切换到虚拟滚动模式时，自动滚动到第501条
  useEffect(() => {
    // 检测是否从非虚拟滚动切换到虚拟滚动
    if (!prevShouldUseVirtualRef.current && shouldUseVirtual) {
      const targetIndex = VIRTUAL_THRESHOLD;

      if (tblRef.current && detailData?.rows?.[targetIndex]) {
        // 使用 setTimeout 确保 DOM 已更新为虚拟滚动模式
        setTimeout(() => {
          try {
            // 使用 scrollTo 方法滚动到指定的索引
            tblRef.current?.scrollTo?.({ index: targetIndex });
            console.log(`已切换到虚拟滚动模式，自动滚动到第${targetIndex + 1}条数据`);
          } catch (error) {
            console.warn('滚动失败:', error);
          }
        }, 100);
      }
    }

    // 更新前一个状态
    prevShouldUseVirtualRef.current = shouldUseVirtual;
  }, [shouldUseVirtual, detailData?.rows, VIRTUAL_THRESHOLD]);

  // 动态计算scroll.x
  useEffect(() => {
    const isSmallScreen = screenWidth < 1200;
    const dynamicCols = columns.filter((col: any) => col.dataIndex !== timeField && col.dataIndex !== '_source');
    const sourceCol = columns.find((col: any) => col.dataIndex === '_source');

    let totalWidth = 190; // 时间字段固定宽度

    dynamicCols.forEach((col: any) => {
      totalWidth += col.width || 150;
    });

    const hasOtherColumns = dynamicCols.length > 0;
    if (!hasOtherColumns && sourceCol && sourceCol.width) {
      totalWidth += sourceCol.width;
    } else if (!hasOtherColumns) {
      totalWidth += isSmallScreen ? Math.min(600, screenWidth - 300) : 400;
    }

    totalWidth += 26; // 展开按钮的宽度

    if (isSmallScreen && totalWidth > screenWidth) {
      setScrollX(screenWidth);
    } else {
      setScrollX(Math.max(totalWidth, 800));
    }
  }, [columns, timeField, screenWidth]);

  const enhancedColumns = useMemo(() => {
    const hasSourceCol = columns.find((col) => col.dataIndex === '_source');
    if (!hasSourceCol) {
      return columns.map((col, idx) => {
        if (col.dataIndex === timeField) {
          return col;
        }
        return {
          ...col,
          title: (
            <ColumnHeader
              canMoveLeft={idx > 1}
              canMoveRight={idx < columns.length - 1}
              colIndex={idx}
              title={col.title}
              onDelete={handleDeleteColumn}
              onMoveLeft={handleMoveLeft}
              onMoveRight={handleMoveRight}
            />
          ),
        };
      });
    }
    return columns;
  }, [columns]);

  return (
    <div ref={containerRef} className={styles.virtualLayout}>
      <div className={styles.tableWrapper}>
        <Table
          ref={tblRef}
          columns={enhancedColumns}
          components={{
            header: {
              cell: ResizableTitle,
            },
          }}
          dataSource={detailData?.rows || []}
          expandable={{
            expandedRowRender: (record) => (
              <ExpandedRow data={record} enhancedColumns={enhancedColumns} keywords={keyWordsFormat || []} />
            ),
          }}
          pagination={false}
          rowKey="_key"
          scroll={{
            x: ((detailData?.rows as any[]) || []).length > 0 ? scrollX : 0,
            y: containerHeight - headerHeight - limitNoticeHeight - 1,
          }}
          showSorterTooltip={{
            title: '点击排序，按住Ctrl+点击可多列排序',
          }}
          size="small"
          sortDirections={['ascend', 'descend']}
          virtual={shouldUseVirtual}
          onChange={debouncedHandleTableChange}
        />
      </div>
      {hasReachedLimit && (
        <div ref={limitNoticeRef} className={styles.limitNotice}>
          <span className={styles.noticeText}>
            这是前 <strong>{MAX_LOAD_COUNT}</strong> 条与您搜索匹配的文档，请细化搜索条件以查看其他结果。
          </span>
          <span className={styles.backToTop} onClick={scrollToTop}>
            回到顶部
          </span>
        </div>
      )}
    </div>
  );
};

export default VirtualTable;
