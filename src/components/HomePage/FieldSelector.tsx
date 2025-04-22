import { Collapse, Space, Tag, Cascader } from 'antd';
import { EyeOutlined } from '@ant-design/icons';
import { getFieldTypeColor } from '../../utils/logDataHelpers';

interface FieldSelectorProps {
  availableFields: Array<{ name: string; type: string }>;
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
            onChange={(value: string[]) => {
              if (value && value.length > 0) {
                const lastValue = value[value.length - 1];
                // 从格式化的value中提取实际表名
                const tableName = lastValue.startsWith('tbl-') 
                  ? lastValue.substring(4)
                  : lastValue;
                onTableChange(tableName);
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
                key={field.name} 
                style={{ 
                  padding: '8px 16px',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  cursor: 'pointer',
                  borderRadius: '4px',
                  background: selectedFields.includes(field.name) ? '#e6f7ff' : 'transparent',
                  marginBottom: 4,
                  transition: 'all 0.3s ease'
                }}
                onClick={() => onToggleField(field.name)}
                className={`field-selection-item ${lastAddedField === field.name ? 'field-added' : ''} ${lastRemovedField === field.name ? 'field-removed' : ''}`}
              >
                <Space>
                  <Tag color={getFieldTypeColor(field.type)} style={{ marginRight: 8 }}>
                    {field.type.substr(0, 1).toUpperCase()}
                  </Tag>
                  {field.name}
                </Space>
                {selectedFields.includes(field.name) && (
                  <EyeOutlined style={{ color: '#1890ff' }} />
                )}
              </div>
            ))
          },
          {
            key: 'selected',
            label: '已选字段',
            children: selectedFields.map(fieldName => {
              const field = availableFields.find(f => f.name === fieldName);
              if (!field) return null;
              
              return (
                <div 
                  key={field.name} 
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
                    <Tag color={getFieldTypeColor(field.type)} style={{ marginRight: 8 }}>
                      {field.type.substr(0, 1).toUpperCase()}
                    </Tag>
                    {field.name}
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
