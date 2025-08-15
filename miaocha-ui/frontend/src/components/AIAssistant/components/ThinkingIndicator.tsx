import React from 'react';
import styles from './ThinkingIndicator.module.less';

interface IThinkingIndicatorProps {
  message?: string;
}

export const ThinkingIndicator: React.FC<IThinkingIndicatorProps> = ({ 
  message = 'AI正在思考中...' 
}) => {
  return (
    <div className={styles.thinkingContainer}>
      <div className={styles.thinkingContent}>
        <div className={styles.dotsContainer}>
          <div className={styles.dot}></div>
          <div className={styles.dot}></div>
          <div className={styles.dot}></div>
        </div>
        <span className={styles.thinkingText}>{message}</span>
      </div>
    </div>
  );
};