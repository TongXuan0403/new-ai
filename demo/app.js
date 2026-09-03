const pageTitles = {
  home: "今天从哪里开始？",
  chat: "倾诉对话",
  diary: "情绪日记",
  library: "知识库",
  admin: "管理端概览",
};

const articles = [
  {
    title: "考试周睡不着时，先降低入睡压力",
    category: "睡眠",
    meta: "5 分钟阅读 · 已审核",
    body:
      "当人越努力要求自己立刻睡着，大脑越容易保持警觉。可以先把目标从“必须睡着”调成“让身体休息十分钟”。",
  },
  {
    title: "焦虑升高时的 3 分钟落地呼吸",
    category: "自助练习",
    meta: "3 分钟练习 · 已审核",
    body:
      "从脚底触地感开始，依次注意你能看到、听到和触碰到的事物。这个练习的目标不是消灭焦虑，而是让身体稍微稳住。",
  },
  {
    title: "关系冲突后的边界表达",
    category: "关系",
    meta: "4 分钟阅读 · 待审核",
    body:
      "边界表达可以从事实、感受、需要和请求四步开始。先说清楚具体事件，再说明它对你的影响。",
  },
  {
    title: "把大任务拆成今天能做的一步",
    category: "压力",
    meta: "4 分钟阅读 · 已审核",
    body:
      "压力过大时，大脑会倾向于把任务看成一整块。把任务拆到 15 分钟内能完成的一步，往往更容易重新启动。",
  },
];

const trend = [
  { day: "周四", score: 5 },
  { day: "周五", score: 6 },
  { day: "周六", score: 6 },
  { day: "周日", score: 4 },
  { day: "周一", score: 6 },
  { day: "周二", score: 7 },
  { day: "今天", score: 7 },
];

const nav = document.querySelector("#nav");
const title = document.querySelector("#pageTitle");
const views = document.querySelectorAll(".view");
const navItems = document.querySelectorAll(".nav-item");
const agreeButton = document.querySelector("#agreeButton");
const consentNotice = document.querySelector("#consentNotice");
const chatForm = document.querySelector("#chatForm");
const chatInput = document.querySelector("#chatInput");
const messages = document.querySelector("#messages");
const feedbackBar = document.querySelector("#feedbackBar");
const promptChips = document.querySelector("#promptChips");
const showCrisisButton = document.querySelector("#showCrisisButton");
const crisisCard = document.querySelector("#crisisCard");
const scoreRange = document.querySelector("#scoreRange");
const scoreLabel = document.querySelector("#scoreLabel");
const diaryForm = document.querySelector("#diaryForm");
const saveNote = document.querySelector("#saveNote");
const barChart = document.querySelector("#barChart");
const articleList = document.querySelector("#articleList");
const articleDetail = document.querySelector("#articleDetail");
const newSessionButton = document.querySelector("#newSessionButton");

function switchView(viewName) {
  navItems.forEach((item) => {
    item.classList.toggle("active", item.dataset.view === viewName);
  });

  views.forEach((view) => {
    view.classList.toggle("active", view.id === `${viewName}View`);
  });

  title.textContent = pageTitles[viewName];
}

function addMessage(role, text) {
  const item = document.createElement("div");
  item.className = `message ${role}`;
  item.innerHTML = `<p>${text}</p>`;
  messages.append(item);
  messages.scrollTop = messages.scrollHeight;
}

function createReply(text) {
  const isRisk = /活不下去|不想活|自杀|伤害自己|结束/.test(text);
  if (isRisk) {
    crisisCard.classList.remove("hidden");
    return "听起来你现在承受得很重。此刻请先把安全放在第一位：如果你可能伤害自己或已经处于危险，请立即拨打 120、110、12356，或联系身边可信任的人陪你一起处理。";
  }

  return "我听到你正在努力把事情撑住，同时又有些焦虑。我们可以先不急着解决全部问题，只把当下最重的一部分拿出来看：这件事里，最让你担心的结果是什么？如果愿意，也可以先做一次慢呼吸，把肩膀放低一点。";
}

function renderChart() {
  barChart.innerHTML = "";
  trend.forEach((item) => {
    const bar = document.createElement("div");
    bar.className = "bar";
    bar.innerHTML = `
      <div class="bar-fill" style="height: ${item.score * 10}%"></div>
      <small>${item.day}</small>
    `;
    barChart.append(bar);
  });
}

function renderArticles(selectedIndex = 0) {
  articleList.innerHTML = "";
  articles.forEach((article, index) => {
    const button = document.createElement("button");
    button.type = "button";
    button.className = index === selectedIndex ? "active" : "";
    button.innerHTML = `
      <strong>${article.title}</strong>
      <span>${article.category} · ${article.meta}</span>
    `;
    button.addEventListener("click", () => {
      renderArticles(index);
      articleDetail.innerHTML = `
        <span class="tag focus">${article.meta.includes("待审核") ? "待审核" : "已审核"}</span>
        <h2>${article.title}</h2>
        <p>${article.body}</p>
        <p>如果困扰持续、加重或影响学习生活，建议联系校内心理中心、医生或合格的心理服务人员。</p>
      `;
    });
    articleList.append(button);
  });
}

nav.addEventListener("click", (event) => {
  const item = event.target.closest(".nav-item");
  if (item) switchView(item.dataset.view);
});

document.addEventListener("click", (event) => {
  const target = event.target.closest("[data-view-target]");
  if (target) switchView(target.dataset.viewTarget);

  const action = event.target.closest("[data-action]");
  if (!action) return;
  if (action.dataset.action === "start-chat") switchView("chat");
  if (action.dataset.action === "start-diary") switchView("diary");
});

agreeButton.addEventListener("click", () => {
  consentNotice.classList.add("hidden");
});

promptChips.addEventListener("click", (event) => {
  const chip = event.target.closest("button");
  if (!chip) return;
  chatInput.value = chip.textContent;
  chatInput.focus();
});

chatForm.addEventListener("submit", (event) => {
  event.preventDefault();
  const text = chatInput.value.trim();
  if (!text) return;

  addMessage("user", text);
  chatInput.value = "";
  feedbackBar.classList.add("hidden");

  setTimeout(() => {
    addMessage("ai", createReply(text));
    feedbackBar.classList.remove("hidden");
  }, 450);
});

showCrisisButton.addEventListener("click", () => {
  crisisCard.classList.toggle("hidden");
});

newSessionButton.addEventListener("click", () => {
  messages.innerHTML = "";
  addMessage("ai", "新的倾诉已经开始。你可以只写一个词，或者从今天最明显的感受开始。");
  feedbackBar.classList.add("hidden");
  crisisCard.classList.add("hidden");
});

scoreRange.addEventListener("input", () => {
  scoreLabel.textContent = scoreRange.value;
});

document.querySelectorAll(".emotion-options button").forEach((button) => {
  button.addEventListener("click", () => {
    document
      .querySelectorAll(".emotion-options button")
      .forEach((item) => item.classList.remove("selected"));
    button.classList.add("selected");
  });
});

diaryForm.addEventListener("submit", (event) => {
  event.preventDefault();
  trend[trend.length - 1].score = Number(scoreRange.value);
  renderChart();
  saveNote.classList.remove("hidden");
});

renderChart();
renderArticles();
