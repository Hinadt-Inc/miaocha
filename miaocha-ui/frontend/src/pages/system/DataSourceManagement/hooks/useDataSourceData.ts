import { useState } from 'react';
import { getAllDataSources } from '@/api/datasource';
import type { DataSource } from '@/types/datasourceTypes';
import type { RequestData, ParamsType } from '@ant-design/pro-components';
import type { SortOrder } from 'antd/lib/table/interface';

export type DataSourceItem = DataSource;

export const useDataSourceData = () => {
  const [searchKeyword, setSearchKeyword] = useState<string>('');
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
  });
  const [loading, setLoading] = useState({
    table: false,
    submit: false,
    test: false,
    testExisting: {} as Record<string, boolean>, // 为每个数据源维护测试状态
  });

  const setSubmitLoading = (loading: boolean) => {
    setLoading((prev) => ({ ...prev, submit: loading }));
  };

  const setTestLoading = (loading: boolean) => {
    setLoading((prev) => ({ ...prev, test: loading }));
  };

  const setTestExistingLoading = (id: string, loading: boolean) => {
    setLoading((prev) => ({
      ...prev,
      testExisting: { ...prev.testExisting, [id]: loading },
    }));
  };

  // 获取数据源列表
  const fetchDataSources: (
    params: ParamsType & {
      current?: number;
      pageSize?: number;
    },
    _: Record<string, SortOrder>,
    __: Record<string, (string | number)[] | null>,
  ) => Promise<RequestData<DataSourceItem>> = async (params) => {
    try {
      const data = await getAllDataSources();
      if (!data) {
        return {
          data: [],
          success: false,
          total: 0,
        };
      }

      // 直接使用最新获取的数据进行过滤
      let filteredData = [...data];

      // 使用 searchKeyword 状态进行前端筛选
      const keyword = searchKeyword.trim();
      if (keyword) {
        const lowercaseKeyword = keyword.toLowerCase();
        filteredData = filteredData.filter(
          (item) =>
            item.name.toLowerCase().includes(lowercaseKeyword) ||
            item.database.toLowerCase().includes(lowercaseKeyword) ||
            item.ip?.toLowerCase().includes(lowercaseKeyword) ||
            item.description?.toLowerCase().includes(lowercaseKeyword),
        );
      }

      // 分页处理
      const pageSize = params.pageSize ?? pagination.pageSize;
      const current = params.current ?? pagination.current;
      const start = (current - 1) * pageSize;
      const end = start + pageSize;

      return {
        data: filteredData.slice(start, end),
        success: true,
        total: filteredData.length,
      };
    } catch {
      // 数据加载失败时的错误处理已由全局错误处理器处理
      return {
        data: [],
        success: false,
        total: 0,
      };
    }
  };

  // 处理分页变更
  const handlePageChange = (page: number, pageSize: number) => {
    setPagination({
      current: page,
      pageSize,
    });
  };

  // 处理搜索
  const handleSearchChange = (value: string) => {
    setSearchKeyword(value);
    // 搜索时重置为第一页，但保留每页条数
    setPagination((prev) => ({ ...prev, current: 1 }));
  };

  return {
    // 数据状态
    searchKeyword,
    pagination,
    loading,

    // 操作方法
    fetchDataSources,
    handlePageChange,
    handleSearchChange,
    setSubmitLoading,
    setTestLoading,
    setTestExistingLoading,
  };
};
