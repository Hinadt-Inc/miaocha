import { Tag, Space, Button } from 'antd';
import { FilterOperator } from '../../types/logDataTypes';
import { FilterOutlined, PlusOutlined } from '@ant-design/icons';
import { getFilterDisplayText } from '../../utils/logDataHelpers';

interface FilterPanelProps {
  filters: Array<{
    id: string;
    field: string;
    operator: FilterOperator;
    value: string | string[] | [number, number] | null;
    color: string;
  }>;
  onRemoveFilter: (id: string) => void;
  onAddFilter: () => void; // 实际点击事件在父组件处理
}

export const FilterPanel = ({ 
  filters,
  onRemoveFilter,
  onAddFilter
}: FilterPanelProps) => {
  return (
    <div>
      <Space>
        {filters.map(filter => (
          <Tag 
            key={filter.id} 
            color={filter.color} 
            closable 
            onClose={() => onRemoveFilter(filter.id)}
            icon={<FilterOutlined />}
            style={{ display: 'flex', alignItems: 'center', padding: '0 8px' }}
          >
            {getFilterDisplayText({
              field: filter.field,
              operator: filter.operator,
              value: filter.value
            })}
          </Tag>
        ))}
        <Button 
          type="link" 
          icon={<PlusOutlined />} 
          onClick={onAddFilter}
        >
          添加过滤器
        </Button>
      </Space>
    </div>
  );
};
