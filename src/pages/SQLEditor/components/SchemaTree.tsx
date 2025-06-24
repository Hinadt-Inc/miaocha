import { useCallback, useMemo, useState, memo, useEffect } from 'react';
import { Button, Card, Empty, Space, Spin, Tooltip, Tree } from 'antd';
import {
  CopyOutlined,
  FileSearchOutlined,
  ReloadOutlined,
  TableOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
} from '@ant-design/icons';
import { SchemaResult } from '../types';
import './SchemaTree.less';

interface SchemaTreeProps {
  databaseSchema: SchemaResult | null;
  loadingSchema: boolean;
  refreshSchema: () => void;
  handleTreeNodeDoubleClick: (tableName: string) => void;
  handleInsertTable: (tableName: string, columns: SchemaResult['tables'][0]['columns']) => void;
  handleInsertField?: (fieldName: string) => void;
  fullscreen: boolean;
  collapsed?: boolean;
  toggleSider?: () => void;
}

const SchemaTree: React.FC<SchemaTreeProps> = ({
  databaseSchema,
  loadingSchema,
  refreshSchema,
  handleTreeNodeDoubleClick,
  handleInsertTable,
  handleInsertField,
  fullscreen,
  collapsed = false,
  toggleSider,
}) => {
  // 延迟加载状态
  const [lazyLoadStarted, setLazyLoadStarted] = useState(false);
  // 表节点展开状态缓存 - 减少重新渲染
  const [expandedKeys, setExpandedKeys] = useState<string[]>([]);

  // 树形结构数据 - 只在必要时计算
  const treeData = useMemo(() => {
    if (!databaseSchema) return [];

    // 第一次加载延迟200ms，减少同时大量节点渲染
    if (!lazyLoadStarted) {
      setLazyLoadStarted(true);
      return [];
    }

    return databaseSchema.tables.map((table) => ({
      title: table.tableName,
      content: table.tableComment || '',
      key: table.tableName,
      children: table.columns.map((column) => ({
        title: `${column.columnName} ${column.isPrimaryKey ? '🔑 ' : ''}(${column.dataType})`,
        content: column.columnComment,
        key: `${table.tableName}-${column.columnName}`,
        isLeaf: true,
      })),
    }));
  }, [databaseSchema, lazyLoadStarted]);

  // 使用useCallback包装函数，避免不必要的重新渲染
  const handleExpand = useCallback((keys: React.Key[]) => {
    setExpandedKeys(keys as string[]);
  }, []);

  // 渲染树节点标题
  const renderTreeNodeTitle = useCallback(
    (node: { key: string; title: string; content?: string }) => {
      // 折叠状态下只显示图标
      if (collapsed) {
        const isTable = !node.key.includes('-');
        return (
          <div className="tree-node-wrapper-collapsed">
            {isTable ? <TableOutlined className="tree-table-icon" /> : <span className="tree-spacer"></span>}
          </div>
        );
      }

      const isTable = !node.key.includes('-');
      const fieldName = !isTable ? node.key.split('-')[1] : '';

      return (
        <div className="tree-node-wrapper" onDoubleClick={() => isTable && handleTreeNodeDoubleClick(node.key)}>
          {isTable ? <TableOutlined className="tree-table-icon" /> : <span className="tree-spacer"></span>}
          <Tooltip title={node.content}>
            <span className="tree-node-title">{node.title}</span>
          </Tooltip>
          {isTable ? (
            <Tooltip title="插入表和字段">
              <CopyOutlined
                className="tree-copy-icon"
                onClick={(e) => {
                  e.stopPropagation();
                  const table = databaseSchema?.tables.find((t) => t.tableName === node.key);
                  if (table) {
                    handleInsertTable(table.tableName, table.columns);
                  }
                }}
              />
            </Tooltip>
          ) : (
            handleInsertField && (
              <Tooltip title="插入字段">
                <CopyOutlined
                  className="tree-copy-icon"
                  onClick={(e) => {
                    e.stopPropagation();
                    handleInsertField(fieldName);
                  }}
                />
              </Tooltip>
            )
          )}
        </div>
      );
    },
    [collapsed, databaseSchema, handleInsertTable, handleTreeNodeDoubleClick, handleInsertField],
  );

  // 延迟加载树节点
  useEffect(() => {
    if (databaseSchema && !lazyLoadStarted) {
      const timer = setTimeout(() => {
        setLazyLoadStarted(true);
      }, 200);
      return () => clearTimeout(timer);
    }
  }, [databaseSchema, lazyLoadStarted]);

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
      className={`schema-tree-card ${collapsed ? 'schema-tree-card-collapsed' : ''}`}
      styles={{ body: { padding: collapsed ? '8px 0' : undefined } }}
    >
      {(() => {
        if (loadingSchema) {
          return (
            <div className="loading-spinner">
              <Spin />
            </div>
          );
        }

        if (databaseSchema?.tables && lazyLoadStarted) {
          return (
            <Tree
              showLine
              defaultExpandAll={false}
              titleRender={renderTreeNodeTitle}
              treeData={treeData}
              height={fullscreen ? window.innerHeight - 250 : undefined}
              virtual={true} // 始终启用虚拟滚动提高性能
              expandedKeys={expandedKeys}
              onExpand={handleExpand}
              motion={{}} // 禁用动画，提高性能
            />
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

// 使用React.memo包装组件，避免不必要的重新渲染
export default memo(SchemaTree);
