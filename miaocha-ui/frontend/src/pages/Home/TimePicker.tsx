import { useState, useMemo, lazy, Suspense, useEffect } from 'react';
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
  currentTimeOption?: ILogTimeSubmitParams; // 当前选择的时间选项
}

const TimePicker = (props: IProps) => {
  const { activeTab, setActiveTab, onSubmit, currentTimeOption } = props;
  
  // 根据当前时间选项初始化选中的标签
  const getInitialSelectedTag = () => {
    if (currentTimeOption?.type === 'quick' && currentTimeOption?.value && QUICK_RANGES[currentTimeOption.value]) {
      return currentTimeOption.value;
    }
    return 'last_15m';
  };
  
  const [selectedTag, setSelectedTag] = useState<string>(getInitialSelectedTag); // 选中的标签
  const [absoluteOption, setAbsoluteOption] = useState<ILogTimeSubmitParams>(); // 绝对时间

  // 当currentTimeOption变化时，同步更新selectedTag
  useEffect(() => {
    if (currentTimeOption?.type === 'quick' && currentTimeOption?.value && QUICK_RANGES[currentTimeOption.value]) {
      setSelectedTag(currentTimeOption.value);
    }
  }, [currentTimeOption]);

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
      type: 'quick',
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
        type: 'absolute',
      });
    } else {
      setAbsoluteOption({} as any);
    }
  };
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
