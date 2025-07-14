import { Component, ErrorInfo, ReactNode } from 'react';
import { Result, Button } from 'antd';
import { ReloadOutlined, HomeOutlined } from '@ant-design/icons';
import styles from './index.module.less';

interface Props {
  children?: ReactNode;
  fallback?: ReactNode;
  onError?: (error: Error, errorInfo: ErrorInfo) => void;
}

interface State {
  hasError: boolean;
  error?: Error;
  errorInfo?: ErrorInfo;
}

class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
  };

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('ErrorBoundary caught an error:', error, errorInfo);

    this.setState({
      error,
      errorInfo,
    });

    // 调用外部错误处理函数
    if (this.props.onError) {
      this.props.onError(error, errorInfo);
    }

    // 这里可以集成错误监控服务
    this.logErrorToService(error, errorInfo);
  }

  private logErrorToService = (error: Error, errorInfo: ErrorInfo) => {
    // 可以集成 Sentry、LogRocket 等错误监控服务
    console.error('Error logged to service:', {
      error: error.message,
      stack: error.stack,
      componentStack: errorInfo.componentStack,
      timestamp: new Date().toISOString(),
      userAgent: navigator.userAgent,
      url: window.location.href,
    });
  };

  private handleReload = () => {
    window.location.reload();
  };

  private handleGoHome = () => {
    window.location.href = '/';
  };

  private handleRetry = () => {
    this.setState({ hasError: false, error: undefined, errorInfo: undefined });
  };

  public render() {
    if (this.state.hasError) {
      // 如果提供了自定义 fallback，使用它
      if (this.props.fallback) {
        return this.props.fallback;
      }

      // 默认错误页面
      return (
        <div className={styles.errorBoundaryContainer}>
          <Result
            status="error"
            title="页面出现了错误"
            subTitle="抱歉，页面出现了未预期的错误。您可以尝试刷新页面或返回首页。"
            extra={[
              <Button key="retry" type="primary" icon={<ReloadOutlined />} onClick={this.handleRetry}>
                重试
              </Button>,
              <Button key="reload" icon={<ReloadOutlined />} onClick={this.handleReload}>
                刷新页面
              </Button>,
              <Button key="home" icon={<HomeOutlined />} onClick={this.handleGoHome}>
                返回首页
              </Button>,
            ]}
          >
            {process.env.NODE_ENV === 'development' && this.state.error && (
              <div className={styles.errorDetails}>
                <details className={styles.errorDetails}>
                  <summary>错误详情 (仅开发环境显示)</summary>
                  <p className={styles.errorMessage}>{this.state.error.toString()}</p>
                  {this.state.errorInfo && <p className={styles.errorStack}>{this.state.errorInfo.componentStack}</p>}
                </details>
              </div>
            )}
          </Result>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
