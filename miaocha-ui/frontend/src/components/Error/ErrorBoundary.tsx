// 只捕获“渲染阶段”的错误：构造函数、render、生命周期（以及函数组件渲染时抛错）
// 它不捕获：
//   Promise 异常/异步回调里的错误（axios 拦截器、fetch、setTimeout、事件回调等）
//   window 上派发的自定义事件错误
import { Component, ErrorInfo, ReactNode } from 'react';
import { Result, Button } from 'antd';
import { ReloadOutlined, HomeOutlined } from '@ant-design/icons';

interface Props {
  children?: ReactNode;
}

interface State {
  hasError: boolean; // 是否出现错误
  error?: Error; // 当前错误
  errorInfo?: ErrorInfo; // 当前错误信息
}

class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
  };

  public static getDerivedStateFromError(error: Error): State {
    console.log(1, error);
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    this.setState({ error, errorInfo });
  }

  private handleReload = () => {
    window.location.reload();
  };

  private handleGoHome = () => {
    window.location.href = '/';
  };

  render() {
    const { hasError } = this.state;

    if (hasError) {
      return (
        <Result
          status="error"
          title="页面出现了错误"
          subTitle={
            process.env.NODE_ENV === 'development' && this.state.error
              ? this.state.error.message
              : '抱歉，发生了未预期的错误'
          }
          extra={[
            <Button key="reload" type="primary" icon={<ReloadOutlined />} onClick={this.handleReload}>
              刷新页面
            </Button>,
            <Button key="home" icon={<HomeOutlined />} onClick={this.handleGoHome}>
              返回首页
            </Button>,
          ]}
        >
          {process.env.NODE_ENV === 'development' && this.state.errorInfo && (
            <details open>
              <summary>错误详情 (仅开发环境显示)</summary>
              <pre style={{ whiteSpace: 'pre-wrap' }}>{this.state.error?.stack}</pre>
            </details>
          )}
        </Result>
      );
    }

    return this.props.children as ReactNode;
  }
}

export default ErrorBoundary;
