-- =====================================================================
-- 心理健康助手 ai-spingboot · 建表脚本（mental_health_assistant）
-- MySQL 8.0+ / InnoDB / utf8mb4
-- 执行：mysql -uroot -p < schema.sql
-- 种子数据（admin/demo 用户、分类、文章）由后端 DataInitializer 启动时自动写入
-- =====================================================================
CREATE DATABASE IF NOT EXISTS `mental_health_assistant`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `mental_health_assistant`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. 用户表
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username`   VARCHAR(50)  NOT NULL COMMENT '用户名',
  `email`      VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `phone`      VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  `password`   VARCHAR(100) NOT NULL COMMENT '密码（BCrypt密文）',
  `nickname`   VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
  `avatar`     VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `gender`     TINYINT      NOT NULL DEFAULT 0 COMMENT '性别 0未知 1男 2女',
  `birthday`   DATE         DEFAULT NULL COMMENT '生日',
  `user_type`  TINYINT      NOT NULL DEFAULT 1 COMMENT '用户类型 1普通用户 2管理员',
  `status`     TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 0禁用 1正常',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  KEY `idx_user_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 咨询会话表
DROP TABLE IF EXISTS `consultation_session`;
CREATE TABLE `consultation_session` (
  `id`                      BIGINT      NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `user_id`                 BIGINT      NOT NULL COMMENT '用户ID',
  `session_title`           VARCHAR(200) DEFAULT NULL COMMENT '会话标题',
  `started_at`              DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
  `last_emotion_analysis`   TEXT        COMMENT '最近一次情绪分析结果(JSON)',
  `last_emotion_updated_at` DATETIME    DEFAULT NULL COMMENT '情绪分析更新时间',
  `updated_at`              DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='咨询会话表';

-- 3. 咨询消息表
DROP TABLE IF EXISTS `consultation_message`;
CREATE TABLE `consultation_message` (
  `id`           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `session_id`   BIGINT      NOT NULL COMMENT '会话ID',
  `sender_type`  TINYINT     NOT NULL COMMENT '发送者 1用户 2AI助手',
  `message_type` TINYINT     NOT NULL DEFAULT 1 COMMENT '消息类型 1文本',
  `content`      MEDIUMTEXT  NOT NULL COMMENT '消息内容',
  `emotion_tag`  VARCHAR(50) DEFAULT NULL COMMENT '情绪标签',
  `ai_model`     VARCHAR(50) DEFAULT NULL COMMENT 'AI模型名称',
  `created_at`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_message_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='咨询消息表';

-- 4. 知识分类表
DROP TABLE IF EXISTS `knowledge_category`;
CREATE TABLE `knowledge_category` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name`       VARCHAR(100) NOT NULL COMMENT '分类名称',
  `sort_no`    INT          NOT NULL DEFAULT 0 COMMENT '排序号，越小越靠前',
  `status`     TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 0禁用 1正常',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识分类表';

-- 5. 知识文章表
DROP TABLE IF EXISTS `knowledge_article`;
CREATE TABLE `knowledge_article` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文章ID',
  `category_id`  BIGINT       NOT NULL DEFAULT 0 COMMENT '分类ID',
  `title`        VARCHAR(200) NOT NULL COMMENT '标题',
  `summary`      VARCHAR(500) DEFAULT NULL COMMENT '摘要',
  `content`      MEDIUMTEXT   COMMENT '正文（富文本HTML）',
  `cover_image`  VARCHAR(255) DEFAULT NULL COMMENT '封面图路径',
  `tags`         VARCHAR(500) DEFAULT NULL COMMENT '标签，逗号分隔',
  `status`       TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0草稿 1已发布 2已下线',
  `read_count`   INT          NOT NULL DEFAULT 0 COMMENT '阅读量',
  `author`       VARCHAR(50)  DEFAULT NULL COMMENT '作者',
  `published_at` DATETIME     DEFAULT NULL COMMENT '发布时间',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_article_category` (`category_id`),
  KEY `idx_article_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识文章表';

-- 6. 情绪日记表
DROP TABLE IF EXISTS `emotion_diary`;
CREATE TABLE `emotion_diary` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日记ID',
  `user_id`          BIGINT       NOT NULL COMMENT '用户ID',
  `user_name`        VARCHAR(50)  DEFAULT NULL COMMENT '用户名',
  `diary_date`       DATE         NOT NULL COMMENT '记录所属日期',
  `mood_score`       INT          DEFAULT NULL COMMENT '情绪评分 0-10',
  `dominant_emotion` VARCHAR(50)  DEFAULT NULL COMMENT '主导情绪',
  `emotion_triggers` VARCHAR(500) DEFAULT NULL COMMENT '情绪诱因',
  `diary_content`    TEXT         COMMENT '日记正文',
  `sleep_quality`    INT          DEFAULT NULL COMMENT '睡眠质量',
  `stress_level`     INT          DEFAULT NULL COMMENT '压力等级 1-5',
  `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_diary_user` (`user_id`),
  KEY `idx_diary_date` (`diary_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='情绪日记表';

SET FOREIGN_KEY_CHECKS = 1;

-- 校验：应返回 6
-- SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='mental_health_assistant';
