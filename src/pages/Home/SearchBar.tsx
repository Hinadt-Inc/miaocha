import { useState, useCallback, useMemo, useEffect } from 'react';
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
  Statistic,
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

const TimePicker = lazy(() => import('./TimePicker.tsx'));

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
  { key: 'last_5m', label: '最近5分钟' },
  { key: 'last_15m', label: '最近15分钟' },
  { key: 'last_30m', label: '最近30分钟' },
  { key: 'last_1h', label: '最近1小时' },
  { key: 'last_8h', label: '最近8小时' },
  { key: 'last_24h', label: '最近24小时' },
  { key: 'last_7d', label: '最近7天' },
  { key: 'today', label: '今天' },
  { key: 'yesterday', label: '昨天' },
  { key: 'last_week', label: '本周' },
];

interface IProps {
  totalCount?: number;
  onSearch: (data: ISearchLogsParams) => void;
  // searchQuery: string;
  // whereSql: string;
  // timeRange?: [string, string] | null;
  // timeRangePreset?: string | null;
  // timeDisplayText?: string;
  // timeGrouping?: 'minute' | 'hour' | 'day' | 'month';
  // onSearch: (query: string) => void;
  // onWhereSqlChange: (sql: string) => void;

  // onSubmitSql: () => void;
  // onTimeRangeChange?: (range: [string, string], preset?: string, displayText?: string) => void;
  // onOpenTimeSelector?: () => void;
  // onTimeGroupingChange?: (grouping: 'minute' | 'hour' | 'day' | 'month') => void;
}

