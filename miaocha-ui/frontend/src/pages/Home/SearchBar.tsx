// @ts-ignore
import { useState, useMemo, useEffect, Suspense, lazy, forwardRef, useImperativeHandle, useRef } from 'react';
import { AutoComplete, Button, Space, Tag, Popover, Statistic, Tooltip } from 'antd';
import CountUp from 'react-countup';
import SpinIndicator from '@/components/SpinIndicator';
import SaveSearchButton from './SaveSearchButton';
import SavedSearchesButton from './SavedSearchesButton';
import ShareButton from './ShareButton';
import AutoRefresh from './AutoRefresh/index';
import styles from './SearchBar.module.less';
import { QUICK_RANGES, TIME_GROUP, getLatestTime, DATE_FORMAT_THOUSOND } from './utils';
import { ILogSearchParams, ILogColumnsResponse, ITimeOption } from './types';
import dayjs from 'dayjs';
const TimePicker = lazy(() => import('./TimePicker.tsx'));

interface IProps {
  searchParams: ILogSearchParams; // 搜索参数
  totalCount?: number; // 记录总数
  onSearch: (params: ILogSearchParams) => void; // 搜索回调函数
  onRefresh?: () => void; // 刷新回调函数
  setWhereSqlsFromSider: any; // 设置whereSqlsFromSider
  columns?: ILogColumnsResponse[]; // 字段列表数据
  onSqlsChange?: (sqls: string[]) => void; // SQL列表变化回调函数
  activeColumns?: string[]; // 激活的字段列表
  getDistributionWithSearchBar?: () => void; // 获取字段分布回调函数
  sortConfig?: any[]; // 排序配置
  commonColumns?: string[]; // 普通字段列表（不含有.的字段）
  loading?: boolean; // 加载状态
  keywords: string[]; // 新增
  setKeywords: (k: string[]) => void; // 新增
  sqls: string[]; // 新增
  setSqls: (s: string[]) => void; // 新增
  setWhereSqlsFromSiderArr: any[]; // 新增
  sharedParams?: any; // 分享参数
  hasAppliedSharedParams?: boolean; // 是否已应用分享参数
}

