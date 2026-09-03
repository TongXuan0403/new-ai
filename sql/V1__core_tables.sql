-- =====================================================================
-- Codex-AI 心理健康助手 MVP · V1 基础表
-- MySQL 8.0+ / InnoDB / utf8mb4
-- 约定：表名小写下划线；业务表统一 created_at/updated_at；
--       不建物理外键，用索引 + 应用层保证关系。
-- 执行：mysql -uroot -p < V1__core_tables.sql
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `mental_health`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `mental_health`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------
-- 1. 用户表（MVP 以 user_type 区分普通用户/管理员，不建角色关联表）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username`   VARCHAR(50)  NOT NULL COMMENT '用户名（字母数字下划线）',
  `email`      VARCHAR(100) DEFAULT NULL COMMENT '邮箱（可选，不阻塞注册）',
  `phone`      VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  `password`   VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 密文）',
  `nickname`   VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
  `avatar`     VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `gender`     TINYINT      NOT NULL DEFAULT 0 COMMENT '性别 0未知 1男 2女',
  `bio`        VARCHAR(255) DEFAULT NULL COMMENT '个人简介',
  `user_type`  TINYINT      NOT NULL DEFAULT 1 COMMENT '用户类型 1普通用户 2管理员',
  `status`     TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 0禁用 1正常',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  KEY `idx_user_type_status` (`user_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ---------------------------------------------------------------------
-- 2. 咨询会话表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `consultation_session`;
CREATE TABLE `consultation_session` (
  `id`                      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `user_id`                 BIGINT       NOT NULL COMMENT '用户ID',
  `session_title`           VARCHAR(200) DEFAULT NULL COMMENT '会话标题，默认“新的倾诉”',
  `mood`                    VARCHAR(50)  DEFAULT NULL COMMENT '会话情绪（兼容现有实现）',
  `model`                   VARCHAR(50)  DEFAULT NULL COMMENT '使用的AI模型',
  `status`                  VARCHAR(20)  NOT NULL DEFAULT 'active' COMMENT '会话状态 active/closed',
  `risk_level`              TINYINT      NOT NULL DEFAULT 0 COMMENT '风险等级 0正常 1关注 2预警 3危机',
  `started_at`              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
  `last_emotion_analysis`   TEXT         COMMENT '最近一次情绪分析结果(JSON)',
  `last_emotion_updated_at` DATETIME     DEFAULT NULL COMMENT '情绪分析更新时间',
  `updated_at`              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_user` (`user_id`),
  KEY `idx_session_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='咨询会话表';

-- ---------------------------------------------------------------------
-- 3. 咨询消息表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `consultation_message`;
CREATE TABLE `consultation_message` (
  `id`           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `session_id`   BIGINT      NOT NULL COMMENT '会话ID',
  `sender_type`  TINYINT     NOT NULL COMMENT '发送者 1用户 2AI助手',
  `message_type` TINYINT     NOT NULL DEFAULT 1 COMMENT '消息类型 1文本',
  `content`      MEDIUMTEXT  NOT NULL COMMENT '消息内容',
  `emotion_tag`  VARCHAR(50) DEFAULT NULL COMMENT '情绪标签',
  `ai_model`     VARCHAR(50) DEFAULT NULL COMMENT 'AI模型名',
  `risk_level`   TINYINT     NOT NULL DEFAULT 0 COMMENT '风险等级 0-3',
  `created_at`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_message_session` (`session_id`),
  KEY `idx_message_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='咨询消息表';

-- ---------------------------------------------------------------------
-- 4. 情绪日记表
-- 产品设计采用方案 A：score 为“总体状态”1-10 分，1 很差，10 很好。
-- 必填仅 emotion_status + score，其余可选项。
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `emotion_diary`;
CREATE TABLE `emotion_diary` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日记ID',
  `user_id`        BIGINT       NOT NULL COMMENT '用户ID',
  `emotion_status` VARCHAR(50)  NOT NULL COMMENT '主情绪（平静/焦虑/低落/愤怒/疲惫/开心）',
  `score`          INT          NOT NULL DEFAULT 7 COMMENT '总体状态 1-10，1很差 10很好',
  `event`          VARCHAR(1000) DEFAULT NULL COMMENT '触发事件（可选）',
  `sleep_status`   VARCHAR(20)  DEFAULT NULL COMMENT '睡眠 较好/一般/较差',
  `energy_status`  VARCHAR(20)  DEFAULT NULL COMMENT '精力 充足/中等/不足',
  `log_date`       DATE         NOT NULL COMMENT '记录所属日期',
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_diary_user_date` (`user_id`, `log_date`),
  KEY `idx_diary_user` (`user_id`),
  KEY `idx_diary_logdate` (`log_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='情绪日记表';

-- ---------------------------------------------------------------------
-- 5. 知识分类表（自关联树）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `knowledge_category`;
CREATE TABLE `knowledge_category` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `parent_id`  BIGINT      NOT NULL DEFAULT 0 COMMENT '父分类ID，0为根',
  `name`       VARCHAR(100) NOT NULL COMMENT '分类名称',
  `sort_no`    INT         NOT NULL DEFAULT 0 COMMENT '排序号，越小越靠前',
  `status`     TINYINT     NOT NULL DEFAULT 1 COMMENT '状态 0禁用 1正常',
  `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识分类表';

-- ---------------------------------------------------------------------
-- 6. 知识文章表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `knowledge_article`;
CREATE TABLE `knowledge_article` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文章ID',
  `category_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '分类ID',
  `title`         VARCHAR(200) NOT NULL COMMENT '标题',
  `summary`       VARCHAR(500) DEFAULT NULL COMMENT '摘要',
  `content`       MEDIUMTEXT   COMMENT '正文（富文本HTML）',
  `source`        VARCHAR(100) DEFAULT NULL COMMENT '来源',
  `cover_url`     VARCHAR(255) DEFAULT NULL COMMENT '封面URL',
  `author_id`     BIGINT       DEFAULT NULL COMMENT '作者用户ID',
  `tags`          VARCHAR(500) DEFAULT NULL COMMENT '标签，逗号分隔',
  `status`        VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT草稿 PENDING待审 PUBLISHED已发布 REJECTED已驳回 OFFLINE已下线',
  `audit_remark`  VARCHAR(500) DEFAULT NULL COMMENT '审核备注/驳回原因',
  `view_count`    INT          NOT NULL DEFAULT 0 COMMENT '浏览量',
  `minutes`       INT          NOT NULL DEFAULT 5 COMMENT '预计阅读/练习时长（分钟）',
  `published_at`  DATETIME     DEFAULT NULL COMMENT '发布时间',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_article_category` (`category_id`),
  KEY `idx_article_status` (`status`),
  KEY `idx_article_published` (`published_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识文章表';

-- ---------------------------------------------------------------------
-- 7. 系统文件信息表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_file_info`;
CREATE TABLE `sys_file_info` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `original_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
  `storage_name`  VARCHAR(255) NOT NULL COMMENT '存储文件名',
  `file_url`      VARCHAR(500) NOT NULL COMMENT '访问URL',
  `content_type`  VARCHAR(100) DEFAULT NULL COMMENT 'MIME类型',
  `file_size`     BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
  `business_type` VARCHAR(50)  DEFAULT NULL COMMENT '业务类型 avatar/cover 等',
  `uploader_id`   BIGINT       DEFAULT NULL COMMENT '上传者ID',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_file_uploader` (`uploader_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统文件信息表';

SET FOREIGN_KEY_CHECKS = 1;
