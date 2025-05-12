import { useState, useCallback, useMemo } from 'react';
import {
  AutoComplete,
  Button,
  Space,
  Dropdown,
  Tooltip,
  Card,
  Tag,
  Select,
  Popover,
  Flex,
  Row,
  Col,
} from 'antd';
import {
  SearchOutlined,
  CodeOutlined,
  TagsOutlined,
  SaveOutlined,
  StarOutlined,
  ClockCircleOutlined,
} from '@ant-design/icons';
import type { MenuProps } from 'antd';
import dayjs from 'dayjs';
import { lazy } from 'react';
import { debounce } from '@/utils/logDataHelpers';
import styles from './SearchBar.module.less';

const KibanaTimePicker = lazy(() =>
  import('./KibanaTimePicker.tsx').then((module) => ({
    default: module.KibanaTimePicker,
  })),
);

// 日志常用字段和值
const LOG_FIELDS = [
  { label: 'service', value: 'service', example: 'hina-cloud-engine' },
  { label: 'level', value: 'level', example: 'INFO, ERROR, WARN' },
  { label: 'logger', value: 'logger', example: 'c.h.c.e.c.i.HttpHeadInterceptor' },
  { label: 'method', value: 'method', example: 'printHeaders, sendKafka, onMessage' },
  { label: 'thread', value: 'thread', example: 'http-nio-21102-exec-5' },
  { label: 'reqType', value: 'marker.reqType', example: 'EXECUTE, CALL' },
  { label: 'msg', value: 'msg', example: 'POST /gather, GET /ha' },
  { label: 'logId', value: 'logId', example: '4a5bed50-5993-47e2-97bb-4cf6c4f57ce9' },
];

// 日志常用查询模板
const QUERY_TEMPLATES = [
  {
    name: '错误日志',
    query: "level = 'ERROR'",
    description: '查询所有错误级别日志',
  },
  {
    name: 'API请求日志',
    query: "logger LIKE 'c.h.c.engine.endpoint.web%'",
    description: '查询所有API请求相关日志',
  },
  {
    name: 'Kafka消息处理',
    query: "method = 'onMessage' AND msg LIKE '%批量拉取%'",
    description: '查询Kafka消息处理日志',
  },
  {
    name: '微信用户请求',
    query: "msg LIKE '%MicroMessenger%' OR msg LIKE '%WeChat%'",
    description: '查询来自微信用户的请求',
  },
];

// 快速时间范围选项
const TIME_PRESETS = [
  { key: 'last_15m', label: '最近15分钟' },
  { key: 'last_1h', label: '最近1小时' },
  { key: 'last_24h', label: '最近24小时' },
  { key: 'last_7d', label: '最近7天' },
  { key: 'today', label: '今天' },
  { key: 'yesterday', label: '昨天' },
  { key: 'this_week', label: '本周' },
];

interface SearchBarProps {
  searchQuery: string;
  whereSql: string;
  timeRange?: [string, string] | null;
  timeRangePreset?: string | null;
  timeDisplayText?: string;
  timeGrouping?: 'minute' | 'hour' | 'day' | 'month';
  onSearch: (query: string) => void;
  onWhereSqlChange: (sql: string) => void;
  onSubmitSearch: () => void;
  onSubmitSql: () => void;
  onTimeRangeChange?: (range: [string, string], preset?: string, displayText?: string) => void;
  onOpenTimeSelector?: () => void;
  onTimeGroupingChange?: (grouping: 'minute' | 'hour' | 'day' | 'month') => void;
}

