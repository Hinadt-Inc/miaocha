import { useState, useCallback, useEffect, useMemo, useRef, lazy, Suspense } from 'react';
import { Collapse, Space, Tag, Cascader, Select, Spin } from 'antd';
import { useRequest } from 'ahooks';
import { getTableColumns } from '@/api/logs';
import { getFieldTypeColor } from '../../utils/logDataHelpers';
import styles from './Sider.module.less';

interface IProps {
  // selectedTable: string;
  // availableFields: Array<{ columnName: string; dataType: string }>;
  // selectedFields: string[];
  // onToggleField: (fieldName: string) => void;
  // lastAddedField: string | null;
  // lastRemovedField: string | null;
  // availableTables: Array<{
  //   datasourceId: number;
  //   datasourceName: string;
  //   databaseName: string;
  //   tables: Array<{
  //     tableName: string;
  //     tableComment: string;
  //     columns: Array<{
  //       columnName: string;
  //       dataType: string;
  //       columnComment: string;
  //       isPrimaryKey: boolean;
  //       isNullable: boolean;
  //     }>;
  //   }>;
  // }>;
  // onTableChange: (value: string) => void;
  // collapsed: boolean;
  moduleNames: IStatus[]; // 模块名称列表
  fetchModuleNames: any;
}

const Sider: React.FC<IProps> = (props) => {
  const {
    moduleNames,
    fetchModuleNames,
    // 以下是旧的属性
    selectedTable,
    availableFields,
    selectedFields,
    onToggleField,
    lastAddedField,
    lastRemovedField,
    availableTables,
    onTableChange,
    collapsed,
  } = props;

  // 日志字段
  const [logColumns, setLogColumns] = useState<ILogColumnsResponse[]>([]);
  // 已选模块
  const [selectedModule, setSelectedModule] = useState<string[] | undefined>(undefined);

  // 获取日志字段
  const fetchLogColumns = useRequest(getTableColumns, {
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
    fetchLogColumns.run({ datasourceId: Number(datasourceId), module: String(module) });
  };

  // 当 moduleNames 加载完成后，自动选择第一个数据源和第一个模块
  useEffect(() => {
    if (moduleNames?.length > 0 && !selectedModule) {
      const firstModule = moduleNames[0];
      if (firstModule.children?.[0]) {
        const datasourceId = String(firstModule.value);
        const module = String(firstModule.children[0].value);
        setSelectedModule([datasourceId, module]);
        fetchLogColumns.run({ datasourceId: Number(datasourceId), module });
      }
    }
  }, [moduleNames]);

  const toggleColumns = (item: ILogColumnsResponse, index: number) => {
    console.log(item, index);
    logColumns[index].selected = !logColumns[index].selected;
    setLogColumns([...logColumns]);
  };

  return (
    <div className={styles.layoutSider}>
      <Cascader
        variant="filled"
        placeholder="请选择模块"
        style={{ width: '100%' }}
        options={moduleNames}
        expandTrigger="hover"
        value={selectedModule}
        onChange={changeLogColumns}
        allowClear
        showSearch
        loading={fetchModuleNames.loading}
      />

      <Spin spinning={fetchLogColumns.loading} size="small">
        <Collapse
          size="small"
          className={styles.collapse}
          defaultActiveKey={['selected', 'available']}
          ghost
          items={[
            {
              key: 'selected',
              label: '已选字段',
              children: logColumns?.map((item: ILogColumnsResponse, index: number) => {
                if (!item.selected) return null;

                return (
                  <div
                    className={styles.item}
                    key={item.columnName}
                    onClick={() => toggleColumns(item, index)}
                  >
                    <Tag color={getFieldTypeColor(item.dataType)}>
                      {item.dataType.substr(0, 1).toUpperCase()}
                    </Tag>
                    {item.columnName}
                  </div>
                );
              }),
            },
            {
              key: 'available',
              label: '可用字段',
              children: logColumns?.map((item: ILogColumnsResponse, index: number) => {
                if (item.selected) return null;
                return (
                  <div
                    className={styles.item}
                    key={item.columnName}
                    onClick={() => toggleColumns(item, index)}
                  >
                    <Tag color={getFieldTypeColor(item.dataType)}>
                      {item.dataType.substr(0, 1).toUpperCase()}
                    </Tag>
                    {item.columnName}
                    {/* <EyeOutlined style={{ color: '#1890ff' }} /> */}
                  </div>
                );
              }),
            },
          ]}
        />
      </Spin>
    </div>
  );
};
export default Sider;
