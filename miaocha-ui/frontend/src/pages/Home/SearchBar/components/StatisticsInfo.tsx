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
        formatter={(value: any) => (
          <CountUp duration={STATISTICS.COUNT_UP_DURATION} end={Number(value)} separator={STATISTICS.SEPARATOR} />
        )}
        value={totalCount}
      />
      条记录
    </Space>
  );
};

export default StatisticsInfo;
