import { Button, Breadcrumb } from 'antd';
import { PlusOutlined, HomeOutlined } from '@ant-design/icons';
import { Link } from 'react-router-dom';

interface MachinePageHeaderProps {
  loading: boolean;
  onAdd: () => void;
}

const MachinePageHeader: React.FC<MachinePageHeaderProps> = ({ loading, onAdd }) => {
  return (
    <>
      <Breadcrumb
        items={[
          {
            title: (
              <Link to="/">
                <HomeOutlined />
              </Link>
            ),
          },
          { title: '服务器管理' },
        ]}
      />
      <div className="actions">
        <Button icon={<PlusOutlined />} loading={loading} type="primary" onClick={onAdd}>
          新增机器
        </Button>
      </div>
    </>
  );
};

export default MachinePageHeader;
