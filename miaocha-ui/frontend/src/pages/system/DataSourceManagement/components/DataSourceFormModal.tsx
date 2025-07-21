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
      title={currentDataSource ? '编辑数据源' : '新增数据源'}
      width="600px"
      open={visible}
      onOpenChange={(open) => !open && onCancel()}
      onFinish={onSubmit}
      modalProps={{
        destroyOnClose: true,
        maskClosable: false,
        centered: true,
      }}
      layout="horizontal"
      labelCol={{ span: 6, style: { textAlign: 'right' } }}
      wrapperCol={{ span: 18 }}
      grid={true}
      rowProps={{ gutter: [24, 16] }}
      initialValues={getInitialValues()}
      submitter={{
        submitButtonProps: {
          loading: submitLoading,
        },
        render: (props, doms: React.ReactNode[]) => {
          return [
            <Button
              key="test"
              type="default"
              loading={testLoading}
              icon={<LinkOutlined />}
              onClick={() => {
                // 获取当前表单的值，并测试连接
                const values = props.form?.getFieldsValue() as TestConnectionParams;
                // 在编辑模式下，如果密码为空，则使用原有密码
                if (currentDataSource && !values.password) {
                  values.password = currentDataSource.password || '';
                }
                void onTestConnection(values);
              }}
              title="验证数据库连接配置是否正确"
            >
              测试数据库连接
            </Button>,
            ...doms,
          ];
        },
      }}
    >
      <ProFormText
        colProps={{ span: 24 }}
        name="name"
        label="数据源名称"
        placeholder="例如：生产环境数据库、测试环境数据库"
        rules={[
          { required: true, message: '请输入数据源名称，便于识别和管理' },
          { max: 128, message: '数据源名称不能超过128个字符' },
        ]}
        fieldProps={{ maxLength: 128 }}
      />
      <ProFormSelect
        colProps={{ span: 24 }}
        name="type"
        label="数据库类型"
        options={dataSourceTypeOptions}
        placeholder="请选择数据库类型"
        rules={[{ required: true, message: '请选择数据库类型' }]}
      />
      <ProFormText
        colProps={{ span: 24 }}
        name="username"
        label="数据库用户名"
        placeholder="请输入数据库登录用户名"
        rules={[
          { required: true, message: '请输入数据库用户名' },
          { max: 128, message: '用户名不能超过128个字符' },
        ]}
        fieldProps={{ maxLength: 128 }}
      />
      <ProFormText.Password
        colProps={{ span: 24 }}
        name="password"
        label="数据库密码"
        placeholder={currentDataSource ? '如不修改密码请留空' : '请输入数据库密码'}
        rules={[
          { 
            required: !currentDataSource, 
            message: '请输入数据库密码' 
          },
          { max: 128, message: '密码不能超过128个字符' },
        ]}
        fieldProps={{ 
          maxLength: 128,
          allowClear: !!currentDataSource, // 编辑模式下显示清空按钮
        }}
        tooltip={currentDataSource ? "编辑模式下，如果不需要修改密码可以留空，系统将保持原有密码不变" : "请输入数据库登录密码"}
      />
      


      <ProFormText
        colProps={{ span: 24 }}
        name="jdbcUrl"
        label="JDBC 连接地址"
        placeholder="例如：jdbc:mysql://192.168.1.100:3306/database_name?useSSL=false&serverTimezone=UTC"
        rules={[
          { required: true, message: '请输入JDBC连接地址' },
          {
            pattern: /^jdbc:/,
            message: 'JDBC连接地址必须以 jdbc: 开头',
          },
          { max: 128, message: 'JDBC连接地址不能超过128个字符' },
        ]}
        labelCol={{ span: 6, style: { textAlign: 'right' } }}
        wrapperCol={{ span: 18 }}
        tooltip="请输入完整的JDBC连接字符串，包含数据库类型、主机地址、端口号、数据库名称等信息"
        fieldProps={{ maxLength: 128 }}
      />

      <ProFormTextArea
        colProps={{ span: 24 }}
        name="description"
        label="数据源描述"
        placeholder="请简要描述该数据源的用途，例如：生产环境主数据库，用于存储用户订单数据"
        labelCol={{ span: 6, style: { textAlign: 'right' } }}
        wrapperCol={{ span: 18 }}
        fieldProps={{
          rows: 3,
          maxLength: 200,
          showCount: true,
        }}
        tooltip="详细描述数据源的用途和特点，便于团队成员理解和使用"
      />
    </ModalForm>
  );
};

export default DataSourceFormModal;
