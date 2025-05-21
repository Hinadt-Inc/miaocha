import { useState, useMemo, useCallback } from 'react';
import { Checkbox, Button, InputNumber, Select, Typography, Tag } from 'antd';
import dayjs from 'dayjs';
import styles from './Relative.module.less';
import { RELATIVE_TIME, DATE_FORMAT } from './utils';

const { Text } = Typography;

interface IProps {
  onSubmit: (params: ILogTimeSubmitParams) => void; // 提交时间
}

interface ITime extends IRelativeTime {
  number: number; // 数字框的值
  isExact: boolean; // 是否精确到秒
}

const TimePicker = (props: IProps) => {
  const CURRENT = '现在';
  const { onSubmit } = props;
  const defaultTime = RELATIVE_TIME[0];

  // 开始时间的配置项
  const [startOption, setStartOption] = useState<ITime>({
    ...defaultTime,
    number: 0, // 数字框的值
    isExact: false, // 是否精确到秒
  });

  // 结束时间的配置项
  const [endOption, setEndOption] = useState<ITime>({
    ...defaultTime,
    number: 0, // 数字框的值
    isExact: false, // 是否精确到秒
  });

  // 获取时间文本的公共函数
  const getTimeText = useCallback((option: ITime) => {
    const now = dayjs();
    const { number, unitEN, isExact, label, format } = option;
    // 时间的文本
    let timeString: any;
    if (number === 0 && unitEN === 'second') {
      timeString = CURRENT;
    } else if (label.endsWith('前')) {
      timeString = now.subtract(number, unitEN as any).format(isExact ? format : DATE_FORMAT);
    } else if (label.endsWith('后')) {
      timeString = now.add(number, unitEN as any).format(isExact ? format : DATE_FORMAT);
    }
    return timeString;
  }, []);

  // 开始时间
  const startTimeText = useMemo(() => getTimeText(startOption), [startOption, getTimeText]);

  // 结束时间
  const endTimeText = useMemo(() => getTimeText(endOption), [endOption, getTimeText]);

  // 校验时间是否合法
  const validateTime = useCallback(() => {
    const now = dayjs();
    const startTime = startTimeText === CURRENT ? now : dayjs(startTimeText);
    const endTime = endTimeText === CURRENT ? now : dayjs(endTimeText);
    return startTime.isBefore(endTime) || startTime.isSame(endTime);
  }, [startTimeText, endTimeText]);

  // 获取Select配置的公共函数
  const getSelectProps = useCallback((value: string) => {
    return {
      value,
      options: RELATIVE_TIME,
      optionRender: (option: any) => {
        const { label } = option;
        return <Tag color={String(label).endsWith('前') ? 'error' : 'processing'}>{label}</Tag>;
      },
    };
  }, []);

  // 下拉选择的配置项-开始时间
  const startSelectProps = useMemo(() => getSelectProps(startOption.value), [startOption.value, getSelectProps]);

  // 下拉选择的配置项-结束时间
  const endSelectProps = useMemo(() => getSelectProps(endOption.value), [endOption.value, getSelectProps]);

  // 提交时间
  const handleSubmit = useCallback(() => {
    const now = dayjs().format(DATE_FORMAT);
    const range = [
      startTimeText === CURRENT ? now : getTimeText(startOption),
      endTimeText === CURRENT ? now : getTimeText(endOption),
    ];
    const startLabel = startTimeText === CURRENT ? CURRENT : `${startOption.number}${startOption.label}`;
    const endLabel = endTimeText === CURRENT ? CURRENT : `${endOption.number}${endOption.label}`;
    const params = {
      range,
      label: `${startLabel} ~ ${endLabel}`,
      value: `${startLabel} ~ ${endLabel}`,
    };
    onSubmit(params);
  }, [startTimeText, endTimeText, startOption, endOption, onSubmit, getTimeText]);

  return (
    <div className={styles.relativeLayout}>
      <div className={styles.form}>
        <div className={styles.item}>
          <div className={styles.one}>
            <Text strong>开始时间</Text>
            <Button
              color="primary"
              variant="link"
              size="small"
              onClick={() => setStartOption((prev: any) => ({ ...prev, ...defaultTime, number: 0 }))}
            >
              设置为当前时间
            </Button>
          </div>
          <Text type="secondary">{startTimeText}</Text>
          <InputNumber
            min="0"
            max="999999999"
            changeOnWheel
            value={String(startOption.number || 0)}
            onChange={(number) => setStartOption((prev: any) => ({ ...prev, number }))}
            parser={(value) => (value ? parseInt(value) : 0) as any}
            formatter={(value) => (value ? parseInt(value).toString() : '0')}
            addonAfter={
              <Select
                {...startSelectProps}
                onChange={(_, item) => setStartOption((prev: any) => ({ ...prev, ...item }))}
              />
            }
          />
          <Checkbox onChange={(e) => setStartOption((prev: any) => ({ ...prev, isExact: e.target.checked }))}>
            精确到{startOption.unitCN}
          </Checkbox>
        </div>

        <div className={styles.item}>
          <div className={styles.one}>
            <Text strong>结束时间</Text>
            <Button
              color="primary"
              variant="link"
              size="small"
              onClick={() => setEndOption((prev: any) => ({ ...prev, ...defaultTime, number: 0 }))}
            >
              设置为当前时间
            </Button>
          </div>
          <Text type="secondary">{endTimeText}</Text>
          <InputNumber
            min="0"
            max="999999999"
            changeOnWheel
            value={String(endOption.number || 0)}
            onChange={(number) => setEndOption((prev: any) => ({ ...prev, number }))}
            parser={(value) => (value ? parseInt(value) : 0) as any}
            formatter={(value) => (value ? parseInt(value).toString() : '0')}
            addonAfter={
              <Select {...endSelectProps} onChange={(_, item) => setEndOption((prev: any) => ({ ...prev, ...item }))} />
            }
          />
          <Checkbox onChange={(e) => setEndOption((prev: any) => ({ ...prev, isExact: e.target.checked }))}>
            精确到{endOption.unitCN}
          </Checkbox>
        </div>
      </div>
      <div className={styles.btn}>
        <Text type="danger">{!validateTime() && '请确保开始时间不大于结束时间'}</Text>
        <Button size="small" type="primary" disabled={!validateTime()} onClick={handleSubmit}>
          确定
        </Button>
      </div>
    </div>
  );
};
export default TimePicker;
