-- =====================================================================
-- 心理健康助手 · 演示种子数据（成长计划/预约管理/校园报告数据）
-- 生成时间：2026-09-04   执行：mysql -uroot -p123456 --default-character-set=utf8mb4 < seed_demo_data.sql
-- =====================================================================
USE `mental_health_assistant`;
SET NAMES utf8mb4;

-- ---------- 成长计划 growth_plan（10 条） ----------
INSERT INTO growth_plan (title, summary, theme, content, duration_days, reviewer, reviewed_at, status, view_count, created_at, updated_at)
VALUES ('21 天情绪调节进阶计划', '在情绪觉察基础上，系统学习调节技巧，从识别走向主动调节。', '情绪', '## 第一周：觉察升级\\n1. 每天记录 3 次情绪瞬间（时间/情绪/触发事件）。\\n2. 给情绪强度打分 1-10。\\n\\n## 第二周：调节工具箱\\n1. 每天练习一种调节方法：4-7-8 呼吸、STOP 暂停法、改写认知。\\n2. 记录方法是否有效。\\n\\n## 第三周：整合应用\\n1. 面对真实压力事件时主动使用调节技巧。\\n2. 复盘哪些方法对自己最有效，建立个人情绪调节手册。', 21, '专业审核', '2026-09-04 09:00:00', 1, 68, '2026-09-04 09:00:00', '2026-09-04 09:00:00');
INSERT INTO growth_plan (title, summary, theme, content, duration_days, reviewer, reviewed_at, status, view_count, created_at, updated_at)
VALUES ('7 天正念呼吸训练计划', '每天 10 分钟正念呼吸，让注意力回到当下，缓解焦虑。', '正念', '## 每日训练\\n1. 每天固定时间做 10 分钟正念呼吸。\\n2. 关注鼻尖空气进出的感觉，走神时温和地把注意力带回来。\\n3. 结束后记录身体的放松感受。\\n\\n## 进阶\\n第 4 天起尝试正念行走：散步时只感受脚掌与地面的接触。', 7, '专业审核', '2026-09-04 09:00:00', 1, 52, '2026-09-04 09:00:00', '2026-09-04 09:00:00');
INSERT INTO growth_plan (title, summary, theme, content, duration_days, reviewer, reviewed_at, status, view_count, created_at, updated_at)
VALUES ('14 天睡眠节律重建计划', '从作息、光照与睡前仪式入手，重建稳定的生物钟。', '睡眠', '## 第 1-3 天：摸底\\n1. 记录每晚入睡与醒来时间，找出节律偏移。\\n2. 早晨起床后立刻拉开窗帘接触自然光 10 分钟。\\n\\n## 第 4-10 天：固定\\n1. 固定起床时间，周末也不例外。\\n2. 睡前一小时调暗灯光、远离电子屏。\\n\\n## 第 11-14 天：巩固\\n1. 午睡不超过 30 分钟，且不晚于下午 3 点。\\n2. 白天适度运动，睡前 3 小时避免剧烈运动与咖啡因。', 14, '专业审核', '2026-09-04 09:00:00', 1, 41, '2026-09-04 09:00:00', '2026-09-04 09:00:00');
INSERT INTO growth_plan (title, summary, theme, content, duration_days, reviewer, reviewed_at, status, view_count, created_at, updated_at)
VALUES ('30 天自我关怀成长计划', '学会像对待朋友一样对待自己，建立稳定内在支持系统。', '自我成长', '## 第一周：认识自我批评\\n1. 记录每天对自己说过的苛责话语。\\n2. 把它们改写为对朋友的语气。\\n\\n## 第二周：练习自我关怀\\n1. 每天做一次 5 分钟自我关怀冥想。\\n2. 给自己写一封温柔的信。\\n\\n## 第三周：身体与情绪关怀\\n1. 每天留 20 分钟做让自己舒服的事。\\n2. 学会在累的时候说“我值得休息”。\\n\\n## 第四周：整合与巩固\\n1. 建立个人自我关怀清单，放入随时可看的位置。\\n2. 复盘 30 天变化，写下三个最重要的收获。', 30, '专业审核', '2026-09-04 09:00:00', 1, 35, '2026-09-04 09:00:00', '2026-09-04 09:00:00');
INSERT INTO growth_plan (title, summary, theme, content, duration_days, reviewer, reviewed_at, status, view_count, created_at, updated_at)
VALUES ('21 天学习专注力提升计划', '用番茄工作法 + 环境管理，训练深度专注能力。', '学习效能', '## 第一周：环境准备\\n1. 每天学习前清理桌面，手机放到 3 米外。\\n2. 固定学习地点与时间段。\\n\\n## 第二周：专注训练\\n1. 采用 25 分钟番茄 + 5 分钟休息循环，每天 4 个番茄。\\n2. 休息时离开座位活动身体。\\n\\n## 第三周：挑战与巩固\\n1. 逐步把番茄延长到 40 分钟。\\n2. 记录每天进入专注状态的速度，寻找个人最佳启动方式。', 21, '专业审核', '2026-09-04 09:00:00', 1, 59, '2026-09-04 09:00:00', '2026-09-04 09:00:00');
INSERT INTO growth_plan (title, summary, theme, content, duration_days, reviewer, reviewed_at, status, view_count, created_at, updated_at)
VALUES ('14 天社交勇气训练计划', '从最小社交行动开始，逐步克服社交回避。', '人际', '## 第 1-4 天：热身\\n1. 每天向一位同学微笑并打招呼。\\n2. 在课堂上主动回答一次问题。\\n\\n## 第 5-9 天：主动连接\\n1. 邀请一位同学一起吃午饭或散步。\\n2. 主动开启一次话题，准备 2 个开放性问题。\\n\\n## 第 10-14 天：深化\\n1. 参加一次小组活动或社团聚会。\\n2. 练习表达自己的感受与需求，记录他人积极回应。', 14, '专业审核', '2026-09-04 09:00:00', 1, 47, '2026-09-04 09:00:00', '2026-09-04 09:00:00');
INSERT INTO growth_plan (title, summary, theme, content, duration_days, reviewer, reviewed_at, status, view_count, created_at, updated_at)
VALUES ('7 天晨间能量启动计划', '用 7 天建立一套晨间仪式，让一天从清醒开始。', '作息', '## 每日晨间仪式\\n1. 起床后先喝一杯温水。\\n2. 做 5 分钟拉伸或开合跳唤醒身体。\\n3. 写下今天最重要的 3 件事。\\n4. 出门前晒 10 分钟太阳。\\n\\n连续 7 天执行，记录每天上午的精神状态评分。', 7, '专业审核', '2026-09-04 09:00:00', 1, 63, '2026-09-04 09:00:00', '2026-09-04 09:00:00');
INSERT INTO growth_plan (title, summary, theme, content, duration_days, reviewer, reviewed_at, status, view_count, created_at, updated_at)
VALUES ('28 天自尊自信重塑计划', '通过成就清单、优势探索与自我对话，重建稳定的自我价值感。', '自尊自信', '## 第一周：看见自己\\n1. 每天写下 3 件做成的“小事”（哪怕很小）。\\n2. 回顾过往，列出自己 10 个优点。\\n\\n## 第二周：优势实践\\n1. 选择 1-2 个优势，本周主动使用。\\n2. 记录使用优势时他人的反馈。\\n\\n## 第三周：接纳局限\\n1. 写下自己的局限，区分“可改变”与“需接纳”。\\n2. 练习用“我在进步”代替“我不够好”。\\n\\n## 第四周：整合\\n1. 为自己写一份《自我价值宣言》。\\n2. 每天早晨朗读一遍，持续巩固。', 28, '专业审核', '2026-09-04 09:00:00', 1, 39, '2026-09-04 09:00:00', '2026-09-04 09:00:00');
INSERT INTO growth_plan (title, summary, theme, content, duration_days, reviewer, reviewed_at, status, view_count, created_at, updated_at)
VALUES ('14 天时间管理减压计划', '用四象限与每日规划，把压力转化为可控的行动。', '压力', '## 第 1-4 天：清空\\n1. 把所有待办事项全部写下来，避免脑子记挂。\\n2. 用四象限（重要-紧急）分类。\\n\\n## 第 5-9 天：规划\\n1. 每天睡前为次日列“最多 3 件要事”。\\n2. 给每件事分配具体时间块。\\n\\n## 第 10-14 天：复盘\\n1. 每天 5 分钟复盘：完成度、被打断原因。\\n2. 保留“留白时间”，应对意外，减少焦虑。', 14, '专业审核', '2026-09-04 09:00:00', 1, 55, '2026-09-04 09:00:00', '2026-09-04 09:00:00');
INSERT INTO growth_plan (title, summary, theme, content, duration_days, reviewer, reviewed_at, status, view_count, created_at, updated_at)
VALUES ('21 天感恩练习计划', '每天发现生活里值得感激的小事，提升幸福感与心理韧性。', '积极心理', '## 每日任务\\n1. 每晚睡前写下 3 件今天值得感谢的事，越具体越好。\\n2. 每周挑一件，向当事人当面或文字表达感谢。\\n3. 用相机或文字记录一个“美好瞬间”。\\n\\n## 进阶\\n第 15 天起，把感恩对象从“人”扩展到“环境、身体、经历”。', 21, '专业审核', '2026-09-04 09:00:00', 1, 72, '2026-09-04 09:00:00', '2026-09-04 09:00:00');

