import React, { useState, useEffect } from 'react';

import { SearchOutlined, DeleteOutlined } from '@ant-design/icons';
import { useRequest } from 'ahooks';
import { Modal, List, Button, message, Input, Tag, Empty, Popconfirm, Checkbox } from 'antd';
import dayjs from 'dayjs';

import * as logsApi from '@/api/logs';

import { useDataInit } from '../hooks/useDataInit';

import styles from './index.module.less';

interface SavedSearchesModalProps {
  visible: boolean;
  onClose: () => void;
}

const SavedSearchesModal: React.FC<SavedSearchesModalProps> = ({ visible, onClose }) => {
  const { handleLoadCacheData } = useDataInit();
  const [cachedSearches, setCachedSearches] = useState<ICachedSearchCondition[]>([]);
  const [filteredCachedSearches, setFilteredCachedSearches] = useState<ICachedSearchCondition[]>([]);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [selectedKeys, setSelectedKeys] = useState<string[]>([]);
  const [messageApi, contextHolder] = message.useMessage();

  // 获取缓存的搜索条件
  const { run: fetchCachedSearches } = useRequest(logsApi.fetchCachedSearchConditions, {
    manual: true,
    onSuccess: (data) => {
      setCachedSearches(data || []);
      setFilteredCachedSearches(data || []);
    },
    onError: (error) => {
      console.error('获取缓存的搜索条件失败:', error);
      messageApi.error('获取缓存的搜索条件失败');
    },
  });

  // 删除缓存搜索条件
  const { run: deleteCachedSearches, loading: deleteLoading } = useRequest(logsApi.deleteCachedSearchConditions, {
    manual: true,
    onSuccess: () => {
      messageApi.success('删除成功');
      fetchCachedSearches(); // 重新获取列表
      setSelectedKeys([]); // 清空选择
    },
    onError: (error) => {
      console.error('删除缓存搜索条件失败:', error);
      messageApi.error('删除失败');
    },
  });

  // 当弹窗打开时获取数据
  useEffect(() => {
    if (visible) {
      fetchCachedSearches();
    }
  }, [visible, fetchCachedSearches]);

  // 搜索过滤
  useEffect(() => {
    if (!searchKeyword) {
      setFilteredCachedSearches(cachedSearches);
    } else {
      const filteredCached = cachedSearches.filter(
        (search) =>
          search.data.name.toLowerCase().includes(searchKeyword.toLowerCase()) ||
          search.data.description?.toLowerCase().includes(searchKeyword.toLowerCase()),
      );
      setFilteredCachedSearches(filteredCached);
    }
  }, [searchKeyword, cachedSearches]);

  // 加载缓存搜索
  const handleLoad = (search: ICachedSearchCondition) => {
    const { name, description: _description, targetBuckets: _t, ...rest } = search.data;
    handleLoadCacheData(rest as ILogSearchParams);
    messageApi.success(`已加载缓存搜索条件: ${name}`);
    onClose();
  };

  // 删除单个缓存搜索条件
  const handleDeleteSingle = (item: ICachedSearchCondition) => {
    // 根据缓存组进行分组删除
    const cacheGroup = item.cacheGroup || 'default';
    deleteCachedSearches({
      cacheGroup,
      cacheKeys: [item.cacheKey],
    });
  };

  // 批量删除选中的缓存搜索条件
  const handleDeleteSelected = () => {
    if (selectedKeys.length === 0) {
      messageApi.warning('请选择要删除的条件');
      return;
    }

    // 按照缓存组分组删除
    const groupedByCache = selectedKeys.reduce(
      (acc, key) => {
        const item = cachedSearches.find((search) => search.cacheKey === key);
        if (item) {
          const cacheGroup = item.cacheGroup || 'default';
          if (!acc[cacheGroup]) {
            acc[cacheGroup] = [];
          }
          acc[cacheGroup].push(key);
        }
        return acc;
      },
      {} as Record<string, string[]>,
    );

    // 为每个缓存组发送删除请求
    Object.entries(groupedByCache).forEach(([cacheGroup, cacheKeys]) => {
      deleteCachedSearches({
        cacheGroup,
        cacheKeys,
      });
    });
  };

  // 选择缓存条件
  const handleSelect = (cacheKey: string) => {
    setSelectedKeys([...selectedKeys, cacheKey]);
  };

  // 取消选择缓存条件
  const handleUnselect = (cacheKey: string) => {
    setSelectedKeys(selectedKeys.filter((key) => key !== cacheKey));
  };

  // 全选
  const handleSelectAllItems = () => {
    setSelectedKeys(filteredCachedSearches.map((item) => item.cacheKey));
  };

  // 取消全选
  const handleUnselectAll = () => {
    setSelectedKeys([]);
  };

  // 处理选择框变化
  const handleCheckboxChange = (cacheKey: string) => (e: any) => {
    const checked = e.target.checked;
    if (checked) {
      handleSelect(cacheKey);
    } else {
      handleUnselect(cacheKey);
    }
  };

  // 处理全选框变化
  const handleSelectAllChange = (e: any) => {
    const checked = e.target.checked;
    if (checked) {
      handleSelectAllItems();
    } else {
      handleUnselectAll();
    }
  };

  // 时间格式转译
  const formatCreateTime = (createTime: string | number) => {
    // 输入验证
    if (!createTime) {
      return '无效时间';
    }

    const time = dayjs(createTime);

    // 验证时间是否有效
    if (!time.isValid()) {
      return '无效时间';
    }

    const now = dayjs();
    // 使用 startOf('day') 确保比较的是日期而不是具体时间点
    const diffDays = now.startOf('day').diff(time.startOf('day'), 'day');

    // 处理未来时间
    if (diffDays < 0) {
      return time.format('YYYY-MM-DD HH:mm');
    }

    if (diffDays === 0) {
      return `今天 ${time.format('HH:mm')}`;
    } else if (diffDays === 1) {
      return `昨天 ${time.format('HH:mm')}`;
    } else if (diffDays < 7) {
      return `${diffDays}天前 ${time.format('HH:mm')}`;
    } else if (time.year() === now.year()) {
      // 同一年显示月日
      return time.format('MM-DD HH:mm');
    } else {
      // 不同年份显示完整日期
      return time.format('YYYY-MM-DD HH:mm');
    }
  };

  // 时间范围转译
  const translateTimeRange = (timeRange: string) => {
    const timeRangeMap: Record<string, string> = {
      auto: '自动',
      last_5m: '最近5分钟',
      last_15m: '最近15分钟',
      last_30m: '最近30分钟',
      last_1h: '最近1小时',
      last_8h: '最近8小时',
      last_24h: '最近24小时',
      last_7d: '最近7天',
      last_2week: '最近2周',
      today: '今天',
      yesterday: '昨天',
      this_week: '本周',
      last_week: '上周',
      custom: '自定义时间范围',
    };

    return timeRangeMap[timeRange] || timeRange;
  };

  // 渲染搜索条件预览
  const renderSearchPreview = (searchParams: any) => {
    const previews = [];

    // 1. 模块
    if (searchParams?.module) {
      previews.push(
        <Tag key="module" color="purple">
          模块: {searchParams.module}
        </Tag>,
      );
    }

    // 2. 时间分组
    const timeGrouping = searchParams?.timeGrouping;
    if (timeGrouping) {
      const timeGranularityMap: Record<string, string> = {
        HOUR: '按小时分组',
        DAY: '按天分组',
        WEEK: '按周分组',
        MONTH: '按月分组',
        YEAR: '按年分组',
        MINUTE: '按分钟分组',
        SECOND: '按秒分组',
      };
      const groupText = timeGranularityMap[timeGrouping.toUpperCase()] || `按${timeGrouping}分组`;
      previews.push(
        <Tag key="timeGrouping" color="geekblue">
          {groupText}
        </Tag>,
      );
    }

    // 3. 时间
    if (searchParams?.timeRange) {
      previews.push(
        <Tag key="time" color="orange">
          时间: {translateTimeRange(searchParams.timeRange)}
        </Tag>,
      );
    }

    // 4. 关键词
    if (searchParams?.keywords?.length > 0) {
      previews.push(
        <Tag key="keywords" color="blue">
          关键词: {searchParams.keywords.join(', ')}
        </Tag>,
      );
    }

    // 5. SQL
    if (searchParams?.whereSqls?.length > 0) {
      // 如果SQL条件较少，显示具体内容
      if (searchParams.whereSqls.length <= 3) {
        searchParams.whereSqls.forEach((sql: string, index: number) => {
          previews.push(
            <Tag key={`sql-${sql.substring(0, 20)}-${index}`} color="green">
              SQL: {sql}
            </Tag>,
          );
        });
      } else {
        // 如果SQL条件较多，显示前2个和总数
        searchParams.whereSqls.slice(0, 2).forEach((sql: string, index: number) => {
          previews.push(
            <Tag key={`sql-${sql.substring(0, 20)}-${index}`} color="green">
              SQL: {sql}
            </Tag>,
          );
        });
        previews.push(
          <Tag key="sql-more" color="green">
            ...等共{searchParams.whereSqls.length}个SQL条件
          </Tag>,
        );
      }
    }

    return previews.length > 0 ? previews : <Tag color="default">无特定条件</Tag>;
  };

  return (
    <>
      {contextHolder}
      <Modal
        className={styles.savedSearchesModal}
        footer={null}
        open={visible}
        title="检索书签"
        width={800}
        onCancel={onClose}
      >
        <div className={styles.topBar}>
          <div className={styles.leftActions}>
            {filteredCachedSearches.length > 0 && (
              <>
                <Checkbox
                  checked={selectedKeys.length === filteredCachedSearches.length}
                  indeterminate={selectedKeys.length > 0 && selectedKeys.length < filteredCachedSearches.length}
                  onChange={handleSelectAllChange}
                >
                  全选
                </Checkbox>
                {selectedKeys.length > 0 && (
                  <Popconfirm
                    cancelText="取消"
                    okText="确定"
                    title={`确定要删除选中的 ${selectedKeys.length} 个搜索条件吗？`}
                    onConfirm={handleDeleteSelected}
                  >
                    <Button danger icon={<DeleteOutlined />} loading={deleteLoading} size="small" type="primary">
                      删除选中 ({selectedKeys.length})
                    </Button>
                  </Popconfirm>
                )}
              </>
            )}
          </div>
          <div className={styles.rightActions}>
            <Input
              allowClear
              className={styles.searchInput}
              placeholder="搜索缓存的条件..."
              prefix={<SearchOutlined />}
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
            />
          </div>
        </div>
        {filteredCachedSearches.length === 0 ? (
          <Empty description={cachedSearches.length === 0 ? '暂无缓存的搜索条件' : '没有找到匹配的缓存搜索条件'} />
        ) : (
          <List
            className={styles.searchList}
            dataSource={filteredCachedSearches}
            renderItem={(item: ICachedSearchCondition) => (
              <List.Item
                actions={[
                  <Button key="load" size="small" type="primary" onClick={() => handleLoad(item)}>
                    加载
                  </Button>,
                  <Popconfirm
                    key="delete"
                    cancelText="取消"
                    okText="确定"
                    title="确定要删除这个搜索条件吗？"
                    onConfirm={() => handleDeleteSingle(item)}
                  >
                    <Button danger loading={deleteLoading} size="small">
                      删除
                    </Button>
                  </Popconfirm>,
                ]}
                className={styles.searchItem}
              >
                <List.Item.Meta
                  avatar={
                    <Checkbox
                      checked={selectedKeys.includes(item.cacheKey)}
                      onChange={handleCheckboxChange(item.cacheKey)}
                    />
                  }
                  description={
                    <div className={styles.searchDescription}>
                      {item.data.description && <div className={styles.description}>{item.data.description}</div>}
                      <div className={styles.searchPreview}>{renderSearchPreview(item.data)}</div>
                    </div>
                  }
                  title={
                    <div className={styles.searchTitle}>
                      <span className={styles.searchName}>{item.data.name}</span>
                      <span className={styles.searchTime}>{formatCreateTime(item.createTime)}</span>
                    </div>
                  }
                />
              </List.Item>
            )}
          />
        )}
      </Modal>
    </>
  );
};

export default SavedSearchesModal;
