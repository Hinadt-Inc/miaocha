// 获取字段类型对应的图标颜色
export const getFieldTypeColor = (type: string): string => {
  const typeMap: Record<string, string> = {
    T: 'purple',
    S: 'blue',
    V: 'cyan',
    D: 'green',
    I: 'orange',
  };
  const firstChar = type.charAt(0).toUpperCase();
  return typeMap[firstChar] || 'default';
};