-- ---------- 新学生用户（校园报告数据支撑） ----------
INSERT INTO user (username, email, phone, password, nickname, gender, user_type, status, created_at, updated_at)
VALUES ('wangwu', 'wangwu@stu.edu.cn', '13800000001', '$2a$10$BtZbX2w0JISu4O/i2eSZce713seYQrYvhRu1DlmA/qQvG8SVd8uRi', '王五', 1, 1, 1, '2026-08-30 10:00:00', '2026-08-30 10:00:00');
INSERT INTO user (username, email, phone, password, nickname, gender, user_type, status, created_at, updated_at)
VALUES ('zhaoliu', 'zhaoliu@stu.edu.cn', '13800000002', '$2a$10$BtZbX2w0JISu4O/i2eSZce713seYQrYvhRu1DlmA/qQvG8SVd8uRi', '赵六', 2, 1, 1, '2026-08-31 10:00:00', '2026-08-31 10:00:00');
INSERT INTO user (username, email, phone, password, nickname, gender, user_type, status, created_at, updated_at)
VALUES ('sunqi', 'sunqi@stu.edu.cn', '13800000003', '$2a$10$BtZbX2w0JISu4O/i2eSZce713seYQrYvhRu1DlmA/qQvG8SVd8uRi', '孙七', 1, 1, 1, '2026-09-01 10:00:00', '2026-09-01 10:00:00');
INSERT INTO user (username, email, phone, password, nickname, gender, user_type, status, created_at, updated_at)
VALUES ('zhouba', 'zhouba@stu.edu.cn', '13800000004', '$2a$10$BtZbX2w0JISu4O/i2eSZce713seYQrYvhRu1DlmA/qQvG8SVd8uRi', '周八', 2, 1, 1, '2026-09-02 10:00:00', '2026-09-02 10:00:00');
INSERT INTO user (username, email, phone, password, nickname, gender, user_type, status, created_at, updated_at)
VALUES ('wujiu', 'wujiu@stu.edu.cn', '13800000005', '$2a$10$BtZbX2w0JISu4O/i2eSZce713seYQrYvhRu1DlmA/qQvG8SVd8uRi', '吴九', 1, 1, 1, '2026-09-03 10:00:00', '2026-09-03 10:00:00');
SET @sid_wangwu = LAST_INSERT_ID();
SET @sid_1 = @sid_wangwu + 1;
SET @sid_2 = @sid_wangwu + 2;
SET @sid_3 = @sid_wangwu + 3;
SET @sid_4 = @sid_wangwu + 4;

