import React, { memo } from 'react';
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
  console.log('渲染：监听UserPageHeader组件'); // 如需调试可打开
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
          allowClear
          placeholder="搜索昵称/邮箱，多个关键词，空格分隔"
          style={{ width: 300 }}
          value={searchText}
          onChange={(e) => onSearch(e.target.value)}
        />
        <Button icon={<ReloadOutlined />} loading={loading} onClick={onReload}>
          刷新
        </Button>
        <Button icon={<PlusOutlined />} type="primary" onClick={() => onAdd()}>
          添加用户
        </Button>
      </Space>
    </>
  );
};

export default memo(UserPageHeader);