const SearchBar = (props: IProps) => {
  const {
    totalCount = 0,
    onSearch,
    // todo
    searchQuery,
    whereSql,
    timeRange,
    timeRangePreset,
    timeDisplayText,
    timeGrouping,
    onWhereSqlChange,
    onSubmitSql,
    onTimeRangeChange,
    onTimeGroupingChange,
  } = props;

  // 搜索关键词
  const [keyword, setKeyword] = useState<string>('');
  const [keywordList, setKeywordList] = useState<string[]>([]);
  // sql
  const [sql, setSql] = useState<string>('');
  const [sqlList, setSqlList] = useState<string[]>([]);

  // 历史记录
  const [keywordHistory, setKeywordHistory] = useState<string[]>(() => {
    const saved = localStorage.getItem('keywordHistory');
    return saved ? JSON.parse(saved) : [];
  });
  const [sqlHistory, setSqlHistory] = useState<string[]>(() => {
    const saved = localStorage.getItem('sqlHistory');
    return saved ? JSON.parse(saved) : [];
  });

  // 处理搜索输入变化
  const changeKeyword = (value: string) => {
    setKeyword((value || '')?.trim());
  };

  const changeSql = (value: string) => {
    setSql((value || '')?.trim());
  };

  useEffect(() => {
    const params: ISearchLogsParams = {
      offset: 0,
    };
    if (keywordList.length > 0) {
      params['keywords'] = keywordList.join(' ');
    }
    if (sqlList.length > 0) {
      params['whereSqls'] = sqlList.join(' AND ');
    }
    onSearch(params);
  }, [keywordList, sqlList]);

  // 处理关键词搜索
  const handleParams = () => {
    // 保存搜索历史
    if (keyword) {
      if (!keywordHistory.includes(keyword)) {
        const newHistory = [keyword, ...keywordHistory].slice(0, 10);
        setKeywordHistory(newHistory);
        localStorage.setItem('keywordHistory', JSON.stringify(newHistory));
      }
      if (!keywordList.includes(keyword)) {
        setKeywordList((prev) => [...prev, keyword]);
      }
    }

    // 保存SQL历史
    if (sql) {
      if (!sqlHistory.includes(sql)) {
        const newHistory = [sql, ...sqlHistory].slice(0, 10);
        setSqlHistory(newHistory);
        localStorage.setItem('sqlHistory', JSON.stringify(newHistory));
      }
      if (!sqlList.includes(sql)) {
        setSqlList((prev) => [...prev, sql]);
      }
    }
    setKeyword('');
    setSql('');
  };

  // 显示过滤标签 - 使用 useMemo 缓存复杂渲染逻辑
  const filterRender = useMemo(() => {
    if (keywordList.length === 0 && sqlList.length === 0) {
      return null;
    }
    return (
      <div className={styles.filter}>
        <Space wrap>
          {keywordList.map((item: string) => (
            <Tag
              key={item}
              color="purple"
              closable
              onClose={() => {
                setKeywordList((prev) => prev.filter((k) => k !== item));
              }}
            >
              <span className={styles.checkableTag} onClick={() => setKeyword(item)}>
                {item}
              </span>
            </Tag>
          ))}
          {sqlList.map((item: string) => (
            <Tag
              key={item}
              color="success"
              closable
              onClose={() => {
                setSqlList((prev) => prev.filter((k) => k !== item));
              }}
            >
              <span className={styles.checkableTag} onClick={() => setSql(item)}>
                {item}
              </span>
            </Tag>
          ))}

          {/* {timeRange && activeFilters.time && (
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
          )} */}
        </Space>
      </div>
    );
  }, [
    keywordList,
    sqlList,
    // handleSubmit,
    // sqlInputValue,
    // timeRange,
    // activeFilters,
    // onSearch,
    // onWhereSqlChange,
    // onSearch,
    // onSubmitSql,
    // onTimeRangeChange,
    // timeDisplayText,
    // getTimeRangeDisplayText,
    // onTimeGroupingChange,
  ]);

  // 旧

  const [savedQueries, setSavedQueries] = useState<{ name: string; query: string }[]>(() => {
    const saved = localStorage.getItem('savedQueries');
    return saved ? (JSON.parse(saved) as { name: string; query: string }[]) : [];
  });

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

  return (
    <div className={styles.searchBar}>
      <div className={styles.top}>
        <div className={styles.left}>
          <Space>
            找到
            <b>
              <Statistic value={totalCount} />
            </b>
            条记录
          </Space>
        </div>
        <div className={styles.right}>
          <Dropdown menu={{ items: templateMenuItems }} trigger={['click']}>
            <Button type="link" size="small">
              模板
            </Button>
          </Dropdown>
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
          <Popover
            trigger="click"
            content={
              <TimePicker
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
            open={showTimePicker}
            onOpenChange={setShowTimePicker}
            placement="bottomRight"
            style={{ width: 'auto', maxWidth: '450px' }}
            arrow={true}
          >
            <Button type="link" size="small">
              {timeDisplayText ??
                (timeRangePreset
                  ? (TIME_PRESETS.find((p) => p.key === timeRangePreset)?.label ?? '时间范围')
                  : '时间范围')}
            </Button>
          </Popover>
        </div>
      </div>
      <div className={styles.form}>
        <div className={styles.item}>
          <Space.Compact style={{ width: '100%' }}>
            <AutoComplete
              allowClear
              placeholder="输入关键词搜索"
              style={{ width: '100%' }}
              value={keyword}
              onChange={changeKeyword}
              options={keywordHistory.map((item: string) => ({
                value: item,
                label: item,
              }))}
            />
          </Space.Compact>
        </div>
        <div className={styles.item}>
          <Space.Compact style={{ width: '100%' }}>
            <AutoComplete
              allowClear
              placeholder="WHERE子句，例如: level = 'ERROR' AND marker.reqType = 'EXECUTE'"
              style={{ width: '100%' }}
              value={sql}
              onChange={changeSql}
              options={[
                ...sqlHistory.map((item: string) => ({
                  value: item,
                  label: item,
                })),
                ...LOG_FIELDS.map((item: IStatus) => ({
                  value: `${item.value} = ''`,
                  label: (
                    <>
                      <Tag bordered={false} color="processing">
                        {item.label}:
                      </Tag>
                      <Tag bordered={false} color="success">
                        {item.example}
                      </Tag>
                    </>
                  ),
                })),
              ]}
            />
          </Space.Compact>
        </div>
        <div className={styles.item}>
          <Button size="small" type="primary" onClick={handleParams}>
            搜索
          </Button>
        </div>
      </div>

      <div className={styles.footer}>{filterRender}</div>
    </div>
  );
};

export default SearchBar;
