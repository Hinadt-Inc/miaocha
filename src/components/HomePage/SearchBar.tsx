import { useState, useEffect, useRef, useCallback } from 'react';
import { AutoComplete, Button, Space, Dropdown, Tooltip, Card, Tag, Select, Popover } from 'antd';
import { SearchOutlined, CodeOutlined, InfoCircleOutlined, TagsOutlined, SaveOutlined, StarOutlined, ClockCircleOutlined } from '@ant-design/icons';
import type { MenuProps } from 'antd';
import dayjs from 'dayjs';
import { KibanaTimePicker } from './KibanaTimePicker';

// 节流函数
const useThrottle = (value: any, delay: number) => {
  const [throttledValue, setThrottledValue] = useState(value);
  const lastExecuted = useRef<number>(Date.now());

  useEffect(() => {
    const now = Date.now();
    const handler = setTimeout(() => {
      if (now - lastExecuted.current >= delay) {
        setThrottledValue(value);
        lastExecuted.current = now;
      }
    }, delay - (now - lastExecuted.current));

    return () => {
      clearTimeout(handler);
    };
  }, [value, delay]);

  return throttledValue;
};

// 日志常用字段和值
const LOG_FIELDS = [
  { label: 'service', value: 'service', example: 'hina-cloud-engine' },
  { label: 'level', value: 'level', example: 'INFO, ERROR, WARN' },
  { label: 'logger', value: 'logger', example: 'c.h.c.e.c.i.HttpHeadInterceptor' },
  { label: 'method', value: 'method', example: 'printHeaders, sendKafka, onMessage' },
  { label: 'thread', value: 'thread', example: 'http-nio-21102-exec-5' },
  { label: 'reqType', value: 'marker.reqType', example: 'EXECUTE, CALL' },
  { label: 'msg', value: 'msg', example: 'POST /gather, GET /ha' },
  { label: 'logId', value: 'logId', example: '4a5bed50-5993-47e2-97bb-4cf6c4f57ce9' }
];

// 日志常用查询模板
const QUERY_TEMPLATES = [
  { 
    name: '错误日志', 
    query: "level = 'ERROR'", 
    description: '查询所有错误级别日志' 
  },
  { 
    name: 'API请求日志', 
    query: "logger LIKE 'c.h.c.engine.endpoint.web%'", 
    description: '查询所有API请求相关日志' 
  },
  { 
    name: 'Kafka消息处理', 
    query: "method = 'onMessage' AND msg LIKE '%批量拉取%'", 
    description: '查询Kafka消息处理日志' 
  },
  {
    name: '微信用户请求',
    query: "msg LIKE '%MicroMessenger%' OR msg LIKE '%WeChat%'",
    description: '查询来自微信用户的请求'
  }
];

// 快速时间范围选项
const TIME_PRESETS = [
  { key: 'last_15m', label: '最近15分钟' },
  { key: 'last_1h', label: '最近1小时' },
  { key: 'last_24h', label: '最近24小时' },
  { key: 'last_7d', label: '最近7天' },
  { key: 'today', label: '今天' },
  { key: 'yesterday', label: '昨天' },
  { key: 'this_week', label: '本周' }
];

interface SearchBarProps {
  searchQuery: string;
  whereSql: string;
  timeRange?: [string, string] | null;
  timeRangePreset?: string | null;
  timeDisplayText?: string;  // 新增时间显示文本属性
  onSearch: (query: string) => void;
  onWhereSqlChange: (sql: string) => void;
  onSubmitSearch: () => void;
  onSubmitSql: () => void;
  onTimeRangeChange?: (range: [string, string], preset?: string, displayText?: string) => void;
  onOpenTimeSelector?: () => void;
}

