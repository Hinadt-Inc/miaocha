import { ModalForm, ProFormText, ProFormSelect, ProFormTextArea } from '@ant-design/pro-components';
import { Button } from 'antd';
import { LinkOutlined } from '@ant-design/icons';
import type { CreateDataSourceParams, TestConnectionParams } from '@/types/datasourceTypes';

export interface DataSourceFormData {
  id?: string;
  name: string;
  type: string;
  description?: string;
  jdbcUrl: string;
  username: string;
  password?: string;
  ip?: string;
  port?: number;
  database?: string;
  createTime?: string;
  createUser?: string;
  updateTime?: string;
  updateUser?: string;
}

interface DataSourceFormModalProps {
  visible: boolean;
  currentDataSource?: DataSourceFormData;
  testLoading: boolean;
  submitLoading: boolean;
  onSubmit: (values: Omit<CreateDataSourceParams, 'id'>) => Promise<boolean>;
  onCancel: () => void;
  onTestConnection: (values: TestConnectionParams) => Promise<void>;
}

const dataSourceTypeOptions = [{ label: 'Doris', value: 'Doris' }];

const DataSourceFormModal: React.FC<DataSourceFormModalProps> = ({
  visible,
  currentDataSource,
  testLoading,
  submitLoading,
  onSubmit,
  onCancel,
  onTestConnection,
}) => {
  // 处理初始值，编辑模式下密码字段留空
  const getInitialValues = () => {
    if (!currentDataSource) return undefined;
    
    // 编辑模式下，完全移除password字段，这样表单中的密码输入框就是空的
    const { password, ...otherValues } = currentDataSource;
    return otherValues;
  };

  return (
    <ModalForm
      grid={true}
      initialValues={getInitialValues()}
      labelCol={{ span: 6, style: { textAlign: 'right' } }}
      layout="horizontal"
      modalProps={{
        destroyOnClose: true,
        maskClosable: false,
        centered: true,
      }}
      open={visible}
      rowProps={{ gutter: [24, 16] }}
      submitter={{
        submitButtonProps: {
          loading: submitLoading,
        },
        render: (props, doms: React.ReactNode[]) => {
          return [
            <Button
              key="test"
              icon={<LinkOutlined />}
              loading={testLoading}
              title="验证数据库连接配置是否正确"
              type="default"
              onClick={() => {
                // 获取当前表单的值，并测试连接
                const values = props.form?.getFieldsValue() as TestConnectionParams;
                // 在编辑模式下，如果密码为空，则使用原有密码
                if (currentDataSource && !values.password) {
                  values.password = currentDataSource.password || '';
                }
                void onTestConnection(values);
              }}
            >
              测试数据库连接
            </Button>,
            ...doms,
          ];
        },
      }}
      title={currentDataSource ? '编辑数据源' : '新增数据源'}
      width="600px"
      wrapperCol={{ span: 18 }}
      onFinish={onSubmit}
      onOpenChange={(open) => !open && onCancel()}
    >
      <ProFormText
        colProps={{ span: 24 }}
        fieldProps={{ maxLength: 128 }}
        label="数据源名称"
        name="name"
        placeholder="例如：生产环境数据库、测试环境数据库"
        rules={[
          { required: true, message: '请输入数据源名称，便于识别和管理' },
          { max: 128, message: '数据源名称不能超过128个字符' },
        ]}
      />
      <ProFormSelect
        colProps={{ span: 24 }}
        label="数据库类型"
        name="type"
        options={dataSourceTypeOptions}
        placeholder="请选择数据库类型"
        rules={[{ required: true, message: '请选择数据库类型' }]}
      />
      <ProFormText
        colProps={{ span: 24 }}
        fieldProps={{ maxLength: 128 }}
        label="数据库用户名"
        name="username"
        placeholder="请输入数据库登录用户名"
        rules={[
          { required: true, message: '请输入数据库用户名' },
          { max: 128, message: '用户名不能超过128个字符' },
        ]}
      />
      <ProFormText.Password
        colProps={{ span: 24 }}
        fieldProps={{ 
          maxLength: 128,
          allowClear: !!currentDataSource, // 编辑模式下显示清空按钮
        }}
        label="数据库密码"
        name="password"
        placeholder={currentDataSource ? '如不修改密码请留空' : '请输入数据库密码'}
        rules={[
          { 
            required: !currentDataSource, 
            message: '请输入数据库密码' 
          },
          { max: 128, message: '密码不能超过128个字符' },
        ]}
        tooltip={currentDataSource ? "编辑模式下，如果不需要修改密码可以留空，系统将保持原有密码不变" : "请输入数据库登录密码"}
      />
      


      <ProFormText
        colProps={{ span: 24 }}
        fieldProps={{ maxLength: 128 }}
        label="JDBC 连接地址"
        labelCol={{ span: 6, style: { textAlign: 'right' } }}
        name="jdbcUrl"
        placeholder="例如：jdbc:mysql://192.168.1.100:3306/database_name?useSSL=false&serverTimezone=UTC"
        rules={[
          { required: true, message: '请输入JDBC连接地址' },
          {
            pattern: /^jdbc:/,
            message: 'JDBC连接地址必须以 jdbc: 开头',
          },
          { max: 128, message: 'JDBC连接地址不能超过128个字符' },
        ]}
        tooltip="请输入完整的JDBC连接字符串，包含数据库类型、主机地址、端口号、数据库名称等信息"
        wrapperCol={{ span: 18 }}
      />

      <ProFormTextArea
        colProps={{ span: 24 }}
        fieldProps={{
          rows: 3,
          maxLength: 200,
          showCount: true,
        }}
        label="数据源描述"
        labelCol={{ span: 6, style: { textAlign: 'right' } }}
        name="description"
        placeholder="请简要描述该数据源的用途，例如：生产环境主数据库，用于存储用户订单数据"
        tooltip="详细描述数据源的用途和特点，便于团队成员理解和使用"
        wrapperCol={{ span: 18 }}
      />
    </ModalForm>
  );
};

export default DataSourceFormModal;
