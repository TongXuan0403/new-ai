-- =====================================================================
-- Codex-AI 心理健康助手 MVP · V6 P1 字段扩展
-- ai_model 增加可追溯的提示词/规则版本信息（模型名|prompt=xx|rule=xx）
-- =====================================================================
USE `mental_health`;
SET NAMES utf8mb4;

ALTER TABLE `consultation_message`
  MODIFY COLUMN `ai_model` VARCHAR(200) DEFAULT NULL COMMENT '模型名（可含 prompt/rule 版本追溯）';
