import { useState } from 'react';
import { message } from 'antd';
import { Filter, FilterOperator, UseFiltersReturn } from '../types/logDataTypes';
import { getOperatorsByFieldType } from '../utils/logDataHelpers';

export const useFilters = (): UseFiltersReturn => {
  const [filters, setFilters] = useState<Filter[]>([
    { id: 'default-source', field: 'source', operator: 'is', value: 'nginx', color: 'blue' },
    { id: 'default-status', field: 'status', operator: 'is', value: '200', color: 'green' }
  ]);
  const [showFilterModal, setShowFilterModal] = useState(false);
  const [selectedFilterField, setSelectedFilterField] = useState<string>('');
  const [selectedFilterOperator, setSelectedFilterOperator] = useState<FilterOperator>('is');

  // 打开添加过滤器对话框
  const openFilterModal = () => {
    setSelectedFilterField('');
    setSelectedFilterOperator('is');
    setShowFilterModal(true);
  };

  // 处理过滤器字段变化
  const handleFilterFieldChange = (fieldName: string) => {
    setSelectedFilterField(fieldName);
    const fieldType = fieldName.includes('.') ? 'keyword' : 'text'; // 简化字段类型判断
    const operators = getOperatorsByFieldType(fieldType);
    setSelectedFilterOperator(operators[0]?.value || 'is');
  };

  // 添加过滤器
  const addFilter = (values: { 
    field: string; 
    operator: FilterOperator; 
    value: string | string[] | [number, number] | null 
  }) => {
    const { field, operator, value } = values;
    const colors = ['blue', 'green', 'orange', 'red', 'purple', 'cyan', 'magenta', 'gold', 'lime', 'geekblue'];
    const color = colors[Math.floor(Math.random() * colors.length)];
    
    const newFilter: Filter = {
      id: `filter-${Date.now()}`,
      field,
      operator,
      value,
      color,
    };
    
    setFilters(prev => [...prev, newFilter]);
    setShowFilterModal(false);
    message.success(`已添加过滤器: ${field}`);
  };

  // 删除过滤器
  const removeFilter = (filterId: string) => {
    setFilters(filters.filter(f => f.id !== filterId));
  };

  return {
    filters,
    showFilterModal,
    setShowFilterModal,
    selectedFilterField,
    selectedFilterOperator,
    openFilterModal,
    handleFilterFieldChange,
    addFilter,
    removeFilter
  };
};
