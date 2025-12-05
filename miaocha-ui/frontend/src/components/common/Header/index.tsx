import { UserOutlined } from '@ant-design/icons';
import { Layout, Dropdown, Avatar } from 'antd';

import styles from './index.module.less';

const { Header: AntdHeader } = Layout;

const Header = () => {
  const menuItems = [
    {
      key: 'profile',
      label: '个人中心',
    },
    {
      key: 'logout',
      label: '退出登录',
    },
  ];

  return (
    <AntdHeader className={styles.header}>
      <div className={styles.logo}>日志管理系统</div>
      <div className={styles.userInfo}>
        <Dropdown menu={{ items: menuItems }} trigger={['click']}>
          <div className={styles.user}>
            <Avatar icon={<UserOutlined />} />
            <span className={styles.name}>管理员</span>
          </div>
        </Dropdown>
      </div>
    </AntdHeader>
  );
};

export default Header;
