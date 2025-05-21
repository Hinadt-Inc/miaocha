import { useState, useEffect } from 'react';
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
}

const Sider: React.FC<IProps> = (props) => {
  const { detailLoading, modules, moduleLoading, onChangeColumns, onSearch, searchParams } = props;
  const [columns, setColumns] = useState<ILogColumnsResponse[]>([]); // 日志表字段
  const [selectedModule, setSelectedModule] = useState<string[]>([]); // 已选模块
  const [distributions, setDistributions] = useState<Record<string, IFieldDistributions>>({}); // 字段值分布列表

  // 获取日志字段
  const getColumns = useRequest(api.fetchColumns, {
    manual: true,
    onSuccess: (res) => {
      // 确保log_time和message字段默认被选中
      const processedColumns = res?.map((column) => {
        if (column.columnName === 'log_time') {
          return { ...column, selected: true };
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
      const fieldName = params[0]?.fields?.[0];
      if (!fieldName) {
        return;
      }
      setDistributions({
        ...distributions,
        [fieldName]: res?.fieldDistributions?.[0],
      } as any);
    },
  });

  // 选择模块时触发，避免重复请求和状态更新
  const changeModules = (value: any[]) => {
    console.log('changeModules', value);
    if (!value) {
      setSelectedModule([]);
      return;
    }
    const [datasourceId, module] = value;
    setSelectedModule([String(datasourceId), String(module)]);
    getColumns.run({ datasourceId: Number(datasourceId), module: String(module) });
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
  const toggleColumn = (_: any, index: number) => {
    // 如果是log_time字段，不允许取消选择
    if (columns[index].columnName === 'log_time' || columns[index].isFixed) {
      return;
    }

    columns[index].selected = !columns[index].selected;
    const updatedColumns = [...columns];
    setColumns(updatedColumns);

    // 通知父组件列变化
    if (onChangeColumns) {
      onChangeColumns(updatedColumns);
    }
  };

  const getDistribution = (data: ILogColumnsResponse, activeKey: string) => {
    console.log('getDistribution', data, activeKey);
    if (activeKey) return;
    const params: ILogSearchParams = {
      ...searchParams,
      fields: [data.columnName] as any,
    };
    queryDistribution.run(params);
  };

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

      <Spin spinning={getColumns.loading || detailLoading || queryDistribution.loading} size="small">
        <Collapse
          ghost
          size="small"
          className={styles.collapse}
          defaultActiveKey={['selected', 'available']}
          items={[
            {
              key: 'selected',
              label: '已选字段',
              children: columns?.map((item, index) =>
                !item.selected ? null : (
                  <FieldListItem
                    key={item.columnName}
                    column={item}
                    columnIndex={index}
                    fieldData={{
                      searchParams,
                      distributions,
                      isSelected: true,
                      onToggle: toggleColumn,
                      onSearch,
                      onDistribution: (activeKey: string) => getDistribution(item, activeKey),
                    }}
                  />
                ),
              ),
            },
            {
              key: 'available',
              label: '可用字段',
              children: columns?.map((item, index) =>
                item.selected ? null : (
                  <FieldListItem
                    key={item.columnName}
                    column={item}
                    columnIndex={index}
                    fieldData={{
                      searchParams,
                      isSelected: false,
                      distributions,
                      onToggle: toggleColumn,
                      onSearch,
                      onDistribution: (activeKey: string) => getDistribution(item, activeKey),
                    }}
                  />
                ),
              ),
            },
          ]}
        />
      </Spin>
    </div>
  );
};
export default Sider;
