-- =============================================
-- UNDO V27 (SAFE): Drop system_cache_config table and related indexes
-- This will remove all cached configuration records created by V27.
-- =============================================

DROP TABLE IF EXISTS system_cache_config;

