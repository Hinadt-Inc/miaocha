import { useState, useEffect, useCallback, Suspense } from 'react';
import { Tabs } from 'antd';

import SpinIndicator from '@/components/SpinIndicator';
import { QuickTimePicker, AbsoluteTimePicker, RelativeTimePicker } from './components';
import { ITimePickerProps, ILogTimeSubmitParams } from './types';
import { QUICK_RANGES } from './utils';
import styles from './styles/TimePicker.module.less';

/**
 * 时间选择器组件
 * 支持快速选择、相对时间和绝对时间三种模式
 */
const TimePicker: React.FC<ITimePickerProps> = ({ activeTab, setActiveTab, onSubmit, currentTimeOption }) => {
  // 根据当前时间选项初始化选中的标签
  const getInitialSelectedTag = useCallback(() => {
    if (currentTimeOption?.type === 'quick' && currentTimeOption?.value && QUICK_RANGES[currentTimeOption.value]) {
      return currentTimeOption.value;
    }
    return 'last_15m';
  }, [currentTimeOption]);

  const [selectedTag, setSelectedTag] = useState<string>(getInitialSelectedTag);

  // 当currentTimeOption变化时，同步更新selectedTag
  useEffect(() => {
    if (currentTimeOption?.type === 'quick' && currentTimeOption?.value && QUICK_RANGES[currentTimeOption.value]) {
      setSelectedTag(currentTimeOption.value);
    }
  }, [currentTimeOption]);

  // 切换标签
  const changeTag = useCallback(
    (value: string) => {
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
    },
    [onSubmit],
  );

  // 标签页配置
  const tabItems = [
    {
      key: 'quick',
      label: '快速选择',
      children: <QuickTimePicker selectedTag={selectedTag} onTagChange={changeTag} />,
    },
    {
      key: 'relative',
      label: '相对时间',
      children: (
        <Suspense fallback={<SpinIndicator />}>
          <RelativeTimePicker onSubmit={onSubmit} />
        </Suspense>
      ),
    },
    {
      key: 'absolute',
      label: '绝对时间',
      children: <AbsoluteTimePicker onSubmit={onSubmit} />,
    },
  ];

  return (
    <div className={styles.timePickerLayout}>
      <Tabs activeKey={activeTab} items={tabItems} onChange={setActiveTab} />
    </div>
  );
};

export default TimePicker;
