-- =====================================================================
-- Codex-AI 心理健康助手 MVP · V4 修复 user_consent 唯一键
-- 问题：原唯一键 (user_id, privacy, sensitive, revoked_at) 把秒级时间戳
--       revoked_at 纳入唯一约束，同一秒内「撤回→再同意→再撤回」会撞键，
--       导致撤回后重新同意返回 500（Duplicate entry）。
-- 修复：引入生成列 active_key：有效同意（revoked_at IS NULL）时其值为
--       user_id+版本集（每用户每版本集唯一一条）；撤回后置 NULL（MySQL
--       唯一索引对 NULL 不去重），从而保留历史且同秒撤回也不冲突。
-- 执行：mysql -uroot -p < V4__fix_user_consent_unique.sql
-- =====================================================================

USE `mental_health`;
SET NAMES utf8mb4;

-- 1) 清理重复：同一用户同一版本集仅保留最新一条（历史撤回记录在测试期重复产生）
DELETE FROM `user_consent`
WHERE `id` NOT IN (
  SELECT `max_id` FROM (
    SELECT MAX(`id`) AS `max_id`
    FROM `user_consent`
    GROUP BY `user_id`, `privacy_policy_version`, `sensitive_info_version`, `product_boundary_version`
  ) `t`
);

-- 2) 若 V2 旧键或上一版错误键存在则删除
ALTER TABLE `user_consent` DROP INDEX `uk_user_consent_active`;

-- 3) 新增生成列 active_key（撤回后为 NULL）
ALTER TABLE `user_consent`
  ADD COLUMN `active_key` VARCHAR(255) GENERATED ALWAYS AS (
    IF(`revoked_at` IS NULL,
       CONCAT(`user_id`, '|', COALESCE(`privacy_policy_version`,''),
              '|', COALESCE(`sensitive_info_version`,''),
              '|', COALESCE(`product_boundary_version`,'')),
       NULL)
  ) STORED COMMENT '有效同意唯一键（撤回后为 NULL）' AFTER `product_boundary_version`;

-- 4) 唯一键建立在生成列上：有效同意每用户每版本集仅一条，撤回记录不限
ALTER TABLE `user_consent`
  ADD UNIQUE KEY `uk_user_consent_active` (`active_key`);
