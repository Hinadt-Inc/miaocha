import { useState, useMemo, useCallback, useEffect } from 'react';

import { Checkbox, Button, InputNumber, Select, Typography } from 'antd';
import type { SelectProps } from 'antd/es/select';
import dayjs from 'dayjs';
import type { ManipulateType } from 'dayjs';

import styles from '../styles/Relative.module.less';
import { IRelativeTimePickerProps, ILogTimeSubmitParams, IRelativeTime, IRelativeTimeState } from '../types';
import { RELATIVE_TIME, DATE_FORMAT_THOUSOND } from '../utils';

const { Text } = Typography;

/**
 * 相对时间选择组件
 */
const RelativeTimePicker: React.FC<IRelativeTimePickerProps> = ({ onSubmit, currentTimeOption }) => {
  const CURRENT = '现在';
  const defaultTime = RELATIVE_TIME[0];

  // 开始时间的配置项
  const [startOption, setStartOption] = useState<IRelativeTimeState>(() => {
    // 如果有回显数据且类型为 relative，则使用回显数据
    if (currentTimeOption?.type === 'relative' && currentTimeOption.startOption) {
      return {
        ...currentTimeOption.startOption,
        number: currentTimeOption.startOption.number ?? 0,
        isExact: currentTimeOption.startOption.isExact ?? false,
      };
    }
    return {
      ...defaultTime,
      number: 0,
      isExact: false,
    };
  });

  // 结束时间的配置项
  const [endOption, setEndOption] = useState<IRelativeTimeState>(() => {
    // 如果有回显数据且类型为 relative，则使用回显数据
    if (currentTimeOption?.type === 'relative' && currentTimeOption.endOption) {
      return {
        ...currentTimeOption.endOption,
        number: currentTimeOption.endOption.number ?? 0,
        isExact: currentTimeOption.endOption.isExact ?? false,
      };
    }
    return {
      ...defaultTime,
      number: 0,
      isExact: false,
    };
  });

  // 当 currentTimeOption 变化时，同步更新状态
  useEffect(() => {
    if (currentTimeOption?.type === 'relative') {
      if (currentTimeOption.startOption) {
        setStartOption({
          ...currentTimeOption.startOption,
          number: currentTimeOption.startOption.number ?? 0,
          isExact: currentTimeOption.startOption.isExact ?? false,
        });
      }
      if (currentTimeOption.endOption) {
        setEndOption({
          ...currentTimeOption.endOption,
          number: currentTimeOption.endOption.number ?? 0,
          isExact: currentTimeOption.endOption.isExact ?? false,
        });
      }
    }
  }, [currentTimeOption]);

  // 获取时间文本的公共函数（返回字符串，去除 any）
  const getTimeText = useCallback((option: IRelativeTimeState): string => {
    const now = dayjs();
    const { number, unitEN, isExact, label, format } = option;

    if (number === 0 && unitEN === 'second') {
      return CURRENT;
    }

    const unit = unitEN as ManipulateType;
    const fmt = isExact ? format : DATE_FORMAT_THOUSOND;

    if (label.endsWith('前')) {
      return now.subtract(number, unit).format(fmt);
    }
    // 其余情况均视为「后」
    return now.add(number, unit).format(fmt);
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

  // 获取 Select 配置
  const getSelectProps = useCallback(
    (value: string): SelectProps<string, IRelativeTime> => ({
      value,
      options: RELATIVE_TIME,
    }),
    [],
  );

  // 下拉选择的配置项-开始时间
  const startSelectProps = useMemo(() => getSelectProps(startOption.value), [startOption.value, getSelectProps]);

  // 下拉选择的配置项-结束时间
  const endSelectProps = useMemo(() => getSelectProps(endOption.value), [endOption.value, getSelectProps]);

  // 设置为当前时间
  const setToCurrentTime = useCallback(
    (type: 'start' | 'end') => {
      const resetOption: IRelativeTimeState = { ...defaultTime, number: 0, isExact: false };
      if (type === 'start') {
        setStartOption(resetOption);
      } else {
        setEndOption(resetOption);
      }
    },
    [defaultTime],
  );

  // 提交时间
  const handleSubmit = useCallback(() => {
    const now = dayjs().format(DATE_FORMAT_THOUSOND);
    const range = [
      startTimeText === CURRENT ? now : getTimeText(startOption),
      endTimeText === CURRENT ? now : getTimeText(endOption),
    ];
    const startLabel = startTimeText === CURRENT ? CURRENT : `${startOption.number}${startOption.label}`;
    const endLabel = endTimeText === CURRENT ? CURRENT : `${endOption.number}${endOption.label}`;

    const startExactText = startOption.isExact ? `(精确到${startOption.unitCN})` : '';
    const endExactText = endOption.isExact ? `(精确到${endOption.unitCN})` : '';
    const labelText = `${startLabel}${startExactText} ~ ${endLabel}${endExactText}`;

    const params: ILogTimeSubmitParams = {
      range,
      label: labelText,
      value: `${startLabel} ~ ${endLabel}`,
      type: 'relative',
      startOption,
      endOption,
    };
    onSubmit(params);
  }, [startTimeText, endTimeText, startOption, endOption, onSubmit, getTimeText]);

  return (
    <div className={styles.relativeLayout}>
      <div className={styles.form}>
        <div className={styles.item}>
          <div className={styles.one}>
            <Text strong>开始时间</Text>
            <Button color="primary" size="small" variant="link" onClick={() => setToCurrentTime('start')}>
              设置为当前时间
            </Button>
          </div>
          <Text type="secondary">{startTimeText}</Text>
          <InputNumber
            addonAfter={
              <Select
                {...startSelectProps}
                onChange={(_value, option) => setStartOption((prev) => ({ ...prev, ...(option as IRelativeTime) }))}
              />
            }
            changeOnWheel
            formatter={(value) => {
              const v = String(value ?? '');
              const n = parseInt(v, 10);
              return Number.isNaN(n) ? '0' : String(n);
            }}
            max={999999999}
            min={0}
            parser={(value) => {
              const v = String(value ?? '0');
              const n = parseInt(v, 10);
              return Number.isNaN(n) ? 0 : n;
            }}
            value={startOption.number || 0}
            onChange={(num) => setStartOption((prev) => ({ ...prev, number: Number(num ?? 0) }))}
          />
          <Checkbox
            checked={startOption.isExact}
            onChange={(e) => setStartOption((prev) => ({ ...prev, isExact: e.target.checked }))}
          >
            精确到{startOption.unitCN}
          </Checkbox>
        </div>

        <div className={styles.item}>
          <div className={styles.one}>
            <Text strong>结束时间</Text>
            <Button color="primary" size="small" variant="link" onClick={() => setToCurrentTime('end')}>
              设置为当前时间
            </Button>
          </div>
          <Text type="secondary">{endTimeText}</Text>
          <InputNumber
            addonAfter={
              <Select
                {...endSelectProps}
                onChange={(_value, option) => setEndOption((prev) => ({ ...prev, ...(option as IRelativeTime) }))}
              />
            }
            changeOnWheel
            formatter={(value) => {
              const v = String(value ?? '');
              const n = parseInt(v, 10);
              return Number.isNaN(n) ? '0' : String(n);
            }}
            max={999999999}
            min={0}
            parser={(value) => {
              const v = String(value ?? '0');
              const n = parseInt(v, 10);
              return Number.isNaN(n) ? 0 : n;
            }}
            value={endOption.number || 0}
            onChange={(num) => setEndOption((prev) => ({ ...prev, number: Number(num ?? 0) }))}
          />
          <Checkbox
            checked={endOption.isExact}
            onChange={(e) => setEndOption((prev) => ({ ...prev, isExact: e.target.checked }))}
          >
            精确到{endOption.unitCN}
          </Checkbox>
        </div>
      </div>
      <div className={styles.btn}>
        <Text type="danger">{!validateTime() && '请确保开始时间不大于结束时间'}</Text>
        <Button disabled={!validateTime()} size="small" type="primary" onClick={handleSubmit}>
          确定
        </Button>
      </div>
    </div>
  );
};

export default RelativeTimePicker;
