import { useState, useMemo, useEffect, Suspense, lazy, forwardRef, useImperativeHandle, useRef } from 'react';
import { AutoComplete, Button, Space, Tag, Popover, Statistic, Tooltip, message } from 'antd';
import CountUp from 'react-countup';
import SpinIndicator from '@/components/SpinIndicator';
import styles from './SearchBar.module.less';
import { QUICK_RANGES, TIME_GROUP, getLatestTime, DATE_FORMAT_THOUSOND } from './utils';
import dayjs from 'dayjs';
const TimePicker = lazy(() => import('./TimePicker.tsx'));

interface IProps {
  searchParams: ILogSearchParams; // 搜索参数
  totalCount?: number; // 记录总数
  onSearch: (params: ILogSearchParams) => void; // 搜索回调函数
  setWhereSqlsFromSider: any; // 设置whereSqlsFromSider
  columns?: ILogColumnsResponse[]; // 字段列表数据
  onSqlsChange?: (sqls: string[]) => void; // SQL列表变化回调函数
  activeColumns?: string[]; // 激活的字段列表
  getDistributionWithSearchBar?: () => void; // 获取字段分布回调函数
  selectedModule?: string; // 当前选中的模块
  moduleQueryConfig?: any; // 模块查询配置
  sortConfig?: any[]; // 排序配置
}

