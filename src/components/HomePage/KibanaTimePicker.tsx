import React, { useState, useEffect } from 'react';
import { Button, DatePicker, Space, Tabs, Radio, Input, Tooltip, Popover } from 'antd';
import { 
  ClockCircleOutlined, 
  LeftOutlined, 
  RightOutlined,
  ReloadOutlined
} from '@ant-design/icons';
import dayjs from 'dayjs';
import type { Dayjs } from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import zhCN from 'dayjs/locale/zh-cn';

// 加载dayjs插件
dayjs.extend(relativeTime);
dayjs.locale(zhCN);

const { RangePicker } = DatePicker;
const { TabPane } = Tabs;

// 时间范围预设选项
const QUICK_RANGES = [
  { key: 'last_15m', label: '最近15分钟', from: () => dayjs().subtract(15, 'minute'), to: () => dayjs() },
  { key: 'last_30m', label: '最近30分钟', from: () => dayjs().subtract(30, 'minute'), to: () => dayjs() },
  { key: 'last_1h', label: '最近1小时', from: () => dayjs().subtract(1, 'hour'), to: () => dayjs() },
  { key: 'last_3h', label: '最近3小时', from: () => dayjs().subtract(3, 'hour'), to: () => dayjs() },
  { key: 'last_6h', label: '最近6小时', from: () => dayjs().subtract(6, 'hour'), to: () => dayjs() },
  { key: 'last_12h', label: '最近12小时', from: () => dayjs().subtract(12, 'hour'), to: () => dayjs() },
  { key: 'last_24h', label: '最近24小时', from: () => dayjs().subtract(24, 'hour'), to: () => dayjs() },
  { key: 'last_7d', label: '最近7天', from: () => dayjs().subtract(7, 'day'), to: () => dayjs() },
  { key: 'last_30d', label: '最近30天', from: () => dayjs().subtract(30, 'day'), to: () => dayjs() },
  { key: 'today', label: '今天', from: () => dayjs().startOf('day'), to: () => dayjs() },
  { key: 'yesterday', label: '昨天', from: () => dayjs().subtract(1, 'day').startOf('day'), to: () => dayjs().subtract(1, 'day').endOf('day') },
  { key: 'this_week', label: '本周', from: () => dayjs().startOf('week'), to: () => dayjs() },
  { key: 'this_month', label: '本月', from: () => dayjs().startOf('month'), to: () => dayjs() },
  { key: 'last_month', label: '上个月', from: () => dayjs().subtract(1, 'month').startOf('month'), to: () => dayjs().subtract(1, 'month').endOf('month') },
  { key: 'this_year', label: '今年', from: () => dayjs().startOf('year'), to: () => dayjs() },
];

// 相对时间单位选项
const RELATIVE_TIME_UNITS = [
  { label: '秒', value: 's', singular: '秒', plural: '秒' },
  { label: '分钟', value: 'm', singular: '分钟', plural: '分钟' },
  { label: '小时', value: 'h', singular: '小时', plural: '小时' },
  { label: '天', value: 'd', singular: '天', plural: '天' },
  { label: '周', value: 'w', singular: '周', plural: '周' },
  { label: '月', value: 'M', singular: '月', plural: '月' },
  { label: '年', value: 'y', singular: '年', plural: '年' },
];

// 相对时间应用选项
const RELATIVE_TIME_OPTIONS = [
  { label: '现在', value: 'now' },
  { label: '今天开始', value: 'startOfDay' },
  { label: '今天结束', value: 'endOfDay' },
  { label: '本周开始', value: 'startOfWeek' },
  { label: '本周结束', value: 'endOfWeek' },
  { label: '本月开始', value: 'startOfMonth' },
  { label: '本月结束', value: 'endOfMonth' },
  { label: '本年开始', value: 'startOfYear' },
  { label: '本年结束', value: 'endOfYear' },
];

interface KibanaTimePickerProps {
  value?: [string, string];
  presetKey?: string;
  onChange?: (range: [string, string], presetKey?: string) => void;
  onTimeGroupingChange?: (value: string) => void;
  timeGrouping?: string;
}

