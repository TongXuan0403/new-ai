-- =====================================================================
-- Codex-AI 心理健康助手 MVP · V3 演示数据
-- 执行：mysql -uroot -p < V3__seed_data.sql
-- 账号：admin / Admin@123（管理员），demo / Demo@123（普通用户）
-- =====================================================================

USE `mental_health`;
SET NAMES utf8mb4;

-- 管理员与普通用户（BCrypt 密文）
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `user_type`, `status`, `created_at`) VALUES
  (1, 'admin', '$2b$10$Q6GoLSftMOJDrXLmuXpEUebBBphXc3TKRJIes18C0g.hCJsydaewe', '运营管理员', 2, 1, NOW()),
  (2, 'demo',  '$2b$10$bE/JnAPsDeQu.ow/dCbF9.3.j3/HYi/QmnBlWucsXDfhA1cEys6vS', '林同学',    1, 1, NOW());

-- 知识分类
INSERT INTO `knowledge_category` (`id`, `parent_id`, `name`, `sort_no`, `status`) VALUES
  (1, 0, '压力',   1, 1),
  (2, 0, '睡眠',   2, 1),
  (3, 0, '关系',   3, 1),
  (4, 0, '自助练习', 4, 1);

-- 知识文章（正文为受控富文本，前端白名单渲染）
INSERT INTO `knowledge_article`
  (`id`, `category_id`, `title`, `summary`, `content`, `tags`, `source`, `author_id`, `status`, `minutes`, `view_count`, `published_at`, `created_at`, `updated_at`) VALUES
  (1, 2, '考试周睡不着时，先降低入睡压力',
   '把目标从“必须睡着”调成“让身体休息十分钟”，减少入睡压力。',
   '<p>当人越努力要求自己立刻睡着，大脑越容易保持警觉。可以先把目标从“必须睡着”调成“让身体休息十分钟”。</p><p>把手机放到够不着的位置，允许自己只是躺着休息。明天需要完成的事，可以先写在纸上，让大脑暂时不用反复提醒你。</p>',
   '睡眠,压力,考试',
   '运营团队', 1, 'PUBLISHED', 5, 0, NOW(), NOW(), NOW()),
  (2, 4, '焦虑升高时的 3 分钟落地呼吸',
   '一个目标不是消灭焦虑，而是让身体稍微稳住的三分钟练习。',
   '<p>从脚底触地感开始，依次注意你能看到、听到和触碰到的事物。这个练习的目标不是消灭焦虑，而是让身体稍微稳住。</p><p>你可以用三轮呼吸完成练习：吸气时感受空气进入，呼气时让肩膀和下颌松一点。</p>',
   '焦虑,呼吸,放松',
   '运营团队', 1, 'PUBLISHED', 3, 0, NOW(), NOW(), NOW()),
  (3, 3, '关系冲突后的边界表达',
   '用事实、感受、需要和请求四步，练习边界表达。',
   '<p>边界表达可以从事实、感受、需要和请求四步开始。先说清楚具体事件，再说明它对你的影响。</p><p>例如：“刚才临时改变安排时我有些慌乱，我需要提前知道变化。下次可以先和我说一声吗？”</p>',
   '关系,边界,沟通',
   '运营团队', 1, 'PENDING_REVIEW', 4, 0, NULL, NOW(), NOW()),
  (4, 1, '把大任务拆成今天能做的一步',
   '把任务拆到 15 分钟内能完成的一步，重新启动行动。',
   '<p>压力过大时，大脑会倾向于把任务看成一整块。把任务拆到 15 分钟内能完成的一步，往往更容易重新启动。</p><p>先写下下一步动作，而不是完整目标。例如从“准备汇报”改成“打开文档，写出三个小标题”。</p>',
   '压力,行动,拆解',
   '运营团队', 1, 'PUBLISHED', 4, 0, NOW(), NOW(), NOW());

-- 危机资源（默认内置；校内资源由运营方上线前核验配置）
INSERT INTO `crisis_resource` (`id`, `resource_type`, `name`, `phone`, `description`, `region`, `enabled`, `sort_order`) VALUES
  (1, 'emergency', '紧急医疗救援',     '120',   '立即危险或需要急救时拨打',       NULL, 1, 1),
  (2, 'emergency', '紧急警情',         '110',   '需要警务协助时拨打',             NULL, 1, 2),
  (3, 'hotline',   '全国统一心理援助热线', '12356', '心理援助资源，供需要情绪支持的人拨打', NULL, 1, 3),
  (4, 'school',    '校内心理中心',     NULL,   '工作日 09:00 - 18:00 · 上线前由运营方配置本校联系方式', '待配置', 1, 4);
