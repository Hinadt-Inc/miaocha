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
  handleInsertTable: (tableName: string, columns?: ExtendedSchemaResult['tables'][0]['columns']) => void;
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
  // toggleSider, // 暂未使用，先注释
}) => {
  const [expandedKeys, setExpandedKeys] = useState<React.Key[]>([]);
  const [selectedKeys, setSelectedKeys] = useState<React.Key[]>([]);
  const [treeHeight, setTreeHeight] = useState<number>(500); // 默认高度

  // 性能优化：限制同时展开的表数量
  const MAX_EXPANDED_TABLES = 10;

  // 容器ref用于计算高度
  const containerRef = useRef<HTMLDivElement>(null);
  
  // 使用ref来追踪上一次的loading状态，用于检测刷新完成
  const prevLoadingSchemaRef = useRef<boolean>(false);

  // 监听刷新完成，重置展开状态
  useEffect(() => {
    // 当loadingSchema从true变为false时，表示刚刚完成了一次刷新
    if (prevLoadingSchemaRef.current && !loadingSchema) {
      setExpandedKeys([]);
      setSelectedKeys([]);
    }
    prevLoadingSchemaRef.current = loadingSchema;
  }, [loadingSchema]);

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

// 优化：拆分出独立的表节点组件，使用 memo 避免不必要的重渲染
const TableNodeComponent = memo(({ 
  table, 
  isLoading, 
  isLoaded, 
  handleInsertTable 
}: {
  table: any;
  isLoading: boolean;
  isLoaded: boolean;
  handleInsertTable: (tableName: string, columns?: any) => void;
}) => (
  <div className={styles.tableNode}>
    <div className={styles.nodeContent}>
      <Space size={4}>
        <TableOutlined />
        <Tooltip title={table.tableComment || '点击展开查看字段'}>
          <span className={styles.tableName}>{table.tableName}</span>
        </Tooltip>
        {isLoading && <Spin size="small" />}
        {isLoaded && (
          <span className={styles.loadedHint}>✓</span>
        )}
      </Space>
    </div>
    <div className={styles.nodeActions}>
      <Tooltip title="插入表和字段">
        <CopyOutlined 
          className={styles.actionIcon}
          onClick={(e) => {
            e.stopPropagation();
            const columns = table.isLoaded ? table.columns : undefined;
            handleInsertTable(table.tableName, columns);
          }}
        />
      </Tooltip>
    </div>
  </div>
));

// 优化：拆分出独立的字段节点组件
const ColumnNodeComponent = memo(({ 
  column, 
  handleInsertField 
}: {
  column: any;
  handleInsertField?: (fieldName: string) => void;
}) => (
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
));

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
            <TableNodeComponent
              table={table}
              isLoading={isLoading || false}
              isLoaded={isLoaded || false}
              handleInsertTable={handleInsertTable}
            />
          ),
        key: tableKey,
        isLeaf: false,
        children: isLoaded && table.columns ? table.columns.map((column) => ({
          title: (
            <ColumnNodeComponent
              column={column}
              handleInsertField={handleInsertField}
            />
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
  }, [databaseSchema, loadingSchema, handleInsertTable, handleInsertField]);

  // 处理树节点展开 - 添加性能优化
  const handleExpand = useCallback(
    async (expandedKeysValue: React.Key[]) => {
      // 性能优化：限制同时展开的表数量
      const tableKeys = expandedKeysValue.filter(key => 
        typeof key === 'string' && key.startsWith('table-')
      );
      
      if (tableKeys.length > MAX_EXPANDED_TABLES) {
        // 警告用户并移除最旧的展开项
        console.warn(`⚠️ 为了性能考虑，最多同时展开 ${MAX_EXPANDED_TABLES} 个表`);
        const limitedKeys = [...tableKeys.slice(-MAX_EXPANDED_TABLES)];
        expandedKeysValue = expandedKeysValue.filter(key => 
          !key.toString().startsWith('table-') || limitedKeys.includes(key)
        );
      }

      // 先更新展开状态
      setExpandedKeys(expandedKeysValue);
      
      // 检查是否有新展开的表节点需要加载数据
      const expandedTableKeys = expandedKeysValue.filter(key => 
        typeof key === 'string' && key.startsWith('table-')
      );
      
      for (const key of expandedTableKeys) {
        const tableName = (key as string).replace('table-', '');
        
        if (databaseSchema && 'tables' in databaseSchema && selectedSource) {
          const table = databaseSchema.tables.find(t => t.tableName === tableName);
          
          // 修复：无论是新展开的还是刷新后重新展开的，只要表未加载且未正在加载，都要加载
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
    },
    [databaseSchema, selectedSource, fetchTableSchema, MAX_EXPANDED_TABLES]
  );

  // 处理节点选择
  const handleSelect = useCallback(
    (selectedKeysValue: React.Key[], info: any) => {
      setSelectedKeys(selectedKeysValue);
      
      const { node } = info;
      if (node?.data) {
        if (node.key.startsWith('table-')) {
          // 表节点被选中，自动展开如果未展开
          const tableKey = node.key;
          if (!expandedKeys.includes(tableKey)) {
            const newExpandedKeys = [...expandedKeys, tableKey];
            setExpandedKeys(newExpandedKeys);
            // 手动触发展开处理逻辑
            handleExpand(newExpandedKeys);
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
    [handleInsertField, expandedKeys, handleExpand]
  );

  // 右键菜单处理
  const handleRightClick = useCallback(
    ({ event, node }: { event: React.MouseEvent; node: any }) => {
      event.preventDefault();
      
      if (node?.data) {
        if (node.key.startsWith('table-')) {
          const { table } = node.data;
          // 支持随时插入表名，无需等待列加载
          const columns = table.isLoaded ? table.columns : undefined;
          handleInsertTable(table.tableName, columns);
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

// 性能提示组件
const PerformanceTip = memo(() => {
  const expandedCount = expandedKeys.filter(key => 
    typeof key === 'string' && key.startsWith('table-')
  ).length;
  
  if (expandedCount >= 8) {
    return (
      <div className={styles.performanceTip}>
        💡 已展开 {expandedCount} 个表，建议关闭不需要的表以提升性能
      </div>
    );
  }
  return null;
});

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
      <div>
        <PerformanceTip />
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
          selectable={true}
          blockNode={true} // 使节点占据整行，增加点击区域
          virtual
          height={treeHeight} // 使用动态计算的高度撑满父容器，启用虚拟滚动提高性能
        />
      </div>
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
