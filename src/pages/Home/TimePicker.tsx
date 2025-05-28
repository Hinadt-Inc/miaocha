import { useState, useMemo, lazy, Suspense } from 'react';
import { Tabs, Tag, DatePicker, Button, Space } from 'antd';
import SpinIndicator from '@/components/SpinIndicator';
import styles from './TimePicker.module.less';
import { QUICK_RANGES } from './utils';

const Relative = lazy(() => import('./Relative'));

const { RangePicker } = DatePicker;

interface IProps {
  activeTab: string; // 选中的选项卡
  setActiveTab: any; // 设置选中的选项卡
  onSubmit: (params: ILogTimeSubmitParams) => void; // 提交时间
}

const TimePicker = (props: IProps) => {
  const { activeTab, setActiveTab, onSubmit } = props;
  const [selectedTag, setSelectedTag] = useState<string>('last_15m'); // 选中的标签
  const [absoluteOption, setAbsoluteOption] = useState<ILogTimeSubmitParams>(); // 绝对时间

  // 切换标签
  const changeTag = (value: string) => {
    // 是否存在
    const isExist = QUICK_RANGES[value];
    if (!isExist) return;
    const { from, to, format } = isExist;
    const params: ILogTimeSubmitParams = {
      value,
      range: [from().format(format[0]), to().format(format[1])],
      ...isExist,
    };
    setSelectedTag(value);
    onSubmit(params);
  };

  const quickRender = useMemo(() => {
    return (
      <>
        {Object.entries(QUICK_RANGES).map(([value, item]) => (
          <Tag.CheckableTag key={value} checked={selectedTag === value} onChange={() => changeTag(value)}>
            {item.label}
          </Tag.CheckableTag>
        ))}
      </>
    );
  }, [activeTab, selectedTag]);

  const onRangeChange = (dates: any, dateStrings: string[]) => {
    if (dates) {
      setAbsoluteOption({
        label: dateStrings.join(' ~ '),
        value: dateStrings.join(' ~ '),
        range: dateStrings,
      });
    } else {
      setAbsoluteOption({} as any);
    }
  };
  console.log('【打印日志】,activeTab =======>', activeTab);
  return (
    <div className={styles.timePickerLayout}>
      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        items={[
          {
            key: 'quick',
            label: '快速选择',
            children: quickRender,
          },
          {
            key: 'relative',
            label: '相对时间',
            children: (
              <Suspense fallback={<SpinIndicator />}>
                <Relative onSubmit={onSubmit} />
              </Suspense>
            ),
          },
          {
            key: 'absolute',
            label: '绝对时间',
            children: useMemo(
              () => (
                <Space.Compact block>
                  <RangePicker showTime format="YYYY-MM-DD HH:mm:ss" onChange={onRangeChange} />
                  <Button
                    type="primary"
                    onClick={() => onSubmit(absoluteOption as any)}
                    disabled={!absoluteOption?.value}
                  >
                    确定
                  </Button>
                </Space.Compact>
              ),
              [absoluteOption],
            ),
          },
        ]}
      />
    </div>
  );
};
export default TimePicker;
