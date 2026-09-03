-- =====================================================================
-- Codex-AI 心理健康助手 MVP · V5 P1 新增表
-- 覆盖：文章收藏 / 自助练习库与完成记录 / 提示词·模型·风险规则版本化
-- 执行：mysql -uroot -p < V5__p1_tables.sql
-- =====================================================================

USE `mental_health`;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------
-- 14. 文章收藏表 article_favorite
-- 学生端收藏已审核文章；每用户每篇文章最多一条
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `article_favorite`;
CREATE TABLE `article_favorite` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`    BIGINT   NOT NULL COMMENT '用户ID',
  `article_id` BIGINT   NOT NULL COMMENT '文章ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fav_user_article` (`user_id`, `article_id`),
  KEY `idx_fav_article` (`article_id`),
  KEY `idx_fav_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章收藏表';

-- ---------------------------------------------------------------------
-- 15. 自助练习表 exercise
-- 轻量自助练习（呼吸、落地、拆分任务等），学生端只展示已发布
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `exercise`;
CREATE TABLE `exercise` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '练习ID',
  `category_id`   BIGINT       NOT NULL DEFAULT 4 COMMENT '分类ID（默认4自助练习）',
  `title`         VARCHAR(200) NOT NULL COMMENT '练习名称',
  `summary`       VARCHAR(500) DEFAULT NULL COMMENT '一句话简介',
  `content`       MEDIUMTEXT   COMMENT '练习步骤（富文本/分段文本）',
  `minutes`       INT          NOT NULL DEFAULT 5 COMMENT '预计时长（分钟）',
  `tags`          VARCHAR(500) DEFAULT NULL COMMENT '标签，逗号分隔',
  `sort_order`    INT          NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
  `status`        VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT/PUBLISHED/OFFLINE',
  `published_at`  DATETIME     DEFAULT NULL COMMENT '发布时间',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_exercise_status` (`status`),
  KEY `idx_exercise_sort` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自助练习表';

-- ---------------------------------------------------------------------
-- 16. 练习完成记录表 exercise_completion
-- 记录学生完成练习的时间与练习后感受；每用户每练习最多一条
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `exercise_completion`;
CREATE TABLE `exercise_completion` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`      BIGINT       NOT NULL COMMENT '用户ID',
  `exercise_id`  BIGINT       NOT NULL COMMENT '练习ID',
  `mood_after`   VARCHAR(100) DEFAULT NULL COMMENT '练习后感受（可选）',
  `completed_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '完成时间',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_completion_user_exercise` (`user_id`, `exercise_id`),
  KEY `idx_completion_exercise` (`exercise_id`),
  KEY `idx_completion_completed` (`completed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='练习完成记录表';

-- ---------------------------------------------------------------------
-- 17. 系统配置版本表 system_config_version
-- 提示词 / 模型 / 风险规则 后台版本化；每类同时仅一条 ACTIVE
-- status: DRAFT 草稿 / ACTIVE 生效 / DISABLED 停用
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `system_config_version`;
CREATE TABLE `system_config_version` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_type` VARCHAR(20)  NOT NULL COMMENT '类型 PROMPT/MODEL/RISK_RULE',
  `name`        VARCHAR(100) NOT NULL COMMENT '配置名称',
  `version`     VARCHAR(50)  NOT NULL COMMENT '版本号，如 prompt-v1.0',
  `content`     TEXT         COMMENT '配置内容（提示词文本/模型名/风险规则JSON）',
  `status`      VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT/ACTIVE/DISABLED',
  `remark`      VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_by`  BIGINT       DEFAULT NULL COMMENT '创建人ID',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cfg_type_version` (`config_type`, `version`),
  KEY `idx_cfg_type_status` (`config_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置版本表';

