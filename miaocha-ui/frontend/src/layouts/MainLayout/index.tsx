import { Layout } from 'antd';
import styles from './index.module.less';

const { Content } = Layout;

const MainLayout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  return (
    <Layout className={styles.layout}>
      <Content className={styles.content}>{children}</Content>
    </Layout>
  );
};

export default MainLayout;
