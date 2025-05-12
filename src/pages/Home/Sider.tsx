import { Collapse, Space, Tag, Cascader } from 'antd';
import { EyeOutlined } from '@ant-design/icons';
import { getFieldTypeColor } from '../../utils/logDataHelpers';
import styles from './Sider.module.less';

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

const Sider = ({
  selectedTable,
  availableFields,
  selectedFields,
  onToggleField,
  lastAddedField,
  lastRemovedField,
  availableTables,
  onTableChange,
  collapsed,
}: FieldSelectorProps) => {
  if (collapsed) {
    return null;
  }

  return (
    <div>
      <div>
        <div>
          <Cascader
            placeholder="请选择模块"
            style={{ width: '100%' }}
            options={availableTables.map((ds) => ({
              value: `ds-${ds.datasourceId}`,
              label: ds.datasourceName,
              children: ds.tables.map((table) => ({
                value: `tbl-${table.tableName}`,
                label: table.tableName,
                children: table.columns.map((col) => ({
                  value: `col-${col.columnName}`,
                  label: `${col.columnName} (${col.dataType})`,
                })),
              })),
            }))}
            value={
              selectedTable && availableTables.length > 0
                ? [`ds-${selectedTable.split('-')[0]}`, `tbl-${selectedTable.split('-')[1]}`]
                : undefined
            }
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
            children: availableFields.map((field) => {
              const isSelected = selectedFields.includes(field.columnName);
              const isAdded = lastAddedField === field.columnName;
              const isRemoved = lastRemovedField === field.columnName;
              return (
                <div
                  key={field.columnName}
                  className={[
                    styles.fieldSelectionItem,
                    isAdded ? styles.fieldAdded : '',
                    isRemoved ? styles.fieldRemoved : '',
                    isSelected ? styles.selected : '',
                  ]
                    .filter(Boolean)
                    .join(' ')}
                  onClick={() => onToggleField(field.columnName)}
                >
                  <Space>
                    <Tag color={getFieldTypeColor(field.dataType)} style={{ marginRight: 8 }}>
                      {field.dataType.substr(0, 1).toUpperCase()}
                    </Tag>
                    {field.columnName}
                  </Space>
                  {isSelected && <EyeOutlined style={{ color: '#1890ff' }} />}
                </div>
              );
            }),
          },
          {
            key: 'selected',
            label: '已选字段',
            children: selectedFields.map((fieldName) => {
              const field = availableFields.find((f) => f.columnName === fieldName);
              if (!field) return null;

              return (
                <div key={field.columnName} className={styles.selectedFieldItem}>
                  <Space>
                    <Tag color={getFieldTypeColor(field.dataType)} style={{ marginRight: 8 }}>
                      {field.dataType.substr(0, 1).toUpperCase()}
                    </Tag>
                    {field.columnName}
                  </Space>
                  <EyeOutlined style={{ color: '#1890ff' }} />
                </div>
              );
            }),
          },
        ]}
      />
    </div>
  );
};
export default Sider;
