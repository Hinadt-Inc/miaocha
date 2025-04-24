import { Tag, Space, Button, Tooltip } from 'antd';
import { FilterOperator } from '../../types/logDataTypes';
import { FilterOutlined, PlusOutlined, ClockCircleOutlined, SearchOutlined, CodeOutlined } from '@ant-design/icons';
import { getFilterDisplayText } from '../../utils/logDataHelpers';
import dayjs from 'dayjs';

interface FilterPanelProps {
  filters: Array<{
    id: string;
    field: string;
    operator: FilterOperator;
    value: string | string[] | [number, number] | null;
    color: string;
  }>;
  timeRange: [string, string] | null;
  timeRangePreset: string | null;
  keyword: string;
  whereSql: string;
  onRemoveFilter: (id: string) => void;
  onAddFilter: () => void;
  onClearTimeRange: () => void;
  onClearKeyword: () => void;
  onClearWhereSql: () => void;
  onOpenTimeSelector: () => void;
}

export const FilterPanel = ({ 
  filters,
  timeRange,
  timeRangePreset,
  keyword,
  whereSql,
  onRemoveFilter,
  onAddFilter,
  onClearTimeRange,
  onClearKeyword,
  onClearWhereSql,
  onOpenTimeSelector
}: FilterPanelProps) => {
  
  // 格式化时间范围显示
  const getTimeRangeDisplay = (): string => {
    if (!timeRange) return '';
    
    const [start, end] = timeRange;
    const startDay = dayjs(start).format('YYYY-MM-DD');
    const endDay = dayjs(end).format('YYYY-MM-DD');
    const now = dayjs().format('YYYY-MM-DD');
    
    // 判断是否为预设时间范围
    if (timeRangePreset) {
      switch (timeRangePreset) {
        case 'last_15m': return '最近15分钟';
        case 'last_1h': return '最近1小时';
        case 'last_24h': return '最近24小时';
        case 'last_7d': return '最近7天';
        case 'today': return '今天';
        case 'yesterday': return '昨天';
        default: break;
      }
    }
    
    // 如果是当天的时间段，只显示时间
    if (startDay === endDay) {
      if (startDay === now) {
        return `今天 ${dayjs(start).format('HH:mm:ss')} - ${dayjs(end).format('HH:mm:ss')}`;
      }
      return `${dayjs(start).format('MM-DD')} ${dayjs(start).format('HH:mm:ss')} - ${dayjs(end).format('HH:mm:ss')}`;
    }
    
    // 不同天的时间段
    return `${dayjs(start).format('MM-DD HH:mm:ss')} - ${dayjs(end).format('MM-DD HH:mm:ss')}`;
  };
  
  // 是否显示过滤器面板（有任何过滤条件时显示）
  const shouldShowFilterPanel = filters.length > 0 || timeRange || keyword || whereSql;
  
  if (!shouldShowFilterPanel) {
    return null;
  }

  return (
    <div className="filter-panel">
      <div className="filter-panel-header">
        <span className="filter-panel-title">过滤条件</span>
        <Button 
          type="link" 
          icon={<PlusOutlined />} 
          onClick={onAddFilter}
          size="small"
        >
          添加过滤器
        </Button>
      </div>
      <div className="filter-panel-content">
        <Space wrap>
          {/* 时间范围标签 */}
          {timeRange && (
            <Tag 
              color="blue" 
              closable 
              onClose={onClearTimeRange}
              icon={<ClockCircleOutlined />}
              className="filter-tag time-filter-tag"
              onClick={onOpenTimeSelector}
            >
              {getTimeRangeDisplay()}
            </Tag>
          )}
          
          {/* 关键词搜索标签 */}
          {keyword && (
            <Tag 
              color="green" 
              closable 
              onClose={onClearKeyword}
              icon={<SearchOutlined />}
              className="filter-tag"
            >
              关键词: {keyword.length > 20 ? `${keyword.substring(0, 18)}...` : keyword}
            </Tag>
          )}
          
          {/* SQL条件查询标签 */}
          {whereSql && (
            <Tag 
              color="purple" 
              closable 
              onClose={onClearWhereSql}
              icon={<CodeOutlined />}
              className="filter-tag"
            >
              <Tooltip title={whereSql}>
                SQL: {whereSql.length > 30 ? `${whereSql.substring(0, 28)}...` : whereSql}
              </Tooltip>
            </Tag>
          )}
          
          {/* 字段过滤器标签 */}
          {filters.map(filter => (
            <Tag 
              key={filter.id} 
              color={filter.color} 
              closable 
              onClose={() => onRemoveFilter(filter.id)}
              icon={<FilterOutlined />}
              className="filter-tag"
            >
              {getFilterDisplayText({
                field: filter.field,
                operator: filter.operator,
                value: filter.value
              })}
            </Tag>
          ))}
        </Space>
      </div>
    </div>
  );
};