-- ---------- 预约管理 appointment_request（10 条） ----------
INSERT INTO appointment_request (user_id, user_name, resource_id, resource_name, appointment_date, appointment_time, reason, contact, status, remark, created_at, updated_at)
VALUES (2, 'demo', 1, '学校心理健康教育中心', '2026-09-08', '上午 09:00-09:50', '最近持续失眠，想咨询睡眠问题', '13812345678', 1, '已安排咨询师张老师，请提前 10 分钟到场', '2026-09-04 09:12:00', '2026-09-04 09:12:00');
INSERT INTO appointment_request (user_id, user_name, resource_id, resource_name, appointment_date, appointment_time, reason, contact, status, remark, created_at, updated_at)
VALUES (3, '张三', 1, '学校心理健康教育中心', '2026-09-09', '下午 14:00-14:50', '考试压力大，经常焦虑到无法入睡', '13912345678', 1, '已确认，咨询师李老师', '2026-09-04 09:30:00', '2026-09-04 09:30:00');
INSERT INTO appointment_request (user_id, user_name, resource_id, resource_name, appointment_date, appointment_time, reason, contact, status, remark, created_at, updated_at)
VALUES (4, '李四', 1, '学校心理健康教育中心', '2026-09-10', '下午 15:00-15:50', '与室友关系紧张，想聊聊人际困扰', '13712345678', 0, NULL, '2026-09-04 10:05:00', '2026-09-04 10:05:00');
INSERT INTO appointment_request (user_id, user_name, resource_id, resource_name, appointment_date, appointment_time, reason, contact, status, remark, created_at, updated_at)
VALUES (2, 'demo', 2, '全国统一心理援助热线 12356', '2026-09-06', '晚上 19:00-19:50', '夜间情绪低落，希望电话倾诉', '13812345678', 3, '已完成电话支持', '2026-09-02 20:00:00', '2026-09-02 20:00:00');
INSERT INTO appointment_request (user_id, user_name, resource_id, resource_name, appointment_date, appointment_time, reason, contact, status, remark, created_at, updated_at)
VALUES (3, '张三', 1, '学校心理健康教育中心', '2026-09-12', '上午 10:00-10:50', '对未来的职业方向感到迷茫，持续焦虑', '13912345678', 0, NULL, '2026-09-04 11:20:00', '2026-09-04 11:20:00');
INSERT INTO appointment_request (user_id, user_name, resource_id, resource_name, appointment_date, appointment_time, reason, contact, status, remark, created_at, updated_at)
VALUES (4, '李四', 2, '全国统一心理援助热线 12356', '2026-09-07', '下午 15:00-15:50', '家庭变故后情绪持续低落', '13712345678', 2, '用户临时取消', '2026-09-03 15:40:00', '2026-09-03 15:40:00');
INSERT INTO appointment_request (user_id, user_name, resource_id, resource_name, appointment_date, appointment_time, reason, contact, status, remark, created_at, updated_at)
VALUES (@sid_wangwu, '王五', 1, '学校心理健康教育中心', '2026-09-11', '上午 09:00-09:50', '开学适应困难，想预约一次初步评估', '13800000001', 1, '已确认，首次评估', '2026-09-04 13:00:00', '2026-09-04 13:00:00');
INSERT INTO appointment_request (user_id, user_name, resource_id, resource_name, appointment_date, appointment_time, reason, contact, status, remark, created_at, updated_at)
VALUES (@sid_1, '赵六', 1, '学校心理健康教育中心', '2026-09-15', '下午 14:00-14:50', '社交焦虑，不敢在公开场合发言', '13800000002', 0, NULL, '2026-09-04 14:10:00', '2026-09-04 14:10:00');
INSERT INTO appointment_request (user_id, user_name, resource_id, resource_name, appointment_date, appointment_time, reason, contact, status, remark, created_at, updated_at)
VALUES (@sid_2, '孙七', 2, '全国统一心理援助热线 12356', '2026-09-13', '晚上 20:00-20:50', '与异地恋女友分手，情绪崩溃', '13800000003', 1, '已确认，建议结合线下咨询', '2026-09-04 15:00:00', '2026-09-04 15:00:00');
INSERT INTO appointment_request (user_id, user_name, resource_id, resource_name, appointment_date, appointment_time, reason, contact, status, remark, created_at, updated_at)
VALUES (@sid_3, '周八', 1, '学校心理健康教育中心', '2026-09-16', '下午 15:00-15:50', '考研压力大，长期失眠焦虑', '13800000004', 0, NULL, '2026-09-04 16:20:00', '2026-09-04 16:20:00');

