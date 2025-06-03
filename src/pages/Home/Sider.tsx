import { useState, useEffect, useMemo } from 'react';
import { Collapse, Cascader, Spin } from 'antd';
import { useRequest } from 'ahooks';
import * as api from '@/api/logs';
import styles from './Sider.module.less';

import FieldListItem from './FieldListItem';

interface IProps {
  searchParams: ILogSearchParams; // 搜索参数
  modules: IStatus[]; // 模块名称列表
  moduleLoading: boolean; // 模块名称是否正在加载
  detailLoading: boolean; // 日志数据是否正在加载
  onSearch: (params: ILogSearchParams) => void; // 搜索回调函数
  onChangeColumns?: (params: ILogColumnsResponse[]) => void; // 列变化回调函数
  setWhereSqlsFromSider: any; // 设置where条件
}

const Sider: React.FC<IProps> = (props) => {
  const { detailLoading, modules, moduleLoading, onChangeColumns, onSearch, searchParams, setWhereSqlsFromSider } =
    props;
  const [columns, setColumns] = useState<ILogColumnsResponse[]>([]); // 日志表字段
  const [selectedModule, setSelectedModule] = useState<string[]>([]); // 已选模块
  const [distributions, setDistributions] = useState<Record<string, IFieldDistributions>>({}); // 字段值分布列表
  const [activeColumns, setActiveColumns] = useState<string[]>([]); // 激活的字段
  // 获取日志字段
  const getColumns = useRequest(api.fetchColumns, {
    manual: true,
    onSuccess: (res) => {
      // 确保log_time和message字段默认被选中
      const processedColumns = res?.map((column) => {
        if (column.columnName === 'log_time') {
          return { ...column, selected: true, _createTime: new Date().getTime() };
        }
        return column;
      });

      setColumns(processedColumns);
      // 初始加载时也通知父组件列变化
      if (onChangeColumns) {
        onChangeColumns(processedColumns);
      }
    },
    onError: () => {
      setColumns([]);
    },
  });

  // 获取指定字段的TOP5分布数据
  const queryDistribution = useRequest(api.fetchDistributions, {
    manual: true,
    onSuccess: (res, params: ILogSearchParams[]) => {
      const { fields = [] } = params[0] || {};
      const target: any = {};
      fields.forEach((fieldName) => {
        target[fieldName] = res?.fieldDistributions?.find((item) => item.fieldName === fieldName) || {};
      });
      setDistributions(target);
    },
    onError: () => {
      setDistributions({});
    },
  });

  // 选择模块时触发，避免重复请求和状态更新
  const changeModules = (value: any[]) => {
    if (!value) {
      setSelectedModule([]);
      return;
    }
    const [datasourceId, module] = value;
    const moduleStr = String(module);
    setSelectedModule([String(datasourceId), moduleStr]);
    getColumns.run({ datasourceId: Number(datasourceId), module: moduleStr });
    onSearch({
      ...searchParams,
      datasourceId: Number(datasourceId),
      module: moduleStr,
      offset: 0,
    });
  };

  // 当 modules 加载完成后，自动选择第一个数据源和第一个模块
  useEffect(() => {
    if (modules?.length > 0 && selectedModule?.length === 0) {
      const first = modules[0];
      if (first?.children?.[0]) {
        const datasourceId = String(first.value);
        const module = String(first.children[0].value);
        setSelectedModule([datasourceId, module]);
        getColumns.run({ datasourceId: Number(datasourceId), module });
      }
    }
  }, [modules]);

  // 切换字段选中状态
  const toggleColumn = (data: ILogColumnsResponse) => {
    const index = columns.findIndex((item) => item.columnName === data.columnName);
    // 如果是log_time字段，不允许取消选择
    if (columns[index].columnName === 'log_time' || columns[index].isFixed) {
      return;
    }
    // 添加
    if (!columns[index].selected) {
      columns[index].selected = true;
      columns[index]._createTime = new Date().getTime();
    } else {
      // 删除
      columns[index].selected = false;
      delete columns[index]._createTime;
    }
    // 排序
    const sortedColumns = columns.sort((a, b) => (a._createTime || 0) - (b._createTime || 0));
    const updatedColumns = [...sortedColumns];
    setColumns(updatedColumns);

    // 通知父组件列变化
    if (onChangeColumns) {
      onChangeColumns(updatedColumns);
    }
  };

  // 获取字段值分布
  const getDistribution = (columnName: string, newActiveColumns: string[], sql: string) => {
    if (!newActiveColumns.includes(columnName)) return;
    const params: ILogSearchParams = {
      ...searchParams,
      fields: newActiveColumns,
      offset: 0,
    };
    if (sql) {
      params.whereSqls = [...(searchParams?.whereSqls || []), sql];
    }
    queryDistribution.run(params);
  };

  const fieldListProps = useMemo(() => {
    return {
      searchParams,
      distributions,
      activeColumns,
      onToggle: toggleColumn,
      setWhereSqlsFromSider,
      onActiveColumns: setActiveColumns,
      onDistribution: (columnName: string, newActiveColumns: string[], sql: string) =>
        getDistribution(columnName, newActiveColumns, sql),
    };
  }, [searchParams, distributions, activeColumns, toggleColumn, getDistribution, setWhereSqlsFromSider]);

  return (
    <div className={styles.layoutSider}>
      <Cascader
        showSearch
        allowClear={false}
        variant="filled"
        placeholder="请选择模块"
        expandTrigger="hover"
        options={modules}
        value={selectedModule}
        onChange={changeModules}
        loading={moduleLoading}
        disabled={moduleLoading || detailLoading}
      />

      <Spin spinning={getColumns.loading || queryDistribution.loading} size="small">
        <Collapse
          ghost
          size="small"
          className={styles.collapse}
          defaultActiveKey={['selected', 'available']}
          items={[
            {
              key: 'selected',
              label: '已选字段',
              children: columns
                ?.filter((item) => item.selected)
                ?.sort((a, b) => (a._createTime || 0) - (b._createTime || 0))
                ?.map((item, index) => (
                  <FieldListItem
                    key={item.columnName}
                    isSelected
                    column={item}
                    columnIndex={index}
                    fieldData={fieldListProps}
                  />
                )),
            },
            {
              key: 'available',
              label: '可用字段',
              children: columns
                ?.filter((item) => !item.selected)
                .sort((a, b) => {
                  if (!a.columnName) return -1;
                  if (!b.columnName) return 1;
                  return a.columnName.localeCompare(b.columnName);
                })
                ?.map((item, index) => (
                  <FieldListItem
                    key={item.columnName}
                    isSelected={false}
                    column={item}
                    columnIndex={index}
                    fieldData={fieldListProps}
                  />
                )),
            },
          ]}
        />
      </Spin>
    </div>
  );
};
export default Sider;
