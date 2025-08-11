-- 1) 动作表（待推送/已推送）
DROP TABLE IF EXISTS action_outbox;

-- 2) 通道序号表
DROP TABLE IF EXISTS channel_sequence;

-- 3) 前端连接注册表
DROP TABLE IF EXISTS channel_registry;
