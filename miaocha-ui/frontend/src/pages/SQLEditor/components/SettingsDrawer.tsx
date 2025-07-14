import { Drawer, Space, Form, Select, Checkbox } from 'antd';
import { SettingOutlined } from '@ant-design/icons';
import { EditorSettings } from '../types';

const { Option } = Select;

interface SettingsDrawerProps {
  visible: boolean;
  onClose: () => void;
  editorSettings: EditorSettings;
  updateEditorSettings: (settings: Partial<EditorSettings>) => void;
}

const SettingsDrawer: React.FC<SettingsDrawerProps> = ({ visible, onClose, editorSettings, updateEditorSettings }) => {
  return (
    <Drawer
      title={
        <Space>
          <SettingOutlined />
          <span>编辑器设置</span>
        </Space>
      }
      width={400}
      open={visible}
      onClose={onClose}
    >
      <Form layout="vertical">
        <Form.Item label="字体大小">
          <Select value={editorSettings.fontSize} onChange={(value) => updateEditorSettings({ fontSize: value })}>
            {[12, 14, 16, 18, 20].map((size) => (
              <Option key={size} value={size}>
                {size}px
              </Option>
            ))}
          </Select>
        </Form.Item>
        <Form.Item label="主题">
          <Select value={editorSettings.theme} onChange={(value) => updateEditorSettings({ theme: value })}>
            <Option value="vs">浅色</Option>
            <Option value="vs-dark">深色</Option>
            <Option value="sqlTheme">SQL 主题</Option>
          </Select>
        </Form.Item>
        <Form.Item label="Tab 大小">
          <Select value={editorSettings.tabSize} onChange={(value) => updateEditorSettings({ tabSize: value })}>
            {[2, 4, 8].map((size) => (
              <Option key={size} value={size}>
                {size} 空格
              </Option>
            ))}
          </Select>
        </Form.Item>
        <Form.Item>
          <Space direction="vertical" style={{ width: '100%' }}>
            <Checkbox
              checked={editorSettings.wordWrap}
              onChange={(e) => updateEditorSettings({ wordWrap: e.target.checked })}
            >
              自动换行
            </Checkbox>
            <Checkbox
              checked={editorSettings.autoComplete}
              onChange={(e) => updateEditorSettings({ autoComplete: e.target.checked })}
            >
              自动完成
            </Checkbox>
            <Checkbox
              checked={editorSettings.minimap}
              onChange={(e) => updateEditorSettings({ minimap: e.target.checked })}
            >
              显示缩略图
            </Checkbox>
          </Space>
        </Form.Item>
      </Form>
    </Drawer>
  );
};

export default SettingsDrawer;
