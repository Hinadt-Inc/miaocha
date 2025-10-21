import { Button, Breadcrumb, Input, Space } from 'antd';
import { PlusOutlined, SearchOutlined, HomeOutlined } from '@ant-design/icons';
import { Link } from 'react-router-dom';

interface DataSourcePageHeaderProps {
  // 只保留面包屑相关的props，工具栏将单独处理
}

interface DataSourceToolBarProps {
  searchKeyword: string;
  onSearchChange: (value: string) => void;
  onAdd: () => void;
}

// 面包屑组件
const DataSourcePageHeader: React.FC<DataSourcePageHeaderProps> = () => {
  return (
    <Breadcrumb
      items={[
        {
          title: (
            <Link to="/">
              <HomeOutlined />
            </Link>
          ),
        },
        { title: '数据源管理' },
      ]}
    />
  );
};

// 工具栏组件
export const DataSourceToolBar: React.FC<DataSourceToolBarProps> = ({ searchKeyword, onSearchChange, onAdd }) => {
  return (
    <Space size={16}>
      <Input
        allowClear
        placeholder="搜索数据源（支持名称、JDBC地址、描述等关键词）"
        style={{ width: 300 }}
        suffix={<SearchOutlined />}
        value={searchKeyword}
        onChange={(e) => onSearchChange(e.target.value)}
      />
      <Button key="button" icon={<PlusOutlined />} type="primary" onClick={onAdd}>
        新增数据源
      </Button>
    </Space>
  );
};

export default DataSourcePageHeader;
