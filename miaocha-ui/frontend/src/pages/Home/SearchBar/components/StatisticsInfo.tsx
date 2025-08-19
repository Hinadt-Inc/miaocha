/**
 * 搜索结果统计信息组件
 */

import React from 'react';
import { Space, Statistic } from 'antd';
import CountUp from 'react-countup';
import { STATISTICS } from '../constants';

interface IStatisticsInfoProps {
  totalCount: number;
}

const StatisticsInfo: React.FC<IStatisticsInfoProps> = ({ totalCount }) => {
  return (
    <Space>
      找到
      <Statistic
        value={totalCount}
        formatter={(value: any) => (
          <CountUp end={Number(value)} duration={STATISTICS.COUNT_UP_DURATION} separator={STATISTICS.SEPARATOR} />
        )}
      />
      条记录
    </Space>
  );
};

export default StatisticsInfo;
