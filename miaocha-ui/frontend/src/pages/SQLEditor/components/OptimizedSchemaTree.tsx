import React, { useCallback, useMemo, useState, memo, useRef, useEffect } from 'react';
import { Button, Card, Empty, Space, Tooltip, Tree, Spin } from 'antd';
import { CopyOutlined, FileSearchOutlined, ReloadOutlined, TableOutlined, DownOutlined } from '@ant-design/icons';
import type { DataNode } from 'antd/es/tree';
import { ExtendedSchemaResult } from '../types';
import Loading from '@/components/Loading';
import styles from './OptimizedSchemaTree.module.less';

// 扩展DataNode类型以支持自定义data属性
interface ExtendedDataNode extends Omit<DataNode, 'children'> {
  children?: ExtendedDataNode[];
  data?: {
    tableName?: string;
    columnName?: string;
    column?: any;
    table?: any;
  };
}

// 工具函数：获取CSS类名
const cx = (...classNames: (string | undefined | false)[]): string => {
  return classNames.filter(Boolean).join(' ');
};

interface OptimizedSchemaTreeProps {
  databaseSchema: ExtendedSchemaResult | { error: string } | null;
  loadingSchema: boolean;
  loadingTables: Set<string>; // 保留但在组件内部不直接使用，通过databaseSchema.tables[].isLoading获取状态
  refreshSchema: () => void;
  fetchTableSchema: (tableName: string) => Promise<any>;
  selectedSource?: string;
  handleInsertTable: (tableName: string, columns: ExtendedSchemaResult['tables'][0]['columns']) => void;
  handleInsertField?: (fieldName: string) => void;
  collapsed?: boolean;
  toggleSider?: () => void;
}

/**
 * 优化的数据库结构树组件
 * 支持快速展示表列表和按需加载表结构详情
 */
