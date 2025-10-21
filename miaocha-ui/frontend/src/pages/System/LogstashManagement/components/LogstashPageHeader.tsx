import { Button, Breadcrumb } from 'antd';
import { PlusOutlined, SyncOutlined, HomeOutlined } from '@ant-design/icons';
import { Link } from 'react-router-dom';

interface LogstashPageHeaderProps {
  loading: boolean;
  onAdd: () => void;
  onReload: () => void;
  className?: string;
}

const LogstashPageHeader = ({ loading, onAdd, onReload, className }: LogstashPageHeaderProps) => {
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
          { title: 'Logstash管理' },
        ]}
      />
      <div className={className || 'table-toolbar'}>
        <Button icon={<SyncOutlined />} loading={loading} type="default" onClick={onReload}>
          刷新
        </Button>
        <Button icon={<PlusOutlined />} type="primary" onClick={onAdd}>
          新增Logstash进程
        </Button>
      </div>
    </>
  );
};

export default LogstashPageHeader;