const SearchBar = forwardRef((props: IProps, ref: any) => {
  const {
    loading = false,
    keywords,
    setKeywords,
    sqls,
    setSqls,
    setWhereSqlsFromSiderArr,
    onRefresh,
    sharedParams,
    hasAppliedSharedParams,
  } = props;
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
    sortConfig = [],
    commonColumns = [],
  } = props;

  const [timeGroup, setTimeGroup] = useState<string>('auto'); // 时间分组
  const [activeTab, setActiveTab] = useState('quick'); // 选项卡值
  const [keyword, setKeyword] = useState<string>(''); // 当前输入的关键词
  const [sql, setSql] = useState<string>(''); // sql字符串

  // 查询条件初始化标记
  const [initialized] = useState(true); // 直接设置为true，不需要恢复逻辑

  // 初始化时使用默认值，避免依赖还未传递的 props
  const [timeOption, setTimeOption] = useState<ITimeOption>(() => {
    const { timeRange } = searchParams as any;
    const isQuick = QUICK_RANGES[timeRange];
    if (!isQuick) {
      const defaultRange = QUICK_RANGES['last_15m'];
      return {
        value: 'last_15m',
        range: [defaultRange.from().format(defaultRange.format[0]), defaultRange.to().format(defaultRange.format[1])],
        ...defaultRange,
        type: 'quick',
      } as any;
    }
    const { from, to, format } = isQuick;
    return {
      value: timeRange,
      range: [from().format(format[0]), to().format(format[1])],
      ...QUICK_RANGES[timeRange],
      type: 'quick',
    } as any;
  }); // 时间选项
  const [openTimeRange, setOpenTimeRange] = useState<boolean>(false); // 显隐浮层
  const [openTimeGroup, setOpenTimeGroup] = useState<boolean>(false); // 显隐浮层-时间分组

  // 监听 searchParams 中的时间变化，同步更新 timeOption
  useEffect(() => {
    if (searchParams.startTime && searchParams.endTime) {
      const currentTimeString = `${timeOption?.range?.[0]} ~ ${timeOption?.range?.[1]}`;
      const newTimeString = `${searchParams.startTime} ~ ${searchParams.endTime}`;

      // 如果 searchParams 中的时间与当前 timeOption 不一致，更新 timeOption
      if (currentTimeString !== newTimeString) {
        const newTimeOption = {
          value: searchParams.timeRange || newTimeString,
          range: [searchParams.startTime, searchParams.endTime],
          label:
            searchParams.timeRange && QUICK_RANGES[searchParams.timeRange]
              ? QUICK_RANGES[searchParams.timeRange].label
              : searchParams.timeRange || newTimeString,
          type:
            searchParams.timeType ||
            (searchParams.timeRange && QUICK_RANGES[searchParams.timeRange] ? 'quick' : 'absolute'),
          ...(searchParams.timeType === 'relative' &&
            searchParams.relativeStartOption &&
            searchParams.relativeEndOption && {
              startOption: searchParams.relativeStartOption,
              endOption: searchParams.relativeEndOption,
            }),
        };

        setTimeOption(newTimeOption as any);
      }
    }
  }, [searchParams.startTime, searchParams.endTime, searchParams.timeRange]);

  // 暴露给父组件的方法
  useImperativeHandle(
    ref,
    () => ({
      // 渲染sql
      addSql: (sql: string) => {
        setSqls([...sqls, sql]);
      },
      removeSql: (sql: string) => {
        setSqls(sqls.filter((item: string) => item !== sql));
      },
      // 渲染时间
      setTimeOption,
      // 设置时间分组
      setTimeGroup,
      // 自动刷新方法（供父组件调用）
      autoRefresh: () => {
        // 更新时间到最新
        const latestTime = getLatestTime(timeOption);
        setTimeOption((prev: any) => ({ ...prev, range: [latestTime.startTime, latestTime.endTime] }));
        // 这会触发useEffect，自动调用onSearch
      },
    }),
    [sqls, timeOption],
  );

  // 加载已保存的搜索条件
  const handleLoadSearch = (savedSearchParams: any) => {
    try {
      // 恢复关键词
      if (savedSearchParams.keywords && Array.isArray(savedSearchParams.keywords)) {
        setKeywords(savedSearchParams.keywords);
      } else {
        setKeywords([]);
      }

      // 恢复SQL条件
      if (savedSearchParams.whereSqls && Array.isArray(savedSearchParams.whereSqls)) {
        setSqls(savedSearchParams.whereSqls);
      } else {
        setSqls([]);
      }

      // 恢复时间分组
      if (savedSearchParams.timeGrouping) {
        setTimeGroup(savedSearchParams.timeGrouping);
      }

      // 恢复时间范围
      if (savedSearchParams.startTime && savedSearchParams.endTime && savedSearchParams.timeRange) {
        const timeOption: ITimeOption = {
          value: savedSearchParams.timeRange,
          range: [savedSearchParams.startTime, savedSearchParams.endTime],
          label: QUICK_RANGES[savedSearchParams.timeRange]?.label || '自定义时间',
          type: QUICK_RANGES[savedSearchParams.timeRange] ? 'quick' : 'absolute',
        };
        setTimeOption(timeOption);
      }
    } catch (error) {
      console.error('加载搜索条件失败:', error);
    }
  };

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

  // 处理点击keyword逻辑
  const handleClickSearchBarKeyword = (item: string) => {
    setKeyword(item);
    // 从keywords数组中移除该项
    setKeywords(keywords.filter((keyword: string) => keyword !== item));
  };

  // 处理点击sql逻辑
  const handleClickSearchBarSql = (item: string) => {
    setSql(item);
    // 从sqls数组中移除该项
    setSqls(sqls.filter((sql: string) => sql !== item));
    // 从sider中移除该项
    setWhereSqlsFromSider(setWhereSqlsFromSiderArr.filter((sub: any) => sub.label !== item));
  };

  // 处理删除关键词
  const handleCloseKeyword = (item: string) => {
    setKeywords(keywords.filter((keyword: string) => keyword !== item));
    const latestTime = getLatestTime(timeOption);
    setTimeOption((prev: any) => ({ ...prev, range: [latestTime.startTime, latestTime.endTime] }));
  };

  // 处理删除SQL
  const handleCloseSql = (item: string) => {
    setSqls(sqls.filter((sub: string) => sub !== item));
    setWhereSqlsFromSider(setWhereSqlsFromSiderArr.filter((sub: any) => sub.label !== item));
    const latestTime = getLatestTime(timeOption);
    setTimeOption((prev: any) => ({ ...prev, range: [latestTime.startTime, latestTime.endTime] }));
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
          {[...new Set(sqls)].map((item: string) => (
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
    // 只有在组件初始化完成后才执行搜索逻辑
    if (!initialized) return;
    // 等待 commonColumns 准备好后再执行（页面初始化时需要等待，后端说模块下一定会有普通字段）
    if (commonColumns.length === 0) return;
    const fieldsHasDot = activeColumns?.some((item: any) => item.includes('.'));
    const resSortConfig = sortConfig?.filter((item) => !item.fieldName.includes('.'));
    const params = {
      ...searchParams,
      ...(keywords.length > 0 && { keywords }),
      ...(sqls.length > 0 && { whereSqls: sqls }),
      startTime: dayjs(timeOption?.range?.[0]).format(DATE_FORMAT_THOUSOND),
      endTime: dayjs(timeOption?.range?.[1]).format(DATE_FORMAT_THOUSOND),
      timeRange: timeOption?.value,
      timeType: timeOption?.type, // 添加时间类型信息
      ...(timeOption?.type === 'relative' &&
        timeOption?.startOption &&
        timeOption?.endOption && {
          relativeStartOption: timeOption.startOption,
          relativeEndOption: timeOption.endOption,
        }),
      timeGrouping: timeGroup,
      offset: 0,
      fields: fieldsHasDot ? [...commonColumns, ...(activeColumns || [])] : commonColumns,
      sortFields: resSortConfig || [],
    };
    if (keywords.length === 0) {
      delete params.keywords;
    }
    if (sqls.length === 0) {
      delete params.whereSqls;
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
  }, [timeOption, timeGroup, activeColumns, sortConfig, onSqlsChange, initialized, commonColumns, keywords, sqls]);

  // 处理关键词和SQL搜索
  const handleSubmit = () => {
    const keywordTrim = String(keyword || '')?.trim();
    const sqlTrim = String(sql || '')?.trim();

    // 添加到关键词列表
    if (keywordTrim && !keywords.includes(keywordTrim)) {
      setKeywords([...keywords, keywordTrim]);
    }

    // 添加到SQL列表
    if (sqlTrim && !sqls.includes(sqlTrim)) {
      setSqls([...sqls, sqlTrim]);
    }

    // 清空输入框
    setKeyword('');
    setSql('');

    const latestTime = getLatestTime(timeOption);
    setTimeOption((prev: any) => ({ ...prev, range: [latestTime.startTime, latestTime.endTime] }));
  };

  // 左侧渲染内容
  const leftRender = useMemo(() => {
    return (
      <Space>
        找到
        <Statistic
          value={totalCount}
          formatter={(value: any) => <CountUp end={Number(value)} duration={1} separator="," />}
        />
        条记录
      </Space>
    );
  }, [totalCount]);

  // 处理自动刷新
  const handleAutoRefresh = () => {
    if (onRefresh) {
      // 自动刷新时，直接调用父组件的onRefresh方法
      // 父组件会通过ref调用SearchBar的autoRefresh方法来更新时间
      onRefresh();
    }
  };

  // 提交时间范围
  const submitTime = (params: ILogTimeSubmitParams) => {
    // 转换 ILogTimeSubmitParams 到 ITimeOption
    const timeOption: ITimeOption = {
      value: params.value || '',
      range: [params.range?.[0] || '', params.range?.[1] || ''],
      label: params.label || '',
      type: params.type || 'absolute',
      startOption: params.startOption,
      endOption: params.endOption,
    };
    setTimeOption(timeOption);
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
            <TimePicker
              activeTab={activeTab}
              setActiveTab={setActiveTab}
              onSubmit={submitTime}
              currentTimeOption={timeOption}
            />
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
          options={[]}
        />
      </Space.Compact>
    );
  }, [keyword]);

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
  }, [sql, columns]);

  const changeTimeGroup = (text: string) => {
    const latestTime = getLatestTime(timeOption);
    setTimeGroup(text);
    setTimeOption((prev: any) => ({ ...prev, range: [latestTime.startTime, latestTime.endTime] }));
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
      <div className={styles.top}>
        <div className={styles.left}>{leftRender}</div>
        <div className={styles.right}>
          <Space size={8}>
            <AutoRefresh onRefresh={handleAutoRefresh} loading={loading} disabled={false} />
            <SaveSearchButton
              searchParams={{
                keywords,
                whereSqls: sqls,
                timeRange: timeOption.value,
                startTime: timeOption.range?.[0],
                endTime: timeOption.range?.[1],
                timeGrouping: timeGroup,
                module: searchParams.module,
                sortConfig,
                timeType: timeOption?.type, // 添加时间类型信息
                ...(timeOption?.type === 'relative' &&
                  timeOption?.startOption &&
                  timeOption?.endOption && {
                    relativeStartOption: timeOption.startOption,
                    relativeEndOption: timeOption.endOption,
                  }),
              }}
              size="small"
            />
            <SavedSearchesButton onLoadSearch={handleLoadSearch} size="small" />
            <ShareButton
              searchParams={{
                keywords,
                whereSqls: sqls,
                timeRange: timeOption.value,
                startTime: timeOption.range?.[0],
                endTime: timeOption.range?.[1],
                timeGrouping: timeGroup,
                module: searchParams.module,
                sortConfig,
                timeType: timeOption?.type, // 添加时间类型信息
                ...(timeOption?.type === 'relative' &&
                  timeOption?.startOption &&
                  timeOption?.endOption && {
                    relativeStartOption: timeOption.startOption,
                    relativeEndOption: timeOption.endOption,
                  }),
              }}
              size="small"
            />
            {timeGroupRender}
            {timeRender}
          </Space>
        </div>
      </div>
      <div className={styles.form}>
        <div className={styles.item}>{keywordRender}</div>
        <div className={styles.item}>{sqlRender}</div>
        <div className={styles.item}>
          <Button size="small" type="primary" onClick={handleSubmit} loading={loading}>
            搜索
          </Button>
        </div>
      </div>
      <div className={styles.footer}>{filterRender}</div>
    </div>
  );
});

export default SearchBar;
