import React, { useCallback, useMemo, useState, memo, useRef, useEffect } from 'react';
import { Button, Card, Empty, Space, Spin, Tooltip } from 'antd';
import {
  CopyOutlined,
  FileSearchOutlined,
  ReloadOutlined,
  TableOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
} from '@ant-design/icons';
import { FixedSizeList as List } from 'react-window';
import { SchemaResult } from '../types';
import './VirtualizedSchemaTree.less';

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
  handleTreeNodeDoubleClick: (tableName: string) => void;
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
  }: {
    index: number;
    style: React.CSSProperties;
    data: {
      nodes: TreeNode[];
      onToggleExpand: (key: string) => void;
      onDoubleClick: (tableName: string) => void;
      onInsertTable: (tableName: string) => void;
      onInsertField?: (fieldName: string) => void;
      collapsed: boolean;
    };
  }) => {
    const { nodes, onToggleExpand, onDoubleClick, onInsertTable, onInsertField, collapsed } = data;
    const node = nodes[index];

    const handleNodeKeyDown = useCallback(
      (e: React.KeyboardEvent) => {
        if (e.key === 'Enter') {
          e.preventDefault();
          if (node?.isTable) {
            if (e.shiftKey) {
              onDoubleClick(node.key);
            } else {
              onToggleExpand(node.key);
            }
          }
        } else if (e.key === ' ') {
          e.preventDefault();
          if (node?.isTable) {
            onToggleExpand(node.key);
          }
        }
      },
      [node?.isTable, node?.key, onToggleExpand, onDoubleClick],
    );

    const handleNodeClick = useCallback(() => {
      if (node?.isTable) {
        onToggleExpand(node.key);
      }
    }, [node?.isTable, node?.key, onToggleExpand]);

    const handleNodeDoubleClick = useCallback(
      (e: React.MouseEvent) => {
        e.stopPropagation();
        if (node?.isTable) {
          onDoubleClick(node.key);
        }
      },
      [node?.isTable, node?.key, onDoubleClick],
    );

    const handleInsertTableClick = useCallback(
      (e: React.MouseEvent) => {
        e.stopPropagation();
        if (node?.isTable) {
          onInsertTable(node.key);
        }
      },
      [node?.isTable, node?.key, onInsertTable],
    );

    const handleInsertFieldClick = useCallback(
      (e: React.MouseEvent) => {
        e.stopPropagation();
        if (node && !node.isTable && onInsertField) {
          const fieldName = node.key.split('-')[1];
          onInsertField(fieldName);
        }
      },
      [node, onInsertField],
    );

    if (!node) return null;

    // 折叠状态下的渲染
    if (collapsed) {
      return (
        // eslint-disable-next-line react/forbid-dom-props
        <div style={style} className="virtual-tree-node virtual-tree-node-collapsed">
          {node.isTable && <TableOutlined className="tree-table-icon" />}
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
        className={`virtual-tree-node level-${node.level} ${node.isTable ? 'table-node' : 'column-node'}`}
        onClick={handleNodeClick}
        onDoubleClick={handleNodeDoubleClick}
        onKeyDown={handleNodeKeyDown}
        tabIndex={0}
        aria-label={`${node.isTable ? 'Table' : 'Column'}: ${node.title}`}
      >
        <div className="tree-node-content">
          {/* 缩进 */}
          <div className={`tree-indent level-${node.level}`} />

          {/* 展开/折叠指示器 */}
          {node.isTable && (
            <div className={`expand-indicator ${node.isExpanded ? 'expanded' : 'collapsed'}`}>{expandIcon}</div>
          )}

          {/* 图标 */}
          {node.isTable ? <TableOutlined className="tree-table-icon" /> : <span className="tree-spacer" />}

          {/* 标题 */}
          <Tooltip title={node.content}>
            <span className="tree-node-title">{node.title}</span>
          </Tooltip>

          {/* 操作按钮 */}
          {node.isTable ? (
            <Tooltip title="插入表和字段">
              <CopyOutlined className="tree-copy-icon" onClick={handleInsertTableClick} />
            </Tooltip>
          ) : (
            onInsertField && (
              <Tooltip title="插入字段">
                <CopyOutlined className="tree-copy-icon" onClick={handleInsertFieldClick} />
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
  handleTreeNodeDoubleClick,
  handleInsertTable,
  handleInsertField,
  collapsed = false,
  toggleSider,
}) => {
  const [expandedKeys, setExpandedKeys] = useState<Set<string>>(new Set());
  const [lazyLoadStarted, setLazyLoadStarted] = useState(false);
  const listRef = useRef<List>(null);

  // 构建扁平化的树节点列表
  const flattenedNodes = useMemo(() => {
    if (!databaseSchema || 'error' in databaseSchema || !lazyLoadStarted) {
      return [];
    }

    const nodes: TreeNode[] = [];

    databaseSchema.tables.forEach((table) => {
      // 添加表节点
      const tableNode: TreeNode = {
        title: table.tableName,
        content: table.tableComment || '',
        key: table.tableName,
        level: 0,
        isTable: true,
        isExpanded: expandedKeys.has(table.tableName),
        children: table.columns.map((column) => ({
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
  }, [databaseSchema, expandedKeys, lazyLoadStarted]);

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

  // 处理插入表
  const handleInsertTableClick = useCallback(
    (tableName: string) => {
      if (databaseSchema && 'tables' in databaseSchema) {
        const table = databaseSchema.tables.find((t) => t.tableName === tableName);
        if (table) {
          handleInsertTable(tableName, table.columns);
        }
      }
    },
    [databaseSchema, handleInsertTable],
  );

  // 延迟加载
  useEffect(() => {
    if (databaseSchema && !lazyLoadStarted) {
      const timer = setTimeout(() => {
        setLazyLoadStarted(true);
      }, 100); // 减少延迟时间
      return () => clearTimeout(timer);
    }
  }, [databaseSchema, lazyLoadStarted]);

  // 计算列表高度 - 使用容器自适应高度，改为更合理的初始值
  const [containerHeight, setContainerHeight] = useState(window.innerHeight * 0.85); // 使用屏幕高度70%作为初始值
  const containerRef = useRef<HTMLDivElement>(null);

  // 使用 ResizeObserver 监听容器高度变化
  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    // 创建ResizeObserver来监听容器尺寸变化
    const resizeObserver = new ResizeObserver((entries) => {
      for (const entry of entries) {
        const height = entry.contentRect.height;
        if (height > 50) {
          // 设置最小高度阈值，避免无效高度
          setContainerHeight(height);
        }
      }
    });

    resizeObserver.observe(container);

    // 初始计算 - 延迟执行确保DOM已渲染
    const calculateInitialHeight = () => {
      const height = container.offsetHeight;
      if (height > 50) {
        setContainerHeight(height);
      } else {
        // 如果容器高度为0，尝试使用父元素高度
        const parentHeight = container.parentElement?.offsetHeight;
        if (parentHeight && parentHeight > 50) {
          setContainerHeight(parentHeight - 48); // 减去Card头部和padding
        }
      }
    };

    // 立即计算
    calculateInitialHeight();

    // 延迟再次计算，确保布局完成
    const timer = setTimeout(calculateInitialHeight, 100);

    return () => {
      resizeObserver.disconnect();
      clearTimeout(timer);
    };
  }, []);

  // 列表数据
  const listData = useMemo(
    () => ({
      nodes: flattenedNodes,
      onToggleExpand: handleToggleExpand,
      onDoubleClick: handleTreeNodeDoubleClick,
      onInsertTable: handleInsertTableClick,
      onInsertField: handleInsertField,
      collapsed,
    }),
    [
      flattenedNodes,
      handleToggleExpand,
      handleTreeNodeDoubleClick,
      handleInsertTableClick,
      handleInsertField,
      collapsed,
    ],
  );

  return (
    <Card
      title={
        <Space>
          {toggleSider && (
            <Button
              type="text"
              icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
              onClick={toggleSider}
              size="small"
            />
          )}
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
      className={`virtualized-schema-tree-card ${collapsed ? 'virtualized-schema-tree-card-collapsed' : ''}`}
      styles={{
        body: {
          padding: collapsed ? '8px 0' : undefined,
          height: '100%',
          display: 'flex',
          flexDirection: 'column',
        },
      }}
    >
      {(() => {
        if (loadingSchema) {
          return (
            <div className="loading-spinner">
              <Spin />
            </div>
          );
        }

        if (databaseSchema && 'tables' in databaseSchema && lazyLoadStarted) {
          return (
            <div ref={containerRef} className="virtualized-tree-container">
              <List
                ref={listRef}
                height={containerHeight}
                width="100%"
                itemCount={flattenedNodes.length}
                itemSize={collapsed ? 32 : 28} // 行高
                itemData={listData}
                overscanCount={5} // 预渲染项目数量
              >
                {TreeNodeRenderer}
              </List>
            </div>
          );
        }

        return (
          <Empty
            description={collapsed ? undefined : '请选择数据源获取数据库结构'}
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          />
        );
      })()}
    </Card>
  );
};

export default memo(VirtualizedSchemaTree);