export const SearchBar = ({ 
  searchQuery, 
  whereSql,
  timeRange,
  timeRangePreset,
  timeDisplayText,  // 添加这个参数
  onSearch,
  onWhereSqlChange,
  onSubmitSearch,
  onSubmitSql,
  onTimeRangeChange,
  onOpenTimeSelector
}: SearchBarProps) => {
  const [searchHistory, setSearchHistory] = useState<string[]>(() => {
    const saved = localStorage.getItem('searchHistory');
    return saved ? JSON.parse(saved) : [];
  });
  const [sqlHistory, setSqlHistory] = useState<string[]>(() => {
    const saved = localStorage.getItem('sqlHistory');
    return saved ? JSON.parse(saved) : [];
  });
  const [showFieldSelector, setShowFieldSelector] = useState(false);
  const [selectedField, setSelectedField] = useState<string | null>(null);
  const [fieldValue, setFieldValue] = useState<string>('');
  const [savedQueries, setSavedQueries] = useState<{name: string, query: string}[]>(() => {
    const saved = localStorage.getItem('savedQueries');
    return saved ? JSON.parse(saved) : [];
  });
  const [activeFilters, setActiveFilters] = useState<{
    keywords: boolean;
    sql: boolean;
    time: boolean;
  }>({
    keywords: false,
    sql: false,
    time: timeRange != null
  });

  // 添加时间选择器显示状态
  const [showTimePicker, setShowTimePicker] = useState(false);

  // 添加节流状态
  const [searchInputValue, setSearchInputValue] = useState(searchQuery);
  const [sqlInputValue, setSqlInputValue] = useState(whereSql);
  
  // 使用节流处理搜索输入，节流时间为500毫秒
  const throttledSearchValue = useThrottle(searchInputValue, 500);
  const throttledSqlValue = useThrottle(sqlInputValue, 500);
  
  // 监听节流后的值变化
  useEffect(() => {
    if (throttledSearchValue !== searchQuery) {
      onSearch(throttledSearchValue);
    }
  }, [throttledSearchValue, onSearch, searchQuery]);
  
  useEffect(() => {
    if (throttledSqlValue !== whereSql) {
      onWhereSqlChange(throttledSqlValue);
    }
  }, [throttledSqlValue, onWhereSqlChange, whereSql]);
  
  // 处理搜索输入变化
  const handleSearchInputChange = (value: string) => {
    setSearchInputValue(value);
  };
  
  // 处理SQL输入变化
  const handleSqlInputChange = (value: string) => {
    setSqlInputValue(value);
  };

  // 处理关键词搜索
  const handleKeywordSearch = () => {
    if (searchQuery.trim()) {
      // 保存搜索历史
      if (!searchHistory.includes(searchQuery.trim())) {
        const newHistory = [searchQuery.trim(), ...searchHistory].slice(0, 10);
        setSearchHistory(newHistory);
        localStorage.setItem('searchHistory', JSON.stringify(newHistory));
      }
      setActiveFilters(prev => ({ ...prev, keywords: true }));
      onSubmitSearch();
    }
  };

  // 处理SQL查询
  const handleSqlSearch = () => {
    if (whereSql.trim()) {
      // 保存SQL历史
      if (!sqlHistory.includes(whereSql.trim())) {
        const newHistory = [whereSql.trim(), ...sqlHistory].slice(0, 10);
        setSqlHistory(newHistory);
        localStorage.setItem('sqlHistory', JSON.stringify(newHistory));
      }
      setActiveFilters(prev => ({ ...prev, sql: true }));
      onSubmitSql();
    }
  };

  // 添加字段条件
  const addFieldCondition = () => {
    if (selectedField && fieldValue) {
      const fieldInfo = LOG_FIELDS.find(f => f.value === selectedField);
      if (fieldInfo) {
        const condition = `${selectedField} = '${fieldValue}'`;
        const newWhereSql = whereSql ? `${whereSql} AND ${condition}` : condition;
        onWhereSqlChange(newWhereSql);
        setSelectedField(null);
        setFieldValue('');
        setShowFieldSelector(false);
      }
    }
  };

  // 保存当前查询
  const saveCurrentQuery = (name: string) => {
    if (whereSql && name) {
      const newSavedQuery = { name, query: whereSql };
      const newSavedQueries = [...savedQueries, newSavedQuery];
      setSavedQueries(newSavedQueries);
      localStorage.setItem('savedQueries', JSON.stringify(newSavedQueries));
    }
  };

  // 应用查询模板
  const applyQueryTemplate = (query: string) => {
    onWhereSqlChange(query);
  };

  // 应用时间范围预设
  const applyTimePreset = (preset: typeof TIME_PRESETS[0]) => {
    if (onTimeRangeChange) {
      const now = dayjs();
      let start;
      let end = now;
      
      switch (preset.key) {
        case 'last_15m':
          start = now.subtract(15, 'minute');
          break;
        case 'last_1h':
          start = now.subtract(1, 'hour');
          break;
        case 'last_24h':
          start = now.subtract(24, 'hour');
          break;
        case 'last_7d':
          start = now.subtract(7, 'day');
          break;
        case 'today':
          start = now.startOf('day');
          break;
        case 'yesterday':
          start = now.subtract(1, 'day').startOf('day');
          end = now.subtract(1, 'day').endOf('day');
          break;
        case 'this_week':
          start = now.startOf('week');
          break;
        default:
          start = now.subtract(15, 'minute');
      }
      
      onTimeRangeChange(
        [start.format('YYYY-MM-DD HH:mm:ss'), end.format('YYYY-MM-DD HH:mm:ss')],
        preset.key
      );
      setActiveFilters(prev => ({ ...prev, time: true }));
    }
  };

  // 获取时间范围显示文本
  const getTimeRangeDisplayText = (): string => {
    if (!timeRange) return '选择时间范围';
    
    // 如果是预设，优先使用预设名称
    if (timeRangePreset) {
      const preset = TIME_PRESETS.find(p => p.key === timeRangePreset);
      if (preset) {
        return preset.label;
      }
    }
    
    const [start, end] = timeRange;
    const startDayjs = dayjs(start);
    const endDayjs = dayjs(end);
    
    const now = dayjs();
    const today = now.format('YYYY-MM-DD');
    const yesterday = now.subtract(1, 'day').format('YYYY-MM-DD');
    
    const startDate = startDayjs.format('YYYY-MM-DD');
    const endDate = endDayjs.format('YYYY-MM-DD');
    const startYear = startDayjs.format('YYYY');
    const endYear = endDayjs.format('YYYY');
    
    // 不同年份的情况，显示完整年月日时间
    if (startYear !== endYear) {
      return `${startDayjs.format('YYYY-MM-DD HH:mm:ss')} - ${endDayjs.format('YYYY-MM-DD HH:mm:ss')}`;
    }
    
    // 同一天的情况
    if (startDate === endDate) {
      // 如果是今天
      if (startDate === today) {
        return `今天 ${startDayjs.format('HH:mm:ss')} - ${endDayjs.format('HH:mm:ss')}`;
      }
      // 如果是昨天
      if (startDate === yesterday) {
        return `昨天 ${startDayjs.format('HH:mm:ss')} - ${endDayjs.format('HH:mm:ss')}`;
      }
      // 其他同一天的情况
      return `${startDayjs.format('MM-DD')} ${startDayjs.format('HH:mm:ss')} - ${endDayjs.format('HH:mm:ss')}`;
    }
    
    // 跨天但在同一月
    if (startDayjs.format('YYYY-MM') === endDayjs.format('YYYY-MM')) {
      return `${startDayjs.format('MM-DD HH:mm:ss')} - ${endDayjs.format('DD HH:mm:ss')}`;
    }
    
    // 跨月但在同一年
    return `${startDayjs.format('MM-DD HH:mm:ss')} - ${endDayjs.format('MM-DD HH:mm:ss')}`;
  };

  // 查询模板菜单项
  const templateMenuItems: MenuProps['items'] = [
    {
      key: 'templates',
      type: 'group',
      label: '常用查询模板',
      children: QUERY_TEMPLATES.map((template, index) => ({
        key: `template-${index}`,
        label: (
          <Tooltip title={template.description}>
            <div onClick={() => applyQueryTemplate(template.query)}>
              {template.name}
            </div>
          </Tooltip>
        ),
      })),
    },
    {
      type: 'divider',
    },
    {
      key: 'saved',
      type: 'group',
      label: '我的保存查询',
      children: savedQueries.length > 0 
        ? savedQueries.map((saved, index) => ({
            key: `saved-${index}`,
            label: (
              <div onClick={() => applyQueryTemplate(saved.query)}>
                {saved.name}
              </div>
            ),
          }))
        : [{ key: 'no-saved', label: '暂无保存的查询', disabled: true }],
    }
  ];

  // 时间预设菜单项
  const timePresetMenuItems: MenuProps['items'] = TIME_PRESETS.map(preset => ({
    key: preset.key,
    label: preset.label,
    onClick: () => applyTimePreset(preset)
  }));

  // 显示过滤标签
  const renderFilterTags = () => {
    return (
      <div>
        <Space wrap>
          {searchQuery && activeFilters.keywords && (
            <Tag 
              color="green" 
              closable 
              onClose={() => {
                onSearch('');
                setActiveFilters(prev => ({ ...prev, keywords: false }));
                onSubmitSearch();
              }}
              icon={<SearchOutlined />}
            >
              关键词: {searchQuery.length > 20 ? `${searchQuery.substring(0, 18)}...` : searchQuery}
            </Tag>
          )}
          
          {whereSql && activeFilters.sql && whereSql.split(/\s+AND\s+/i).map((condition, index) => (
            <Tag 
              key={index} 
              color="purple"
              closable
              onClose={() => {
                const conditions = whereSql.split(/\s+AND\s+/i);
                conditions.splice(index, 1);
                const newWhereSql = conditions.join(' AND ');
                onWhereSqlChange(newWhereSql);
                if (!newWhereSql) {
                  setActiveFilters(prev => ({ ...prev, sql: false }));
                }
                onSubmitSql();
              }}
              icon={<CodeOutlined />}
            >
              {condition}
            </Tag>
          ))}
          
          {timeRange && activeFilters.time && (
            <Tag 
              color="blue" 
              closable 
              onClose={() => {
                if (onTimeRangeChange) {
                  onTimeRangeChange([dayjs().subtract(15, 'minute').format('YYYY-MM-DD HH:mm:ss'), dayjs().format('YYYY-MM-DD HH:mm:ss')], 'last_15m');
                }
                setActiveFilters(prev => ({ ...prev, time: false }));
              }}
              icon={<ClockCircleOutlined />}
              onClick={() => setShowTimePicker(true)}
              style={{ cursor: 'pointer' }}
            >
              {timeDisplayText || getTimeRangeDisplayText()}
            </Tag>
          )}
        </Space>
      </div>
    );
  };

  return (
    <div className="search-bar-container">
      <Card>
        <div className="search-sections">
          <div className="search-section">
            <div className="search-section-title">
            关键词搜索
            </div>
            <div className="search-section-content">
              <Space.Compact style={{ width: '100%' }}>
                <AutoComplete
                  placeholder="输入关键词搜索，例如: 'error' || 'timeout'"
                  style={{ width: 'calc(100% - 32px)' }}
                  value={searchInputValue}
                  onChange={handleSearchInputChange}
                  options={searchHistory.map(query => ({
                    value: query,
                    label: query
                  }))}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') {
                      handleKeywordSearch();
                    }
                  }}
                />
                <Button 
                  icon={<SearchOutlined />} 
                  type="primary"
                  onClick={handleKeywordSearch}
                />
              </Space.Compact>
            </div>
          </div>
          
          <div className="search-section">
            <div className="search-section-title">
              <Space>
                <span>SQL条件查询</span>
                <Dropdown menu={{ items: templateMenuItems }} trigger={['hover']}>
                  <Button type="text" size="small" icon={<StarOutlined />}>
                    模板
                  </Button>
                </Dropdown>
                <Button 
                  type="text" 
                  size="small" 
                  icon={<TagsOutlined />} 
                  onClick={() => setShowFieldSelector(!showFieldSelector)}
                >
                  字段筛选
                </Button>
                <Button
                  type="text"
                  size="small"
                  icon={<SaveOutlined />}
                  onClick={() => {
                    if (whereSql) {
                      const name = prompt('请输入查询名称:');
                      if (name) saveCurrentQuery(name);
                    } else {
                      alert('请先输入查询条件');
                    }
                  }}
                >
                  保存查询
                </Button>
                {/* 将Dropdown替换为Popover */}
                <Popover
                  content={
                    <KibanaTimePicker
                      value={timeRange as [string, string]}
                      presetKey={timeRangePreset || undefined}
                      onChange={(range, preset, displayText) => {
                        if (onTimeRangeChange) {
                          onTimeRangeChange(range, preset, displayText);
                          setActiveFilters(prev => ({ ...prev, time: true }));
                          setShowTimePicker(false);
                        }
                      }}
                      timeGrouping="minute"
                      onTimeGroupingChange={(value) => {
                        // 如果需要处理时间分组变化，可以在这里添加
                        console.log('Time grouping changed:', value);
                      }}
                    />
                  }
                  trigger="hover"
                  open={showTimePicker}
                  onOpenChange={setShowTimePicker}
                  placement="bottomRight"
                  overlayStyle={{ width: 'auto', maxWidth: '450px' }}
                  arrow={true}
                >
                  <Button type="text" size="small" icon={<ClockCircleOutlined />}>
                    {timeDisplayText || (timeRangePreset ? TIME_PRESETS.find(p => p.key === timeRangePreset)?.label || '时间范围' : '时间范围')}
                  </Button>
                </Popover>
              </Space>
            </div>
            <div className="search-section-content">
              {showFieldSelector && (
                <div style={{ marginBottom: '8px', display: 'flex', gap: '8px' }}>
                  <Select
                    placeholder="选择字段"
                    style={{ width: '30%' }}
                    value={selectedField}
                    onChange={setSelectedField}
                    options={LOG_FIELDS.map(field => ({
                      label: field.label,
                      value: field.value
                    }))}
                  />
                  <AutoComplete
                    placeholder="输入字段值"
                    style={{ width: '50%' }}
                    value={fieldValue}
                    onChange={setFieldValue}
                    options={
                      selectedField ? 
                        LOG_FIELDS
                          .find(f => f.value === selectedField)?.example
                          .split(', ')
                          .map(val => ({ value: val })) 
                        : []
                    }
                  />
                  <Button 
                    type="primary" 
                    style={{ width: '20%' }} 
                    onClick={addFieldCondition}
                    disabled={!selectedField || !fieldValue}
                  >
                    添加条件
                  </Button>
                </div>
              )}
              <Space.Compact style={{ width: '100%' }}>
                <AutoComplete
                  placeholder="WHERE子句，例如: level = 'ERROR' AND marker.reqType = 'EXECUTE'"
                  style={{ width: 'calc(100% - 32px)' }}
                  value={sqlInputValue}
                  onChange={handleSqlInputChange}
                  options={[
                    ...sqlHistory.map(sql => ({
                      value: sql,
                      label: sql
                    })),
                    ...LOG_FIELDS.map(field => ({
                      value: `${field.value} = ''`,
                      label: `${field.label}: ${field.example}`
                    }))
                  ]}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') {
                      handleSqlSearch();
                    }
                  }}
                />
                <Button 
                  icon={<CodeOutlined />} 
                  type="primary"
                  onClick={handleSqlSearch}
                />
              </Space.Compact>
              
              
            </div>
            
          </div>
          
        </div>
        {/* 显示过滤条件标签 */}
        {(activeFilters.keywords || activeFilters.sql || activeFilters.time) && renderFilterTags()}
      </Card>
    </div>
  );
};
