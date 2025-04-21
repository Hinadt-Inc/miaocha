import { useState } from 'react';
import { AutoComplete, Button, Space, Dropdown, DatePicker } from 'antd';
const { RangePicker } = DatePicker;
import { SearchOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';

interface SearchBarProps {
  searchQuery: string;
  timeRange: [string, string] | null;
  onSearch: (query: string) => void;
  onTimeRangeChange: (range: [string, string] | null) => void;
}

export const SearchBar = ({ 
  searchQuery, 
  timeRange,
  onSearch,
  onTimeRangeChange
}: SearchBarProps) => {
  const [showTimePicker, setShowTimePicker] = useState(false);
  const [searchHistory, setSearchHistory] = useState<string[]>(() => {
    const saved = localStorage.getItem('searchHistory');
    return saved ? JSON.parse(saved) : [];
  });

  const handleSearch = () => {
    if (searchQuery.trim()) {
      // 保存搜索历史
      if (!searchHistory.includes(searchQuery.trim())) {
        const newHistory = [searchQuery.trim(), ...searchHistory].slice(0, 10);
        setSearchHistory(newHistory);
        localStorage.setItem('searchHistory', JSON.stringify(newHistory));
      }
      onSearch(searchQuery);
    }
  };

  return (
    <div style={{ padding: '16px 24px', background: '#fff', boxShadow: '0 1px 4px rgba(0, 21, 41, 0.08)' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Space size="middle" style={{ width: '100%' }}>
          <Space.Compact>
            <AutoComplete
              placeholder="输入搜索内容"
              style={{ width: 1100 }}
              value={searchQuery}
              onChange={onSearch}
              options={searchHistory.map(query => ({
                value: query,
                label: query
              }))}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  handleSearch();
                }
              }}
            />
            <Button 
              icon={<SearchOutlined />} 
              type="primary"
              onClick={handleSearch}
            />
          </Space.Compact>
          
          <Space.Compact>
            <Dropdown
              menu={{
                items: [
                  {
                    key: '15m',
                    label: '最近15分钟',
                    onClick: () => {
                      const now = dayjs();
                      onTimeRangeChange([
                        now.subtract(15, 'minute').format('YYYY-MM-DD HH:mm:ss'),
                        now.format('YYYY-MM-DD HH:mm:ss')
                      ]);
                      setShowTimePicker(false);
                    }
                  },
                  {
                    key: '1h',
                    label: '最近1小时',
                    onClick: () => {
                      const now = dayjs();
                      onTimeRangeChange([
                        now.subtract(1, 'hour').format('YYYY-MM-DD HH:mm:ss'),
                        now.format('YYYY-MM-DD HH:mm:ss')
                      ]);
                      setShowTimePicker(false);
                    }
                  },
                  {
                    key: 'custom',
                    label: '自定义时间',
                    onClick: () => setShowTimePicker(!showTimePicker)
                  }
                ]
              }}
              trigger={['click']}
            >
              <Button>
                {timeRange 
                  ? `${dayjs(timeRange[0]).format('YYYY-MM-DD HH:mm:ss')} ~ ${dayjs(timeRange[1]).format('YYYY-MM-DD HH:mm:ss')}`
                  : '最近15分钟'}
              </Button>
            </Dropdown>
            {showTimePicker && (
              <RangePicker
                showTime={{ format: 'HH:mm:ss' }}
                format="YYYY-MM-DD HH:mm:ss"
                style={{ width: 400 }}
                onOk={(values) => {
                  if (values && values[0] && values[1]) {
                    onTimeRangeChange([
                      values[0].format('YYYY-MM-DD HH:mm:ss'),
                      values[1].format('YYYY-MM-DD HH:mm:ss')
                    ]);
                  }
                  setShowTimePicker(false);
                }}
              />
            )}
          </Space.Compact>
        </Space>
      </div>
    </div>
  );
};
