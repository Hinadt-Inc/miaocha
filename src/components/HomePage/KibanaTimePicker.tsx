import React, { useState, useEffect } from 'react';
import { Button, DatePicker, Space, Tabs, Radio, Input, Tooltip } from 'antd';
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
  onChange?: (range: [string, string], presetKey?: string, displayText?: string) => void;
  onTimeGroupingChange?: (value: 'minute' | 'hour' | 'day' | 'month') => void;
  timeGrouping?: 'minute' | 'hour' | 'day' | 'month';
  displayText?: string;
}

export const KibanaTimePicker: React.FC<KibanaTimePickerProps> = ({
  value,
  presetKey,
  onChange,
  onTimeGroupingChange,
  timeGrouping = 'minute'
}) => {
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
  
  // 内部状态跟踪时间分组
  const [currentTimeGrouping, setCurrentTimeGrouping] = useState(timeGrouping);
  
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
      // 获取显示文本
      let displayText: string;
      
      // 如果是快速预设，显示预设名称
      if (key && key !== 'custom' && key !== 'relative' && key !== 'moved' && key !== 'now') {
        const preset = QUICK_RANGES.find(r => r.key === key);
        if (preset) {
          displayText = preset.label;
        } else {
          displayText = formatTimeRange(start, end);
        }
      } else {
        displayText = formatTimeRange(start, end);
      }
      
      onChange([
        start.format('YYYY-MM-DD HH:mm:ss'),
        end.format('YYYY-MM-DD HH:mm:ss')
      ], key, displayText);
    }
  };
  
  // 用于格式化时间范围显示的辅助函数
  const formatTimeRange = (start: Dayjs, end: Dayjs): string => {
    const now = dayjs();
    const today = now.format('YYYY-MM-DD');
    const yesterday = now.subtract(1, 'day').format('YYYY-MM-DD');
    
    const startDate = start.format('YYYY-MM-DD');
    const endDate = end.format('YYYY-MM-DD');
    const startYear = start.format('YYYY');
    const endYear = end.format('YYYY');
    
    // 不同年份的情况，显示完整年月日时间
    if (startYear !== endYear) {
      return `${start.format('YYYY-MM-DD HH:mm:ss')} - ${end.format('YYYY-MM-DD HH:mm:ss')}`;
    }
    
    // 同一天的情况
    if (startDate === endDate) {
      // 如果是今天
      if (startDate === today) {
        return `今天 ${start.format('HH:mm:ss')} - ${end.format('HH:mm:ss')}`;
      }
      // 如果是昨天
      if (startDate === yesterday) {
        return `昨天 ${start.format('HH:mm:ss')} - ${end.format('HH:mm:ss')}`;
      }
      // 其他同一天的情况
      return `${start.format('MM-DD')} ${start.format('HH:mm:ss')} - ${end.format('HH:mm:ss')}`;
    }
    
    // 跨天但在同一月
    if (start.format('YYYY-MM') === end.format('YYYY-MM')) {
      return `${start.format('MM-DD HH:mm:ss')} - ${end.format('DD HH:mm:ss')}`;
    }
    
    // 跨月但在同一年
    return `${start.format('MM-DD HH:mm:ss')} - ${end.format('MM-DD HH:mm:ss')}`;
  };
  
  // 应用快速预设时间范围
  const applyQuickRange = (range: typeof QUICK_RANGES[0]) => {
    updateTimeRange(range.from(), range.to(), range.key);
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
    const now = dayjs();
    const today = now.format('YYYY-MM-DD');
    const yesterday = now.subtract(1, 'day').format('YYYY-MM-DD');
    const currentYear = now.format('YYYY');
    
    const startDate = start.format('YYYY-MM-DD');
    const endDate = end.format('YYYY-MM-DD');
    const startYear = start.format('YYYY');
    const endYear = end.format('YYYY');
    
    // 不同年份的情况，显示完整年月日时间
    if (startYear !== endYear) {
      return `${start.format('YYYY-MM-DD HH:mm:ss')} - ${end.format('YYYY-MM-DD HH:mm:ss')}`;
    }
    
    // 同一天的情况
    if (startDate === endDate) {
      // 如果是今天
      if (startDate === today) {
        return `今天 ${start.format('HH:mm:ss')} - ${end.format('HH:mm:ss')}`;
      }
      // 如果是昨天
      if (startDate === yesterday) {
        return `昨天 ${start.format('HH:mm:ss')} - ${end.format('HH:mm:ss')}`;
      }
      // 其他同一天的情况
      return `${start.format('MM-DD')} ${start.format('HH:mm:ss')} - ${end.format('HH:mm:ss')}`;
    }
    
    // 跨天但在同一月
    if (start.format('YYYY-MM') === end.format('YYYY-MM')) {
      return `${start.format('MM-DD HH:mm:ss')} - ${end.format('DD HH:mm:ss')}`;
    }
    
    // 跨月但在同一年
    return `${start.format('MM-DD HH:mm:ss')} - ${end.format('MM-DD HH:mm:ss')}`;
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
  
  // 监听外部 timeGrouping 属性变化
  useEffect(() => {
    setCurrentTimeGrouping(timeGrouping);
  }, [timeGrouping]);
  
  return (
    <div className="kibana-time-picker-container" style={{ width: '400px' }}>
      {/* 顶部显示已选择时间范围和时间导航控件 */}
      <div className="time-header" style={{ 
        display: 'flex', 
        justifyContent: 'space-between', 
        alignItems: 'center', 
        marginBottom: '8px',
        borderBottom: '1px solid #f0f0f0',
        paddingBottom: '8px' 
      }}>
        <div className="time-display">
          <Space align="center">
            <ClockCircleOutlined />
            <div style={{ fontWeight: 'bold' }}>{formatDisplayTime()}</div>
          </Space>
        </div>
        <div className="time-navigation">
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
      
      {/* 时间选择器主内容 */}
      <Tabs activeKey={activeTab} onChange={setActiveTab}>
        <TabPane tab="快速选择" key="quick">
          <div className="kibana-quick-ranges" style={{ 
            display: 'grid', 
            gridTemplateColumns: 'repeat(3, 1fr)', 
            gap: '8px', 
            maxHeight: '200px', 
            overflowY: 'auto' 
          }}>
            {QUICK_RANGES.map(range => (
              <Button 
                key={range.key}
                type={selectedRange.presetKey === range.key ? 'primary' : 'default'}
                size="small"
                onClick={() => applyQuickRange(range)}
                style={{ width: '100%', textAlign: 'center' }}
              >
                {range.label}
              </Button>
            ))}
          </div>
          
          <div className="time-grouping-section" style={{ marginTop: '16px' }}>
            <div style={{ marginBottom: '8px' }}>时间分组：</div>
            <Radio.Group
              options={timeGroupingOptions}
              value={currentTimeGrouping}
              optionType="button"
              buttonStyle="solid"
              onChange={e => {
                const newTimeGrouping = e.target.value;
                setCurrentTimeGrouping(newTimeGrouping);
                if (onTimeGroupingChange) {
                  onTimeGroupingChange(newTimeGrouping);
                  
                  // 在修改时间分组时，也重新应用当前时间范围以触发数据更新
                  if (selectedRange.presetKey && selectedRange.presetKey !== 'custom' && 
                      selectedRange.presetKey !== 'relative' && selectedRange.presetKey !== 'moved') {
                    // 如果是预设，重新应用该预设
                    const preset = QUICK_RANGES.find(r => r.key === selectedRange.presetKey);
                    if (preset) {
                      updateTimeRange(preset.from(), preset.to(), preset.key);
                    }
                  } else {
                    // 如果是自定义时间范围，直接重新应用当前的时间范围
                    updateTimeRange(selectedRange.start, selectedRange.end, selectedRange.presetKey);
                  }
                }
              }}
              style={{ display: 'flex', flexWrap: 'wrap' }}
            />
          </div>
        </TabPane>
        
        <TabPane tab="绝对时间" key="absolute">
          <div className="absolute-time-panel">
            <RangePicker
              showTime
              format="YYYY-MM-DD HH:mm:ss"
              style={{ width: '100%', marginBottom: '16px' }}
              value={[selectedRange.start, selectedRange.end]}
              onOk={(dates: any) => applyAbsoluteRange(dates)}
            />
            <Button type="primary" onClick={() => applyAbsoluteRange([selectedRange.start, selectedRange.end])}>
              应用
            </Button>
          </div>
        </TabPane>
        
        <TabPane tab="相对时间" key="relative">
          <div className="relative-time-panel" style={{ maxWidth: '380px' }}>
            <div className="relative-time-section">
              <div className="relative-time-label" style={{ marginBottom: '8px' }}>开始时间：</div>
              <Space align="center" style={{ flexWrap: 'wrap' }}>
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
                  style={{ flexWrap: 'wrap' }}
                />
              </Space>
              <div style={{ marginTop: '8px', marginBottom: '8px' }}>前的</div>
              <Radio.Group
                options={RELATIVE_TIME_OPTIONS}
                value={relativeTimeSettings.fromOption}
                onChange={e => setRelativeTimeSettings({
                  ...relativeTimeSettings,
                  fromOption: e.target.value
                })}
                style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}
              />
            </div>
            
            <div className="relative-time-section" style={{ marginTop: '16px' }}>
              <div className="relative-time-label" style={{ marginBottom: '8px' }}>结束时间：</div>
              <Radio.Group
                options={RELATIVE_TIME_OPTIONS}
                value={relativeTimeSettings.toOption}
                onChange={e => setRelativeTimeSettings({
                  ...relativeTimeSettings,
                  toOption: e.target.value
                })}
                style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}
              />
            </div>
            
            <Button 
              type="primary" 
              style={{ marginTop: '16px' }}
              onClick={applyRelativeRange}
            >
              应用
            </Button>
          </div>
        </TabPane>
      </Tabs>
    </div>
  );
};
