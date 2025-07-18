import React, { useCallback, useMemo, useState, memo, useRef, useEffect } from 'react';
import { Button, Card, Empty, Space, Tooltip } from 'antd';
import { CopyOutlined, FileSearchOutlined, ReloadOutlined, TableOutlined } from '@ant-design/icons';
import { VariableSizeList as List } from 'react-window';
import { SchemaResult } from '../types';
import Loading from '@/components/Loading';
import styles from './VirtualizedSchemaTree.module.less';

// 工具函数：获取CSS类名
const cx = (...classNames: (string | undefined | false)[]): string => {
  return classNames.filter(Boolean).join(' ');
};

interface TreeNode {
  title: string;
  content: string;
  key: string;
  level: number;
  isTable: boolean;
  isExpanded?: boolean;
  children?: TreeNode[];
}

interface VirtualizedSchemaTreeProps {
  databaseSchema: SchemaResult | { error: string } | null;
  loadingSchema: boolean;
  refreshSchema: () => void;
  handleInsertTable: (tableName: string, columns: SchemaResult['tables'][0]['columns']) => void;
  handleInsertField?: (fieldName: string) => void;
  collapsed?: boolean;
  toggleSider?: () => void;
}

// 树节点渲染器组件
const TreeNodeRenderer = memo(
  ({
    index,
    style,
    data,
    isScrolling,
  }: {
    index: number;
    style: React.CSSProperties;
    isScrolling?: boolean;
    data: {
      nodes: TreeNode[];
      onToggleExpand: (key: string) => void;
      onInsertTable: (tableName: string) => void;
      onInsertField?: (fieldName: string) => void;
      collapsed: boolean;
    };
  }) => {
    const { nodes, onToggleExpand, onInsertTable, onInsertField, collapsed } = data;
    const node = nodes[index];

    const handleNodeKeyDown = useCallback(
      (e: React.KeyboardEvent) => {
        if (e.key === 'Enter') {
          e.preventDefault();
          if (node?.isTable) {
            onToggleExpand(node.key);
          }
        } else if (e.key === ' ') {
          e.preventDefault();
          if (node?.isTable) {
            onToggleExpand(node.key);
          }
        }
      },
      [node?.isTable, node?.key, onToggleExpand],
    );

    const handleNodeClick = useCallback(() => {
      if (node?.isTable && !isScrolling) {
        onToggleExpand(node.key);
      }
    }, [node?.isTable, node?.key, onToggleExpand, isScrolling]);

    const handleInsertTableClick = useCallback(
      (e: React.MouseEvent) => {
        e.stopPropagation();
        if (node?.isTable && !isScrolling) {
          onInsertTable(node.key);
        }
      },
      [node?.isTable, node?.key, onInsertTable, isScrolling],
    );

    const handleInsertFieldClick = useCallback(
      (e: React.MouseEvent) => {
        e.stopPropagation();
        if (node && !node.isTable && onInsertField && !isScrolling) {
          const fieldName = node.key.split('-')[1];
          onInsertField(fieldName);
        }
      },
      [node, onInsertField, isScrolling],
    );

    if (!node) return null;

    // 滚动时使用简化渲染，减少重绘
    if (isScrolling) {
      return (
        // eslint-disable-next-line react/forbid-dom-props
        <div
          style={style}
          className={cx(
            styles.virtualTreeNode,
            styles.virtualTreeNodeScrolling,
            styles[`level${node.level}`],
            node.isTable ? styles.tableNode : styles.columnNode,
            collapsed ? styles.virtualTreeNodeCollapsed : '',
          )}
        >
          <div className={styles.treeNodeContent}>
            {!collapsed && (
              <>
                <div className={cx(styles.treeIndent, styles[`level${node.level}`])} />
                {node.isTable && (
                  <div className={cx(styles.expandIndicator, node.isExpanded ? styles.expanded : styles.collapsed)}>
                    {node.children && node.children.length > 0 ? (node.isExpanded ? '▼' : '▶') : null}
                  </div>
                )}
                {node.isTable ? (
                  <TableOutlined className={styles.treeTableIcon} />
                ) : (
                  <span className={styles.treeSpacer} />
                )}
                <span className={styles.treeNodeTitle}>{node.title}</span>
              </>
            )}
            {collapsed && node.isTable && <TableOutlined className={styles.treeTableIcon} />}
          </div>
        </div>
      );
    }

    // 折叠状态下的渲染
    if (collapsed) {
      return (
        // eslint-disable-next-line react/forbid-dom-props
        <div style={style} className={cx(styles.virtualTreeNode, styles.virtualTreeNodeCollapsed)}>
          {node.isTable && <TableOutlined className={styles.treeTableIcon} />}
        </div>
      );
    }

    let expandIcon = null;
    if (node.children && node.children.length > 0) {
      expandIcon = node.isExpanded ? '▼' : '▶';
    }

    return (
      // eslint-disable-next-line react/forbid-dom-props, jsx-a11y/click-events-have-key-events, jsx-a11y/no-static-element-interactions, jsx-a11y/no-noninteractive-tabindex
      <div
        style={style}
        className={cx(
          styles.virtualTreeNode,
          styles[`level${node.level}`],
          node.isTable ? styles.tableNode : styles.columnNode,
        )}
        onClick={handleNodeClick}
        onKeyDown={handleNodeKeyDown}
        tabIndex={0}
        aria-label={`${node.isTable ? 'Table' : 'Column'}: ${node.title}`}
      >
        <div className={styles.treeNodeContent}>
          {/* 缩进 */}
          <div className={cx(styles.treeIndent, styles[`level${node.level}`])} />

          {/* 展开/折叠指示器 */}
          {node.isTable && (
            <div className={cx(styles.expandIndicator, node.isExpanded ? styles.expanded : styles.collapsed)}>
              {expandIcon}
            </div>
          )}

          {/* 图标 */}
          {node.isTable ? <TableOutlined className={styles.treeTableIcon} /> : <span className={styles.treeSpacer} />}

          {/* 标题 */}
          <Tooltip title={node.content}>
            <span className={styles.treeNodeTitle}>{node.title}</span>
          </Tooltip>

          {/* 操作按钮 */}
          {node.isTable ? (
            <Tooltip title="插入表和字段">
              <CopyOutlined className={styles.treeCopyIcon} onClick={handleInsertTableClick} />
            </Tooltip>
          ) : (
            onInsertField && (
              <Tooltip title="插入字段">
                <CopyOutlined className={styles.treeCopyIcon} onClick={handleInsertFieldClick} />
              </Tooltip>
            )
          )}
        </div>
      </div>
    );
  },
);

