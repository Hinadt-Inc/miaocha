import { useEffect, useState } from 'react'
import { getMachines } from '../../api/machine'
import type { Machine } from '../../types/machineTypes'
import { SimpleTable } from '../../components/common/SimpleTable'
import type { TableColumnsType } from 'antd'
import { Breadcrumb } from 'antd'
import { Link } from 'react-router-dom'
import './MachineManagementPage.less'

const MachineManagementPage = () => {
  const [machines, setMachines] = useState<Machine[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    const fetchMachines = async () => {
      setLoading(true)
      try {
        const res = await getMachines()
        setMachines(res)
      } finally {
        setLoading(false)
      }
    }
    fetchMachines()
  }, [])

  const columns: TableColumnsType<Machine> = [
    { title: '名称', dataIndex: 'name', key: 'name' },
    { title: 'IP地址', dataIndex: 'ip', key: 'ip' },
    { title: '端口', dataIndex: 'port', key: 'port' },
    { title: '用户名', dataIndex: 'username', key: 'username' },
    { title: '创建时间', dataIndex: 'createTime', key: 'createTime' },
    { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime' }
  ]

  return (
    <div className="machine-management-page">
      <div className="header">
        <Breadcrumb
          items={[
            {
              title: <Link to="/system">系统设置</Link>,
            },
            {
              title: '机器管理',
            },
          ]}
        />
      </div>
      <div className="table-container">
        <SimpleTable
        dataSource={machines}
        columns={columns}
        loading={loading}
        rowKey="id"
        />
      </div>
    </div>
  )
}

export default MachineManagementPage
