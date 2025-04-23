import { Collapse, Space, Tag, Cascader } from 'antd';
import { EyeOutlined } from '@ant-design/icons';
import { getFieldTypeColor } from '../../utils/logDataHelpers';

interface FieldSelectorProps {
  selectedTable: string;
  availableFields: Array<{ columnName: string; dataType: string }>;
  selectedFields: string[];
  onToggleField: (fieldName: string) => void;
  lastAddedField: string | null;
  lastRemovedField: string | null;
  availableTables: Array<{
    datasourceId: number;
    datasourceName: string;
    databaseName: string;
    tables: Array<{
      tableName: string;
      tableComment: string;
      columns: Array<{
        columnName: string;
        dataType: string;
        columnComment: string;
        isPrimaryKey: boolean;
        isNullable: boolean;
      }>;
    }>;
  }>;
  onTableChange: (value: string) => void;
  collapsed: boolean;
}

export const FieldSelector = ({
  selectedTable,
  availableFields,
  selectedFields,
  onToggleField,
  lastAddedField,
  lastRemovedField,
  availableTables,
  onTableChange,
  collapsed
}: FieldSelectorProps) => {
  if (collapsed) {
    return null;
  }

  return (
    <>
      <div style={{ padding: '12px 16px', borderBottom: '1px solid #f0f0f0' }}>
        <div>
          <Cascader
            placeholder="选择数据表"
            style={{ width: '100%' }}
            options={availableTables.map(ds => ({
              value: `ds-${ds.datasourceId}`,
              label: ds.datasourceName,
              children: ds.tables.map(table => ({
                value: `tbl-${table.tableName}`,
                label: table.tableName,
                children: table.columns.map(col => ({
                  value: `col-${col.columnName}`,
                  label: `${col.columnName} (${col.dataType})`
                }))
              }))
            }))}
            value={selectedTable && availableTables.length > 0 ? [
              `ds-${selectedTable.split('-')[0]}`, 
              `tbl-${selectedTable.split('-')[1]}`
            ] : undefined}
            loading={availableTables.length === 0}
            onChange={(value: string[]) => {
              if (value && value.length > 0) {
                const tableName = value[value.length - 1].split('-')[1];
                const datasourceId = value[0].split('-')[1];
                onTableChange(`${datasourceId}-${tableName}`);
              } else {
                onTableChange('');
              }
            }}
            allowClear
            showSearch
            displayRender={(labels) => labels[labels.length - 1]}
          />
        </div>
      </div>
      
      <Collapse 
        defaultActiveKey={['available']} 
        ghost
        items={[
          {
            key: 'available',
            label: '可用字段',
            children: availableFields.map(field => (
              <div 
                key={field.columnName} 
                style={{ 
                  padding: '8px 16px',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  cursor: 'pointer',
                  borderRadius: '4px',
                  background: selectedFields.includes(field.columnName) ? '#e6f7ff' : 'transparent',
                  marginBottom: 4,
                  transition: 'all 0.3s ease'
                }}
                onClick={() => onToggleField(field.columnName)}
                className={`field-selection-item ${lastAddedField === field.columnName ? 'field-added' : ''} ${lastRemovedField === field.columnName ? 'field-removed' : ''}`}
              >
                <Space>
                  <Tag color={getFieldTypeColor(field.dataType)} style={{ marginRight: 8 }}>
                    {field.dataType.substr(0, 1).toUpperCase()}
                  </Tag>
                  {field.columnName}
                </Space>
                {selectedFields.includes(field.columnName) && (
                  <EyeOutlined style={{ color: '#1890ff' }} />
                )}
              </div>
            ))
          },
          {
            key: 'selected',
            label: '已选字段',
            children: selectedFields.map(fieldName => {
              const field = availableFields.find(f => f.columnName === fieldName);
              if (!field) return null;
              
              return (
                <div 
                  key={field.columnName} 
                  style={{ 
                    padding: '8px 16px',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    borderRadius: '4px',
                    background: '#e6f7ff',
                    marginBottom: 4
                  }}
                >
                  <Space>
                    <Tag color={getFieldTypeColor(field.dataType)} style={{ marginRight: 8 }}>
                      {field.dataType.substr(0, 1).toUpperCase()}
                    </Tag>
                    {field.columnName}
                  </Space>
                  <EyeOutlined style={{ color: '#1890ff' }} />
                </div>
              );
            })
          }
        ]}
      />
    </>
  );
};
