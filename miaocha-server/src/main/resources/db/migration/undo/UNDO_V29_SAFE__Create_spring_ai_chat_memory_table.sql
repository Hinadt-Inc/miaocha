-- UNDO for V29__Create_spring_ai_chat_memory_table.sql
-- This operation will remove all stored chat memory data.
-- Data loss risk: HIGH (drops the entire chat memory table)
DROP TABLE IF EXISTS SPRING_AI_CHAT_MEMORY;

