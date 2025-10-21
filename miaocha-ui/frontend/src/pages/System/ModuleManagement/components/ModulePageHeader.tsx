import { Space, Input, Button, Breadcrumb } from 'antd';
import { SearchOutlined, PlusOutlined, ReloadOutlined, HomeOutlined } from '@ant-design/icons';
import { Link } from 'react-router-dom';
import styles from '../ModuleManagement.module.less';

interface ModulePageHeaderProps {
  searchText: string;
  loading: boolean;
  onSearch: (value: string) => void;
  onAdd: () => void;
  onReload: () => void;
}

const ModulePageHeader: React.FC<ModulePageHeaderProps> = ({ searchText, loading, onSearch, onAdd, onReload }) => {
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
          { title: '模块管理', key: 'system/module' },
        ]}
      />
      <Space>
        <Input
          allowClear
          className={styles.searchInput}
          placeholder="搜索模块/数据源/表名"
          suffix={<SearchOutlined />}
          value={searchText}
          onChange={(e) => onSearch(e.target.value)}
        />
        <Button icon={<ReloadOutlined />} loading={loading} onClick={onReload}>
          刷新
        </Button>
        <Button icon={<PlusOutlined />} type="primary" onClick={onAdd}>
          添加模块
        </Button>
      </Space>
    </>
  );
};

export default ModulePageHeader;
