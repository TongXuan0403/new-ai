const STORAGE_KEY = "codex-ai-mvp-state-v1";

const pageTitles = {
  home: "今天从哪里开始？",
  chat: "倾诉对话",
  diary: "情绪日记",
  library: "知识库",
  privacy: "隐私与数据",
  admin: "运营与安全概览",
};

const defaultArticles = [
  {
    id: 1,
    title: "考试周睡不着时，先降低入睡压力",
    category: "睡眠",
    type: "文章",
    minutes: 5,
    status: "PUBLISHED",
    body: [
      "当人越努力要求自己立刻睡着，大脑越容易保持警觉。可以先把目标从“必须睡着”调成“让身体休息十分钟”。",
      "把手机放到够不着的位置，允许自己只是躺着休息。明天需要完成的事，可以先写在纸上，让大脑暂时不用反复提醒你。",
    ],
    tip: "如果失眠持续影响学习生活，建议联系校内心理中心或医生获得专业评估。",
  },
  {
    id: 2,
    title: "焦虑升高时的 3 分钟落地呼吸",
    category: "自助练习",
    type: "练习",
    minutes: 3,
    status: "PUBLISHED",
    body: [
      "从脚底触地感开始，依次注意你能看到、听到和触碰到的事物。这个练习的目标不是消灭焦虑，而是让身体稍微稳住。",
      "你可以用三轮呼吸完成练习：吸气时感受空气进入，呼气时让肩膀和下颌松一点。",
    ],
    tip: "如果练习让你感到不舒服，可以停下来，睁开眼睛，看看周围熟悉的物品，并联系可信任的人。",
  },
  {
    id: 3,
    title: "关系冲突后的边界表达",
    category: "关系",
    type: "文章",
    minutes: 4,
    status: "PENDING_REVIEW",
    body: [
      "边界表达可以从事实、感受、需要和请求四步开始。先说清楚具体事件，再说明它对你的影响。",
      "例如：“刚才临时改变安排时我有些慌乱，我需要提前知道变化。下次可以先和我说一声吗？”",
    ],
    tip: "表达边界不等于要求对方立刻同意，它首先是在为自己的感受和需要留出位置。",
  },
  {
    id: 4,
    title: "把大任务拆成今天能做的一步",
    category: "压力",
    type: "文章",
    minutes: 4,
    status: "PUBLISHED",
    body: [
      "压力过大时，大脑会倾向于把任务看成一整块。把任务拆到 15 分钟内能完成的一步，往往更容易重新启动。",
      "先写下下一步动作，而不是完整目标。例如从“准备汇报”改成“打开文档，写出三个小标题”。",
    ],
    tip: "完成一小步不是降低要求，而是给自己一个可以开始的位置。",
  },
];

const defaultSessions = [
  {
    id: "session-1",
    title: "新的倾诉",
    topic: "焦虑与压力",
    createdAt: "今天 10:21",
    messages: [
      {
        role: "ai",
        text: "你好，我在。你可以从最困扰你的那一小段开始说，也可以只说现在身体里最明显的感受。",
        time: "10:21",
      },
    ],
  },
  {
    id: "session-2",
    title: "汇报前的紧张",
    topic: "学业与考试",
    createdAt: "昨天 22:08",
    messages: [
      {
        role: "ai",
        text: "如果愿意，我们可以先把“担心说不好”拆成一个更具体的担心。",
        time: "22:08",
      },
    ],
  },
  {
    id: "session-3",
    title: "睡前反复想事",
    topic: "睡眠与精力",
    createdAt: "周二 23:40",
    messages: [
      {
        role: "ai",
        text: "睡前思绪很多时，可以先把明天要处理的事写下来，让它们暂时离开脑内循环。",
        time: "23:40",
      },
    ],
  },
];

const defaultDiaries = [
  { id: "diary-1", emotion: "平静", score: 7, event: "完成了一次睡前记录。小组汇报临近，肩颈有些紧绷。", sleep: "一般", energy: "中等", createdAt: "2026-09-02T21:18:00" },
  { id: "diary-2", emotion: "焦虑", score: 5, event: "课程任务堆在一起，担心来不及完成。", sleep: "较差", energy: "不足", createdAt: "2026-09-01T22:06:00" },
  { id: "diary-3", emotion: "疲惫", score: 6, event: "今天事情比较多，但完成了最重要的一件。", sleep: "一般", energy: "不足", createdAt: "2026-08-31T20:42:00" },
  { id: "diary-4", emotion: "平静", score: 6, event: "和朋友散步，情绪比上午稳定一些。", sleep: "较好", energy: "中等", createdAt: "2026-08-30T19:36:00" },
  { id: "diary-5", emotion: "低落", score: 4, event: "收到一条让人失望的消息，暂时不太想处理其他事。", sleep: "较差", energy: "不足", createdAt: "2026-08-29T22:13:00" },
];