-- ---------------------------------------------------------------------
-- 种子数据：初始版本（与代码内置默认一致，运行时优先使用 ACTIVE 版本）
-- ---------------------------------------------------------------------
INSERT INTO `system_config_version` (`config_type`, `name`, `version`, `content`, `status`, `remark`, `created_by`, `created_at`, `updated_at`) VALUES
  ('PROMPT', '心理支持系统提示词', 'prompt-v1.0',
   '你是一位温暖、克制、有同理心的 AI 心理支持助手，面向 18 岁及以上大学生，提供情绪表达与自助管理支持。你不是心理医生、不是心理咨询师，也不提供诊断、治疗、用药或紧急救援服务。\n角色与边界：\n- 倾听并复述用户的感受与事件，不假设未提供的事实\n- 帮助用户梳理情绪和处境，但不下结论、不贴标签\n- 每次给出一个低负担、当下可完成的小行动，例如呼吸、记录、联系可信任的人、拆分任务\n- 当困扰持续、加重或影响学习生活时，建议联系校内心理中心、医生或合格的心理服务人员\n- 全程使用简体中文，语言温暖自然但不做过度承诺\n禁止出现：\n- 不得声称自己是医生/咨询师，不得说"你患有/确诊为……"等诊断表达\n- 不得给出用药、剂量、停药、治疗方案建议\n- 不得承诺"只要坚持和我聊就会好""我会一直陪着你"等空泛承诺\n- 不得让用户"不要告诉任何人"或暗示替用户保密\n- 不得暗示"已通知老师/专人联系你/有人正在监控"等不存在的处置\n- 不得使用"你肯定/你一定/你就是"等绝对化判断\n回复结构建议：\n1. 用一到两句复述感受或事件\n2. 提一个可选的澄清问题，避免连续追问\n3. 给出一项低负担、当下可完成的建议\n4. 必要时建议线下支持\n回复长度适中，一次聚焦一个点。',
   'ACTIVE', 'P1 版本化初始版本，与代码内置提示词一致', 1, NOW(), NOW()),
  ('MODEL', '默认对话模型', 'model-v1.0',
   'deepseek-ai/DeepSeek-V3',
   'ACTIVE', 'P1 版本化初始版本', 1, NOW(), NOW()),
  ('RISK_RULE', '风险识别关键词规则', 'rule-v1.0',
   '{"crisis":["自杀","杀了自己","结束生命","不想活了","活不下去","正在自伤","已经伤害自己","马上伤害","伤害别人","杀人","想死","现在就死","已经割","已经吃药"],"harmOthers":["伤害别人","杀人","报复社会"],"warning":["自残","伤害自己","撑不住","崩溃","失控","严重失眠","无望","没有意义","活得好累","坚持不下去","想消失"],"concern":["持续低落","很无助","最近很痛苦","情绪很低"]}',
   'ACTIVE', 'P1 版本化初始版本，与代码内置规则一致', 1, NOW(), NOW());

-- ---------------------------------------------------------------------
-- 种子数据：自助练习演示内容（复用知识分类 id=4 自助练习）
-- ---------------------------------------------------------------------
INSERT INTO `exercise` (`id`, `category_id`, `title`, `summary`, `content`, `minutes`, `tags`, `sort_order`, `status`, `published_at`, `created_at`, `updated_at`) VALUES
  (1, 4, '焦虑升高时的 3 分钟落地呼吸',
   '一个目标不是消灭焦虑，而是让身体稍微稳住的三分钟练习。',
   '步骤一：从脚底触地感开始，留意脚踩在地面上的感觉。\n步骤二：依次注意你当下能看到、听到和触碰到的事物各一件。\n步骤三：完成三轮呼吸，吸气时感受空气进入，呼气时让肩膀和下颌松一点。\n步骤四：结束后用一句话记录此刻的状态，不需要评判好坏。',
   3, '焦虑,呼吸,放松', 1, 'PUBLISHED', NOW(), NOW(), NOW()),
  (2, 4, '把大任务拆成今天能做的一步',
   '把任务拆到 15 分钟内能完成的一步，重新启动行动。',
   '步骤一：写下当前让你卡住的任务，不需要整理得很完美。\n步骤二：把它拆成 15 分钟内能完成的最小一步，例如"打开文档，写出三个小标题"。\n步骤三：只做这最小的一步，完成后标记为完成。\n步骤四：如果还愿意继续，再拆下一步；否则就到此为止。',
   4, '压力,行动,拆解', 2, 'PUBLISHED', NOW(), NOW(), NOW()),
  (3, 4, '睡前给大脑减负的纸笔练习',
   '把明天要做的事写在纸上，让大脑暂时不用反复提醒你。',
   '步骤一：准备纸笔或手机备忘录。\n步骤二：把明天需要完成的事逐条写下来，不需要排优先级。\n步骤三：在每个任务旁标一个"最可能拖延的环节"。\n步骤四：合上本子，提醒自己：这些事已经记下来了，现在可以休息。',
   5, '睡眠,焦虑,书写', 3, 'PUBLISHED', NOW(), NOW(), NOW()),
  (4, 4, '关系冲突后的边界表达练习',
   '用事实、感受、需要和请求四步，练习边界表达。',
   '步骤一（事实）：先只描述具体事件，不评价对方。\n步骤二（感受）：说明这件事带给你的感受。\n步骤三（需要）：说明你希望被理解或需要什么。\n步骤四（请求）：提出一个具体、可执行的请求。\n例如："刚才临时改变安排时我有些慌乱，我需要提前知道变化，下次可以先和我说一声吗？"',
   4, '关系,边界,表达', 4, 'DRAFT', NULL, NOW(), NOW());

SET FOREIGN_KEY_CHECKS = 1;
