<template>
  <div>
    <div class="page-intro">
      <div>
        <span class="section-kicker">当下可做的轻量练习</span>
        <h2>自助练习</h2>
        <p>每个练习 3–5 分钟，目标不是立刻变好，而是让此刻稍微稳住。</p>
      </div>
      <label class="search-box">
        <span>⌕</span>
        <input v-model="keyword" type="search" placeholder="搜索练习" @input="debouncedSearch" />
      </label>
    </div>

    <div class="library-layout">
      <section class="panel article-list-panel">
        <div class="section-heading">
          <h2>练习列表</h2>
          <span class="muted" style="color:var(--muted);font-size:12px">{{ exercises.length }} 个已发布</span>
        </div>
        <div v-if="exercises.length" class="article-list">
          <button v-for="exercise in exercises" :key="exercise.id" type="button"
                  class="article-card" :class="{ active: selected?.id === exercise.id }"
                  @click="selectExercise(exercise)">
            <span class="article-card-thumb">{{ exercise.categoryName || '练习' }}</span>
            <span>
              <strong>{{ exercise.title }}<span v-if="exercise.completed" class="done-badge">已完成</span></strong>
              <small>{{ exercise.minutes }} 分钟 · {{ exercise.summary }}</small>
              <small v-if="exercise.tags" class="article-tags">{{ exercise.tags.split(',').map((t) => '#' + t.trim()).join(' ') }}</small>
            </span>
          </button>
        </div>
        <div v-else class="empty-state">没有找到符合条件的练习。</div>
      </section>

      <article class="panel article-detail">
        <template v-if="selected">
          <span class="tag focus">{{ selected.completed ? '已完成' : '未完成' }}</span>
          <h2>{{ selected.title }}</h2>
          <p v-if="selected.summary" class="detail-summary">{{ selected.summary }}</p>
          <div class="exercise-steps">
            <p v-for="(line, index) in renderLines(selected.content)" :key="index">{{ line }}</p>
          </div>
          <div v-if="selected.tags" class="article-tagline">
            <span v-for="tag in selected.tags.split(',')" :key="tag" class="tag-chip static"># {{ tag.trim() }}</span>
          </div>
          <div class="article-divider"></div>
          <div class="complete-area">
            <button v-if="!selected.completed" class="primary-btn" type="button" @click="doComplete(selected)">
              标记完成
            </button>
            <div v-else class="completed-line">
              <span>✓ 已在本日完成</span>
              <span v-if="selected.completedAt" class="muted" style="color:var(--muted);font-size:12px">完成于 {{ formatTime(selected.completedAt) }}</span>
            </div>
            <p class="complete-hint">完成练习不会替代专业帮助；如果困扰持续或加重，请联系校内心理中心或专业人员。</p>
          </div>
        </template>
        <div v-else class="empty-state">选择一个练习开始。</div>
      </article>
    </div>

    <section v-if="completions.length" class="panel completion-panel">
      <div class="section-heading">
        <h2>我的完成记录</h2>
        <span class="muted" style="color:var(--muted);font-size:12px">共 {{ completions.length }} 次</span>
      </div>
      <div class="completion-list">
        <div v-for="c in completions" :key="c.id" class="completion-card">
          <div>
            <strong>{{ c.exerciseTitle }}</strong>
            <small>{{ c.minutes }} 分钟 · {{ c.moodAfter ? '练习后感受：' + c.moodAfter : '未填写感受' }}</small>
          </div>
          <span class="muted" style="color:var(--muted);font-size:12px">{{ formatTime(c.completedAt) }}</span>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { listExercises, getExercise, completeExercise, myExerciseCompletions } from '../api'
import { toast } from '../utils/toast'

const keyword = ref('')
const exercises = ref([])
const selected = ref(null)
const completions = ref([])
let searchTimer = null

async function load() {
  try {
    const result = await listExercises({
      keyword: keyword.value || undefined,
      page: 1,
      pageSize: 50
    })
    exercises.value = result?.records || []
    if (!selected.value || !exercises.value.some((e) => e.id === selected.value.id)) {
      selected.value = exercises.value[0] || null
    }
  } catch (e) {
    exercises.value = []
  }
}

async function loadCompletions() {
  try {
    completions.value = await myExerciseCompletions()
  } catch (e) {
    completions.value = []
  }
}

function selectExercise(exercise) {
  selected.value = exercise
}

async function doComplete(exercise) {
  try {
    await completeExercise(exercise.id, { moodAfter: '' })
    exercise.completed = true
    exercise.completedAt = new Date().toISOString()
    toast('练习已完成，已记录')
    loadCompletions()
  } catch (e) {
    toast('标记失败，请稍后再试')
  }
}

function debouncedSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(load, 350)
}

function renderLines(content) {
  if (!content) return []
  return String(content).split('\n').map((line) => line.trim()).filter(Boolean)
}

function formatTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').substring(0, 16)
}

onMounted(() => {
  load()
  loadCompletions()
})
</script>

<style scoped>
.done-badge {
  display: inline-block;
  margin-left: 6px;
  padding: 1px 8px;
  border-radius: 999px;
  background: #ecfdf5;
  color: #059669;
  font-size: 11px;
  font-weight: 600;
  vertical-align: middle;
}
.detail-summary {
  color: var(--muted, #6b7280);
}
.exercise-steps p {
  margin: 8px 0;
  line-height: 1.7;
}
.complete-area {
  margin-top: 8px;
}
.completed-line {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #059669;
  font-weight: 600;
}
.complete-hint {
  margin-top: 10px;
  color: var(--muted, #6b7280);
  font-size: 12px;
}
.completion-panel {
  margin-top: 20px;
}
.completion-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.completion-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid var(--border, #e5e7eb);
  border-radius: 12px;
  background: #fff;
}
.completion-card small {
  display: block;
  color: var(--muted, #6b7280);
  margin-top: 2px;
}
</style>