export const KibanaTimePicker: React.FC<KibanaTimePickerProps> = ({
  value,
  presetKey,
  onChange,
  onTimeGroupingChange,
  timeGrouping = 'minute'
}) => {
  // 控制面板是否显示
  const [visible, setVisible] = useState(false);
  
  // 当前选中的时间范围
  const [selectedRange, setSelectedRange] = useState<{
    start: Dayjs;
    end: Dayjs;
    presetKey?: string;
  }>(() => {
    if (value && value[0] && value[1]) {
      return {
        start: dayjs(value[0]),
        end: dayjs(value[1]),
        presetKey
      };
    }
    
    // 默认最近15分钟
    const defaultPreset = QUICK_RANGES.find(r => r.key === 'last_15m') || QUICK_RANGES[0];
    return {
      start: defaultPreset.from(),
      end: defaultPreset.to(),
      presetKey: defaultPreset.key
    };
  });
  
  // 选项卡值
  const [activeTab, setActiveTab] = useState('quick');
  
  // 相对时间设置
  const [relativeTimeSettings, setRelativeTimeSettings] = useState({
    fromValue: 15,
    fromUnit: 'm',
    fromOption: 'now',
    toValue: 0,
    toUnit: 's',
    toOption: 'now'
  });
  
  // 时间分组选项
  const timeGroupingOptions = [
    { label: '按分钟', value: 'minute' },
    { label: '按小时', value: 'hour' },
    { label: '按天', value: 'day' },
    { label: '按周', value: 'week' },
    { label: '按月', value: 'month' },
  ];
  
  // 更新值到父组件
  const updateTimeRange = (start: Dayjs, end: Dayjs, key?: string) => {
    setSelectedRange({
      start,
      end,
      presetKey: key
    });
    
    if (onChange) {
      onChange([
        start.format('YYYY-MM-DD HH:mm:ss'),
        end.format('YYYY-MM-DD HH:mm:ss')
      ], key);
    }
  };
  
  // 应用快速预设时间范围
  const applyQuickRange = (range: typeof QUICK_RANGES[0]) => {
    updateTimeRange(range.from(), range.to(), range.key);
    setVisible(false);
  };
  
  // 应用绝对时间范围
  const applyAbsoluteRange = (dates: [Dayjs, Dayjs]) => {
    if (dates && dates[0] && dates[1]) {
      updateTimeRange(dates[0], dates[1], 'custom');
    }
  };
  
  // 应用相对时间范围
  const applyRelativeRange = () => {
    let start: Dayjs;
    let end: Dayjs;
    
    // 处理开始时间
    if (relativeTimeSettings.fromOption === 'now') {
      start = dayjs().subtract(
        relativeTimeSettings.fromValue,
        relativeTimeSettings.fromUnit as any
      );
    } else {
      // 处理其他选项
      const option = relativeTimeSettings.fromOption;
      if (option === 'startOfDay') start = dayjs().startOf('day');
      else if (option === 'endOfDay') start = dayjs().endOf('day');
      else if (option === 'startOfWeek') start = dayjs().startOf('week');
      else if (option === 'endOfWeek') start = dayjs().endOf('week');
      else if (option === 'startOfMonth') start = dayjs().startOf('month');
      else if (option === 'endOfMonth') start = dayjs().endOf('month');
      else if (option === 'startOfYear') start = dayjs().startOf('year');
      else if (option === 'endOfYear') start = dayjs().endOf('year');
      else start = dayjs();
      
      // 如果需要，再应用相对偏移
      if (relativeTimeSettings.fromValue !== 0) {
        start = start.subtract(
          relativeTimeSettings.fromValue,
          relativeTimeSettings.fromUnit as any
        );
      }
    }
    
    // 处理结束时间
    if (relativeTimeSettings.toOption === 'now') {
      end = dayjs();
    } else {
      // 处理其他选项
      const option = relativeTimeSettings.toOption;
      if (option === 'startOfDay') end = dayjs().startOf('day');
      else if (option === 'endOfDay') end = dayjs().endOf('day');
      else if (option === 'startOfWeek') end = dayjs().startOf('week');
      else if (option === 'endOfWeek') end = dayjs().endOf('week');
      else if (option === 'startOfMonth') end = dayjs().startOf('month');
      else if (option === 'endOfMonth') end = dayjs().endOf('month');
      else if (option === 'startOfYear') end = dayjs().startOf('year');
      else if (option === 'endOfYear') end = dayjs().endOf('year');
      else end = dayjs();
      
      // 如果需要，再应用相对偏移
      if (relativeTimeSettings.toValue !== 0) {
        end = end.subtract(
          relativeTimeSettings.toValue,
          relativeTimeSettings.toUnit as any
        );
      }
    }
    
    // 确保开始时间早于结束时间
    if (start.isAfter(end)) {
      const temp = start;
      start = end;
      end = temp;
    }
    
    updateTimeRange(start, end, 'relative');
    setVisible(false);
  };
  
  // 移动时间范围
  const moveTimeRange = (direction: 'forward' | 'backward') => {
    const { start, end } = selectedRange;
    const diff = end.diff(start); // 得到毫秒差值
    
    let newStart: Dayjs;
    let newEnd: Dayjs;
    
    if (direction === 'forward') {
      newStart = start.add(diff, 'millisecond');
      newEnd = end.add(diff, 'millisecond');
    } else {
      newStart = start.subtract(diff, 'millisecond');
      newEnd = end.subtract(diff, 'millisecond');
    }
    
    updateTimeRange(newStart, newEnd, 'moved');
  };
  
  // 返回到现在
  const goToNow = () => {
    const { start, end } = selectedRange;
    const diff = end.diff(start); // 得到毫秒差值
    
    const now = dayjs();
    const newStart = now.subtract(diff, 'millisecond');
    
    updateTimeRange(newStart, now, 'now');
  };
  
  // 格式化显示时间
  const formatDisplayTime = () => {
    const { start, end, presetKey } = selectedRange;
    
    // 如果是快速预设，显示预设名称
    if (presetKey && presetKey !== 'custom' && presetKey !== 'relative' && presetKey !== 'moved' && presetKey !== 'now') {
      const preset = QUICK_RANGES.find(r => r.key === presetKey);
      if (preset) {
        return preset.label;
      }
    }
    
    // 否则显示格式化的时间范围
    // 如果是今天的时间，只显示时间部分
    const today = dayjs().format('YYYY-MM-DD');
    const startDate = start.format('YYYY-MM-DD');
    const endDate = end.format('YYYY-MM-DD');
    
    const startFormat = startDate === today ? 'HH:mm:ss' : 'MM-DD HH:mm:ss';
    const endFormat = endDate === today ? 'HH:mm:ss' : 'MM-DD HH:mm:ss';
    
    // 如果开始和结束日期相同，简化显示
    if (startDate === endDate) {
      return `${start.format('MM-DD')} ${start.format('HH:mm:ss')} - ${end.format('HH:mm:ss')}`;
    }
    
    // 如果是不同的年份，显示完整日期
    if (start.format('YYYY') !== end.format('YYYY')) {
      return `${start.format('YYYY-MM-DD HH:mm:ss')} - ${end.format('YYYY-MM-DD HH:mm:ss')}`;
    }
    
    return `${start.format(startFormat)} - ${end.format(endFormat)}`;
  };
  
  // 监听外部值变化
  useEffect(() => {
    if (value && value[0] && value[1]) {
      setSelectedRange({
        start: dayjs(value[0]),
        end: dayjs(value[1]),
        presetKey
      });
    }
  }, [value, presetKey]);
  
  // 时间选择面板内容
  const timePickerContent = (
    <div className="kibana-time-picker-popover" style={{ width: '400px' }}>
      <Tabs activeKey={activeTab} onChange={setActiveTab}>
        <TabPane tab="快速选择" key="quick">
          <div className="kibana-quick-ranges">
            {QUICK_RANGES.map(range => (
              <div 
                key={range.key}
                className={`kibana-quick-range-button ${selectedRange.presetKey === range.key ? 'kibana-quick-range-button-active' : ''}`}
                onClick={() => applyQuickRange(range)}
              >
                {range.label}
              </div>
            ))}
          </div>
        </TabPane>
        <TabPane tab="绝对时间" key="absolute">
          <div className="absolute-time-panel">
            <RangePicker
              showTime
              format="YYYY-MM-DD HH:mm:ss"
              style={{ width: '100%', marginBottom: '16px' }}
              defaultValue={[selectedRange.start, selectedRange.end]}
              onOk={(dates: any) => applyAbsoluteRange(dates)}
            />
            <Button type="primary" block onClick={() => setVisible(false)}>
              应用
            </Button>
          </div>
        </TabPane>
        <TabPane tab="相对时间" key="relative">
          <div className="relative-time-panel">
            <div className="relative-time-section">
              <div className="relative-time-label">开始时间：</div>
              <Space align="center">
                <Input 
                  type="number" 
                  style={{ width: '80px' }} 
                  value={relativeTimeSettings.fromValue}
                  onChange={e => setRelativeTimeSettings({
                    ...relativeTimeSettings,
                    fromValue: Number(e.target.value) || 0
                  })}
                />
                <Radio.Group
                  options={RELATIVE_TIME_UNITS}
                  value={relativeTimeSettings.fromUnit}
                  optionType="button"
                  buttonStyle="solid"
                  onChange={e => setRelativeTimeSettings({
                    ...relativeTimeSettings,
                    fromUnit: e.target.value
                  })}
                />
                <span>前的</span>
                <Radio.Group
                  options={RELATIVE_TIME_OPTIONS}
                  value={relativeTimeSettings.fromOption}
                  onChange={e => setRelativeTimeSettings({
                    ...relativeTimeSettings,
                    fromOption: e.target.value
                  })}
                />
              </Space>
            </div>
            
            <div className="relative-time-section" style={{ marginTop: '16px' }}>
              <div className="relative-time-label">结束时间：</div>
              <Radio.Group
                options={RELATIVE_TIME_OPTIONS}
                value={relativeTimeSettings.toOption}
                onChange={e => setRelativeTimeSettings({
                  ...relativeTimeSettings,
                  toOption: e.target.value
                })}
              />
            </div>
            
            <Button 
              type="primary" 
              block 
              style={{ marginTop: '16px' }}
              onClick={applyRelativeRange}
            >
              应用
            </Button>
          </div>
        </TabPane>
      </Tabs>
      
      {activeTab === 'quick' && (
        <div className="time-grouping-section" style={{ marginTop: '16px', borderTop: '1px solid #f0f0f0', paddingTop: '16px' }}>
          <div style={{ marginBottom: '8px' }}>时间分组：</div>
          <Radio.Group
            options={timeGroupingOptions}
            value={timeGrouping}
            optionType="button"
            buttonStyle="solid"
            onChange={e => {
              if (onTimeGroupingChange) {
                onTimeGroupingChange(e.target.value);
              }
            }}
          />
        </div>
      )}
    </div>
  );
  
  return (
    <div className="kibana-time-picker">
      <Popover
        content={timePickerContent}
        title="选择时间范围"
        trigger="click"
        open={visible}
        onOpenChange={setVisible}
        placement="bottomRight"
        overlayStyle={{ width: '400px' }}
      >
        <div className={`kibana-time-control ${visible ? 'kibana-time-control-active' : ''}`}>
          <Space align="center">
            <ClockCircleOutlined />
            <div className="kibana-time-display">
              <span className="kibana-time-display-text">
                {formatDisplayTime()}
              </span>
            </div>
          </Space>
        </div>
      </Popover>
      
      <div className="time-navigation" style={{ marginTop: '8px' }}>
        <Space>
          <Tooltip title="向前移动时间段">
            <Button 
              icon={<LeftOutlined />} 
              size="small"
              onClick={() => moveTimeRange('backward')}
            />
          </Tooltip>
          <Tooltip title="回到当前">
            <Button 
              icon={<ReloadOutlined />} 
              size="small"
              onClick={goToNow}
            />
          </Tooltip>
          <Tooltip title="向后移动时间段">
            <Button 
              icon={<RightOutlined />} 
              size="small"
              onClick={() => moveTimeRange('forward')}
            />
          </Tooltip>
        </Space>
      </div>
    </div>
  );
};