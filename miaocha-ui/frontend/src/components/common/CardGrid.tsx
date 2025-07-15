import React, { ReactNode } from 'react';
import { withPureComponent } from '../../utils/withPureComponent';
import './CardGrid.less';

interface CardGridProps {
  children: ReactNode;
  gutter?: number;
  className?: string;
  style?: React.CSSProperties;
}

const CardGrid: React.FC<CardGridProps> = ({
  children,
  gutter = 16,
  className = '',
  style = {},
}) => {
  return (
    <div 
      className={`card-grid ${className}`}
      style={{ 
        gap: gutter,
        ...style 
      }}
    >
      {children}
    </div>
  );
};

export default withPureComponent(CardGrid);