const defaultRiskEvents = [
  { id: "risk-1", createdAt: "2026-09-03T09:42:00", level: 3, type: "SELF_HARM", summary: "表达强烈无助感，已展示危机卡", ruleVersion: "risk-v0.3", modelVersion: "classifier-v0.1", status: "待复核" },
  { id: "risk-2", createdAt: "2026-09-02T22:13:00", level: 2, type: "SLEEP_ANXIETY", summary: "睡眠与焦虑持续升高", ruleVersion: "risk-v0.3", modelVersion: "classifier-v0.1", status: "已关闭" },
];

const defaultState = {
  consentCompleted: false,
  consentRevoked: false,
  deletionRequested: false,
  sessions: defaultSessions,
  activeSessionId: "session-1",
  diaries: defaultDiaries,
  articles: defaultArticles,
  riskEvents: defaultRiskEvents,
  feedback: [],
};

let state = loadState();
let currentView = "home";
let selectedEmotion = "平静";
let editingDiaryId = null;
let trendDays = 7;
let selectedCategory = "全部";
let selectedArticleId = 1;
let isGenerating = false;
let generationTimer = null;
let toastTimer = null;

const $ = (selector) => document.querySelector(selector);
const nav = $("#nav");
const pageTitle = $("#pageTitle");
const navItems = document.querySelectorAll(".nav-item");
const views = document.querySelectorAll(".view");
const consentGate = $("#consentGate");
const agreeButton = $("#agreeButton");
const consentError = $("#consentError");
const toast = $("#toast");
const chatForm = $("#chatForm");
const chatInput = $("#chatInput");
const messages = $("#messages");
const feedbackBar = $("#feedbackBar");
const crisisCard = $("#crisisCard");
const promptChips = $("#promptChips");
const scoreRange = $("#scoreRange");
const scoreLabel = $("#scoreLabel");
const diaryForm = $("#diaryForm");
const saveNote = $("#saveNote");
const barChart = $("#barChart");
const emptyTrend = $("#emptyTrend");
const diaryList = $("#diaryList");
const articleList = $("#articleList");
const articleDetail = $("#articleDetail");
const articleSearch = $("#articleSearch");
const categoryTabs = $("#categoryTabs");
const sessionItems = $("#sessionItems");
const homeSummary = $("#homeSummary");
const homeArticle = $("#homeArticle");
const homeScore = $("#homeScore");
const homeStateTitle = $("#homeStateTitle");
const homeStateDescription = $("#homeStateDescription");
const homeMoodChip = $("#homeMoodChip");
const deletionStatus = $("#deletionStatus");

function clone(value) {
  return JSON.parse(JSON.stringify(value));
}

function loadState() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (!saved) return clone(defaultState);
    return { ...clone(defaultState), ...JSON.parse(saved) };
  } catch {
    return clone(defaultState);
  }
}

function persist() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

function escapeHtml(value = "") {
  return String(value).replace(/[&<>"']/g, (character) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#039;",
  }[character]));
}

function formatDate(dateString, includeTime = true) {
  const date = new Date(dateString);
  if (Number.isNaN(date.getTime())) return dateString;
  const dateText = new Intl.DateTimeFormat("zh-CN", { month: "2-digit", day: "2-digit" }).format(date);
  if (!includeTime) return dateText;
  return `${dateText} ${new Intl.DateTimeFormat("zh-CN", { hour: "2-digit", minute: "2-digit" }).format(date)}`;
}

function nowTime() {
  return new Intl.DateTimeFormat("zh-CN", { hour: "2-digit", minute: "2-digit" }).format(new Date());
}

function showToast(message) {
  toast.textContent = message;
  toast.classList.remove("hidden");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.add("hidden"), 2600);
}

