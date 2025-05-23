import { useState, useMemo, useEffect, Suspense, lazy, forwardRef, useImperativeHandle, useRef } from 'react';
import { AutoComplete, Button, Space, Tag, Popover, Statistic } from 'antd';
import CountUp from 'react-countup';
import SpinIndicator from '@/components/SpinIndicator';
import styles from './SearchBar.module.less';
import { LOG_FIELDS, QUICK_RANGES, TIME_GROUP } from './utils.ts';

const TimePicker = lazy(() => import('./TimePicker.tsx'));

interface IProps {
  searchParams: ILogSearchParams; // 搜索参数
  totalCount?: number; // 记录总数
  loading?: boolean; // 是否加载中
  onSubmit: (params: ILogSearchParams) => void; // 搜索回调函数
}

const SearchBar = forwardRef((props: IProps, ref) => {
  const searchBarRef = useRef<HTMLDivElement>(null);

  const { searchParams, totalCount = 0, loading, onSubmit } = props;
  const [keyword, setKeyword] = useState<string>(''); // 关键词
  const [keywords, setKeywords] = useState<string[]>([]); // 关键词列表
  const [keywordHistory, setKeywordHistory] = useState<string[]>(() => {
    const saved = localStorage.getItem('keywordHistory');
    return saved ? JSON.parse(saved) : [];
  });
  const [sql, setSql] = useState<string>(''); // sql
  const [sqls, setSqls] = useState<string[]>([]); // sql列表
  const [sqlHistory, setSqlHistory] = useState<string[]>(() => {
    const saved = localStorage.getItem('sqlHistory');
    return saved ? JSON.parse(saved) : [];
  });

  useImperativeHandle(ref, () => ({
    // 渲染sql
    renderSql: (sql: string) => {
      setSqls((prev) => [...prev, sql]);
    },
  }));

  // 获取默认时间选项配置
  const getDefaultTimeOption = () => {
    const { timeRange } = searchParams as any;
    const isQucik = QUICK_RANGES[timeRange];
    if (!isQucik) return {};
    const { from, to, format } = isQucik;
    return {
      value: timeRange,
      range: [from().format(format[0]), to().format(format[1])],
      ...QUICK_RANGES[timeRange],
    } as any;
  };
  const [timeOption, setTimeOption] = useState<ILogTimeSubmitParams>(getDefaultTimeOption); // 时间选项
  const [openTimeRange, setOpenTimeRange] = useState<boolean>(false); // 显隐浮层
  const [openTimeGroup, setOpenTimeGroup] = useState<boolean>(false); // 显隐浮层-时间分组

  // 处理搜索输入变化
  const changeKeyword = (value: string) => {
    setKeyword(value || '');
  };

  // 处理sql输入变化
  const changeSql = (value: string) => {
    setSql(value || '');
  };

  // 显示关键字、sql、时间的标签
  const filterRender = useMemo(() => {
    const { range = [] } = timeOption;
    return (
      <div className={styles.filter}>
        <Space wrap>
          {keywords.map((item: string) => (
            <Tag
              key={item}
              color="purple"
              closable
              onClick={() => setKeyword(item)}
              onClose={() => setKeywords((prev) => prev.filter((k) => k !== item))}
            >
              {item}
            </Tag>
          ))}
          {sqls.map((item: string) => (
            <Tag
              key={item}
              color="success"
              closable
              onClick={() => setSql(item)}
              onClose={() => setSqls((prev) => prev.filter((k) => k !== item))}
            >
              {item}
            </Tag>
          ))}

          {/* 时间范围 */}
          {range.length === 2 && (
            <Tag color="blue" onClick={() => setOpenTimeRange(true)}>
              {range[0]} ~ {range[1]}
            </Tag>
          )}
        </Space>
      </div>
    );
  }, [keywords, sqls, timeOption]);

  // 当keywords或sqls或时间变化时触发搜索
  useEffect(() => {
    const params = {
      ...searchParams,
      ...(keywords.length > 0 && { keywords }),
      ...(sqls.length > 0 && { whereSqls: sqls }),
      startTime: timeOption?.range?.[0],
      endTime: timeOption?.range?.[1],
      offset: 0,
    };
    console.log('【打印日志】,params =======>', params);
    if (keywords.length === 0) {
      delete params.keywords;
    }
    if (sqls.length === 0) {
      delete params.whereSqls;
    }
    onSubmit(params as ILogSearchParams);
  }, [keywords, sqls, timeOption]);

  // 处理关键词搜索
  const handleParams = () => {
    // 保存搜索历史
    const keywordTrim = String(keyword || '')?.trim();
    if (keywordTrim) {
      if (!keywordHistory.includes(keywordTrim)) {
        const newHistory = [keywordTrim, ...keywordHistory].slice(0, 10);
        setKeywordHistory(newHistory);
        localStorage.setItem('keywordHistory', JSON.stringify(newHistory));
      }
      if (!keywords.includes(keywordTrim)) {
        setKeywords((prev) => [...prev, keywordTrim]);
      }
    }

    // 保存SQL历史
    const sqlTrim = String(sql || '')?.trim();
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
    setKeyword('');
    setSql('');

    if (!keywordTrim && !sql) {
      onSubmit({ ...searchParams, offset: 0 } as any);
    }
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
            <TimePicker onSubmit={submitTime} />
          </Suspense>
        }
      >
        <Button color="primary" variant="link" size="small" disabled={loading}>
          {timeOption.label}
        </Button>
      </Popover>
    );
  }, [openTimeRange, timeOption, loading]);

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
    return (
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
                  <Tag>{item.label}:</Tag>
                  <Tag>{item.example}</Tag>
                </>
              ),
            })),
          ]}
        />
      </Space.Compact>
    );
  }, [sql, sqlHistory]);

  const changeTimeGroup = (text: string) => {
    onSubmit({
      ...searchParams,
      timeGrouping: text,
      offset: 0,
    } as any);
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
        <Button color="primary" variant="link" size="small" disabled={loading}>
          按{TIME_GROUP[timeGrouping]}分组
        </Button>
      </Popover>
    );
  }, [searchParams, openTimeGroup, loading]);

  return (
    <div className={styles.searchBar} ref={searchBarRef}>
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
          <Button size="small" type="primary" onClick={handleParams} loading={loading}>
            搜索
          </Button>
        </div>
      </div>
      <div className={styles.footer}>{filterRender}</div>
    </div>
  );
});

export default SearchBar;
