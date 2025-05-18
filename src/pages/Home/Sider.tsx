import { useState, useEffect } from 'react';
import { Collapse, Cascader, Spin } from 'antd';
import { useRequest } from 'ahooks';
import { getTableColumns } from '@/api/logs';
import styles from './Sider.module.less';

import FieldListItem from './FieldListItem';

interface IProps {
  moduleNames: IStatus[]; // 模块名称列表
  logLoading: boolean; // 日志数据是否正在加载
  moduleLoading: boolean; // 模块名称是否正在加载
  fieldDistributions: IFieldDistributions[]; // 字段值分布列表
  onColumnsChange?: (columns: ILogColumnsResponse[]) => void; // 列变化回调函数
}

const Sider: React.FC<IProps> = (props) => {
  const { fieldDistributions, logLoading, moduleNames, moduleLoading, onColumnsChange } = props;
  const [logColumns, setLogColumns] = useState<ILogColumnsResponse[]>([]); // 日志字段
  const [selectedModule, setSelectedModule] = useState<string[] | undefined>(undefined); // 已选模块

  // 获取日志字段
  const fetchColumns = useRequest(getTableColumns, {
    manual: true,
    onSuccess: (res: ILogColumnsResponse[]) => {
      // 确保log_time和message字段默认被选中
      const processedColumns = res.map((column) => {
        if (column.columnName === 'log_time') {
          return { ...column, selected: true };
        }
        return column;
      });

      // 如果没有其他已选字段，自动添加_source字段
      const hasOtherSelected = processedColumns.some((col) => col.selected && col.columnName !== 'log_time');
      if (!hasOtherSelected) {
        processedColumns.unshift({
          columnName: '_source',
          selected: true,
          isFixed: true, // 标记为不可删除
        });
      }
      setLogColumns(processedColumns);
      // 初始加载时也通知父组件列变化
      if (onColumnsChange) {
        onColumnsChange(processedColumns);
      }
    },
  });

  // 选择模块时触发，避免重复请求和状态更新
  const changeLogColumns = (value: any[]) => {
    if (!value) {
      setSelectedModule(undefined);
      return;
    }
    const [datasourceId, module] = value;
    const newValue = [String(datasourceId), String(module)];

    if (selectedModule && selectedModule[0] === newValue[0] && selectedModule[1] === newValue[1]) {
      return;
    }
    setSelectedModule(newValue);
    fetchColumns.run({ datasourceId: Number(datasourceId), module: String(module) });
  };

  // 当 moduleNames 加载完成后，自动选择第一个数据源和第一个模块
  useEffect(() => {
    if (moduleNames?.length > 0 && !selectedModule) {
      const firstModule = moduleNames[0];
      if (firstModule.children?.[0]) {
        const datasourceId = String(firstModule.value);
        const module = String(firstModule.children[0].value);
        setSelectedModule([datasourceId, module]);
        fetchColumns.run({ datasourceId: Number(datasourceId), module });
      }
    }
  }, [moduleNames]);

  // 切换字段选中状态
  const toggleColumns = (_: any, index: number) => {
    // 如果是log_time字段，不允许取消选择
    if (logColumns[index].columnName === 'log_time' || logColumns[index].isFixed) {
      return;
    }

    logColumns[index].selected = !logColumns[index].selected;
    const updatedColumns = [...logColumns];
    setLogColumns(updatedColumns);

    // 通知父组件列变化
    if (onColumnsChange) {
      onColumnsChange(updatedColumns);
    }
  };

  return (
    <div className={styles.layoutSider}>
      <Cascader
        allowClear
        showSearch
        variant="filled"
        placeholder="请选择模块"
        expandTrigger="hover"
        options={moduleNames}
        value={selectedModule}
        onChange={changeLogColumns}
        loading={moduleLoading}
        disabled={moduleLoading || logLoading}
      />

      <Spin spinning={fetchColumns.loading || logLoading} size="small">
        <Collapse
          ghost
          size="small"
          className={styles.collapse}
          defaultActiveKey={['selected', 'available']}
          items={[
            {
              key: 'selected',
              label: '已选字段',
              children: logColumns?.map((item, index) =>
                !item.selected ? null : (
                  <FieldListItem
                    key={item.columnName}
                    item={item}
                    index={index}
                    fieldDistributions={fieldDistributions}
                    isSelected={true}
                    onToggle={toggleColumns}
                  />
                ),
              ),
            },
            {
              key: 'available',
              label: '可用字段',
              children: logColumns?.map((item, index) =>
                item.selected ? null : (
                  <FieldListItem
                    key={item.columnName}
                    item={item}
                    index={index}
                    fieldDistributions={fieldDistributions}
                    isSelected={false}
                    onToggle={toggleColumns}
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
