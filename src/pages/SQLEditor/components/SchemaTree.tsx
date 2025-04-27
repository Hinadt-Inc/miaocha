import { useCallback, useMemo } from 'react';
import { Button, Card, Empty, Space, Spin, Tooltip, Tree } from 'antd';
import { CopyOutlined, FileSearchOutlined, ReloadOutlined, TableOutlined } from '@ant-design/icons';
import { SchemaResult } from '../types';
import './SchemaTree.less';

interface SchemaTreeProps {
  databaseSchema: SchemaResult | null;
  loadingSchema: boolean;
  refreshSchema: () => void;
  handleTreeNodeDoubleClick: (tableName: string) => void;
  handleInsertTable: (tableName: string, columns: Array<{ columnName: string }>) => void;
  fullscreen: boolean;
}

const SchemaTree: React.FC<SchemaTreeProps> = ({
  databaseSchema,
  loadingSchema,
  refreshSchema,
  handleTreeNodeDoubleClick,
  handleInsertTable,
  fullscreen
}) => {
  // 树形结构数据
  const treeData = useMemo(() => {
    if (!databaseSchema) return [];
    
    return databaseSchema.tables.map(table => ({
      title: table.tableName + (table.tableComment ? ` (${table.tableComment})` : ''),
      key: table.tableName,
      children: table.columns.map(column => ({
        title: `${column.columnName} ${column.isPrimaryKey ? '🔑 ' : ''}(${column.dataType})`,
        content: column.columnComment,
        key: `${table.tableName}-${column.columnName}`,
        isLeaf: true
      }))
    }));
  }, [databaseSchema]);

  // 渲染树节点标题
  const renderTreeNodeTitle = useCallback((node: { key: string; title: string; content?: string }) => {
    const isTable = node.key.indexOf('-') === -1;
    return (
      <div 
        className="tree-node-wrapper"
        onDoubleClick={() => isTable && handleTreeNodeDoubleClick(node.key)}
      >
        {isTable ? <TableOutlined className="tree-table-icon" /> : 
          <span className="tree-spacer"></span>}
        <Tooltip title={node.content}>
          <span className="tree-node-title">
            {node.title}
          </span>
        </Tooltip>
        {isTable && (
          <Tooltip title="插入表和字段">
            <CopyOutlined 
              className="tree-copy-icon"
              onClick={(e) => {
                e.stopPropagation();
                const table = databaseSchema?.tables.find(t => t.tableName === node.key);
                if (table) {
                  handleInsertTable(table.tableName, table.columns);
                }
              }}
            />
          </Tooltip>
        )}
      </div>
    );
  }, [databaseSchema, handleInsertTable, handleTreeNodeDoubleClick]);

  return (
    <Card 
      title={
        <Space>
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
        </Space>
      } 
      style={{overflowY: 'auto' }}
    >
      {(() => {
        if (loadingSchema) {
          return (
            <div className="loading-spinner">
              <Spin tip="加载中..." />
            </div>
          );
        }
        
        if (databaseSchema?.tables) {
          return (
            <Tree
              showLine
              defaultExpandAll={false}
              titleRender={renderTreeNodeTitle}
              treeData={treeData}
              height={fullscreen ? window.innerHeight - 250 : undefined}
              virtual={treeData.length > 100} // 大数据量时启用虚拟滚动
            />
          );
        }
        
        return (
          <Empty 
            description="请选择数据源获取数据库结构" 
            image={Empty.PRESENTED_IMAGE_SIMPLE} 
          />
        );
      })()}
    </Card>
  );
};

export default SchemaTree;