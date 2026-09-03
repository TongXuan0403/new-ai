<template>
  <div>
    <div class="welcome-row">
      <div>
        <p class="date-label">{{ todayLabel }}</p>
        <h2>留一点空间，给今天的自己。</h2>
      </div>
      <button class="quiet-button" type="button" @click="$router.push({ name: 'privacy' })">查看我的数据 <span>→</span></button>
    </div>

    <div class="home-grid">
      <article class="state-panel">
        <div class="state-copy">
          <div class="section-heading">
            <div>
              <span class="section-kicker">今日状态</span>
              <h2>{{ todaysDiary ? `总体状态 ${todaysDiary.score} / 10` : '还没有今天的记录' }}</h2>
            </div>
            <span class="status-chip" :class="{ success: todaysDiary }">{{ todaysDiary ? todaysDiary.emotionStatus : '待记录' }}</span>
          </div>
          <p>{{ todaysDiary ? '今天的记录已经留下。回看变化，但不急着给它下结论。' : '记录一个数字，不是给自己下结论，只是留下当下的线索。' }}</p>
          <button class="outline-button" type="button" @click="$router.push({ name: 'diary' })">记录今天的情绪 <span>→</span></button>
        </div>
        <div class="state-visual" aria-label="总体状态">
          <div class="state-ring"><span>{{ todaysDiary ? todaysDiary.score : '—' }}</span><small>/ 10</small></div>
          <span>总体状态</span>
        </div>
      </article>
      <div class="quick-actions">
        <button class="action-tile action-tile-primary" type="button" @click="$router.push({ name: 'chat' })">
          <span class="tile-icon">◌</span><span class="tile-label">开始倾诉</span><strong>把一件事慢慢说清楚</strong><span class="tile-arrow">↗</span>
        </button>
        <button class="action-tile" type="button" @click="$router.push({ name: 'diary' })">
          <span class="tile-icon">▤</span><span class="tile-label">记录情绪</span><strong>花一分钟留下今天的线索</strong><span class="tile-arrow">↗</span>
        </button>
      </div>
    </div>

    <div class="content-grid">
      <section class="panel">
        <div class="section-heading">
          <div><span class="section-kicker">你的轨迹</span><h2>最近摘要</h2></div>
          <button class="text-button" type="button" @click="$router.push({ name: 'diary' })">查看日记 <span>→</span></button>
        </div>
        <div v-if="recentDiaries.length" class="summary-list">
          <div v-for="diary in recentDiaries" :key="diary.id" class="summary-item">
            <span class="tag" :class="diary.score >= 7 ? 'calm' : diary.score <= 5 ? 'warm' : 'focus'">{{ diary.emotionStatus }}</span>
            <div><strong>总体状态 {{ diary.score }} / 10</strong><p>{{ diary.event || '没有留下文字，只记录了当下状态。' }}</p></div>
            <time>{{ formatDate(diary.logDate) }}</time>
          </div>
        </div>
        <div v-else class="empty-state">完成一次日记后，这里会出现你的最近摘要。</div>
      </section>
      <section class="panel">
        <div class="section-heading">
          <div><span class="section-kicker">来自知识库</span><h2>给今天的一个建议</h2></div>
          <button class="text-button" type="button" @click="$router.push({ name: 'knowledge' })">全部文章 <span>→</span></button>
        </div>
        <div v-if="featuredArticle" class="featured-article">
          <div class="featured-thumb">{{ featuredArticle.categoryName }}</div>
          <div>
            <strong>{{ featuredArticle.title }}</strong>
            <p>{{ featuredArticle.minutes || 5 }} 分钟阅读 · 已审核</p>
            <button class="text-button" type="button" @click="$router.push({ name: 'knowledge' })">打开内容 <span>→</span></button>
          </div>
        </div>
        <div v-else class="empty-state">知识库暂无已发布内容。</div>
      </section>
    </div>

    <div class="safety-strip">
      <span class="safety-mark">✓</span>
      <span>所有回应都以你的安全为先。持续困扰或影响学习生活时，建议联系校内心理中心或专业人员。</span>
      <button class="text-button danger-link" type="button" @click="$router.push({ name: 'chat' })">开始倾诉 <span>→</span></button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { pageDiaries, listKnowledge } from '../api'

const diaries = ref([])
const featuredArticle = ref(null)
const todayLabel = ref('')

const todaysDiary = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  return diaries.value.find((d) => d.logDate === today) || null
})

const recentDiaries = computed(() => diaries.value.slice(0, 2))

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  if (Number.isNaN(d.getTime())) return String(dateStr).slice(5, 10)
  return new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit' }).format(d)
}

onMounted(async () => {
  todayLabel.value = new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric', month: 'long', day: 'numeric', weekday: 'long'
  }).format(new Date())
  try {
    const diaryPage = await pageDiaries({ page: 1, pageSize: 10 })
    diaries.value = diaryPage?.records || []
  } catch (e) {
    diaries.value = []
  }
  try {
    const knowledge = await listKnowledge({ page: 1, pageSize: 12 })
    featuredArticle.value = knowledge?.records?.[0] || null
  } catch (e) {
    featuredArticle.value = null
  }
})
</script>