const SearchBar = ({
  searchQuery,
  whereSql,
  timeRange,
  timeRangePreset,
  timeDisplayText,
  timeGrouping,
  onSearch,
  onWhereSqlChange,
  onSubmitSearch,
  onSubmitSql,
  onTimeRangeChange,
  onTimeGroupingChange,
}: SearchBarProps) => {
  // 修复 useRef 的使用方式，改用 useState 初始化历史记录
  const [searchHistory, setSearchHistory] = useState<string[]>(() => {
    const saved = localStorage.getItem('searchHistory');
    return saved ? (JSON.parse(saved) as string[]) : [];
  });

  const [sqlHistory, setSqlHistory] = useState<string[]>(() => {
    const saved = localStorage.getItem('sqlHistory');
    return saved ? (JSON.parse(saved) as string[]) : [];
  });

  const [savedQueries, setSavedQueries] = useState<{ name: string; query: string }[]>(() => {
    const saved = localStorage.getItem('savedQueries');
    return saved ? (JSON.parse(saved) as { name: string; query: string }[]) : [];
  });

  const [searchInputValue, setSearchInputValue] = useState(searchQuery);
  const [sqlInputValue, setSqlInputValue] = useState(whereSql);
  const [showFieldSelector, setShowFieldSelector] = useState(false);
  const [selectedField, setSelectedField] = useState<string | null>(null);
  const [fieldValue, setFieldValue] = useState<string>('');
  const [showTimePicker, setShowTimePicker] = useState(false);
  const [activeFilters, setActiveFilters] = useState<{
    keywords: boolean;
    sql: boolean;
    time: boolean;
  }>({
    keywords: !!searchQuery,
    sql: !!whereSql,
    time: timeRange != null,
  });

  // 创建防抖处理函数，减少不必要的更新和渲染
  const debouncedSearch = useMemo(
    () =>
      debounce((value: string) => {
        onSearch(value);
      }, 300),
    [onSearch],
  );

  const debouncedSqlChange = useMemo(
    () =>
      debounce((value: string) => {
        onWhereSqlChange(value);
      }, 300),
    [onWhereSqlChange],
  );

  // 处理搜索输入变化
  const handleSearchInputChange = useCallback(
    (value: string) => {
      setSearchInputValue(value);
      debouncedSearch(value);
    },
    [debouncedSearch],
  );

  // 处理SQL输入变化
  const handleSqlInputChange = useCallback(
    (value: string) => {
      setSqlInputValue(value);
      debouncedSqlChange(value);
      // 当直接清空SQL输入框时，需要更新activeFilters
      if (!value.trim()) {
        setActiveFilters((prev) => ({ ...prev, sql: false }));
      }
    },
    [debouncedSqlChange],
  );

  // 处理关键词搜索
  const handleKeywordSearch = useCallback(() => {
    if (searchInputValue.trim()) {
      // 保存搜索历史
      if (!searchHistory.includes(searchInputValue.trim())) {
        const newHistory = [searchInputValue.trim(), ...searchHistory].slice(0, 10);
        setSearchHistory(newHistory);
        localStorage.setItem('searchHistory', JSON.stringify(newHistory));
      }
      setActiveFilters((prev) => ({ ...prev, keywords: true }));
      onSubmitSearch();
    }
  }, [searchInputValue, searchHistory, onSubmitSearch]);

  // 处理SQL查询
  const handleSqlSearch = useCallback(() => {
    if (sqlInputValue.trim()) {
      // 保存SQL历史
      if (!sqlHistory.includes(sqlInputValue.trim())) {
        const newHistory = [sqlInputValue.trim(), ...sqlHistory].slice(0, 10);
        setSqlHistory(newHistory);
        localStorage.setItem('sqlHistory', JSON.stringify(newHistory));
      }
      setActiveFilters((prev) => ({ ...prev, sql: true }));
      onSubmitSql();
    }
  }, [sqlInputValue, sqlHistory, onSubmitSql]);

  // 添加字段条件
  const addFieldCondition = useCallback(() => {
    if (selectedField && fieldValue) {
      const fieldInfo = LOG_FIELDS.find((f) => f.value === selectedField);
      if (fieldInfo) {
        const condition = `${selectedField} = '${fieldValue}'`;
        const newWhereSql = whereSql ? `${whereSql} AND ${condition}` : condition;
        onWhereSqlChange(newWhereSql);
        setSqlInputValue(newWhereSql); // 更新SQL输入框的值
        setSelectedField(null);
        setFieldValue('');
        setShowFieldSelector(false);
        setActiveFilters((prev) => ({ ...prev, sql: true }));
        onSubmitSql(); // 触发SQL查询
      }
    }
  }, [selectedField, fieldValue, whereSql, onWhereSqlChange, onSubmitSql]);

  // 保存当前查询
  const saveCurrentQuery = useCallback(
    (name: string) => {
      if (whereSql && name) {
        const newSavedQuery = { name, query: whereSql };
        const newSavedQueries = [...savedQueries, newSavedQuery];
        setSavedQueries(newSavedQueries);
        localStorage.setItem('savedQueries', JSON.stringify(newSavedQueries));
      }
    },
    [whereSql, savedQueries],
  );

  // 应用查询模板
  const applyQueryTemplate = useCallback(
    (query: string) => {
      onWhereSqlChange(query);
      setSqlInputValue(query);
      setActiveFilters((prev) => ({ ...prev, sql: !!query }));
      onSubmitSql(); // 触发查询执行
    },
    [onWhereSqlChange, onSubmitSql],
  );

  // 获取时间范围显示文本
  const getTimeRangeDisplayText = useCallback((): string => {
    if (!timeRange) return '选择时间范围';

    // 如果是预设，优先使用预设名称
    if (timeRangePreset) {
      const preset = TIME_PRESETS.find((p) => p.key === timeRangePreset);
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
  }, [timeRange, timeRangePreset]);

  // 查询模板菜单项 - 使用 useMemo 缓存
  const templateMenuItems: MenuProps['items'] = useMemo(
    () => [
      {
        key: 'templates',
        type: 'group',
        label: '常用查询模板',
        children: QUERY_TEMPLATES.map((template, index) => ({
          key: `template-${index}`,
          label: (
            <Tooltip title={template.description}>
              <div onClick={() => applyQueryTemplate(template.query)}>{template.name}</div>
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
        children:
          savedQueries.length > 0
            ? savedQueries.map((saved, index) => ({
                key: `saved-${index}`,
                label: <div onClick={() => applyQueryTemplate(saved.query)}>{saved.name}</div>,
              }))
            : [{ key: 'no-saved', label: '暂无保存的查询', disabled: true }],
      },
    ],
    [savedQueries, applyQueryTemplate],
  );

  // 显示过滤标签 - 使用 useMemo 缓存复杂渲染逻辑
  const filterTags = useMemo(() => {
    return (
      <div className={styles.filterTags}>
        <Space wrap>
          {searchInputValue && activeFilters.keywords && (
            <Tag
              color="green"
              closable
              onClose={() => {
                onSearch('');
                setSearchInputValue('');
                setActiveFilters((prev) => ({ ...prev, keywords: false }));
                onSubmitSearch();
              }}
              icon={<SearchOutlined />}
            >
              {searchInputValue.length > 20
                ? `${searchInputValue.substring(0, 18)}...`
                : searchInputValue}
            </Tag>
          )}

          {sqlInputValue &&
            activeFilters.sql &&
            sqlInputValue.split(/\s+AND\s+/i).map((condition, index) => (
              <Tag
                key={index}
                color="purple"
                closable
                onClose={() => {
                  const conditions = sqlInputValue.split(/\s+AND\s+/i);
                  conditions.splice(index, 1);
                  const newWhereSql = conditions.join(' AND ');
                  onWhereSqlChange(newWhereSql);
                  setSqlInputValue(newWhereSql);
                  if (!newWhereSql) {
                    setActiveFilters((prev) => ({ ...prev, sql: false }));
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
                  onTimeRangeChange(
                    [
                      dayjs().subtract(15, 'minute').format('YYYY-MM-DD HH:mm:ss'),
                      dayjs().format('YYYY-MM-DD HH:mm:ss'),
                    ],
                    'last_15m',
                  );
                }
                setActiveFilters((prev) => ({ ...prev, time: false }));
              }}
              icon={<ClockCircleOutlined />}
              onClick={() => setShowTimePicker(true)}
              style={{ cursor: 'pointer' }}
            >
              {timeDisplayText ?? getTimeRangeDisplayText()}
            </Tag>
          )}
        </Space>
      </div>
    );
  }, [
    searchInputValue,
    sqlInputValue,
    timeRange,
    activeFilters,
    onSearch,
    onWhereSqlChange,
    onSubmitSearch,
    onSubmitSql,
    onTimeRangeChange,
    timeDisplayText,
    getTimeRangeDisplayText,
    onTimeGroupingChange,
  ]);

  return (
    <div className={styles.searchBarContainer}>
      <Card>
        <div className={styles.searchSections}>
          <div className={styles.item}>
            <Space.Compact style={{ width: '100%' }}>
              <AutoComplete
                allowClear
                placeholder="输入关键词搜索"
                style={{ width: '100%' }}
                value={searchInputValue}
                onChange={handleSearchInputChange}
                options={searchHistory.map((query) => ({
                  value: query,
                  label: query,
                }))}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    handleKeywordSearch();
                  }
                }}
              />
            </Space.Compact>
          </div>

          <div className={styles.item}>
            <Space.Compact style={{ width: '100%' }}>
              <AutoComplete
                allowClear
                placeholder="SQL语句，如: level = 'ERROR' AND marker.reqType = 'EXECUTE'"
                style={{ width: '100%' }}
                value={sqlInputValue}
                onChange={handleSqlInputChange}
                options={[
                  ...sqlHistory.map((sql) => ({
                    value: sql,
                    label: sql,
                  })),
                  ...LOG_FIELDS.map((field) => ({
                    value: `${field.value} = ''`,
                    label: `${field.label}: ${field.example}`,
                  })),
                ]}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    handleSqlSearch();
                  }
                }}
              />
              {/* <Button icon={<CodeOutlined />} type="primary" onClick={handleSqlSearch} /> */}
            </Space.Compact>
          </div>
          <div className={styles.item}>
            <Button size="small" type="primary" onClick={handleKeywordSearch}>
              搜索
            </Button>
          </div>
          <div className={styles.item}>
            <Space className={styles.space}>
              <Dropdown menu={{ items: templateMenuItems }} trigger={['hover']}>
                <Button type="link" size="small">
                  模板
                </Button>
              </Dropdown>
              {/* <Button
                  type="text"
                  size="small"
                  icon={<TagsOutlined />}
                  onClick={() => setShowFieldSelector(!showFieldSelector)}
                >
                  字段筛选
                </Button> */}
              <Button
                type="link"
                size="small"
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
                    value={timeRange!}
                    presetKey={timeRangePreset ?? undefined}
                    onChange={(range, preset, displayText) => {
                      if (onTimeRangeChange) {
                        onTimeRangeChange(range, preset, displayText);
                        setActiveFilters((prev) => ({ ...prev, time: true }));
                        setShowTimePicker(false);
                      }
                    }}
                    timeGrouping={timeGrouping}
                    onTimeGroupingChange={onTimeGroupingChange}
                  />
                }
                trigger="hover"
                open={showTimePicker}
                onOpenChange={setShowTimePicker}
                placement="bottomRight"
                style={{ width: 'auto', maxWidth: '450px' }}
                arrow={true}
              >
                <Button type="link" size="small">
                  时间范围
                </Button>
              </Popover>
            </Space>
          </div>
        </div>
        {/* 显示过滤条件标签 */}
        {(activeFilters.keywords || activeFilters.sql || activeFilters.time) && filterTags}
      </Card>
    </div>
  );
};

export default SearchBar;
