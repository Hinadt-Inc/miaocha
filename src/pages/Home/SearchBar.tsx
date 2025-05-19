import { useState, useMemo, useEffect, Suspense, lazy } from 'react';
import { AutoComplete, Button, Space, Dropdown, Tag, Popover, Statistic } from 'antd';
import type { MenuProps } from 'antd';
import CountUp from 'react-countup';
// import dayjs from 'dayjs';
// import { LeftOutlined, RightOutlined } from '@ant-design/icons';
import SpinIndicator from '@/components/SpinIndicator';
import styles from './SearchBar.module.less';
import { LOG_FIELDS, LOG_TEMPLATES, QUICK_RANGES } from './utils.ts';

const TimePicker = lazy(() => import('./TimePicker.tsx'));

interface IProps {
  totalCount?: number; // 记录总数
  loading?: boolean; // 是否加载中
  onSearch: (data: ISearchLogsParams) => void; // 搜索回调函数
}

const SearchBar = (props: IProps) => {
  const { totalCount = 0, loading, onSearch } = props;
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

  // 获取默认时间选项配置
  const getDefaultTimeOption = () => {
    const value = 'last_15m'; // 默认时间范围
    const { from, to, format } = QUICK_RANGES[value];
    return {
      range: [from().format(format[0]), to().format(format[1])],
      ...QUICK_RANGES[value],
    } as any;
  };
  const [timeOption, setTimeOption] = useState<ISubmitTime>(getDefaultTimeOption);
  const [openTime, setOpenTime] = useState<boolean>(false); // 显隐浮层

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

          {timeOption?.range?.length === 2 && (
            <Tag color="blue" onClick={() => setOpenTime(true)}>
              {timeOption?.range[0]} ~ {timeOption?.range[1]}
            </Tag>
          )}
        </Space>
      </div>
    );
  }, [keywords, sqls, timeOption]);

  // 当keywords或sqls或时间变化时触发搜索
  useEffect(() => {
    const params: ISearchLogsParams = {
      offset: 0,
    };
    if (keywords.length > 0) {
      params['keywords'] = keywords;
    }
    if (sqls.length > 0) {
      params['whereSqls'] = sqls;
    }

    // 时间
    const { range = [], value = '' } = timeOption || {};
    if (range?.length === 2 && value) {
      params['startTime'] = range[0];
      params['endTime'] = range[1];
    }
    onSearch(params);
  }, [keywords, sqls, timeOption]);

  // 处理关键词搜索
  const handleParams = () => {
    // 保存搜索历史
    const keywordTrim = String(keyword || '')?.trim();
    setKeyword(keywordTrim);
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
    if (sql) {
      if (!sqlHistory.includes(sql)) {
        const newHistory = [sql, ...sqlHistory].slice(0, 10);
        setSqlHistory(newHistory);
        localStorage.setItem('sqlHistory', JSON.stringify(newHistory));
      }
      if (!sqls.includes(sql)) {
        setSqls((prev) => [...prev, sql]);
      }
    }
    setKeyword('');
    setSql('');
  };

  // 查询模板菜单项 - 使用 useMemo 缓存
  const templateMenuItems: MenuProps['items'] = useMemo(
    () => [
      {
        key: 'templates',
        type: 'group',
        label: '常用查询模板',
        // children: LOG_TEMPLATES.map((template, index) => ({
        //   key: `template-${index}`,
        //   label: (
        //     <Tooltip title={template.description}>
        //       <div onClick={() => applyQueryTemplate(template.query)}>{template.name}</div>
        //     </Tooltip>
        //   ),
        // })),
      },
      {
        type: 'divider',
      },
      // {
      //   key: 'saved',
      //   type: 'group',
      //   label: '我的保存查询',
      //   children:
      //     savedQueries.length > 0
      //       ? savedQueries.map((saved, index) => ({
      //           key: `saved-${index}`,
      //           label: <div onClick={() => applyQueryTemplate(saved.query)}>{saved.name}</div>,
      //         }))
      //       : [{ key: 'no-saved', label: '暂无保存的查询', disabled: true }],
      // },
    ],
    [],
  );

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

  // 移动时间范围
  // const moveTime = (direction: 'forward' | 'backward') => {
  //   const { value = '', range, format = [], number, unit } = timeOption;
  //   const start = dayjs(range[0]);
  //   const end = dayjs(range[1]);
  //   const diff = end.diff(start); // 得到毫秒差值

  //   let _start;
  //   let _end;
  //   if (direction === 'forward') {
  //     _start = start.add(diff, 'millisecond');
  //     _end = end.add(diff, 'millisecond');
  //   } else {
  //     _start = start.subtract(diff, 'millisecond');
  //     _end = end.subtract(diff, 'millisecond');
  //   }
  //   let option: ISubmitTime;
  //   // 快速选择
  //   if (QUICK_RANGES[value] && format?.length === 2) {
  //     const times = [_start.format(format[0]), _end.format(format[1])];
  //     const label = times.join(' ~ ');
  //     option = {
  //       label,
  //       format,
  //       value: label,
  //       range: times,
  //     };
  //   } else {
  //     // 绝对时间
  //     const times = [_start.format(format[0]), _end.format(format[1])];
  //     const label = times.join(' ~ ');
  //     option = {
  //       label,
  //       format,
  //       value: label,
  //       range: times,
  //     };
  //   }

  //   setTimeOption((prev) => ({ ...prev, ...option }));
  // };

  // 提交时间范围
  const onSubmitTime = (params: ISubmitTime) => {
    setTimeOption(params);
    setOpenTime(false);
  };

  const timePickerProps = useMemo(() => {
    return {
      onSubmitTime,
    };
  }, [onSubmitTime]);

  // 右侧渲染内容-时间范围
  const timeRender = useMemo(() => {
    return (
      <>
        {/* <Button
          color="primary"
          variant="link"
          size="small"
          disabled={loading}
          className={styles.moveTime}
          onClick={() => moveTime('backward')}
        >
          <LeftOutlined />
        </Button> */}
        <Popover
          arrow={true}
          trigger="click"
          open={openTime}
          onOpenChange={setOpenTime}
          placement="bottomRight"
          content={
            <Suspense fallback={<SpinIndicator />}>
              <TimePicker {...timePickerProps} />
            </Suspense>
          }
        >
          <Button color="primary" variant="link" size="small" disabled={loading}>
            {timeOption.label}
          </Button>
        </Popover>
        {/* <Button
          color="primary"
          variant="link"
          disabled={loading}
          size="small"
          className={styles.moveTime}
          onClick={() => moveTime('forward')}
        >
          <RightOutlined />
        </Button> */}
      </>
    );
  }, [openTime, timeOption, loading]);

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
    );
  }, [sql, sqlHistory]);

  return (
    <div className={styles.searchBar}>
      <div className={styles.top}>
        <div className={styles.left}>{leftRender}</div>
        <div className={styles.right}>
          {/* <Dropdown menu={{ items: templateMenuItems }} trigger={['click']}>
            <Button color="primary" variant="link" size="small">
              模板
            </Button>
          </Dropdown> */}
          {/* <Button color="primary" variant="link" size="small">
            保存查询
          </Button> */}
          {timeRender}
        </div>
      </div>
      <div className={styles.form}>
        <div className={styles.item}>{keywordRender}</div>
        <div className={styles.item}>{sqlRender}</div>
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