TreeNodeRenderer.displayName = 'TreeNodeRenderer';

const VirtualizedSchemaTree: React.FC<VirtualizedSchemaTreeProps> = ({
  databaseSchema,
  loadingSchema,
  refreshSchema,
  handleInsertTable,
  handleInsertField,
  collapsed = false,
}) => {
  const [expandedKeys, setExpandedKeys] = useState<Set<string>>(new Set());
  const listRef = useRef<List>(null);

  // 使用ref来追踪上一次的loading状态，用于检测刷新完成
  const prevLoadingSchemaRef = useRef<boolean>(false);

  // 监听刷新完成，重置展开状态
  useEffect(() => {
    // 当loadingSchema从true变为false时，表示刚刚完成了一次刷新
    if (prevLoadingSchemaRef.current && !loadingSchema) {
      setExpandedKeys(new Set());
    }
    prevLoadingSchemaRef.current = loadingSchema;
  }, [loadingSchema]);

  // 添加滚动防抖优化，减少快速滚动时的渲染压力
  const scrollTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  const handleScroll = useCallback(() => {
    // 清除之前的timeout
    if (scrollTimeoutRef.current) {
      clearTimeout(scrollTimeoutRef.current);
    }

    // 设置新的timeout，在滚动停止后减少渲染压力
    scrollTimeoutRef.current = setTimeout(() => {
      // 可以在这里添加滚动停止后的优化逻辑
    }, 300);
  }, []);

  // 清理timeout
  useEffect(() => {
    return () => {
      if (scrollTimeoutRef.current) {
        clearTimeout(scrollTimeoutRef.current);
      }
    };
  }, []);

  // 扁平化的树节点列表，添加更多缓存优化
  const flattenedNodes = useMemo(() => {
    // 如果正在加载或者没有数据，返回空数组
    if (loadingSchema || !databaseSchema || 'error' in databaseSchema) {
      return [];
    }

    const nodes: TreeNode[] = [];

    databaseSchema.tables.forEach((table) => {
      // 过滤掉带.的字段，并确保columns存在
      const tableColumnsNotIncludeDot = table.columns?.filter((column) => !column.columnName?.includes('.')) || [];
      // 添加表节点
      const tableNode: TreeNode = {
        title: table.tableName,
        content: table.tableComment || '',
        key: table.tableName,
        level: 0,
        isTable: true,
        isExpanded: expandedKeys.has(table.tableName),
        children: tableColumnsNotIncludeDot?.map((column) => ({
          title: `${column.columnName} ${column.isPrimaryKey ? '🔑 ' : ''}(${column.dataType})`,
          content: column.columnComment || '',
          key: `${table.tableName}-${column.columnName}`,
          level: 1,
          isTable: false,
        })),
      };

      nodes.push(tableNode);

      // 如果表节点展开，添加列节点
      if (expandedKeys.has(table.tableName)) {
        nodes.push(...(tableNode.children || []));
      }
    });

    return nodes;
  }, [databaseSchema, expandedKeys, loadingSchema]);

  // 切换展开/折叠状态
  const handleToggleExpand = useCallback((key: string) => {
    setExpandedKeys((prev) => {
      const newSet = new Set(prev);
      if (newSet.has(key)) {
        newSet.delete(key);
      } else {
        newSet.add(key);
      }
      return newSet;
    });
  }, []);

  // 处理插入表 - 支持随时插入，无需预先加载列信息
  const handleInsertTableClick = useCallback(
    (tableName: string) => {
      // 直接调用插入表方法，无论是否有列信息
      if (databaseSchema && 'tables' in databaseSchema) {
        const table = databaseSchema.tables.find((t) => t.tableName === tableName);
        // 传递列信息（如果有的话），如果没有也没关系
        handleInsertTable(tableName, table?.columns);
      } else {
        // 即使没有数据库结构信息，也能插入表名，传递undefined作为第二个参数
        handleInsertTable(tableName, undefined);
      }
    },
    [databaseSchema, handleInsertTable],
  );

  // 移除延迟加载逻辑，因为它会导致loading状态混乱

  // 使用容器ref来动态计算100%高度
  const containerRef = useRef<HTMLDivElement>(null);
  const [containerHeight, setContainerHeight] = useState(400); // 默认高度

  // 使用ResizeObserver监听容器高度变化，实现真正的100%高度
  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    const updateHeight = () => {
      // 获取容器的实际高度
      const rect = container.getBoundingClientRect();
      if (rect.height > 50) {
        setContainerHeight(rect.height);
      }
    };

    // 创建ResizeObserver监听容器尺寸变化
    const resizeObserver = new ResizeObserver(() => {
      updateHeight();
    });

    resizeObserver.observe(container);

    // 初始计算高度，延迟确保DOM渲染完成
    const timer = setTimeout(updateHeight, 100);

    return () => {
      resizeObserver.disconnect();
      clearTimeout(timer);
    };
  }, []);

  // 动态计算节点高度，避免不同节点类型的高度不一致导致的滚动问题
  const getItemSize = useCallback(
    (index: number) => {
      const node = flattenedNodes[index];
      if (!node) return 28;

      if (collapsed) {
        return 32;
      }

      // 根据节点类型调整高度，确保一致性
      if (node.isTable) {
        return 30; // 表节点稍高一点
      } else {
        return 26; // 列节点稍低一点
      }
    },
    [flattenedNodes, collapsed],
  );

  // 列表数据
  const listData = useMemo(
    () => ({
      nodes: flattenedNodes,
      onToggleExpand: handleToggleExpand,
      onInsertTable: handleInsertTableClick,
      onInsertField: handleInsertField,
      collapsed,
    }),
    [flattenedNodes, handleToggleExpand, handleInsertTableClick, handleInsertField, collapsed],
  );

  return (
    <Card
      title={
        <Space>
          {!collapsed && (
            <>
              <FileSearchOutlined />
              <span>数据库结构</span>
              <Tooltip title="刷新数据库结构">
                <Button
                  type="text"
                  size="small"
                  icon={<ReloadOutlined />}
                  onClick={refreshSchema}
                  loading={loadingSchema}
                />
              </Tooltip>
            </>
          )}
        </Space>
      }
      className={cx(styles.virtualizedSchemaTreeCard, collapsed ? styles.virtualizedSchemaTreeCardCollapsed : '')}
    >
      <div ref={containerRef} className={styles.treeContentWrapper}>
        {(() => {
          // 首先检查loading状态
          if (loadingSchema) {
            return (
              <div className={styles.loadingContainer}>
                <Loading
                  size="default"
                  tip="加载数据库结构..."
                  style={{
                    display: 'flex',
                    justifyContent: 'center',
                    alignItems: 'center',
                    minHeight: '100px',
                  }}
                />
              </div>
            );
          }

          // 检查是否有错误状态
          if (databaseSchema && 'error' in databaseSchema) {
            return (
              <div className={styles.emptyContainer}>
                <Empty
                  description={collapsed ? undefined : `获取数据库结构失败: ${databaseSchema.error}`}
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                />
              </div>
            );
          }

          // 然后检查是否有有效数据
          if (databaseSchema && 'tables' in databaseSchema && databaseSchema.tables.length > 0) {
            return (
              <div className={styles.virtualizedTreeContainer}>
                <List
                  ref={listRef}
                  height={containerHeight}
                  width="100%"
                  itemCount={flattenedNodes.length}
                  itemSize={getItemSize} // 使用动态计算的行高
                  itemData={listData}
                  overscanCount={25} // 增加预渲染项目数量，减少滚动留白
                  useIsScrolling // 启用滚动状态，优化性能
                  onScroll={handleScroll} // 添加滚动处理
                  // 添加滚动优化配置
                  style={{
                    willChange: 'transform',
                    overflowAnchor: 'none', // 防止浏览器的自动滚动锚定
                  }}
                  // 额外的性能优化
                  estimatedItemSize={28} // 估算平均高度，提高滚动性能
                >
                  {TreeNodeRenderer}
                </List>
              </div>
            );
          }

          // 最后显示空状态
          return (
            <div className={styles.emptyContainer}>
              <Empty
                description={collapsed ? undefined : '请选择数据源获取数据库结构'}
                image={Empty.PRESENTED_IMAGE_SIMPLE}
              />
            </div>
          );
        })()}
      </div>
    </Card>
  );
};

export default memo(VirtualizedSchemaTree);
