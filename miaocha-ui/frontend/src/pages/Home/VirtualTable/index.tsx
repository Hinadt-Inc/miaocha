import React, { useEffect, useRef, useState, useMemo, Fragment, useCallback } from 'react';

import { useRequest } from 'ahooks';
import { Table } from 'antd';

import * as api from '@/api/logs';
import { highlightText } from '@/utils/highlightText';

import { useHomeContext } from '../context';
import ExpandedRow from '../ExpandedRow/index';
import { useDataInit } from '../hooks/useDataInit';
import { IModuleQueryConfig } from '../types';
import { formatTimeString } from '../utils';

import { ResizableTitle, ColumnHeader } from './components';
import { useScreenWidth, useExpandedRows } from './hooks';
import { VirtualTableProps } from './types';
import {
  extractSqlKeywords,
  formatSearchKeywords,
  processSorterChange,
  getAutoColumnWidth,
  isFieldSortable,
  createColumnSorter,
} from './utils';
import styles from './VirtualTable.module.less';

/**
 * 虚拟化表格组件
 */
const VirtualTable: React.FC<VirtualTableProps> = (props) => {
  const { whereSqlsFromSider = [], sqls } = props;

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

  const hasMore = useMemo(() => {
    return detailData?.totalCount ? (detailData?.rows?.length || 0) < detailData?.totalCount : false;
  }, [searchParams, detailData, moduleQueryConfig]);

  const getDetailDataRequest = useRequest(
    async (params: ILogSearchParams & { signal?: AbortSignal }) => {
      const requestParams: any = { ...params };
      delete requestParams?.datasourceId;
      return api.fetchLogDetails(requestParams, { signal: params.signal });
    },
    {
      manual: true,
      onSuccess: (res) => {
        const { rows } = res;
        const timeField = moduleQueryConfig?.timeField || 'log_time';

        // 为每条记录添加唯一ID并格式化时间字段
        (rows || []).forEach((item, index) => {
          item._key = `${Date.now()}_${index}`;

          if (item[timeField]) {
            item[timeField] = formatTimeString(item[timeField] as string);
          }
          item._originalSource = { ...item };
        });
        const newDetailData = { ...detailData, rows: [...(detailData?.rows || []), ...rows] };
        setDetailData(newDetailData as ILogDetailsResponse);
      },
      onError: (error) => {
        console.error('获取日志详情失败:', error);
      },
    },
  );

  const onLoadMore = useCallback(async () => {
    if (getDetailDataRequest.loading) return;
    const newSearchParams = { ...searchParams, offset: detailData?.rows?.length || 0 };
    getDetailDataRequest.run(newSearchParams);
  }, [detailData, moduleQueryConfig, searchParams, setDetailData]);

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
  const sqlFilterValue = useMemo(() => extractSqlKeywords(sqls || []), [sqls]);
  const keyWordsFormat = useMemo(() => formatSearchKeywords(searchParams.keywords || []), [searchParams.keywords]);

  // 表格列配置Hook（简化版本，仅提取timeField）
  const timeField = moduleQueryConfig?.timeField || 'log_time';

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

    console.log('_columns====', _columns);

    return [
      {
        title: timeField,
        dataIndex: timeField,
        width: 190,
        resizable: false,
        sorter: {
          compare: (a: any, b: any) => {
            const timeA = a[timeField];
            const timeB = b[timeField];

            if (!timeA && !timeB) return 0;
            if (!timeA) return 1;
            if (!timeB) return -1;

            const parseTime = (timeStr: any) => {
              if (!timeStr) return 0;
              const str = String(timeStr);
              if (str.match(/^\d{4}-\d{2}-\d{2}\s\d{2}:\d{2}:\d{2}/)) {
                return str;
              }
              const date = new Date(str);
              if (!isNaN(date.getTime())) {
                return date.getTime();
              }
              return str;
            };

            const parsedA = parseTime(timeA);
            const parsedB = parseTime(timeB);

            if (typeof parsedA === 'number' && typeof parsedB === 'number') {
              return parsedA - parsedB;
            }

            return String(parsedA).localeCompare(String(parsedB));
          },
          multiple: 1,
        },
        render: (text: any) => {
          if (!text) return '';
          const str = String(text);
          if (str === 'Invalid Date' || str === 'NaN') return '';
          return str.replace('T', ' ');
        },
      },
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
        hidden: _columns.length > 0, // 当有其他字段时隐藏_source列
        render: (_: any, record: ILogColumnsResponse) => {
          // console.log('🔍 [_source 列渲染] record:', record);
          // console.log('🔍 [_source 列渲染] _originalSource:', (record as any)._originalSource);

          // const whereValues = whereSqlsFromSider.map((item) => String(item.value)).filter(Boolean);
          const allKeywords = Array.from(new Set([...keyWordsFormat])).filter(Boolean);
          const finalKeywords = Array.from(
            new Set([...(keyWordsFormat?.length ? allKeywords : sqlFilterValue)]),
          ).filter(Boolean);

          // 使用原始完整数据来渲染 _source 列，如果没有则使用当前记录
          const sourceData = (record as any)._originalSource || record;
          // console.log('🔍 [_source 列渲染] sourceData:', sourceData);

          const entries = Object.entries(sourceData)
            .filter(([key]) => !key.startsWith('_')) // 过滤掉内部字段如 _key, _originalSource
            .map(([key, value]) => {
              let priority = 2;
              let highlightArr: string[] = finalKeywords;

              const whereMatch = whereSqlsFromSider.find((item) => item.field === key);
              if (whereMatch) {
                priority = 0;
                highlightArr = [String(whereMatch.value)];
              } else {
                const valueStr = String(value);
                if (finalKeywords.some((kw) => valueStr.includes(kw))) {
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
            return highlightText(text, [
              ...(keyWordsFormat || []),
              ...((whereSqlsFromSider.map((item) => item.value) || []) as string[]),
            ]);
          },
        };
      }),
    ];
  }, [logTableColumns, keyWordsFormat, columnWidths, whereSqlsFromSider, sqlFilterValue, screenWidth, timeField]);

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

  // 设置列配置
  useEffect(() => {
    const resizableColumns = getBaseColumns.map((col, index) => {
      if (col.dataIndex === timeField) {
        return {
          ...col,
          width: 190,
          onHeaderCell: undefined,
        };
      }

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
      if (!hasMore || getDetailDataRequest.loading) return;

      const scrollElement = tableNode?.querySelector('.ant-table-tbody-virtual-holder');
      if (scrollElement) {
        const { scrollHeight, scrollTop, clientHeight } = scrollElement;
        const distanceToBottom = scrollHeight - scrollTop - clientHeight;
        if (distanceToBottom > 0 && distanceToBottom < 600) {
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
  }, [containerRef.current, tblRef.current, hasMore, getDetailDataRequest.loading, onLoadMore]);

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

  // 列操作处理
  const hasSourceColumn = columns.some((col) => col.dataIndex === '_source') && columns.length === 2;

  const handleDeleteColumn = (colIndex: number) => {
    const col = columns[colIndex];
    const newCols = columns.filter((_, idx) => idx !== colIndex);
    setColumns(newCols);

    const newLogTableColumns = [...logTableColumns];
    const idx = newLogTableColumns.findIndex((c) => c.columnName === col.columnName);
    if (idx > -1) {
      newLogTableColumns[idx].selected = false;
    }
    setLogTableColumns(newLogTableColumns);

    let newFields = [...(searchParams.fields || [])];
    if (col.columnName.indexOf('.') > -1) {
      newFields = newFields.filter((f) => f !== col.columnName);
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
    if (colIndex <= 0) return;
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

  // 包装列头，添加操作按钮
  const enhancedColumns = !hasSourceColumn
    ? columns
        .filter((col) => !col.hidden) // 过滤掉hidden的列
        .map((col, idx) => {
          if (col.dataIndex === timeField) {
            return col;
          }
          return {
            ...col,
            title: (
              <ColumnHeader
                colIndex={idx}
                columns={columns}
                showActions={true}
                title={col.title}
                onDelete={handleDeleteColumn}
                onMoveLeft={handleMoveLeft}
                onMoveRight={handleMoveRight}
              />
            ),
          };
        })
    : columns.filter((col) => !col.hidden); // 过滤掉hidden的列

  console.log('columns======', columns);
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
            <ExpandedRow
              data={record}
              keywords={keyWordsFormat || []}
              moduleQueryConfig={moduleQueryConfig as IModuleQueryConfig}
            />
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
