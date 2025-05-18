import { useState, useCallback, useEffect, useMemo } from 'react';
import { Collapse, Cascader, Spin } from 'antd';
import { useRequest } from 'ahooks';
// import classNames from 'classnames';
import { getTableColumns } from '@/api/logs';
import styles from './Sider.module.less';

import FieldListItem from './FieldListItem';

// 字段值分布列表，按数量降序排序
interface IValueDistributions {
  count: number; // 出现次数
  percentage: number; // 占比百分比
  value: string; // 字段值
}
interface IFieldDistributions {
  fieldName: string; // 字段名称
  nonNullCount: number; // 非空记录数
  nullCount: number; // 空记录数
  totalCount: number; // 总记录数
  uniqueValueCount: number; // 唯一值数量
  valueDistributions: IValueDistributions[];
}

interface IProps {
  moduleNames: IStatus[]; // 模块名称列表
  logLoading: boolean; // 日志数据是否正在加载
  moduleLoading: boolean; // 模块名称是否正在加载
  fieldDistributions: IFieldDistributions[]; // 字段值分布列表
}

const Sider: React.FC<IProps> = (props) => {
  const { fieldDistributions, logLoading, moduleNames, moduleLoading } = props;
  const [logColumns, setLogColumns] = useState<ILogColumnsResponse[]>([]); // 日志字段
  const [selectedModule, setSelectedModule] = useState<string[] | undefined>(undefined); // 已选模块

  // 获取日志字段
  const fetchColumns = useRequest(getTableColumns, {
    manual: true,
    onSuccess: (res: ILogColumnsResponse[]) => {
      setLogColumns(res);
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
    logColumns[index].selected = !logColumns[index].selected;
    setLogColumns([...logColumns]);
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
