-- =====================================================================
-- Codex-AI 心理健康助手 MVP · V2 P0 新增表
-- 覆盖：同意记录 / 风险事件 / 危机资源 / 对话反馈 / 审计日志 / 账号删除申请
-- 执行：mysql -uroot -p < V2__p0_tables.sql
-- =====================================================================

USE `mental_health`;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------
-- 8. 用户同意表 user_consent
-- 记录首次使用确认（年龄、隐私政策、敏感信息、产品边界）与撤回记录
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `user_consent`;
CREATE TABLE `user_consent` (
  `id`                    BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`               BIGINT      NOT NULL COMMENT '用户ID',
  `age_confirmed`         TINYINT     NOT NULL DEFAULT 0 COMMENT '是否确认已满18岁 0否 1是',
  `privacy_policy_version` VARCHAR(50) DEFAULT NULL COMMENT '隐私政策版本',
  `sensitive_info_version` VARCHAR(50) DEFAULT NULL COMMENT '敏感个人信息同意版本',
  `product_boundary_version` VARCHAR(50) DEFAULT NULL COMMENT '产品边界说明版本',
  `consented_at`          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '同意时间',
  `revoked_at`            DATETIME    DEFAULT NULL COMMENT '撤回时间（非空表示已撤回）',
  `created_at`            DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`            DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_consent_active` (`user_id`, `privacy_policy_version`, `sensitive_info_version`, `revoked_at`),
  KEY `idx_consent_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户同意表';

-- ---------------------------------------------------------------------
-- 9. 风险事件表 risk_event
-- 记录风险识别命中事件，默认脱敏摘要；完整内容受更高权限控制
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `risk_event`;
CREATE TABLE `risk_event` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`        BIGINT       NOT NULL COMMENT '用户ID',
  `session_id`     BIGINT       DEFAULT NULL COMMENT '关联会话ID',
  `message_id`     BIGINT       DEFAULT NULL COMMENT '关联消息ID',
  `risk_level`     TINYINT      NOT NULL DEFAULT 0 COMMENT '风险等级 0正常 1关注 2预警 3危机',
  `risk_type`      VARCHAR(50)  DEFAULT NULL COMMENT '风险类型 SELF_HARM/HARM_OTHERS/EMOTIONAL_DISTRESS 等',
  `action_type`    VARCHAR(50)  NOT NULL DEFAULT 'NONE' COMMENT '行动类型 NONE/SHOW_GUIDANCE/SHOW_CRISIS_CARD/BLOCK_RESPONSE',
  `matched_rules`  JSON         DEFAULT NULL COMMENT '命中规则',
  `content_summary` VARCHAR(500) DEFAULT NULL COMMENT '脱敏摘要',
  `rule_version`   VARCHAR(50)  DEFAULT NULL COMMENT '风险规则版本',
  `model_version`  VARCHAR(50)  DEFAULT NULL COMMENT '分类模型版本',
  `status`         VARCHAR(30)  NOT NULL DEFAULT '待复核' COMMENT '处理状态 待复核/处理中/已关闭',
  `crisis_card_shown` TINYINT   NOT NULL DEFAULT 0 COMMENT '危机卡是否展示 0否 1是',
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_risk_user` (`user_id`),
  KEY `idx_risk_session` (`session_id`),
  KEY `idx_risk_status` (`status`),
  KEY `idx_risk_level` (`risk_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险事件表';

-- ---------------------------------------------------------------------
-- 10. 危机资源表 crisis_resource
-- 配置前端危机卡展示的求助资源；默认内置 120 / 110 / 12356
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `crisis_resource`;
CREATE TABLE `crisis_resource` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `resource_type` VARCHAR(30) NOT NULL COMMENT '类型 emergency/hotline/school/local',
  `name`        VARCHAR(100) NOT NULL COMMENT '资源名称',
  `phone`       VARCHAR(30)  DEFAULT NULL COMMENT '电话',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '说明',
  `region`      VARCHAR(100) DEFAULT NULL COMMENT '地区或学校',
  `enabled`     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用 0否 1是',
  `sort_order`  INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_resource_enabled` (`enabled`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='危机资源表';

-- ---------------------------------------------------------------------
-- 11. 对话反馈表 chat_feedback
-- 记录对话结束后的帮助度反馈
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `chat_feedback`;
CREATE TABLE `chat_feedback` (
  `id`                 BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`            BIGINT      NOT NULL COMMENT '用户ID',
  `session_id`         BIGINT      NOT NULL COMMENT '会话ID',
  `assistant_message_id` BIGINT    DEFAULT NULL COMMENT 'AI消息ID',
  `helpfulness`        TINYINT     NOT NULL COMMENT '1有帮助 2一般 3没帮助',
  `reason_tags`        JSON        DEFAULT NULL COMMENT '原因标签',
  `comment`            VARCHAR(500) DEFAULT NULL COMMENT '可选反馈',
  `created_at`         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_feedback_user` (`user_id`),
  KEY `idx_feedback_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话反馈表';

-- ---------------------------------------------------------------------
-- 12. 审计日志表 audit_log
-- 记录管理端敏感操作（查看原文/导出/改状态/改资源/处理删除申请等）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `audit_log`;
CREATE TABLE `audit_log` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `operator_id`   BIGINT       DEFAULT NULL COMMENT '操作人ID',
  `operator_role` VARCHAR(30)  DEFAULT NULL COMMENT '操作角色',
  `action`        VARCHAR(100) NOT NULL COMMENT '操作类型',
  `target_type`   VARCHAR(50)  DEFAULT NULL COMMENT '操作对象类型',
  `target_id`     BIGINT       DEFAULT NULL COMMENT '操作对象ID',
  `ip`            VARCHAR(64)  DEFAULT NULL COMMENT 'IP地址',
  `user_agent`    VARCHAR(500) DEFAULT NULL COMMENT 'UA',
  `detail`        JSON         DEFAULT NULL COMMENT '操作详情',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_audit_operator` (`operator_id`),
  KEY `idx_audit_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

-- ---------------------------------------------------------------------
-- 13. 账号删除申请表 user_deletion_request
-- 申请状态流转：待处理 -> 处理中 -> 已完成；保留最小审计记录
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `user_deletion_request`;
CREATE TABLE `user_deletion_request` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`      BIGINT       NOT NULL COMMENT '用户ID',
  `status`       VARCHAR(30)  NOT NULL DEFAULT '待处理' COMMENT '状态 待处理/处理中/已完成/已取消',
  `reason`       VARCHAR(500) DEFAULT NULL COMMENT '申请原因（可选）',
  `requested_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `processed_at` DATETIME     DEFAULT NULL COMMENT '处理完成时间',
  `canceled_at`  DATETIME     DEFAULT NULL COMMENT '取消时间',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_deletion_user` (`user_id`),
  KEY `idx_deletion_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号删除申请表';

SET FOREIGN_KEY_CHECKS = 1;
