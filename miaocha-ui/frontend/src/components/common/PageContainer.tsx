import React, { ReactNode } from 'react';
import { useTheme } from '../../providers/ThemeProvider';
import { withPureComponent } from '../../utils/withPureComponent';

interface PageContainerProps {
  title: string;
  children: ReactNode;
  extra?: ReactNode;
  loading?: boolean;
  className?: string;
  style?: React.CSSProperties;
}

const PageContainer: React.FC<PageContainerProps> = ({ title, children, extra, className = '', style = {} }) => {
  const { isDarkMode } = useTheme();

  return (
    <div className={`page-container ${isDarkMode ? 'dark' : ''} ${className}`} style={style}>
      <div className="page-title">
        <h1>{title}</h1>
        {extra && <div className="page-extra">{extra}</div>}
      </div>
      <div className="page-content">{children}</div>
    </div>
  );
};

export default withPureComponent(PageContainer);
