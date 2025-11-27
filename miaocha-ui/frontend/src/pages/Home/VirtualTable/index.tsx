import React, { useEffect, useRef, useState, useMemo, Fragment, useCallback } from 'react';

import { Table } from 'antd';

import * as api from '@/api/logs';
import { highlightText } from '@/utils/highlightText';

import { useHomeContext } from '../context';
import ExpandedRow from '../ExpandedRow/index';
import { useDataInit } from '../hooks/useDataInit';
import { formatTimeString } from '../utils';

import { ResizableTitle, ColumnHeader } from './components';
import { useScreenWidth, useExpandedRows } from './hooks';
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
  const {
    searchParams,
    detailData,
    moduleQueryConfig,
    logTableColumns,
    updateSearchParams,
    setDetailData,
    setLogTableColumns,
  } = useHomeContext();
  const { fetchData } = useDataInit();

  const detailDataLoading = useRef(false); // 防止重复请求

  // 状态管理
  const containerRef = useRef<HTMLDivElement>(null);
  const tblRef: Parameters<typeof Table>[0]['ref'] = useRef(null);
  const [containerHeight, setContainerHeight] = useState<number>(0);
  const [headerHeight, setHeaderHeight] = useState<number>(0);
  const [columns, setColumns] = useState<any[]>([]);
  const [columnWidths, setColumnWidths] = useState<Record<string, number>>({});
  const [scrollX, setScrollX] = useState(1300);

  // 自定义hooks
  const screenWidth = useScreenWidth();
  const { expandedRowKeys, handleExpand } = useExpandedRows(detailData?.rows || [], searchParams, moduleQueryConfig);

  // 数据处理
  const sqlFilterValue = useMemo(() => extractSqlKeywords(searchParams.whereSqls || []), [searchParams.whereSqls]);
  const keyWordsFormat = useMemo(() => formatSearchKeywords(searchParams.keywords || []), [searchParams.keywords]);
  // 表格列配置Hook（简化版本，仅提取timeField）
  const timeField = useMemo(() => moduleQueryConfig?.timeField || 'log_time', [moduleQueryConfig]);
  // 构建完整的列配置
  const getBaseColumns = useMemo(() => {
    const otherColumns = logTableColumns?.filter((item) => item.selected && item.columnName !== timeField);
    const _columns: any[] = [];

    if (otherColumns && otherColumns.length > 0) {
      const isSmallScreen = screenWidth < 1200;

      otherColumns.forEach((item: any) => {
        const { columnName = '' } = item;

        let columnWidth;
        if (columnWidths[columnName]) {
          columnWidth = columnWidths[columnName];
        } else if (isSmallScreen) {
          const availableWidth = screenWidth - 250;
          const maxWidthPerColumn = Math.floor(availableWidth / Math.max(otherColumns.length, 2));
          columnWidth = Math.min(getAutoColumnWidth(columnName, screenWidth), maxWidthPerColumn);
        } else {
          columnWidth = getAutoColumnWidth(columnName, screenWidth);
        }

        _columns.push({
          title: columnName,
          dataIndex: columnName,
          width: columnWidth,
          render: (text: string) => highlightText(text, keyWordsFormat || []),
          ...(isFieldSortable(item.dataType)
            ? {
                sorter: {
                  compare: createColumnSorter(columnName, item.dataType),
                  multiple: otherColumns.findIndex((col) => col.columnName === columnName) + 2,
                },
              }
            : {}),
        });
      });
    }

    return [
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
      ...(_columns.length < 1
        ? [
            {
              title: '_source',
              dataIndex: '_source',
              width: (() => {
                const isSmallScreen = screenWidth < 1200;
                if (isSmallScreen) {
                  const hasOtherColumns = _columns.length > 0;
                  if (!hasOtherColumns) {
                    return Math.min(600, screenWidth - 300);
                  }
                }
                return undefined;
              })(),
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
            },
          ]
        : []),
      ..._columns.map((column, idx) => {
        const isLast = idx === _columns.length - 1;
        let columnWidth;
        if (columnWidths[column.dataIndex]) {
          columnWidth = columnWidths[column.dataIndex];
        } else {
          columnWidth = isLast ? undefined : column.width || 150;
        }

        return {
          ...column,
          width: columnWidth,
          render: (text: string) => {
            return highlightText(text, [...(keyWordsFormat || []), ...(sqlFilterValue || [])]);
          },
        };
      }),
    ];
  }, [logTableColumns, keyWordsFormat, columnWidths, sqlFilterValue, screenWidth, timeField]);

  const hasMore = useMemo(() => {
    return detailData?.totalCount ? (detailData?.rows?.length || 0) < detailData?.totalCount : false;
  }, [searchParams, detailData, moduleQueryConfig]);

  const onLoadMore = useCallback(async () => {
    const newSearchParams = { ...searchParams, offset: detailData?.rows?.length || 0 };
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
    const newDetailData = { ...detailData, rows: [...(detailData?.rows || []), ...rows] };
    setDetailData(newDetailData as ILogDetailsResponse);
    detailDataLoading.current = false;
  }, [detailData, moduleQueryConfig, searchParams, setDetailData]);

  // 列宽调整处理
  const handleResize = (index: number) => (width: number) => {
    const column = columns[index];
    if (!column?.dataIndex) return;

    setColumnWidths((prev) => ({
      ...prev,
      [column.dataIndex]: width,
    }));

    setColumns((prevColumns) => {
      const newColumns = [...prevColumns];
      if (newColumns[index]) {
        newColumns[index] = {
          ...newColumns[index],
          width: width,
        };
      }
      return newColumns;
    });
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
  const handleTableChange = (_pagination: any, _filters: any, sorter: any) => {
    const resultSorter = processSorterChange(sorter);
    const newSearchParams = updateSearchParams({
      sortFields: resultSorter,
    });
    fetchData({ searchParams: newSearchParams });
  };

  // 设置列配置
  useEffect(() => {
    const resizableColumns = getBaseColumns.map((col, index) => {
      return {
        ...col,
        onHeaderCell: (column: any) => ({
          width: column.width,
          onResize: handleResize(index),
        }),
      };
    });
    setColumns(resizableColumns);
  }, [getBaseColumns, timeField]);

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

      const scrollElement = tableNode?.querySelector('.ant-table-tbody-virtual-holder');
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
      const scrollElement = tableNode.querySelector('.ant-table-tbody-virtual-holder');
      if (scrollElement) {
        scrollElement.addEventListener('scroll', handleScroll);
      }
    }

    return () => {
      resizeObserver.disconnect();
      if (tableNode) {
        const scrollElement = tableNode.querySelector('.ant-table-tbody-virtual-holder');
        if (scrollElement) {
          scrollElement.removeEventListener('scroll', handleScroll);
        }
      }
    };
  }, [containerRef.current, tblRef.current, hasMore, onLoadMore]);

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
          columnWidth: 26,
          expandedRowKeys,
          onExpand: handleExpand,
          expandedRowRender: (record) => (
            <ExpandedRow data={record} enhancedColumns={enhancedColumns} keywords={keyWordsFormat || []} />
          ),
        }}
        pagination={false}
        rowKey="_key"
        scroll={{
          x: ((detailData?.rows as any[]) || []).length > 0 ? scrollX : 0,
          y: containerHeight - headerHeight - 1,
        }}
        showSorterTooltip={{
          title: '点击排序，按住Ctrl+点击可多列排序',
        }}
        size="small"
        sortDirections={['ascend', 'descend']}
        virtual
        onChange={handleTableChange}
      />
    </div>
  );
};

export default VirtualTable;
