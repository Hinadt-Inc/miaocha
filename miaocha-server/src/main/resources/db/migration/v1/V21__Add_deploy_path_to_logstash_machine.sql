-- 为logstash_machine表添加部署路径字段
ALTER TABLE `logstash_machine`
ADD COLUMN `deploy_path` varchar(512) NOT NULL DEFAULT '' COMMENT '实际使用的部署路径，保存创建时确定的路径（默认或自定义）';

-- 注意：新字段暂时使用空字符串作为默认值，应用启动后会根据现有记录和配置文件更新实际路径 