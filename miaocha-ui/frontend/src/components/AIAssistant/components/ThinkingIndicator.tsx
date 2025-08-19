import React from 'react';
import styles from './ThinkingIndicator.module.less';

interface IThinkingIndicatorProps {
  message?: string;
  size?: 'small' | 'medium' | 'large';
  theme?: 'default' | 'welcome';
}

export const ThinkingIndicator: React.FC<IThinkingIndicatorProps> = ({
  message = 'AI正在思考中...',
  size = 'medium',
  theme = 'default',
}) => {
  return (
    <div className={`${styles.thinkingContainer} ${styles[size]} ${styles[theme]}`}>
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
