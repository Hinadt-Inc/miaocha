-- 添加用户密码和角色字段
-- 先更新NULL邮箱为默认值
UPDATE user SET email = CONCAT('user_', uid, '@example.com') WHERE email IS NULL;

ALTER TABLE user
    ADD COLUMN password VARCHAR(100) DEFAULT NULL COMMENT '用户密码',
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '用户角色: SUPER_ADMIN, ADMIN, USER',
    ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT '用户状态: 0-禁用, 1-正常',
    MODIFY COLUMN email VARCHAR(100) NOT NULL COMMENT '用户邮箱',
    DROP COLUMN is_admin,
    ADD UNIQUE KEY uk_email (email);

-- 修改原有的角色
UPDATE user SET role = 'ADMIN' WHERE uid = 'admin';

-- 创建超级管理员账户
INSERT INTO user (nickname, email, uid, role, password, status, create_time, update_time)
VALUES ('超级管理员', 'admin@hinadt.com', 'super_admin', 'SUPER_ADMIN', '$2a$10$vgn1nLgB8Df31zkLsRmamutNCNcZ.r.xFWHA1KdbMJ8ReidZc/CVK', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE role = 'SUPER_ADMIN', password = '$2a$10$vgn1nLgB8Df31zkLsRmamutNCNcZ.r.xFWHA1KdbMJ8ReidZc/CVK';