const OptimizedSchemaTree: React.FC<OptimizedSchemaTreeProps> = ({
  databaseSchema,
  loadingSchema,
  refreshSchema,
  fetchTableSchema,
  selectedSource,
  handleInsertTable,
  handleInsertField,
  collapsed = false,
  toggleSider,
}) => {
  const [expandedKeys, setExpandedKeys] = useState<React.Key[]>([]);
  const [selectedKeys, setSelectedKeys] = useState<React.Key[]>([]);
  const [treeHeight, setTreeHeight] = useState<number>(500); // 默认高度
  
  // 使用ref来存储当前的expandedKeys，避免在useCallback中产生依赖循环
  const expandedKeysRef = useRef<React.Key[]>([]);
  expandedKeysRef.current = expandedKeys;

  // 容器ref用于计算高度
  const containerRef = useRef<HTMLDivElement>(null);

  // 动态计算树组件高度
  useEffect(() => {
    const updateTreeHeight = () => {
      if (containerRef.current) {
        const containerHeight = containerRef.current.clientHeight;
        // 获取卡片头部的实际高度
        const cardHead = containerRef.current.querySelector('.ant-card-head');
        const headHeight = cardHead ? cardHead.clientHeight : 56;
        // 计算可用高度 = 容器高度 - 头部高度 - padding(16px)
        const calculatedHeight = Math.max(containerHeight - headHeight - 16, 200);
        setTreeHeight(calculatedHeight);
      }
    };

    // 延迟计算，确保DOM已经渲染完成
    const timer = setTimeout(updateTreeHeight, 100);
    
    // 监听窗口大小变化
    window.addEventListener('resize', updateTreeHeight);
    
    return () => {
      clearTimeout(timer);
      window.removeEventListener('resize', updateTreeHeight);
    };
  }, [databaseSchema]); // 当数据源变化时重新计算高度

  // 转换数据为Antd Tree所需的格式
  const treeData = useMemo((): ExtendedDataNode[] => {
    if (loadingSchema || !databaseSchema || 'error' in databaseSchema) {
      return [];
    }

    return databaseSchema.tables.map((table) => {
      const tableKey = `table-${table.tableName}`;
      const isLoaded = table.isLoaded;
      const isLoading = table.isLoading;        
      const node: ExtendedDataNode = {
          title: (
            <div className={styles.tableNode}>
              <div className={styles.nodeContent}>
                <Space size={4}>
                  <TableOutlined />
                  <span className={styles.tableName}>{table.tableName}</span>
                  {isLoading && <Spin size="small" />}
                </Space>
                {table.tableComment && (
                  <Tooltip title={table.tableComment}>
                    <span className={styles.tableComment}>({table.tableComment})</span>
                  </Tooltip>
                )}
              </div>
              <div className={styles.nodeActions}>
                <Tooltip title="插入表和字段">
                  <CopyOutlined 
                    className={styles.actionIcon}
                    onClick={(e) => {
                      e.stopPropagation();
                      if (table.isLoaded && table.columns) {
                        handleInsertTable(table.tableName, table.columns);
                      }
                    }}
                  />
                </Tooltip>
              </div>
            </div>
          ),
        key: tableKey,
        isLeaf: false,
        children: isLoaded && table.columns ? table.columns.map((column) => ({
          title: (
            <div className={styles.columnNode}>
              <div className={styles.nodeContent}>
                <Space size={4}>
                  {column.isPrimaryKey && <span className={styles.primaryKey}>🔑</span>}
                  <span className={styles.columnName}>{column.columnName}</span>
                  <span className={styles.dataType}>({column.dataType})</span>
                </Space>
                {column.columnComment && (
                  <Tooltip title={column.columnComment}>
                    <span className={styles.columnComment}>{column.columnComment}</span>
                  </Tooltip>
                )}
              </div>
              <div className={styles.nodeActions}>
                <Tooltip title="插入字段">
                  <CopyOutlined 
                    className={styles.actionIcon}
                    onClick={(e) => {
                      e.stopPropagation();
                      if (handleInsertField) {
                        handleInsertField(column.columnName);
                      }
                    }}
                  />
                </Tooltip>
              </div>
            </div>
          ),
          key: `column-${table.tableName}-${column.columnName}`,
          isLeaf: true,
          data: {
            tableName: table.tableName,
            columnName: column.columnName,
            column,
          },
        } as ExtendedDataNode)) : [],
        data: {
          tableName: table.tableName,
          table,
        },
      };

      return node;
    });
  }, [databaseSchema, loadingSchema]);

  // 处理树节点展开
  const handleExpand = useCallback(
    async (expandedKeysValue: React.Key[]) => {
      // 检查是否有新展开的表节点需要加载数据
      const newExpandedKeys = expandedKeysValue.filter(key => !expandedKeysRef.current.includes(key));
      
      // 先更新展开状态
      setExpandedKeys(expandedKeysValue);
      
      for (const key of newExpandedKeys) {
        if (typeof key === 'string' && key.startsWith('table-')) {
          const tableName = key.replace('table-', '');
          
          if (databaseSchema && 'tables' in databaseSchema && selectedSource) {
            const table = databaseSchema.tables.find(t => t.tableName === tableName);
            
            if (table && !table.isLoaded && !table.isLoading) {
              // 按需加载表结构
              try {
                await fetchTableSchema(tableName);
              } catch (error) {
                console.error(`加载表 ${tableName} 结构失败:`, error);
              }
            }
          }
        }
      }
    },
    [databaseSchema, selectedSource, fetchTableSchema] // 使用ref避免expandedKeys依赖循环
  );

  // 处理节点选择
  const handleSelect = useCallback(
    (selectedKeysValue: React.Key[], info: any) => {
      setSelectedKeys(selectedKeysValue);
      
      const { node } = info;
      if (node?.data) {
        if (node.key.startsWith('table-')) {
          // 表节点被选中，可以进行插入操作
          const { table } = node.data;
          if (table.isLoaded && table.columns) {
            // 这里可以添加快速插入表名的逻辑
          }
        } else if (node.key.startsWith('column-')) {
          // 字段节点被选中
          const { columnName } = node.data;
          if (handleInsertField) {
            handleInsertField(columnName);
          }
        }
      }
    },
    [handleInsertField]
  );

  // 右键菜单处理
  const handleRightClick = useCallback(
    ({ event, node }: { event: React.MouseEvent; node: any }) => {
      event.preventDefault();
      
      if (node?.data) {
        if (node.key.startsWith('table-')) {
          const { table } = node.data;
          if (table.isLoaded && table.columns) {
            handleInsertTable(table.tableName, table.columns);
          }
        } else if (node.key.startsWith('column-')) {
          const { columnName } = node.data;
          if (handleInsertField) {
            handleInsertField(columnName);
          }
        }
      }
    },
    [handleInsertTable, handleInsertField]
  );

  // 渲染内容
  const renderContent = () => {
    if (loadingSchema) {
      return <Loading tip="加载数据库结构中..." />;
    }

    if (!databaseSchema) {
      return (
        <Empty 
          description="请选择数据源" 
          image={Empty.PRESENTED_IMAGE_SIMPLE}
        />
      );
    }

    if ('error' in databaseSchema) {
      return (
        <div className={styles.errorContainer}>
          <Empty 
            description={databaseSchema.error}
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          >
            <Button 
              type="primary" 
              onClick={refreshSchema}
              icon={<ReloadOutlined />}
            >
              重新加载
            </Button>
          </Empty>
        </div>
      );
    }

    if (!databaseSchema.tables || databaseSchema.tables.length === 0) {
      return (
        <Empty 
          description="该数据源下暂无表结构"
          image={Empty.PRESENTED_IMAGE_SIMPLE}
        >
          <Button 
            type="primary" 
            onClick={refreshSchema}
            icon={<ReloadOutlined />}
          >
            重新加载
          </Button>
        </Empty>
      );
    }

    return (
      <Tree
        className={styles.schemaTree}
        treeData={treeData as DataNode[]}
        expandedKeys={expandedKeys}
        selectedKeys={selectedKeys}
        onExpand={handleExpand}
        onSelect={handleSelect}
        onRightClick={handleRightClick}
        showLine={{ showLeafIcon: false }}
        switcherIcon={<DownOutlined />}
        virtual
        height={treeHeight} // 使用动态计算的高度撑满父容器，启用虚拟滚动提高性能
      />
    );
  };

  const headerTitle = databaseSchema && 'databaseName' in databaseSchema 
    ? databaseSchema.databaseName 
    : '数据库结构';

  return (
    <Card
      ref={containerRef}
      className={cx(styles.schemaTreeCard, collapsed && styles.collapsed)}
      title={
        <div className={styles.cardHeader}>
          <Space>
            <FileSearchOutlined />
            <span>{headerTitle}</span>
          </Space>
          <Space>
            <Tooltip title="刷新结构">
              <Button
                type="text"
                size="small"
                icon={<ReloadOutlined />}
                onClick={refreshSchema}
                loading={loadingSchema}
              />
            </Tooltip>
            {toggleSider && (
              <Tooltip title={collapsed ? "展开" : "收起"}>
                <Button
                  type="text"
                  size="small"
                  icon={<CopyOutlined style={{ transform: collapsed ? 'rotate(180deg)' : 'none' }} />}
                  onClick={toggleSider}
                />
              </Tooltip>
            )}
          </Space>
        </div>
      }
      size="small"
    >
      {renderContent()}
    </Card>
  );
};

export default memo(OptimizedSchemaTree);
