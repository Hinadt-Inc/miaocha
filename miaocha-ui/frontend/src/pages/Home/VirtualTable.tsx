import { useEffect, useRef, useState, Fragment, useMemo } from 'react';
import { Table, Button, Tooltip } from 'antd';
import ExpandedRow from './ExpandedRow';
import styles from './VirtualTable.module.less';
import { highlightText } from '@/utils/highlightText';
import { CloseOutlined, DoubleLeftOutlined, DoubleRightOutlined } from '@ant-design/icons';

// 计算文本宽度的函数
const getTextWidth = (text: string, fontSize: number = 14, fontFamily: string = 'Arial'): number => {
  const canvas = document.createElement('canvas');
  const context = canvas.getContext('2d');
  if (!context) return text.length * 8; // 降级处理

  context.font = `${fontSize}px ${fontFamily}`;
  const metrics = context.measureText(text);
  return Math.ceil(metrics.width);
};

// 计算列的自适应宽度
const getAutoColumnWidth = (columnName: string, screenWidth: number, minWidth: number = 120, maxWidth: number = 400): number => {
  const textWidth = getTextWidth(columnName, 14, '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto');
  // 添加padding和排序图标空间：
  // 左右padding各16px，排序图标20px，删除/移动按钮空间40px，安全余量20px
  const totalWidth = textWidth + 112;
  
  // 在小屏幕上进一步限制最大宽度
  let adjustedMaxWidth = maxWidth;
  
  if (screenWidth < 1200) {
    // 小屏幕上，根据屏幕宽度动态调整最大宽度
    adjustedMaxWidth = Math.min(maxWidth, Math.floor((screenWidth - 300) / 4)); // 留出300px给时间列和操作空间，剩余空间平均分配
  }
  
  return Math.min(Math.max(totalWidth, minWidth), adjustedMaxWidth);
};

interface IProps {
  data: any[]; // 数据
  loading?: boolean; // 加载状态
  searchParams: ILogSearchParams; // 搜索参数
  onLoadMore: () => void; // 加载更多数据的回调函数
  hasMore?: boolean; // 是否还有更多数据
  dynamicColumns?: ILogColumnsResponse[]; // 动态列配置
  whereSqlsFromSider: IStatus[];
  onChangeColumns: (params: ILogColumnsResponse[]) => void; // 列变化回调函数
  sqls?: string[]; // SQL语句列表
  onSearch?: (params: ILogSearchParams) => void; // 搜索回调函数
  moduleQueryConfig?: any; // 模块查询配置
  onSortChange?: (sortConfig: any[]) => void; // 排序变化回调函数
}

interface ColumnHeaderProps {
  title: React.ReactNode;
  colIndex: number;
  onDelete: (colIndex: number) => void;
  onMoveLeft: (colIndex: number) => void;
  onMoveRight: (colIndex: number) => void;
  showActions: boolean;
  columns: any[];
}

const ResizableTitle = (props: any) => {
  const { onResize, width, ...restProps } = props;
  if (!width) {
    return <th {...restProps} />;
  }

  return (
    <th {...restProps} style={{ width, position: 'relative' }}>
      {restProps.children}
      <div
        className={styles.resizeHandle}
        onMouseDown={(e) => {
          // 阻止事件冒泡，防止触发排序
          e.preventDefault();
          e.stopPropagation();

          const startX = e.pageX;
          const startWidth = width;

          const handleMouseMove = (e: MouseEvent) => {
            // 计算新宽度
            // 鼠标往右拖，e.pageX - startX 为正，宽度变大。
            // 鼠标往左拖，e.pageX - startX 为负，宽度变小。
            const newWidth = startWidth + (e.pageX - startX);
            // 限制宽度
            if (newWidth >= 80 && newWidth <= 800) {
              onResize(newWidth);
            }
          };

          const handleMouseUp = (e: MouseEvent) => {
            // 拖拽结束时也阻止事件冒泡
            e.preventDefault();
            e.stopPropagation();
            document.removeEventListener('mousemove', handleMouseMove);
            document.removeEventListener('mouseup', handleMouseUp);
          };

          document.addEventListener('mousemove', handleMouseMove);
          document.addEventListener('mouseup', handleMouseUp);
        }}
        onClick={(e) => {
          // 点击调整手柄时阻止冒泡，防止触发排序
          e.preventDefault();
          e.stopPropagation();
        }}
      />
    </th>
  );
};

