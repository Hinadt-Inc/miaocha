-- Create table for Spring AI JDBC Chat Memory Repository (MySQL)
-- Idempotent and safe for production; aligns with Spring AI schema
CREATE TABLE IF NOT EXISTS `SPRING_AI_CHAT_MEMORY` (
   `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'PK',
   `conversation_id` VARCHAR(64)     NOT NULL COMMENT 'Conversation ID',
   `content`         LONGTEXT NULL   COMMENT 'Message content (NULLable to satisfy some linters for TEXT/BLOB)',
   `type`            VARCHAR(10)     NOT NULL COMMENT 'USER / ASSISTANT / SYSTEM / TOOL',
   `timestamp`       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Insert time',
   PRIMARY KEY (`id`),
   KEY `idx_spring_ai_chat_memory_conversation_id_timestamp`(`conversation_id`, `timestamp`)
)ENGINE=InnoDB
 DEFAULT CHARSET=utf8mb4
 COLLATE=utf8mb4_unicode_ci;