const SearchBar = forwardRef((props: IProps, ref) => {
  const searchBarRef = useRef<HTMLDivElement>(null);
  const {
    searchParams,
    totalCount = 0,
    onSearch,
    setWhereSqlsFromSider,
    columns,
    onSqlsChange,
    activeColumns,
    getDistributionWithSearchBar,
    selectedModule,
    moduleQueryConfig,
    sortConfig = [],
  } = props;
  const [messageApi, contextHolder] = message.useMessage();

  const [timeGroup, setTimeGroup] = useState<string>('auto'); // 时间分组
  const [activeTab, setActiveTab] = useState('quick'); // 选项卡值
  const [keywords, setKeywords] = useState<string[]>([]); // 关键词列表
  const [keyword, setKeyword] = useState<string>(''); // 当前输入的关键词
  const [keywordHistory, setKeywordHistory] = useState<string[]>(() => {
    const saved = localStorage.getItem('keywordHistory');
    return saved ? JSON.parse(saved) : [];
  });

  const [sqls, setSqls] = useState<string[]>([]); // sql列表
  const [sql, setSql] = useState<string>(''); // sql字符串

  const [sqlHistory, setSqlHistory] = useState<string[]>(() => {
    const saved = localStorage.getItem('sqlHistory');
    return saved ? JSON.parse(saved) : [];
  });

  // 添加查询条件恢复功能
  const [initialized, setInitialized] = useState(false);

  // 在组件初始化时恢复之前的查询条件
  useEffect(() => {
    if (!initialized) {
      const savedSearchParams = localStorage.getItem('searchBarParams');
      if (savedSearchParams) {
        try {
          const params = JSON.parse(savedSearchParams);

          // 恢复关键词
          if (params.keywords && Array.isArray(params.keywords)) {
            setKeywords(params.keywords);
          }

          // 恢复SQL条件
          if (params.whereSqls && Array.isArray(params.whereSqls)) {
            setSqls(params.whereSqls);
          }

          // 恢复时间分组
          if (params.timeGrouping) {
            setTimeGroup(params.timeGrouping);
          }

          // 恢复时间范围
          if (params.startTime && params.endTime && params.timeRange) {
            const timeOption = {
              value: params.timeRange,
              range: [params.startTime, params.endTime],
              label: QUICK_RANGES[params.timeRange]?.label || '自定义时间',
              type: QUICK_RANGES[params.timeRange] ? ('quick' as const) : ('absolute' as const),
            };
            setTimeOption(timeOption);
          }
        } catch (error) {
          console.error('恢复查询条件失败:', error);
        }
      }
      setInitialized(true);
    }
  }, [initialized]);

  // 暴露给父组件的方法
  useImperativeHandle(ref, () => ({
    // 渲染sql
    addSql: (sql: string) => {
      setSqls((prev) => [...prev, sql]);
    },
    removeSql: (sql: string) => {
      setSqls((prev) => prev.filter((item) => item !== sql));
    },
    // 渲染时间
    setTimeOption,
  }));

  // 获取默认时间选项配置
  const getDefaultTimeOption = () => {
    const { timeRange } = searchParams as any;
    const isQuick = QUICK_RANGES[timeRange];
    if (!isQuick) return {};
    const { from, to, format } = isQuick;
    return {
      value: timeRange,
      range: [from().format(format[0]), to().format(format[1])],
      ...QUICK_RANGES[timeRange],
      type: 'quick',
    } as any;
  };
  const [timeOption, setTimeOption] = useState<ILogTimeSubmitParams>(getDefaultTimeOption); // 时间选项
  const [openTimeRange, setOpenTimeRange] = useState<boolean>(false); // 显隐浮层
  const [openTimeGroup, setOpenTimeGroup] = useState<boolean>(false); // 显隐浮层-时间分组

  // 处理关键词输入变化
  const changeKeyword = (value: string) => {
    setKeyword(value || '');
  };

  // 处理sql输入变化
  const changeSql = (value: string) => {
    setSql(value || '');
  };

  const handleTimeFromTag = () => {
    setOpenTimeRange(true);
  };

  // 清除本地存储中的关键词
  const clearKeywordFromLocalStorage = (item: string) => {
    try {
      const savedParams = localStorage.getItem('searchBarParams');
      if (savedParams) {
        const params = JSON.parse(savedParams);
        params.keywords = params.keywords.filter((k: string) => k !== item);
        localStorage.setItem('searchBarParams', JSON.stringify(params));
      }
    } catch (error) {
      console.error('清除关键词本地存储失败:', error);
    }
  };

  // 清除本地存储中的SQL
  const clearSqlFromLocalStorage = (item: string) => {
    try {
      const savedParams = localStorage.getItem('searchBarParams');
      if (savedParams) {
        const params = JSON.parse(savedParams);
        params.whereSqls = params.whereSqls.filter((s: string) => s !== item);
        localStorage.setItem('searchBarParams', JSON.stringify(params));
      }
    } catch (error) {
      console.error('清除SQL本地存储失败:', error);
    }
  };

  // 处理点击keyword逻辑
  const handleClickSearchBarKeyword = (item: string) => {
    setKeyword(item);
    clearKeywordFromLocalStorage(item);
    // 从keywords数组中移除该项
    setKeywords((prev) => prev.filter((keyword) => keyword !== item));
  };

  // 处理点击sql逻辑
  const handleClickSearchBarSql = (item: string) => {
    setSql(item);
    clearSqlFromLocalStorage(item);
    // 从sqls数组中移除该项
    setSqls((prev) => prev.filter((sql) => sql !== item));
    // 从sider中移除该项
    setWhereSqlsFromSider((prev: any) => prev.filter((sub: any) => sub.label !== item));
  };

  // 处理删除关键词
  const handleCloseKeyword = (item: string) => {
    setKeywords((prev) => prev.filter((keyword) => keyword !== item));
    clearKeywordFromLocalStorage(item);
    const latestTime = getLatestTime(timeOption);
    setTimeOption((prev) => ({ ...prev, range: [latestTime.startTime, latestTime.endTime] }));
  };

  // 处理删除SQL
  const handleCloseSql = (item: string) => {
    setSqls((prev) => prev.filter((sub) => sub !== item));
    clearSqlFromLocalStorage(item);
    setWhereSqlsFromSider((prev: any) => prev.filter((sub: any) => sub.label !== item));
    const latestTime = getLatestTime(timeOption);
    setTimeOption((prev) => ({ ...prev, range: [latestTime.startTime, latestTime.endTime] }));
  };

  // 显示关键词、sql、时间的标签
  const filterRender = useMemo(() => {
    const { range = [] } = timeOption;
    return (
      <div className={styles.filter}>
        <Space wrap>
          {keywords.map((item: string) => (
            <Tooltip key={item} placement="topLeft" title={item}>
              <Tag
                color="orange"
                closable
                onClick={() => handleClickSearchBarKeyword(item)}
                onClose={() => handleCloseKeyword(item)}
              >
                <span className="tagContent">{item}</span>
              </Tag>
            </Tooltip>
          ))}
          {sqls.map((item: string) => (
            <Tooltip key={item} placement="topLeft" title={item}>
              <Tag
                color="success"
                closable
                onClick={() => handleClickSearchBarSql(item)}
                onClose={() => handleCloseSql(item)}
              >
                <span className="tagContent">{item}</span>
              </Tag>
            </Tooltip>
          ))}
          {/* 时间范围 */}
          {range.length === 2 && (
            <Tag color="blue" onClick={handleTimeFromTag}>
              {range[0]} ~ {range[1]}
            </Tag>
          )}
        </Space>
      </div>
    );
  }, [sqls, keywords, timeOption]);

  // 当关键词、sqls或时间变化时触发搜索
  useEffect(() => {
    // 只有在组件初始化完成后才执行搜索和保存逻辑
    if (!initialized) return;
    const fieldsHasDot = activeColumns?.some((item: any) => item.includes('.'));
    const resSortConfig = sortConfig?.filter((item) => !item.fieldName.includes('.'));
    const params = {
      ...searchParams,
      ...(keywords.length > 0 && { keywords }),
      ...(sqls.length > 0 && { whereSqls: sqls }),
      startTime: dayjs(timeOption?.range?.[0]).format(DATE_FORMAT_THOUSOND),
      endTime: dayjs(timeOption?.range?.[1]).format(DATE_FORMAT_THOUSOND),
      timeRange: timeOption?.value,
      timeGrouping: timeGroup,
      offset: 0,
      fields: fieldsHasDot ? activeColumns : [],
      sortFields: resSortConfig || [],
    };
    if (keywords.length === 0) {
      delete params.keywords;
    }
    if (sqls.length === 0) {
      delete params.whereSqls;
    }

    // 保存查询条件到本地存储
    try {
      const searchParamsToSave = {
        keywords: keywords || [],
        whereSqls: params.whereSqls || [],
        startTime: params.startTime,
        endTime: params.endTime,
        timeRange: params.timeRange,
        timeGrouping: params.timeGrouping,
        fields: params.fields,
      };
      localStorage.setItem('searchBarParams', JSON.stringify(searchParamsToSave));
    } catch (error) {
      console.error('保存查询条件失败:', error);
    }
    onSearch(params as ILogSearchParams);

    // 通知父组件sqls数据变化
    if (onSqlsChange) {
      onSqlsChange(sqls);
    }

    // 调用Sider组件的getDistributionWithSearchBar方法
    if (getDistributionWithSearchBar) {
      getDistributionWithSearchBar();
    }
  }, [timeOption, timeGroup, activeColumns, sortConfig, onSqlsChange, initialized]);

  // 处理关键词和SQL搜索
  const handleSubmit = () => {
    const keywordTrim = String(keyword || '')?.trim();
    const sqlTrim = String(sql || '')?.trim();

    // 保存关键词搜索历史
    if (keywordTrim) {
      if (!keywordHistory.includes(keywordTrim)) {
        const newHistory = [keywordTrim, ...keywordHistory].slice(0, 10);
        setKeywordHistory(newHistory);
        localStorage.setItem('keywordHistory', JSON.stringify(newHistory));
      }
      // 添加到关键词列表
      if (!keywords.includes(keywordTrim)) {
        setKeywords((prev) => [...prev, keywordTrim]);
      }
    }

    // 保存SQL历史
    if (sqlTrim) {
      if (!sqlHistory.includes(sqlTrim)) {
        const newHistory = [sqlTrim, ...sqlHistory].slice(0, 10);
        setSqlHistory(newHistory);
        localStorage.setItem('sqlHistory', JSON.stringify(newHistory));
      }
      if (!sqls.includes(sqlTrim)) {
        setSqls((prev) => [...prev, sqlTrim]);
      }
    }

    // 清空输入框
    setKeyword('');
    setSql('');

    const latestTime = getLatestTime(timeOption);
    setTimeOption((prev) => ({ ...prev, range: [latestTime.startTime, latestTime.endTime] }));
  };

  // 左侧渲染内容
  const leftRender = useMemo(() => {
    return (
      <Space>
        找到
        <Statistic
          value={totalCount}
          formatter={(value) => <CountUp end={Number(value)} duration={1} separator="," />}
        />
        条记录
      </Space>
    );
  }, [totalCount]);

  // 提交时间范围
  const submitTime = (params: ILogTimeSubmitParams) => {
    setTimeOption(params);
    setOpenTimeRange(false);
  };

  // 右侧渲染内容-时间范围
  const timeRender = useMemo(() => {
    return (
      <Popover
        arrow={true}
        trigger="click"
        open={openTimeRange}
        onOpenChange={setOpenTimeRange}
        placement="bottomRight"
        content={
          <Suspense fallback={<SpinIndicator />}>
            <TimePicker activeTab={activeTab} setActiveTab={setActiveTab} onSubmit={submitTime} />
          </Suspense>
        }
      >
        <Button color="primary" variant="link" size="small">
          {timeOption.label}
        </Button>
      </Popover>
    );
  }, [openTimeRange, timeOption.label, activeTab]);

  // 渲染关键词搜索输入框，包含历史搜索记录自动补全功能
  const keywordRender = useMemo(() => {
    return (
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
    );
  }, [keywordHistory, keyword]);

  // 渲染SQL查询输入框，包含历史SQL查询记录和常用字段模板
  const sqlRender = useMemo(() => {
    // 获取当前正在输入的词汇，用于字段匹配
    const getCurrentInputWord = (inputValue: string) => {
      if (!inputValue) return '';

      // 按空格分割，获取最后一个词汇
      const words = inputValue.split(/\s+/);
      const lastWord = words[words.length - 1] || '';

      // 如果最后一个词汇包含操作符，提取字段名部分
      const operatorMatch = lastWord.match(/^([a-zA-Z_][a-zA-Z0-9_.]*)/);
      return operatorMatch ? operatorMatch[1] : lastWord;
    };

    // 根据当前输入的词汇筛选字段
    const currentWord = getCurrentInputWord(sql);
    const filteredColumns = currentWord
      ? (columns || []).filter(
          (column) => column.columnName && column.columnName.toLowerCase().includes(currentWord.toLowerCase()),
        )
      : columns || [];

    return (
      <Space.Compact style={{ width: '100%' }}>
        <AutoComplete
          allowClear
          placeholder="WHERE子句，例如: level = 'ERROR' AND marker.reqType = 'EXECUTE'"
          style={{ width: '100%' }}
          value={sql}
          onChange={changeSql}
          options={[
            ...filteredColumns.map((item: ILogColumnsResponse) => ({
              value: (() => {
                // 智能替换/拼接逻辑
                if (!item.columnName) return sql || '';

                // 如果当前输入为空，直接返回字段名
                if (!sql) return item.columnName;

                // 如果当前有匹配的词汇，说明用户正在输入字段名，需要替换
                if (currentWord) {
                  const words = sql.split(/\s+/);
                  words[words.length - 1] = item.columnName;
                  return words.join(' ');
                }

                // 如果没有匹配词汇但输入以空格结尾，直接拼接
                if (sql.endsWith(' ')) {
                  return sql + item.columnName;
                }

                // 否则在当前输入和字段名之间添加空格拼接
                return sql + ' ' + item.columnName;
              })(),
              label: (
                <>
                  {item.columnName}
                  {/* <Tag>{item.dataType}</Tag> */}
                </>
              ),
            })),
          ]}
        />
      </Space.Compact>
    );
  }, [sql, sqlHistory, columns]);

  const changeTimeGroup = (text: string) => {
    const latestTime = getLatestTime(timeOption);
    setTimeGroup(text);
    setTimeOption((prev) => ({ ...prev, range: [latestTime.startTime, latestTime.endTime] }));
    setOpenTimeGroup(false);
  };

  // 渲染时间分组选择框
  const timeGroupRender = useMemo(() => {
    const { timeGrouping } = searchParams as any;
    return (
      <Popover
        arrow={true}
        trigger="click"
        placement="bottomRight"
        open={openTimeGroup}
        onOpenChange={setOpenTimeGroup}
        content={
          <>
            {Object.entries(TIME_GROUP).map(([value, item]) => (
              <Tag.CheckableTag key={value} checked={timeGrouping === value} onChange={() => changeTimeGroup(value)}>
                {item}
              </Tag.CheckableTag>
            ))}
          </>
        }
      >
        <Button color="primary" variant="link" size="small">
          按{TIME_GROUP[timeGrouping]}分组
        </Button>
      </Popover>
    );
  }, [searchParams.timeGrouping, openTimeGroup, timeOption]);

  return (
    <div className={styles.searchBar} ref={searchBarRef}>
      {contextHolder}
      <div className={styles.top}>
        <div className={styles.left}>{leftRender}</div>
        <div className={styles.right}>
          {timeGroupRender}
          {timeRender}
        </div>
      </div>
      <div className={styles.form}>
        <div className={styles.item}>{keywordRender}</div>
        <div className={styles.item}>{sqlRender}</div>
        <div className={styles.item}>
          <Button size="small" type="primary" onClick={handleSubmit}>
            搜索
          </Button>
        </div>
      </div>
      <div className={styles.footer}>{filterRender}</div>
    </div>
  );
});

export default SearchBar;
