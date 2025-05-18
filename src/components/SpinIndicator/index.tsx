import { Spin } from 'antd';
import { LoadingOutlined } from '@ant-design/icons';

const SpinIndicator = () => {
  return <Spin indicator={<LoadingOutlined spin />} size="small" />;
};
export default SpinIndicator;