-- ---------- 校园报告数据：情绪日记 emotion_diary（10 条） ----------
INSERT INTO emotion_diary (user_id, user_name, diary_date, mood_score, dominant_emotion, emotion_triggers, diary_content, sleep_quality, stress_level, created_at)
VALUES (@sid_wangwu, '王五', '2026-08-29', 8, '开心', '日常记录', '顺利完成社团招新，认识了很多新朋友', 1, 2, '2026-08-29 21:30:00');
INSERT INTO emotion_diary (user_id, user_name, diary_date, mood_score, dominant_emotion, emotion_triggers, diary_content, sleep_quality, stress_level, created_at)
VALUES (@sid_1, '赵六', '2026-08-30', 5, '平静', '日常记录', '按计划完成了一天学习，节奏稳定', 3, 2, '2026-08-30 21:30:00');
INSERT INTO emotion_diary (user_id, user_name, diary_date, mood_score, dominant_emotion, emotion_triggers, diary_content, sleep_quality, stress_level, created_at)
VALUES (@sid_2, '孙七', '2026-08-31', 6, '放松', '日常记录', '傍晚去操场跑了 3 公里，心情舒畅', 2, 3, '2026-08-31 21:30:00');
INSERT INTO emotion_diary (user_id, user_name, diary_date, mood_score, dominant_emotion, emotion_triggers, diary_content, sleep_quality, stress_level, created_at)
VALUES (@sid_3, '周八', '2026-09-01', 4, '疲惫', '日常记录', '连续复习到很晚，觉得有些透支', 4, 4, '2026-09-01 21:30:00');
INSERT INTO emotion_diary (user_id, user_name, diary_date, mood_score, dominant_emotion, emotion_triggers, diary_content, sleep_quality, stress_level, created_at)
VALUES (@sid_4, '吴九', '2026-09-02', 7, '平静', '日常记录', '和家里通了电话，被家人鼓励了', 2, 2, '2026-09-02 21:30:00');
INSERT INTO emotion_diary (user_id, user_name, diary_date, mood_score, dominant_emotion, emotion_triggers, diary_content, sleep_quality, stress_level, created_at)
VALUES (@sid_wangwu, '王五', '2026-09-02', 3, '焦虑', '日常记录', '收到课程任务通知，担心时间不够', 4, 5, '2026-09-02 21:30:00');
INSERT INTO emotion_diary (user_id, user_name, diary_date, mood_score, dominant_emotion, emotion_triggers, diary_content, sleep_quality, stress_level, created_at)
VALUES (@sid_1, '赵六', '2026-09-03', 8, '开心', '日常记录', '课堂展示得到老师肯定，很有成就感', 1, 2, '2026-09-03 21:30:00');
INSERT INTO emotion_diary (user_id, user_name, diary_date, mood_score, dominant_emotion, emotion_triggers, diary_content, sleep_quality, stress_level, created_at)
VALUES (@sid_2, '孙七', '2026-09-03', 2, '低落', '日常记录', '和舍友发生小摩擦，心情受到影响', 5, 5, '2026-09-03 21:30:00');
INSERT INTO emotion_diary (user_id, user_name, diary_date, mood_score, dominant_emotion, emotion_triggers, diary_content, sleep_quality, stress_level, created_at)
VALUES (@sid_3, '周八', '2026-09-04', 6, '紧张', '日常记录', '模拟面试前有些紧张，但顺利完成了', 3, 3, '2026-09-04 21:30:00');
INSERT INTO emotion_diary (user_id, user_name, diary_date, mood_score, dominant_emotion, emotion_triggers, diary_content, sleep_quality, stress_level, created_at)
VALUES (@sid_4, '吴九', '2026-09-04', 9, '开心', '日常记录', '与老同学聚餐，聊得很开心', 1, 1, '2026-09-04 21:30:00');