function switchView(viewName) {
  if (!pageTitles[viewName]) return;
  currentView = viewName;
  navItems.forEach((item) => item.classList.toggle("active", item.dataset.view === viewName));
  views.forEach((view) => view.classList.toggle("active", view.id === `${viewName}View`));
  pageTitle.textContent = pageTitles[viewName];
  if (viewName === "home") renderHome();
  if (viewName === "chat") renderChat();
  if (viewName === "diary") renderDiary();
  if (viewName === "library") renderLibrary();
  if (viewName === "privacy") renderPrivacy();
  if (viewName === "admin") renderAdmin();
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function addMessageElement(role, text, time = nowTime(), temporary = false) {
  const item = document.createElement("div");
  item.className = `message ${role}${temporary ? " temporary-message" : ""}`;
  const paragraph = document.createElement("p");
  paragraph.textContent = text;
  item.append(paragraph);
  if (!temporary) {
    const meta = document.createElement("span");
    meta.className = "message-meta";
    meta.textContent = time;
    item.append(meta);
  }
  messages.append(item);
  messages.scrollTop = messages.scrollHeight;
  return item;
}

function renderMessages(session) {
  messages.innerHTML = "";
  session.messages.forEach((message) => addMessageElement(message.role, message.text, message.time));
  if (!session.messages.length) addMessageElement("ai", "新的倾诉已经开始。你可以只写一个词，或者从今天最明显的感受开始。");
}

function activeSession() {
  return state.sessions.find((session) => session.id === state.activeSessionId) || state.sessions[0];
}

function renderChat() {
  if (!state.sessions.length) {
    state.sessions = clone(defaultSessions.slice(0, 1));
    state.activeSessionId = state.sessions[0].id;
    persist();
  }
  sessionItems.innerHTML = "";
  state.sessions.forEach((session) => {
    const item = document.createElement("div");
    item.className = `session-item${session.id === state.activeSessionId ? " active" : ""}`;
    item.innerHTML = `<button class="session-select" data-session-id="${escapeHtml(session.id)}" type="button"><strong>${escapeHtml(session.title)}</strong><span>${escapeHtml(session.createdAt)} · ${escapeHtml(session.topic)}</span></button><button class="session-delete" data-delete-session="${escapeHtml(session.id)}" type="button" title="删除会话" aria-label="删除会话">×</button>`;
    sessionItems.append(item);
  });
  const session = activeSession();
  $("#chatSessionTitle").textContent = session.title;
  renderMessages(session);
  feedbackBar.classList.toggle("hidden", !session.lastAssistantMessageId || Boolean(session.feedback));
  crisisCard.classList.toggle("hidden", !session.riskActive);
  promptChips.classList.toggle("hidden", Boolean(session.messages.find((message) => message.role === "user")));
}

function getRiskLevel(text) {
  const normalized = text.replace(/\s/g, "");
  const crisisPattern = /自杀|杀了自己|结束生命|不想活了|活不下去|正在自伤|已经伤害自己|马上伤害|立即危险|伤害别人|杀人|想死/;
  const warningPattern = /自残|伤害自己|撑不住|崩溃|失控|严重失眠|无望|没有意义/;
  if (crisisPattern.test(normalized)) return { level: 3, type: /伤害别人|杀人/.test(normalized) ? "HARM_OTHERS" : "SELF_HARM", action: "SHOW_CRISIS_CARD" };
  if (warningPattern.test(normalized)) return { level: 2, type: "EMOTIONAL_DISTRESS", action: "SHOW_GUIDANCE" };
  return { level: 0, type: "NONE", action: "NONE" };
}

function createReply(text, risk) {
  if (risk.level >= 3) return "听起来你现在承受得非常重。请先把安全放在第一位：如果你可能伤害自己或他人，或已经处于立即危险，请立刻拨打 120、110 或 12356，并联系身边可信任的人陪你一起处理。";
  if (risk.level === 2) return "我听到你已经累到有些撑不住了。先不要求自己把所有事都解决，我们可以只看接下来的十分钟：找一个相对安全、有人在附近的地方，喝几口水，然后联系一位你信任的人。这样的困扰如果持续或加重，建议联系校内心理中心或专业人员。";
  if (/睡不着|失眠/.test(text)) return "睡不着的时候，越要求自己马上入睡，身体越容易保持警觉。今晚可以先把目标改成“让身体休息十分钟”，把明天要处理的事写下来，再做三轮不刻意用力的慢呼吸。";
  if (/焦虑|紧张|担心/.test(text)) return "我听到你正在担心一件还没有发生的事，身体也许已经先进入了警觉状态。可以先问自己：现在最担心的具体结果是什么？然后只选一个十五分钟内能做的小动作。";
  return "我听到你正在努力把事情撑住，同时又有些难受。我们可以先不急着解决全部问题，只把当下最重的一部分拿出来看：这件事里，最让你担心的结果是什么？如果愿意，也可以先让肩膀放低一点，做一次缓慢的呼气。";
}

function addRiskEvent(risk, text) {
  state.riskEvents.unshift({
    id: `risk-${Date.now()}`,
    createdAt: new Date().toISOString(),
    level: risk.level,
    type: risk.type,
    summary: risk.level >= 3 ? "高风险表达，已展示危机卡" : `出现${text.length > 34 ? `${text.slice(0, 34)}…` : text}`,
    ruleVersion: "risk-v0.3",
    modelVersion: "classifier-v0.1",
    status: "待复核",
  });
}

function streamReply(session, reply) {
  isGenerating = true;
  $("#sendButton").disabled = true;
  const typing = addMessageElement("ai", "", nowTime(), true);
  typing.innerHTML = '<span class="typing-dots"><i></i><i></i><i></i></span>';
  let cursor = 0;
  generationTimer = setInterval(() => {
    if (cursor === 0) typing.innerHTML = "";
    cursor += Math.max(1, Math.ceil(reply.length / 22));
    const paragraph = document.createElement("p");
    paragraph.textContent = reply.slice(0, cursor);
    typing.replaceChildren(paragraph);
    messages.scrollTop = messages.scrollHeight;
    if (cursor >= reply.length) {
      clearInterval(generationTimer);
      generationTimer = null;
      isGenerating = false;
      $("#sendButton").disabled = false;
      typing.classList.remove("temporary-message");
      const meta = document.createElement("span");
      meta.className = "message-meta";
      meta.textContent = nowTime();
      typing.append(meta);
      session.messages.push({ role: "ai", text: reply, time: nowTime() });
      session.lastAssistantMessageId = `message-${Date.now()}`;
      session.feedback = null;
      if (session.messages.filter((message) => message.role === "user").length >= 2) session.title = "正在梳理的一件事";
      persist();
      renderSessionsOnly();
      feedbackBar.classList.remove("hidden");
    }
  }, 55);
}

function renderSessionsOnly() {
  sessionItems.querySelectorAll(".session-item").forEach((item) => item.classList.toggle("active", item.dataset.sessionId === state.activeSessionId));
  $("#chatSessionTitle").textContent = activeSession().title;
}

function submitChat(text) {
  const session = activeSession();
  if (!session || isGenerating) return;
  const cleanText = text.trim();
  if (!cleanText) return;
  const risk = getRiskLevel(cleanText);
  const timestamp = nowTime();
  session.messages.push({ role: "user", text: cleanText, time: timestamp });
  session.feedback = null;
  session.riskActive = risk.level >= 3;
  if (risk.level >= 2) addRiskEvent(risk, cleanText);
  persist();
  addMessageElement("user", cleanText, timestamp);
  chatInput.value = "";
  $("#charCount").textContent = "0 / 2000";
  feedbackBar.classList.add("hidden");
  crisisCard.classList.toggle("hidden", risk.level < 3);
  promptChips.classList.add("hidden");
  streamReply(session, createReply(cleanText, risk));
}

function renderHome() {
  const today = new Date().toISOString().slice(0, 10);
  const todaysDiary = state.diaries.find((diary) => diary.createdAt.slice(0, 10) === today);
  $("#todayLabel").textContent = new Intl.DateTimeFormat("zh-CN", { year: "numeric", month: "long", day: "numeric", weekday: "long" }).format(new Date());
  if (todaysDiary) {
    homeScore.textContent = todaysDiary.score;
    homeStateTitle.textContent = `总体状态 ${todaysDiary.score} / 10`;
    homeStateDescription.textContent = "今天的记录已经留下。回看变化，但不急着给它下结论。";
    homeMoodChip.textContent = todaysDiary.emotion;
    homeMoodChip.className = "status-chip success";
  } else {
    homeScore.textContent = "—";
    homeStateTitle.textContent = "还没有今天的记录";
    homeStateDescription.textContent = "记录一个数字，不是给自己下结论，只是留下当下的线索。";
    homeMoodChip.textContent = "待记录";
    homeMoodChip.className = "status-chip";
  }
  const recent = state.diaries.slice(0, 2);
  homeSummary.innerHTML = recent.length ? recent.map((diary) => `<div class="summary-item"><span class="tag ${diary.score >= 7 ? "calm" : diary.score <= 5 ? "warm" : "focus"}">${escapeHtml(diary.emotion)}</span><div><strong>总体状态 ${diary.score} / 10</strong><p>${escapeHtml(diary.event || "没有留下文字，只记录了当下状态。")}</p></div><time>${formatDate(diary.createdAt, false)}</time></div>`).join("") : '<div class="empty-state">完成一次日记后，这里会出现你的最近摘要。</div>';
  const article = state.articles.find((item) => item.status === "PUBLISHED") || defaultArticles[0];
  homeArticle.innerHTML = `<div class="featured-thumb">${escapeHtml(article.category)}</div><div><strong>${escapeHtml(article.title)}</strong><p>${article.minutes} 分钟${article.type === "练习" ? "练习" : "阅读"} · 已审核</p><button class="text-button" data-open-article="${article.id}" type="button">打开内容 <span>→</span></button></div>`;
}

function resetDiaryForm() {
  editingDiaryId = null;
  $("#diaryFormTitle").textContent = "新建日记";
  $("#diarySubmitText").textContent = "保存日记";
  $("#cancelEditButton").classList.add("hidden");
  selectedEmotion = "平静";
  document.querySelectorAll("#emotionOptions button").forEach((button) => button.classList.toggle("selected", button.dataset.emotion === selectedEmotion));
  scoreRange.value = 7;
  scoreLabel.textContent = "7";
  $("#diaryEvent").value = "";
  $("#diarySleep").value = "一般";
  $("#diaryEnergy").value = "中等";
}

function renderDiary() {
  const sorted = [...state.diaries].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
  diaryList.innerHTML = sorted.length ? sorted.map((diary) => `<article class="diary-item"><div class="diary-item-score">${diary.score}</div><div><strong><span class="tag ${diary.score >= 7 ? "calm" : diary.score <= 5 ? "warm" : "focus"}">${escapeHtml(diary.emotion)}</span></strong><p>${escapeHtml(diary.event || "没有留下文字，只记录了当下状态。")}</p></div><time>${formatDate(diary.createdAt)}</time><div class="diary-item-actions"><button data-edit-diary="${diary.id}" type="button">编辑</button><button data-delete-diary="${diary.id}" type="button">删除</button></div></article>`).join("") : '<div class="empty-state">还没有日记。你可以从一个情绪和一个数字开始。</div>';
  renderTrend();
}

function renderTrend() {
  const cutoff = new Date();
  cutoff.setDate(cutoff.getDate() - trendDays + 1);
  const recent = state.diaries.filter((diary) => new Date(diary.createdAt) >= cutoff).sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
  $("#trendCount").textContent = `${recent.length} 条记录`;
  const hasEnough = recent.length >= 3;
  barChart.classList.toggle("hidden", !hasEnough);
  emptyTrend.classList.toggle("hidden", hasEnough);
  if (!hasEnough) return;
  const bars = recent.slice(-7);
  barChart.innerHTML = bars.map((diary, index) => `<div class="bar ${index === bars.length - 1 ? "today" : ""}"><div class="bar-fill" style="height:${diary.score * 10}%"></div><small>${formatDate(diary.createdAt, false)}</small></div>`).join("");
}

function renderLibrary() {
  const query = articleSearch.value.trim().toLowerCase();
  const published = state.articles.filter((article) => article.status === "PUBLISHED");
  const filtered = published.filter((article) => {
    const matchesCategory = selectedCategory === "全部" || article.category === selectedCategory;
    const matchesQuery = !query || `${article.title}${article.body.join("")}`.toLowerCase().includes(query);
    return matchesCategory && matchesQuery;
  });
  $("#articleCount").textContent = `${filtered.length} 篇已发布`;
  articleList.innerHTML = filtered.map((article) => `<button class="article-card ${article.id === selectedArticleId ? "active" : ""}" data-select-article="${article.id}" type="button"><span class="article-card-thumb">${escapeHtml(article.category)}</span><span><strong>${escapeHtml(article.title)}</strong><small>${article.category} · ${article.minutes} 分钟${article.type === "练习" ? "练习" : "阅读"} · 已审核</small></span><span class="status-chip success">已发布</span></button>`).join("");
  $("#articleEmpty").classList.toggle("hidden", filtered.length > 0);
  const selected = state.articles.find((article) => article.id === selectedArticleId && article.status === "PUBLISHED") || filtered[0] || published[0];
  if (selected) {
    selectedArticleId = selected.id;
    articleDetail.innerHTML = `<span class="tag focus">${selected.type === "练习" ? "轻量练习" : "已审核"}</span><h2>${escapeHtml(selected.title)}</h2>${selected.body.map((paragraph) => `<p>${escapeHtml(paragraph)}</p>`).join("")}<div class="article-divider"></div><div class="article-tip">${escapeHtml(selected.tip)}</div>`;
  } else {
    articleDetail.innerHTML = '<div class="empty-state">选择一篇文章开始阅读。</div>';
  }
  categoryTabs.querySelectorAll("button").forEach((button) => button.classList.toggle("active", button.dataset.category === selectedCategory));
}

function renderPrivacy() {
  deletionStatus.classList.toggle("hidden", !state.deletionRequested);
  if (state.deletionRequested) deletionStatus.textContent = "删除申请已提交，当前状态：待处理。正式系统会保留必要的审计记录，并在处理完成后通知你。";
}

function riskLevelLabel(level) {
  return level === 3 ? "危机" : level === 2 ? "预警" : "关注";
}

function renderAdmin() {
  const pendingRisk = state.riskEvents.filter((event) => event.status === "待复核").length;
  $("#metricRow").innerHTML = `<div class="metric-card"><span>活跃用户</span><strong>1,284</strong><p>较上周 +8.4%</p></div><div class="metric-card"><span>完成日记</span><strong>${(state.diaries.length + 3687).toLocaleString("zh-CN")}</strong><p>近 30 天</p></div><div class="metric-card"><span>帮助度正向</span><strong>${state.feedback.length ? "78%" : "76%"}</strong><p>对话后反馈</p></div><div class="metric-card warning"><span>风险待处理</span><strong>${pendingRisk}</strong><p>仅展示脱敏摘要</p></div>`;
  $("#riskTableBody").innerHTML = state.riskEvents.length ? state.riskEvents.slice(0, 8).map((event) => `<tr><td>${formatDate(event.createdAt)}</td><td><span class="risk ${event.level === 3 ? "high" : event.level === 2 ? "mid" : "low"}">${riskLevelLabel(event.level)}</span></td><td>${escapeHtml(event.summary)}</td><td>${escapeHtml(event.ruleVersion)}</td><td>${escapeHtml(event.status)}</td></tr>`).join("") : "<tr><td colspan=\"5\">暂无风险事件</td></tr>";
  $("#adminArticleList").innerHTML = state.articles.map((article) => `<div class="admin-article"><div><strong>${escapeHtml(article.title)}</strong><small>${escapeHtml(article.category)} · ${article.status === "PUBLISHED" ? "已发布" : "待审核"}</small></div><button class="status-button" data-toggle-article="${article.id}" type="button">${article.status === "PUBLISHED" ? "下线" : "发布"}</button></div>`).join("");
}

function ensureConsent() {
  if (state.consentCompleted && !state.consentRevoked) return true;
  consentGate.scrollIntoView({ behavior: "smooth", block: "center" });
  showToast("完成首次使用确认后，才能继续。");
  return false;
}

function exportData() {
  const payload = {
    exportedAt: new Date().toISOString(),
    user: { displayName: "林同学", userType: 1 },
    consent: { ageConfirmed: state.consentCompleted, privacyPolicyVersion: "privacy-v1.0", sensitiveInfoVersion: "sensitive-v1.0", productBoundaryVersion: "boundary-v1.0" },
    sessions: state.sessions,
    diaries: state.diaries,
    deletionRequest: state.deletionRequested,
  };
  const url = URL.createObjectURL(new Blob([JSON.stringify(payload, null, 2)], { type: "application/json" }));
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = `codex-ai-data-${new Date().toISOString().slice(0, 10)}.json`;
  anchor.click();
  URL.revokeObjectURL(url);
  showToast("数据导出已开始下载。");
}

nav.addEventListener("click", (event) => {
  const item = event.target.closest(".nav-item");
  if (item) switchView(item.dataset.view);
});

document.addEventListener("click", (event) => {
  const target = event.target.closest("[data-view-target]");
  if (target) switchView(target.dataset.viewTarget);
  const action = event.target.closest("[data-action]");
  if (action) {
    if (action.dataset.action === "start-chat" && ensureConsent()) switchView("chat");
    if (action.dataset.action === "start-diary" && ensureConsent()) switchView("diary");
    if (action.dataset.action === "show-crisis") crisisCard.classList.remove("hidden");
  }
  const sessionButton = event.target.closest("[data-session-id]");
  if (sessionButton && !event.target.closest("[data-delete-session]")) {
    state.activeSessionId = sessionButton.dataset.sessionId;
    persist();
    renderChat();
  }
  const deleteSessionButton = event.target.closest("[data-delete-session]");
  if (deleteSessionButton) {
    event.stopPropagation();
    const sessionId = deleteSessionButton.dataset.deleteSession;
    state.sessions = state.sessions.filter((session) => session.id !== sessionId);
    if (state.activeSessionId === sessionId) state.activeSessionId = state.sessions[0]?.id || null;
    persist();
    renderChat();
    showToast("会话已从本地演示记录中删除。");
  }
  const openArticle = event.target.closest("[data-open-article]");
  if (openArticle) {
    selectedArticleId = Number(openArticle.dataset.openArticle);
    switchView("library");
  }
  const selectArticle = event.target.closest("[data-select-article]");
  if (selectArticle) {
    selectedArticleId = Number(selectArticle.dataset.selectArticle);
    renderLibrary();
  }
  const editDiary = event.target.closest("[data-edit-diary]");
  if (editDiary) {
    const diary = state.diaries.find((item) => item.id === editDiary.dataset.editDiary);
    if (!diary) return;
    editingDiaryId = diary.id;
    selectedEmotion = diary.emotion;
    $("#diaryFormTitle").textContent = "编辑日记";
    $("#diarySubmitText").textContent = "更新日记";
    $("#cancelEditButton").classList.remove("hidden");
    document.querySelectorAll("#emotionOptions button").forEach((button) => button.classList.toggle("selected", button.dataset.emotion === diary.emotion));
    scoreRange.value = diary.score;
    scoreLabel.textContent = diary.score;
    $("#diaryEvent").value = diary.event || "";
    $("#diarySleep").value = diary.sleep || "一般";
    $("#diaryEnergy").value = diary.energy || "中等";
    switchView("diary");
    $("#diaryForm").scrollIntoView({ behavior: "smooth", block: "start" });
  }
  const deleteDiary = event.target.closest("[data-delete-diary]");
  if (deleteDiary) {
    state.diaries = state.diaries.filter((diary) => diary.id !== deleteDiary.dataset.deleteDiary);
    persist();
    renderDiary();
    renderHome();
    showToast("这条日记已删除。");
  }
  const feedbackButton = event.target.closest("[data-feedback]");
  if (feedbackButton) {
    const session = activeSession();
    session.feedback = Number(feedbackButton.dataset.feedback);
    state.feedback.push({ sessionId: session.id, helpfulness: session.feedback, createdAt: new Date().toISOString() });
    persist();
    feedbackBar.classList.add("hidden");
    showToast("感谢你的反馈，它会帮助改进回应质量。");
  }
  const toggleArticle = event.target.closest("[data-toggle-article]");
  if (toggleArticle) {
    const article = state.articles.find((item) => item.id === Number(toggleArticle.dataset.toggleArticle));
    if (!article) return;
    article.status = article.status === "PUBLISHED" ? "OFFLINE" : "PUBLISHED";
    persist();
    renderAdmin();
    renderLibrary();
    showToast(article.status === "PUBLISHED" ? "文章已发布到知识库。" : "文章已下线，学生端不可见。");
  }
});

agreeButton.addEventListener("click", () => {
  const allChecked = ["ageCheck", "privacyCheck", "boundaryCheck"].every((id) => $(`#${id}`).checked);
  if (!allChecked) {
    consentError.classList.remove("hidden");
    return;
  }
  state.consentCompleted = true;
  state.consentRevoked = false;
  persist();
  consentGate.classList.add("hidden");
  showToast("已完成首次使用确认，欢迎你。");
});

chatForm.addEventListener("submit", (event) => {
  event.preventDefault();
  if (ensureConsent()) submitChat(chatInput.value);
});

chatInput.addEventListener("input", () => {
  $("#charCount").textContent = `${chatInput.value.length} / 2000`;
  chatInput.style.height = "auto";
  chatInput.style.height = `${Math.min(chatInput.scrollHeight, 130)}px`;
});

promptChips.addEventListener("click", (event) => {
  const chip = event.target.closest("button");
  if (!chip) return;
  chatInput.value = chip.textContent;
  chatInput.dispatchEvent(new Event("input"));
  chatInput.focus();
});

$("#newSessionButton").addEventListener("click", () => {
  if (!ensureConsent()) return;
  const session = { id: `session-${Date.now()}`, title: "新的倾诉", topic: "未分类", createdAt: "刚刚", messages: [{ role: "ai", text: "新的倾诉已经开始。你可以只写一个词，或者从今天最明显的感受开始。", time: nowTime() }] };
  state.sessions.unshift(session);
  state.activeSessionId = session.id;
  persist();
  renderChat();
  showToast("新的倾诉已开始。");
});

$("#copyResourceButton").addEventListener("click", async () => {
  const resourceText = "如果你正处于立即危险，请拨打 120、110 或 12356，并联系身边可信任的人陪同处理。";
  try {
    await navigator.clipboard.writeText(resourceText);
    showToast("求助信息已复制。");
  } catch {
    showToast("复制失败，请直接拨打 120、110 或 12356。");
  }
});

document.querySelectorAll("#emotionOptions button").forEach((button) => {
  button.addEventListener("click", () => {
    selectedEmotion = button.dataset.emotion;
    document.querySelectorAll("#emotionOptions button").forEach((item) => item.classList.toggle("selected", item === button));
  });
});

scoreRange.addEventListener("input", () => {
  scoreLabel.textContent = scoreRange.value;
});

diaryForm.addEventListener("submit", (event) => {
  event.preventDefault();
  if (!ensureConsent()) return;
  const entry = {
    id: editingDiaryId || `diary-${Date.now()}`,
    emotion: selectedEmotion,
    score: Number(scoreRange.value),
    event: $("#diaryEvent").value.trim(),
    sleep: $("#diarySleep").value,
    energy: $("#diaryEnergy").value,
    createdAt: editingDiaryId ? state.diaries.find((diary) => diary.id === editingDiaryId)?.createdAt || new Date().toISOString() : new Date().toISOString(),
  };
  if (editingDiaryId) {
    state.diaries = state.diaries.map((diary) => diary.id === editingDiaryId ? entry : diary);
    saveNote.textContent = "日记已更新，趋势也同步了。";
  } else {
    state.diaries.unshift(entry);
    saveNote.textContent = "日记已保存，趋势也同步了。";
  }
  const wasEditing = Boolean(editingDiaryId);
  persist();
  saveNote.classList.remove("hidden");
  resetDiaryForm();
  renderDiary();
  renderHome();
  showToast(wasEditing ? "日记已更新。" : "今天的记录已保存。");
});

$("#cancelEditButton").addEventListener("click", () => {
  resetDiaryForm();
  saveNote.classList.add("hidden");
});

$("#trendToggle").addEventListener("click", (event) => {
  const button = event.target.closest("button");
  if (!button) return;
  trendDays = Number(button.dataset.days);
  $("#trendToggle").querySelectorAll("button").forEach((item) => item.classList.toggle("active", item === button));
  renderTrend();
});

articleSearch.addEventListener("input", renderLibrary);

categoryTabs.addEventListener("click", (event) => {
  const button = event.target.closest("button");
  if (!button) return;
  selectedCategory = button.dataset.category;
  renderLibrary();
});

$("#exportDataButton").addEventListener("click", exportData);

$("#deleteDataButton").addEventListener("click", () => {
  if (state.deletionRequested) {
    showToast("删除申请正在处理中。");
    return;
  }
  state.deletionRequested = true;
  persist();
  renderPrivacy();
  showToast("删除申请已提交，状态可在这里查看。");
});

$("#revokeConsentButton").addEventListener("click", () => {
  state.consentRevoked = true;
  persist();
  consentGate.classList.remove("hidden");
  consentError.classList.add("hidden");
  ["ageCheck", "privacyCheck", "boundaryCheck"].forEach((id) => { $(`#${id}`).checked = false; });
  showToast("非必要授权已撤回，重新使用前需要再次确认。");
});

$("#newArticleButton").addEventListener("click", () => {
  showToast("文章创建接口预留中，本地演示暂不创建虚构内容。");
});

window.addEventListener("beforeunload", () => {
  if (generationTimer) clearInterval(generationTimer);
});

function initialize() {
  consentGate.classList.toggle("hidden", state.consentCompleted && !state.consentRevoked);
  renderHome();
  renderChat();
  renderDiary();
  renderLibrary();
  renderPrivacy();
  renderAdmin();
}

initialize();