const ColumnHeader: React.FC<ColumnHeaderProps> = ({
  title,
  colIndex,
  onDelete,
  onMoveLeft,
  onMoveRight,
  showActions,
  columns,
}) => {
  const [hovered, setHovered] = useState(false);
  const isLeftLogTime = colIndex > 0 && columns[colIndex - 1]?.dataIndex === '_source';
  const isLast = colIndex === columns.length - 1;
  return (
    <div
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{ display: 'flex', alignItems: 'center' }}
    >
      {title}
      {showActions && hovered && (
        <div className={styles.headerActions}>
          <Tooltip title="移除该列">
            <Button color="primary" variant="link" size="small" onClick={() => onDelete(colIndex)}>
              <CloseOutlined />
            </Button>
          </Tooltip>
          {colIndex > 0 && !isLeftLogTime && (
            <Tooltip title="将列左移​">
              <Button color="primary" variant="link" size="small" onClick={() => onMoveLeft(colIndex)}>
                <DoubleLeftOutlined />
              </Button>
            </Tooltip>
          )}
          {!isLast && (
            <Tooltip title="将列右移​">
              <Button color="primary" variant="link" size="small" onClick={() => onMoveRight(colIndex)}>
                <DoubleRightOutlined />
              </Button>
            </Tooltip>
          )}
        </div>
      )}
    </div>
  );
};

