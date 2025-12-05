import React, { useState, useMemo, useCallback } from 'react';

import { Collapse, Input } from 'antd';

import { useHomeContext } from '../context';

import { ModuleSelector, FieldListItem, VirtualFieldList } from './components';
import styles from './styles/index.module.less';

/**
 * 侧边栏组件 - 模块化重构版本
 * 包含模块选择、字段管理和字段分布查询功能
 */
const Sider: React.FC = () => {
  const { searchParams, logTableColumns } = useHomeContext();

  // 状态管理
  const [searchText, setSearchText] = useState<string>('');

  // 过滤可用字段
  const filteredAvailableColumns = useMemo(() => {
    const availableColumns = (logTableColumns as ILogColumnsResponse[])?.filter(
      (item: ILogColumnsResponse) => !item.selected,
    );
    let filteredColumns = availableColumns;

    if (searchText.trim()) {
      filteredColumns = availableColumns?.filter((item: ILogColumnsResponse) =>
        item.columnName?.toLowerCase().includes(searchText.toLowerCase()),
      );
    }

    // 按字段名排序，保持与已选字段一致的排序逻辑
    return filteredColumns?.sort((a: ILogColumnsResponse, b: ILogColumnsResponse) => {
      const nameA = a.columnName || '';
      const nameB = b.columnName || '';
      return nameA.localeCompare(nameB);
    });
  }, [logTableColumns, searchText]);

  const renderVirtualItem = useCallback(
    (item: ILogColumnsResponse, index: number) => {
      return (
        <FieldListItem
          key={`${searchParams.module}_${item.columnName}`}
          column={item}
          columnIndex={index}
          isSelected={false}
        />
      );
    },
    [searchParams, logTableColumns],
  );

  return (
    <div className={styles.layoutSider}>
      {/* 模块选择器 */}
      <ModuleSelector />
      {/* 字段折叠面板 */}
      <Collapse
        defaultActiveKey={['selected', 'available']}
        expandIcon={() => null}
        ghost
        items={[
          {
            key: 'selected',
            label: '已选字段',
            children: (logTableColumns as ILogColumnsResponse[])
              ?.filter((item: ILogColumnsResponse) => item.selected)
              ?.sort((a: ILogColumnsResponse, b: ILogColumnsResponse) => {
                const nameA = a.columnName || '';
                const nameB = b.columnName || '';

                // log_time 始终排在第一位
                if (nameA === 'log_time') return -1;
                if (nameB === 'log_time') return 1;

                // 其他字段按照 _createTime 排序（添加顺序）
                return (a._createTime || 0) - (b._createTime || 0);
              })
              ?.map((item: ILogColumnsResponse, index: number) => (
                <FieldListItem
                  key={`${searchParams.module}_${item.columnName}`}
                  column={item}
                  columnIndex={index}
                  isSelected
                />
              )),
          },
          {
            key: 'available',
            label: `可用字段 (${filteredAvailableColumns?.length || 0})`,
            children: [
              <Input.Search
                key="search"
                allowClear
                className={styles.searchInput}
                placeholder="搜索字段"
                value={searchText}
                variant="filled"
                onChange={(e) => setSearchText(e.target.value)}
              />,
              <div key="virtual-list-container" className={styles.virtualListWrapper}>
                {filteredAvailableColumns && filteredAvailableColumns.length > 0 ? (
                  <VirtualFieldList
                    containerHeight={700}
                    data={filteredAvailableColumns}
                    itemHeight={35}
                    renderItem={renderVirtualItem}
                  />
                ) : (
                  <div className={styles.emptyState}>{searchText ? '未找到匹配的字段' : '暂无可用字段'}</div>
                )}
              </div>,
            ],
          },
        ]}
        size="small"
      />
    </div>
  );
};

export default Sider;
