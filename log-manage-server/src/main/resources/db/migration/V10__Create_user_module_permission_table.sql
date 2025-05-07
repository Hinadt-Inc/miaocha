-- 创建用户模块权限表
CREATE TABLE `user_module_permission` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `datasource_id` bigint(20) NOT NULL COMMENT '数据源ID',
  `module` varchar(255) NOT NULL COMMENT '模块名称',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_datasource_module` (`user_id`, `datasource_id`, `module`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户模块权限表';
