import { Button, Input, Space, Breadcrumb } from 'antd';
import { PlusOutlined, ReloadOutlined, HomeOutlined } from '@ant-design/icons';
import { Link } from 'react-router-dom';

interface UserPageHeaderProps {
  searchText: string;
  loading: boolean;
  onSearch: (value: string) => void;
  onAdd: () => void;
  onReload: () => void;
}

const UserPageHeader: React.FC<UserPageHeaderProps> = ({ searchText, loading, onSearch, onAdd, onReload }) => {
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
          { title: '用户管理', key: 'system/user' },
        ]}
      />
      <Space>
        <Input
          placeholder="输入关键词搜索"
          value={searchText}
          onChange={(e) => onSearch(e.target.value)}
          style={{ width: 240 }}
          allowClear
        />
        <Button icon={<ReloadOutlined />} onClick={onReload} loading={loading}>
          刷新
        </Button>
        <Button type="primary" icon={<PlusOutlined />} onClick={onAdd}>
          添加用户
        </Button>
      </Space>
    </>
  );
};

export default UserPageHeader;
