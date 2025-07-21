## What's Changed

This version includes several improvements and bug fixes based on community feedback.

* [ISSUE #64] Logstash管理功能优化 (#77) by @Misaki030112
* [ISSUE #67]优化数据源表单，编辑模式下处理密码字段，调整表单布局和验证逻辑 (#78) by @a362248012
* [ISSUE #69] 优化用户角色管理逻辑，添加权限检查 (#76) by @a362248012
* [ISSUE #70] logstash进程信息以模块名称进行排序 (#74) by @Misaki030112
* [ISSUE #63]优化小屏幕下表格显示，调整列宽和滚动条样式 (#71) by @a362248012
* [ISSUE #66] 优化模版管理SQL执行模板 (#75) by @a362248012
* [ISSUE #65]字段百分比实时变化 (#73) by @a362248012
* [ISSUE #62]优化getColumns调用逻辑，增加防抖机制以避免重复请求，并添加组件卸载时的清理工作 (#72) by @a362248012
* [ISSUE #60] VARIANT等类型不可排序功能修复 (#61) by @a362248012
* [ISSUE #52] 首页时间段回显及数据自动加载顺序的问题 (#57) by @a362248012
* [ISSUE #54] 主页切换模块本地缓存影响搜索结果 (#59) by @YangManJame
* [ISSUE #51] 修复模块权限在sql查询中混淆BUG (#53) by @Misaki030112
* [ISSUE #49] 分支支持内部GitLab仓库同步 (#50) by @Misaki030112
* [ISSUE #47] 优化 README 文件样式 (#48) by @Misaki030112
* [ISSUE #42] 建立自动化 RELEASE 发布流水线 (#46) by @Misaki030112
* [ISSUE #31] SQL查询功能体验优化 (#36) by @Misaki030112
* [ISSUE #38] 超级管理员不可修改, 管理员无法修改非当前管理员信息 (#45) by @a362248012
* [ISSUE #40] 完善环境搭建，本地开发文档 (#41) by @Misaki030112
* [ISSUE #35] 同步前端代码 (#37) by @Misaki030112
* [ISSUE #28] 优化LogStash任务耗时信息 (#34) by @Misaki030112
* [ISSUE #32] Github Action 支持PR设置 label 根据PR改动自动部署测试环境 (#33) by @Misaki030112
* [ISSUE #29] 合并秒查前端仓库 (#30) by @Misaki030112
* [ISSUE #22] 编辑数据源时按需更改信息,验证数据源连接 (#27) by @Misaki030112
* [ISSUE #25] 补充项目开发相关文档 (#26) by @Misaki030112
* [ISSUE #24]测试贡献能力 (#23) by @liujxgitlab
* [ISSUE #19] 优化 Github Action 工作流 (#20) by @Misaki030112
* [ISSUE #17] 支持全局 Trace 日志ID 特性 (#18) by @Misaki030112

## Other Changes

- chore(github): 优化CodeCov配置
- chore(issue): 添加ISSUE模板
- perf(logstash): Logstash进程列表按名称排序 (#15)
- perf(github): 优化原有CI action , 添加打包 action (#13)
- fix(logstash): 修复创建logstash进程的时候多个topic无法通过校验的情况 (#11)
- chore(git): 补充提交时git钩子
- feat(user): 获取所有用户信息时按照角色和时间进行排序 (#7)
- feat(machine): 获取机器信息接口增加机器上部署logstashMachine的数量信息 (#6)
- fix(sql-edit): 修复普通用户查看表结构权限问题 (#5)
- chore(all): 开源准备工作 (#4)
... and 197 more commits. [View all changes](https://github.com/Hinadt-Inc/miaocha/commits/v2.0.0)


## New Contributors
* @Misaki030112 made their first contribution in #18
* @a362248012 made their first contribution in #45
* @YangManJame made their first contribution in #59
* @liujxgitlab made their first contribution in #23

**Project Repository**: https://github.com/Hinadt-Inc/miaocha
