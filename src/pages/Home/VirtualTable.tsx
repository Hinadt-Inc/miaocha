import { useEffect, useRef, useState, Fragment } from 'react';
import { Table } from 'antd';
import ExpandedRow from './ExpandedRow';
import styles from './VirtualTable.module.less';

interface IProps {
  data: any[]; // 数据
  loading?: boolean; // 加载状态
  onLoadMore: () => void; // 加载更多数据的回调函数
  hasMore?: boolean; // 是否还有更多数据
  dynamicColumns?: ILogColumnsResponse[]; // 动态列配置
}

const VirtualTable = (props: IProps) => {
  const { data, loading = false, onLoadMore, hasMore = false, dynamicColumns } = props;
  const containerRef = useRef<HTMLDivElement>(null); // 滚动容器的ref
  const tblRef: Parameters<typeof Table>[0]['ref'] = useRef(null); // 表格的ref
  const [containerHeight, setContainerHeight] = useState<number>(0); // 容器高度
  const [headerHeight, setHeaderHeight] = useState<number>(0); // 表头高度

  const columns = [
    {
      title: 'log_time',
      dataIndex: 'log_time',
      width: 190,
    },
    {
      title: '_source',
      dataIndex: '_source',
      width: undefined,
      ellipsis: false,
      render: (_: any, record: ILogColumnsResponse) => {
        return (
          <dl className={styles.source}>
            {Object.entries(record).map(([key, value]) => (
              <Fragment key={key}>
                <dt>{key}</dt>
                <dd>
                  <span>{value}</span>
                </dd>
              </Fragment>
            ))}
          </dl>
        );
      },
    },
  ];

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;
    // 初始设置高度
    setContainerHeight(container.clientHeight);

    // 监听容器大小变化
    const resizeObserver = new ResizeObserver((entries) => {
      for (const entry of entries) {
        if (entry.target === container) {
          setContainerHeight(entry.contentRect.height);
        }
      }
    });

    resizeObserver.observe(container);

    // 获取表头高度
    const tableNode = tblRef.current?.nativeElement;
    if (tableNode) {
      const header = tableNode.querySelector('.ant-table-thead');
      if (header) {
        setHeaderHeight(header.clientHeight);
      }
    }

    // 添加滚动事件监听
    const handleScroll = () => {
      if (!hasMore || loading) return;

      const scrollElement = tableNode?.querySelector('.ant-table-tbody-virtual-holder');
      if (scrollElement) {
        const { scrollHeight, scrollTop, clientHeight } = scrollElement;
        const distanceToBottom = scrollHeight - scrollTop - clientHeight;

        if (distanceToBottom < 100) {
          onLoadMore();
        }
      }
    };

    if (tableNode) {
      const scrollElement = tableNode.querySelector('.ant-table-tbody-virtual-holder');
      if (scrollElement) {
        scrollElement.addEventListener('scroll', handleScroll);
      }
    }

    // 清理事件监听器和ResizeObserver
    return () => {
      resizeObserver.disconnect();
      if (tableNode) {
        const scrollElement = tableNode.querySelector('.ant-table-tbody-virtual-holder');
        if (scrollElement) {
          scrollElement.removeEventListener('scroll', handleScroll);
        }
      }
    };
  }, [containerRef.current, tblRef.current, hasMore, loading, onLoadMore]);

  return (
    <div className={styles.virtualLayout} ref={containerRef}>
      <Table
        virtual
        size="small"
        ref={tblRef}
        rowKey="_key"
        dataSource={data}
        pagination={false}
        columns={columns}
        scroll={{ x: 800, y: containerHeight - headerHeight }}
        expandable={{
          columnWidth: 26,
          expandedRowRender: (record) => <ExpandedRow data={record} />,
        }}
      />
    </div>
  );
};

export default VirtualTable;
