import { useState, useEffect } from 'react';
import { AutoComplete, Button, Space, Dropdown, DatePicker, Tooltip, Segmented, Card, Tag, Select, Menu, Divider } from 'antd';
const { RangePicker } = DatePicker;
import { SearchOutlined, CodeOutlined, ClockCircleOutlined, QuestionCircleOutlined, InfoCircleOutlined, TagsOutlined, SaveOutlined, StarOutlined, DownOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import type { SegmentedValue } from 'antd/es/segmented';
import type { MenuProps } from 'antd';

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

interface SearchBarProps {
  searchQuery: string;
  whereSql: string;
  timeRange: [string, string] | null;
  timeRangePreset: string | null;
  showTimePicker: boolean;
  onSearch: (query: string) => void;
  onWhereSqlChange: (sql: string) => void;
  onTimeRangeChange: (range: [string, string] | null, preset?: string | null) => void;
  onSubmitSearch: () => void;
  onSubmitSql: () => void;
  onToggleTimePicker: (show: boolean) => void;
  onTimeGroupingChange?: (value: SegmentedValue) => void;
}

export const SearchBar = ({ 
  searchQuery, 
  whereSql,
  timeRange,
  timeRangePreset,
  showTimePicker,
  onSearch,
  onWhereSqlChange,
  onTimeRangeChange,
  onSubmitSearch,
  onSubmitSql,
  onToggleTimePicker,
  onTimeGroupingChange
}: SearchBarProps) => {
  const [searchHistory, setSearchHistory] = useState<string[]>(() => {
    const saved = localStorage.getItem('searchHistory');
    return saved ? JSON.parse(saved) : [];
  });
  const [sqlHistory, setSqlHistory] = useState<string[]>(() => {
    const saved = localStorage.getItem('sqlHistory');
    return saved ? JSON.parse(saved) : [];
  });
  const [timeGrouping, setTimeGrouping] = useState<SegmentedValue>('minute');
  const [datePickerValues, setDatePickerValues] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);
  const [savedQueries, setSavedQueries] = useState<{name: string, query: string}[]>(() => {
    const saved = localStorage.getItem('savedQueries');
    return saved ? JSON.parse(saved) : [];
  });
  const [showFieldSelector, setShowFieldSelector] = useState(false);
  const [selectedField, setSelectedField] = useState<string | null>(null);
  const [fieldValue, setFieldValue] = useState<string>('');

  // 初始化日期选择器的值
  useEffect(() => {
    if (timeRange) {
      setDatePickerValues([dayjs(timeRange[0]), dayjs(timeRange[1])]);
    }
  }, [timeRange]);

  // 处理时间分组改变
  const handleTimeGroupingChange = (value: SegmentedValue) => {
    setTimeGrouping(value);
    if (onTimeGroupingChange) {
      onTimeGroupingChange(value);
    }
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
      onSubmitSql();
    }
  };

  // 应用时间范围
  const applyTimeRange = (start: string, end: string, preset?: string) => {
    onTimeRangeChange([start, end], preset);
    // 不自动关闭自定义时间选择器，除非是预设时间
    if (preset && preset !== 'custom') {
      onToggleTimePicker(false);
    }
  };

  // 处理日期选择器改变
  const handleDatePickerChange = (dates: any) => {
    if (dates && dates[0] && dates[1]) {
      setDatePickerValues([dates[0], dates[1]]);
    } else {
      setDatePickerValues(null);
    }
  };

  // 处理日期选择器确认
  const handleDatePickerOk = () => {
    if (datePickerValues && datePickerValues[0] && datePickerValues[1]) {
      applyTimeRange(
        datePickerValues[0].format('YYYY-MM-DD HH:mm:ss'),
        datePickerValues[1].format('YYYY-MM-DD HH:mm:ss'),
        'custom'
      );
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

  // 缩短的时间范围预设项
  const timePresets = [
    { key: 'last_5m', label: '最近5分钟', 
      action: () => {
        const now = dayjs();
        applyTimeRange(
          now.subtract(5, 'minute').format('YYYY-MM-DD HH:mm:ss'),
          now.format('YYYY-MM-DD HH:mm:ss'),
          'last_5m'
        );
      }
    },
    { key: 'last_15m', label: '最近15分钟', 
      action: () => {
        const now = dayjs();
        applyTimeRange(
          now.subtract(15, 'minute').format('YYYY-MM-DD HH:mm:ss'),
          now.format('YYYY-MM-DD HH:mm:ss'),
          'last_15m'
        );
      }
    },
    { key: 'last_30m', label: '最近30分钟', 
      action: () => {
        const now = dayjs();
        applyTimeRange(
          now.subtract(30, 'minute').format('YYYY-MM-DD HH:mm:ss'),
          now.format('YYYY-MM-DD HH:mm:ss'),
          'last_30m'
        );
      }
    },
    { key: 'last_1h', label: '最近1小时', 
      action: () => {
        const now = dayjs();
        applyTimeRange(
          now.subtract(1, 'hour').format('YYYY-MM-DD HH:mm:ss'),
          now.format('YYYY-MM-DD HH:mm:ss'),
          'last_1h'
        );
      }
    },
    { key: 'last_3h', label: '最近3小时', 
      action: () => {
        const now = dayjs();
        applyTimeRange(
          now.subtract(3, 'hour').format('YYYY-MM-DD HH:mm:ss'),
          now.format('YYYY-MM-DD HH:mm:ss'),
          'last_3h'
        );
      }
    },
    { key: 'last_6h', label: '最近6小时', 
      action: () => {
        const now = dayjs();
        applyTimeRange(
          now.subtract(6, 'hour').format('YYYY-MM-DD HH:mm:ss'),
          now.format('YYYY-MM-DD HH:mm:ss'),
          'last_6h'
        );
      }
    },
    { key: 'last_12h', label: '最近12小时', 
      action: () => {
        const now = dayjs();
        applyTimeRange(
          now.subtract(12, 'hour').format('YYYY-MM-DD HH:mm:ss'),
          now.format('YYYY-MM-DD HH:mm:ss'),
          'last_12h'
        );
      }
    },
    { key: 'today', label: '今天', 
      action: () => {
        const now = dayjs();
        applyTimeRange(
          now.startOf('day').format('YYYY-MM-DD HH:mm:ss'),
          now.format('YYYY-MM-DD HH:mm:ss'),
          'today'
        );
      }
    },
    { key: 'yesterday', label: '昨天', 
      action: () => {
        const yesterday = dayjs().subtract(1, 'day');
        applyTimeRange(
          yesterday.startOf('day').format('YYYY-MM-DD HH:mm:ss'),
          yesterday.endOf('day').format('YYYY-MM-DD HH:mm:ss'),
          'yesterday'
        );
      }
    },
    { key: 'this_week', label: '本周', 
      action: () => {
        const now = dayjs();
        applyTimeRange(
          now.startOf('week').format('YYYY-MM-DD HH:mm:ss'),
          now.format('YYYY-MM-DD HH:mm:ss'),
          'this_week'
        );
      }
    },
    { key: 'custom', label: '自定义时间', 
      action: () => {
        // 直接打开时间选择器
        onToggleTimePicker(true);
      }
    }
  ];

  // 获取默认显示的时间选项标签
  const getTimeRangeButtonText = () => {
    if (!timeRange) {
      return '选择时间范围';
    }
    
    // 如果有预设值，显示预设的标签
    if (timeRangePreset) {
      const preset = timePresets.find(p => p.key === timeRangePreset);
      if (preset) {
        return preset.label;
      }
    }
    
    return '选择时间范围';
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

  return (
    <div className="search-bar-container">
      <Card bodyStyle={{ padding: '16px' }}>
        <div className="search-sections">
          <div className="search-section">
            <div className="search-section-title">
              <Space>
                <SearchOutlined />
                <span>关键词搜索</span>
                <Tooltip title="支持 'error' || 'timeout' 或 'error' && 'timeout' 语法，最多支持两层嵌套">
                  <InfoCircleOutlined style={{ color: '#8c8c8c', cursor: 'help' }} />
                </Tooltip>
              </Space>
            </div>
            <div className="search-section-content">
              <Space.Compact style={{ width: '100%' }}>
                <AutoComplete
                  placeholder="输入关键词搜索，例如: 'error' || 'timeout'"
                  style={{ width: 'calc(100% - 32px)' }}
                  value={searchQuery}
                  onChange={onSearch}
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
                <CodeOutlined />
                <span>SQL条件查询</span>
                <Tooltip title="输入SQL WHERE子句，例如: level = 'ERROR' and service_name = 'user-service'">
                  <InfoCircleOutlined style={{ color: '#8c8c8c', cursor: 'help' }} />
                </Tooltip>
                <Dropdown menu={{ items: templateMenuItems }} trigger={['click']}>
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
                  value={whereSql}
                  onChange={onWhereSqlChange}
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
              
              {whereSql && (
                <div style={{ marginTop: '8px' }}>
                  <Space wrap>
                    {whereSql.split(/\s+AND\s+/i).map((condition, index) => (
                      <Tag 
                        key={index} 
                        closable
                        onClose={() => {
                          const conditions = whereSql.split(/\s+AND\s+/i);
                          conditions.splice(index, 1);
                          onWhereSqlChange(conditions.join(' AND '));
                        }}
                      >
                        {condition}
                      </Tag>
                    ))}
                  </Space>
                </div>
              )}
            </div>
          </div>
          
          <div className="search-section">
            <div className="search-section-title">
              <Space>
                <ClockCircleOutlined />
                <span>时间范围</span>
              </Space>
            </div>
            <div className="search-section-content">
              <Dropdown
                menu={{
                  items: [
                    {
                      key: 'recent',
                      type: 'group',
                      label: '最近时间',
                      children: timePresets.slice(0, 7).map(preset => ({
                        key: preset.key,
                        label: preset.label,
                        onClick: preset.action
                      })),
                    },
                    {
                      type: 'divider',
                    },
                    {
                      key: 'fixed',
                      type: 'group',
                      label: '固定时间段',
                      children: timePresets.slice(7).map(preset => ({
                        key: preset.key,
                        label: preset.label,
                        onClick: preset.action
                      })),
                    }
                  ]
                }}
                trigger={['click']}
              >
                <Button style={{ width: '100%' }}>
                  <Space>
                    <ClockCircleOutlined />
                    <span>{getTimeRangeButtonText()}</span>
                    <DownOutlined />
                  </Space>
                </Button>
              </Dropdown>
            </div>
          </div>
        </div>
      </Card>
      
      {showTimePicker && (
        <div className="time-picker-container">
          <div className="time-picker-header">
            <div className="time-picker-title">自定义时间范围</div>
            <div className="time-grouping-selector">
              <span className="time-grouping-label">时间分组：</span>
              <Segmented
                options={[
                  { label: '分钟', value: 'minute' },
                  { label: '小时', value: 'hour' },
                  { label: '天', value: 'day' },
                  { label: '周', value: 'week' },
                  { label: '月', value: 'month' },
                ]}
                value={timeGrouping}
                onChange={handleTimeGroupingChange}
              />
              <Tooltip title="选择时间分组方式，影响图表数据的统计粒度">
                <QuestionCircleOutlined style={{ marginLeft: 8 }} />
              </Tooltip>
            </div>
          </div>
          <RangePicker
            showTime={{ format: 'HH:mm:ss' }}
            format="YYYY-MM-DD HH:mm:ss"
            style={{ width: '100%' }}
            value={datePickerValues}
            onChange={handleDatePickerChange}
            onOk={handleDatePickerOk}
            ranges={{
              '最近5分钟': [dayjs().subtract(5, 'minute'), dayjs()],
              '最近15分钟': [dayjs().subtract(15, 'minute'), dayjs()],
              '最近30分钟': [dayjs().subtract(30, 'minute'), dayjs()],
              '最近1小时': [dayjs().subtract(1, 'hour'), dayjs()],
              '最近3小时': [dayjs().subtract(3, 'hour'), dayjs()],
              '今天': [dayjs().startOf('day'), dayjs()],
              '昨天': [dayjs().subtract(1, 'day').startOf('day'), dayjs().subtract(1, 'day').endOf('day')],
            }}
          />
          <div className="time-picker-actions" style={{ marginTop: '12px', display: 'flex', justifyContent: 'space-between' }}>
            <div className="time-picker-presets">
              <Space wrap>
                {timePresets.slice(0, 6).map(preset => (
                  <Button key={preset.key} size="small" onClick={preset.action}>
                    {preset.label}
                  </Button>
                ))}
              </Space>
            </div>
            <Space>
              <Button size="small" onClick={() => onToggleTimePicker(false)}>
                取消
              </Button>
              <Button 
                type="primary" 
                size="small" 
                onClick={handleDatePickerOk}
                disabled={!datePickerValues}
              >
                确定
              </Button>
            </Space>
          </div>
        </div>
      )}
    </div>
  );
};