const VirtualTable = (props: IProps) => {
  const {
    data,
    loading = false,
    onLoadMore,
    hasMore = false,
    dynamicColumns,
    searchParams,
    onChangeColumns,
    whereSqlsFromSider = [],
    sqls,
    onSearch,
    moduleQueryConfig,
    onSortChange,
  } = props;
  const containerRef = useRef<HTMLDivElement>(null);
  const tblRef: Parameters<typeof Table>[0]['ref'] = useRef(null);
  const [containerHeight, setContainerHeight] = useState<number>(0);
  const [headerHeight, setHeaderHeight] = useState<number>(0);
  const [columns, setColumns] = useState<any[]>([]);
  const [columnWidths, setColumnWidths] = useState<Record<string, number>>({});
  const [scrollX, setScrollX] = useState(1300);
  const [localSortConfig, setLocalSortConfig] = useState<any[]>([]);
  const [screenWidth, setScreenWidth] = useState(window.innerWidth); // 添加屏幕宽度状态

  // 监听窗口大小变化
  useEffect(() => {
    const handleResize = () => {
      setScreenWidth(window.innerWidth);
    };

    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  // 不支持排序的字段类型
  const unsortableFieldTypes = [
    'LONGTEXT', 'MEDIUMTEXT', 'TINYTEXT', 'JSON', 'BLOB',
    'BITMAP', 'ARRAY', 'MAP', 'STRUCT', 'JSONB', 'VARIANT'
  ];

  // 检查字段是否可以排序
  const isFieldSortable = (dataType: string) => {
    // 检查数据类型是否支持排序
    return !unsortableFieldTypes.includes(dataType.toUpperCase());
  };

  // 处理sqls数据，根据SQL语法提取字段名和值
  const sqlFilterValue = useMemo(() => {
    if (!sqls || sqls.length === 0) return [];

    const extractKeywords = (sql: string): string[] => {
      const keywords: string[] = [];

      // 匹配字段 = 值的模式，支持单引号、双引号或无引号
      const patterns = [
        // 字段名 = '值' 或 字段名='值'
        /([a-zA-Z_][a-zA-Z0-9_.]*)\s*=\s*'([^']*)'/g,
        // 字段名 = "值" 或 字段名="值"
        /([a-zA-Z_][a-zA-Z0-9_.]*)\s*=\s*"([^"]*)"/g,
        // 字段名 = 值 (无引号，匹配到空格、AND、OR或结尾)
        /([a-zA-Z_][a-zA-Z0-9_.]*)\s*=\s*([^\s'"\(\)]+)(?=\s|$|and|AND|or|OR|\))/g,
      ];

      patterns.forEach((pattern) => {
        let match;
        while ((match = pattern.exec(sql)) !== null) {
          const fieldName = match[1].trim();
          const value = match[2].trim();

          if (fieldName) keywords.push(fieldName);
          if (value) keywords.push(value);
        }
      });

      return keywords;
    };

    return Array.from(
      new Set(
        sqls
          .map((sql) => extractKeywords(sql))
          .flat()
          .filter((keyword) => keyword.length > 0),
      ),
    );
  }, [sqls]);

  const keyWordsFormat = useMemo(() => {
    // 从searchParams.keywords中获取关键词
    const keywords = searchParams.keywords || [];

    if (!keywords || keywords.length === 0) return [];

    /**
     * 关键词转换规则函数
     * @param {string} input - 输入的字符串
     * @returns {string[]} 转换后的关键词数组
     */
    function convertKeywords(input: string) {
      // 移除首尾空格
      const trimmed = input.trim();

      // 如果输入为空，返回空数组
      if (!trimmed) {
        return [];
      }

      // 定义所有支持的分隔符，按优先级排序
      const separators = ['&&', '||', ' and ', ' AND ', ' or ', ' OR '];

      // 查找第一个匹配的分隔符
      for (const separator of separators) {
        if (trimmed.includes(separator)) {
          return trimmed
            .split(separator)
            .map((item) => item.trim().replace(/^['"]|['"]$/g, '')) // 移除首尾的单引号或双引号
            .filter((item) => item.length > 0);
        }
      }

      // 单个关键词的情况，移除引号
      return [trimmed.replace(/^['"]|['"]$/g, '')];
    }

    // 处理所有关键词并去重
    const allKeywords = keywords
      .map((keyword: string) => convertKeywords(keyword))
      .flat()
      .filter((keyword: string) => keyword.length > 0);

    return Array.from(new Set(allKeywords));
  }, [searchParams.keywords]);

  const handleResize = (index: number) => (width: number) => {
    const column = columns[index];
    if (!column?.dataIndex) return;

    // 更新列宽状态，允许在任何模式下都能手动调整
    setColumnWidths((prev) => ({
      ...prev,
      [column.dataIndex]: width,
    }));

    // 同时更新当前columns状态中的宽度，确保立即生效
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
  const getBaseColumns = useMemo(() => {
    const timeField = moduleQueryConfig?.timeField || 'log_time'; // 如果没有配置则回退到log_time
    const otherColumns = dynamicColumns?.filter((item) => item.selected && item.columnName !== timeField);
    const _columns: any[] = [];

    // 计算总列数（包括时间字段和_source字段）
    // const totalColumns = 1 + (otherColumns?.length || 0) + 1; // 时间字段 + 动态列 + _source列
    // const shouldUseAutoWidth = totalColumns > 3;

    if (otherColumns && otherColumns.length > 0) {
      // 在小屏幕上，计算每列的最大允许宽度
      const isSmallScreen = screenWidth < 1200;
      
      otherColumns.forEach((item: ILogColumnsResponse) => {
        const { columnName = '' } = item;

        // 优先使用用户手动调整的宽度，其次根据屏幕大小动态计算
        let columnWidth;
        if (columnWidths[columnName]) {
          // 如果用户手动调整过宽度，则使用用户设置的宽度
          columnWidth = columnWidths[columnName];
        } else {
          // 在小屏幕上使用更严格的宽度限制
          if (isSmallScreen) {
            const availableWidth = screenWidth - 250; // 减去时间列、操作按钮等固定空间
            const maxWidthPerColumn = Math.floor(availableWidth / Math.max(otherColumns.length, 2));
            columnWidth = Math.min(getAutoColumnWidth(columnName, screenWidth), maxWidthPerColumn);
          } else {
            columnWidth = getAutoColumnWidth(columnName, screenWidth);
          }
        }

        _columns.push({
          title: columnName,
          dataIndex: columnName,
          width: columnWidth,
          render: (text: string) => highlightText(text, keyWordsFormat || []),
          // 只有可排序的字段才添加排序器
          ...(isFieldSortable(item.dataType) ? {
            sorter: {
              compare: (a: any, b: any) => {
                const valueA = a[columnName];
                const valueB = b[columnName];
                
                // 处理 null、undefined 等空值
                if (valueA === null || valueA === undefined) {
                  if (valueB === null || valueB === undefined) return 0;
                  return -1; // 空值排在前面
                }
                if (valueB === null || valueB === undefined) {
                  return 1;
                }

                // 检查数据类型，针对数字类型进行特殊处理
                const dataType = item.dataType?.toUpperCase();
                const isNumericType = ['INT', 'INTEGER', 'BIGINT', 'TINYINT', 'SMALLINT', 
                                     'FLOAT', 'DOUBLE', 'DECIMAL', 'NUMERIC'].includes(dataType);

                if (isNumericType) {
                  // 数字类型字段：尝试转换为数字进行比较
                  const numA = parseFloat(valueA);
                  const numB = parseFloat(valueB);
                  
                  // 如果都能转换为有效数字，按数字比较
                  if (!isNaN(numA) && !isNaN(numB)) {
                    return numA - numB;
                  }
                  
                  // 如果有一个不是有效数字，无效数字排在后面
                  if (isNaN(numA) && !isNaN(numB)) return 1;
                  if (!isNaN(numA) && isNaN(numB)) return -1;
                  
                  // 如果都不是有效数字，按字符串比较
                  return String(valueA).localeCompare(String(valueB));
                } else {
                  // 非数字类型：按字符串比较
                  if (typeof valueA === 'string' && typeof valueB === 'string') {
                    return valueA.localeCompare(valueB);
                  }
                  return (valueA || '').toString().localeCompare((valueB || '').toString());
                }
              },
              multiple: otherColumns.findIndex(col => col.columnName === columnName) + 2, // 动态列排序优先级依次递减，从2开始（时间字段优先级为1）
            }
          } : {}),
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
          compare: (a: any, b: any) => {
            // 处理时间字段排序，考虑到时间字段可能已经被格式化为字符串
            const timeA = a[timeField];
            const timeB = b[timeField];

            // 如果值为空或无效，放到最后
            if (!timeA && !timeB) return 0;
            if (!timeA) return 1;
            if (!timeB) return -1;

            const parseTime = (timeStr: any) => {
              if (!timeStr) return 0;

              const str = String(timeStr);

              // 如果是已经格式化的时间字符串（如 "2025-06-28 14:51:23.208"），直接用字符串比较
              if (str.match(/^\d{4}-\d{2}-\d{2}\s\d{2}:\d{2}:\d{2}/)) {
                return str;
              }

              // 尝试转换为Date对象
              const date = new Date(str);
              if (!isNaN(date.getTime())) {
                return date.getTime();
              }

              // 如果无法解析为日期，返回原字符串用于字符串比较
              return str;
            };

            const parsedA = parseTime(timeA);
            const parsedB = parseTime(timeB);

            // 如果都是数字，按数字比较
            if (typeof parsedA === 'number' && typeof parsedB === 'number') {
              return parsedA - parsedB;
            }

            // 否则按字符串比较
            return String(parsedA).localeCompare(String(parsedB));
          },
          multiple: 1, // 时间字段排序优先级最高
        },
        render: (text: any) => {
          if (!text) return '';
          const str = String(text);
          // 如果是"Invalid Date"或其他无效值，返回原值或空字符串
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
            // 小屏幕上，只有在没有其他列时才计算宽度
            const hasOtherColumns = _columns.length > 0;
            if (!hasOtherColumns) {
              // 只有_source列时，给它更多空间但仍然限制最大宽度
              return Math.min(600, screenWidth - 300);
            }
          }
          
          // 大屏幕上或有其他列时保持原有逻辑：undefined表示自动宽度
          return undefined;
        })(),
        ellipsis: false,
        hidden: _columns.length > 0, // 恢复原有逻辑：有其他列时隐藏_source列
        render: (_: any, record: ILogColumnsResponse) => {
          // const { keywords = [] } = searchParams;
          // 1. 提取所有 whereSqlsFromSider 的字段和值
          // const whereFields = new Set(whereSqlsFromSider.map((item) => item.field));
          const whereValues = whereSqlsFromSider.map((item) => String(item.value)).filter(Boolean);

          // 2. 合并所有 keywords
          const allKeywords = Array.from(new Set([...keyWordsFormat, ...whereValues])).filter(Boolean);

          // 3. 合并所有关键词（包含SQL过滤后的关键词）
          const finalKeywords = Array.from(
            new Set([...(keyWordsFormat?.length ? allKeywords : sqlFilterValue)]),
          ).filter(Boolean);

          // console.log('匹配高亮关键词', finalKeywords);

          // 预处理每个字段的优先级
          const entries = Object.entries(record).map(([key, value]) => {
            let priority = 2; // 默认最低
            let highlightArr: string[] = finalKeywords;

            // whereSqlsFromSider 匹配优先
            const whereMatch = whereSqlsFromSider.find((item) => item.field === key);
            if (whereMatch) {
              priority = 0;
              highlightArr = [String(whereMatch.value)];
            } else {
              // keywords 匹配
              const valueStr = String(value);
              if (finalKeywords.some((kw) => valueStr.includes(kw))) {
                priority = 1;
              }
            }
            return { key, value, priority, highlightArr };
          });

          // 4. 排序
          const sortedEntries = entries.sort((a, b) => a.priority - b.priority);

          // 5. 渲染
          return (
            <dl className={styles.source}>
              {sortedEntries.map(({ key, value, highlightArr }) => (
                <Fragment key={key}>
                  <dt>{key}</dt>
                  <dd>{highlightText(value, highlightArr)}</dd>
                </Fragment>
              ))}
            </dl>
          );
        },
      },
      ..._columns.map((column, idx) => {
        const isLast = idx === _columns.length - 1;

        // 优先使用用户手动调整的宽度
        let columnWidth;
        if (columnWidths[column.dataIndex]) {
          // 如果用户手动调整过宽度，则使用用户设置的宽度
          columnWidth = columnWidths[column.dataIndex];
        } else {
          // 使用之前计算好的自动宽度，最后一列自动撑满
          columnWidth = isLast ? undefined : (column.width || 150);
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
  }, [dynamicColumns, keyWordsFormat, columnWidths, whereSqlsFromSider, sqls, screenWidth]);

  // console.log('columnWidths', columnWidths);

  useEffect(() => {
    const resizableColumns = getBaseColumns.map((col, index) => {
      // 时间字段列宽度始终为190且不可拖拽
      const timeField = moduleQueryConfig?.timeField || 'log_time';
      if (col.dataIndex === timeField) {
        return {
          ...col,
          width: 190,
          onHeaderCell: undefined,
        };
      }

      // 所有非时间字段列都支持宽度调整
      return {
        ...col,
        // 接收当前列的 column 对象作为参数
        onHeaderCell: (column: any) => ({
          width: column.width, // 设置表头单元格的宽度
          onResize: handleResize(index), // 设置表头单元格的拖拽事件处理函数
        }),
      };
    });
    setColumns(resizableColumns);
  }, [getBaseColumns, moduleQueryConfig]);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;
    // 初始设置高度
    setContainerHeight(container.clientHeight);

    // 监听容器大小变化
    const resizeObserver = new ResizeObserver((entries) => {
      for (const entry of entries) {
        if (entry.target === container) {
          setContainerHeight(entry.contentRect.height);
        }
      }
    });

    resizeObserver.observe(container);

    // 获取表头高度
    const tableNode = tblRef.current?.nativeElement;
    if (tableNode) {
      const header = tableNode.querySelector('.ant-table-thead');
      if (header) {
        setHeaderHeight(header.clientHeight);
      }
    }

    // 添加滚动事件监听
    const handleScroll = () => {
      if (!hasMore || loading) return;

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

    // 清理事件监听器和ResizeObserver
    return () => {
      resizeObserver.disconnect();
      if (tableNode) {
        const scrollElement = tableNode.querySelector('.ant-table-tbody-virtual-holder');
        if (scrollElement) {
          scrollElement.removeEventListener('scroll', handleScroll);
        }
      }
    };
  }, [containerRef.current, tblRef.current, hasMore, loading, onLoadMore]);

  // 动态计算scroll.x
  useEffect(() => {
    const isSmallScreen = screenWidth < 1200;
    
    // 只统计动态列（不含时间字段/_source）
    const timeField = moduleQueryConfig?.timeField || 'log_time';
    const dynamicCols = columns.filter((col: any) => col.dataIndex !== timeField && col.dataIndex !== '_source');
    const sourceCol = columns.find((col: any) => col.dataIndex === '_source');

    let totalWidth = 190; // 时间字段固定宽度
    
    // 累加动态列的实际宽度
    dynamicCols.forEach((col: any) => {
      totalWidth += col.width || 150;
    });

    // 添加_source列的宽度（只有在没有其他列且_source列不隐藏时）
    const hasOtherColumns = dynamicCols.length > 0;
    if (!hasOtherColumns && sourceCol && sourceCol.width) {
      totalWidth += sourceCol.width;
    } else if (!hasOtherColumns) {
      // 如果只有时间字段和_source列，给_source一个默认宽度
      totalWidth += isSmallScreen ? Math.min(600, screenWidth - 300) : 400;
    }

    // 添加展开按钮的宽度
    totalWidth += 26;

    // 在小屏幕上，如果计算出的宽度超过屏幕宽度，就使用屏幕宽度
    if (isSmallScreen && totalWidth > screenWidth) {
      setScrollX(screenWidth);
    } else {
      setScrollX(Math.max(totalWidth, 800)); // 最小宽度800px
    }
  }, [columns, moduleQueryConfig, screenWidth]);

  // 列顺序操作
  const hasSourceColumn = columns.some((col) => col.dataIndex === '_source') && columns.length === 2;

  // 删除列
  const handleDeleteColumn = (colIndex: number) => {
    const col = columns[colIndex];
    const newCols = columns.filter((_, idx) => idx !== colIndex);
    setColumns(newCols);
    onChangeColumns(col);
    // 当删除列后，计算剩余的选中字段
    const timeField = moduleQueryConfig?.timeField || 'log_time';
    const _fields = newCols?.filter((item) => ![timeField, '_source'].includes(item.title)) || [];
    if (_fields.length === 0 && onSearch) {
      const params = {
        ...searchParams,
        fields: [],
      };
      onSearch(params);
    }
  };
  // 左移
  const handleMoveLeft = (colIndex: number) => {
    if (colIndex <= 0) return;
    const newCols = [...columns];
    [newCols[colIndex - 1], newCols[colIndex]] = [newCols[colIndex], newCols[colIndex - 1]];
    setColumns(newCols);
  };
  // 右移
  const handleMoveRight = (colIndex: number) => {
    if (colIndex >= columns.length - 1) return;
    const newCols = [...columns];
    [newCols[colIndex], newCols[colIndex + 1]] = [newCols[colIndex + 1], newCols[colIndex]];
    setColumns(newCols);
  };
  // 触发多列字段排序
  const handleTableChange = (_pagination: any, _filters: any, sorter: any) => {
    let resultSorter: any[] = [];
    // 处理排序信息
    if (sorter) {
      // 判断是单个排序还是多个排序
      if (Array.isArray(sorter)) {
        // 多列排序
        // 过滤出有效的排序字段
        const activeSorts = sorter.filter((sort: any) => sort.order);
        resultSorter = activeSorts.map((sort: any) => ({
          fieldName: sort.field || sort.columnKey,
          direction: sort.order === 'ascend' ? 'ASC' : 'DESC',
        }));
      } else {
        // 单列排序
        if (sorter.order) {
          resultSorter.push({
            fieldName: sorter.field || sorter.columnKey,
            direction: sorter.order === 'ascend' ? 'ASC' : 'DESC',
          });
        } else {
        }
      }
    }
    setLocalSortConfig(resultSorter);

    // 通知父组件排序配置变化
    if (onSortChange) {
      onSortChange(resultSorter);
    }
  };

  // 包装列头，添加删除、左移、右移按钮，并根据是否存在_source列来决定是否显示
  // 如果存在_source列，则不显示删除、左移、右移按钮
  const enhancedColumns = !hasSourceColumn
    ? columns.map((col, idx) => {
        const timeField = moduleQueryConfig?.timeField || 'log_time';
        if (col.dataIndex === timeField) {
          return col;
        }
        return {
          ...col,
          title: (
            <ColumnHeader
              title={col.title}
              colIndex={idx}
              onDelete={handleDeleteColumn}
              onMoveLeft={handleMoveLeft}
              onMoveRight={handleMoveRight}
              showActions={true}
              columns={columns}
            />
          ),
        };
      })
    : columns;

  return (
    <div className={styles.virtualLayout} ref={containerRef}>
      <Table
        virtual
        size="small"
        ref={tblRef}
        rowKey="_key"
        dataSource={data}
        pagination={false}
        columns={enhancedColumns}
        onChange={handleTableChange}
        scroll={{ x: data.length > 0 ? scrollX : 0, y: containerHeight - headerHeight - 1 }}
        // 启用多列排序
        sortDirections={['ascend', 'descend']}
        showSorterTooltip={{
          title: '点击排序，按住Ctrl+点击可多列排序',
        }}
        expandable={{
          columnWidth: 26,
          expandedRowRender: (record) => (
            <ExpandedRow data={record} keywords={keyWordsFormat || []} moduleQueryConfig={moduleQueryConfig} />
          ),
        }}
        components={{
          header: {
            cell: ResizableTitle,
          },
        }}
      />
    </div>
  );
};

export default VirtualTable;
