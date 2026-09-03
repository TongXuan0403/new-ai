<template>
  <div>
    <div class="page-intro">
      <div>
        <span class="section-kicker">自我观察</span>
        <h2>情绪日记</h2>
        <p>只记录你愿意留下的部分。必填项只有主情绪和总体状态。</p>
      </div>
      <div class="segmented-control">
        <button :class="{ active: trendDays === 7 }" type="button" @click="setTrendDays(7)">近 7 天</button>
        <button :class="{ active: trendDays === 30 }" type="button" @click="setTrendDays(30)">近 30 天</button>
      </div>
    </div>

    <div class="diary-grid">
      <form class="panel diary-form" @submit.prevent="saveDiary">
        <div class="section-heading">
          <div><span class="section-kicker">今天</span><h2>{{ editingId ? '编辑日记' : '新建日记' }}</h2></div>
          <button v-if="editingId" class="text-button" type="button" @click="cancelEdit">取消编辑</button>
        </div>
        <label class="field-label">
          <span>主情绪 <em>必填</em></span>
          <div class="emotion-options">
            <button v-for="emotion in emotions" :key="emotion" type="button"
                    :class="{ selected: form.emotionStatus === emotion }"
                    @click="form.emotionStatus = emotion">{{ emotion }}</button>
          </div>
        </label>
        <label class="field-label">
          <span>总体状态 <em>1 很差，10 很好</em><output>{{ form.score }}</output></span>
          <input v-model.number="form.score" type="range" min="1" max="10" step="1" />
          <span class="range-labels"><small>很差</small><small>很好</small></span>
        </label>
        <label class="field-label">
          <span>触发事件 <small>可选</small></span>
          <textarea v-model="form.event" maxlength="1000" placeholder="例如：明天要小组展示，担心自己说不好。"></textarea>
        </label>
        <div class="form-row">
          <label class="field-label">
            <span>睡眠 <small>可选</small></span>
            <select v-model="form.sleepStatus">
              <option>一般</option><option>较好</option><option>较差</option>
            </select>
          </label>
          <label class="field-label">
            <span>精力 <small>可选</small></span>
            <select v-model="form.energyStatus">
              <option>中等</option><option>充足</option><option>不足</option>
            </select>
          </label>
        </div>
        <button class="primary-button" type="submit" :disabled="saving">
          {{ saving ? '保存中…' : editingId ? '更新日记' : '保存日记' }} <span>↗</span>
        </button>
        <p v-if="error" class="form-error">{{ error }}</p>
      </form>

      <div class="panel trend-panel">
        <div class="section-heading">
          <div><span class="section-kicker">只描述变化</span><h2>状态趋势</h2></div>
          <span class="muted" style="color:var(--muted);font-size:12px">{{ trend.recordCount || 0 }} 条记录</span>
        </div>
        <div v-if="trendPoints.length >= 3">
          <div class="chart-scale"><span>10</span><span>5</span><span>1</span></div>
          <div class="bar-chart">
            <div v-for="(point, index) in trendPoints" :key="point.date" class="bar" :class="{ today: index === trendPoints.length - 1 }">
              <div class="bar-fill" :style="{ height: `${Math.max(point.score, 1) * 10}%` }" :title="`${point.date} · ${point.score}分`"></div>
              <small>{{ shortDate(point.date) }}</small>
            </div>
          </div>
        </div>
        <div v-else class="empty-trend">
          <span class="empty-icon">⌁</span>
          <strong>还需要几条记录</strong>
          <p>至少有 3 条记录时，这里会显示一段可供回看的变化。</p>
        </div>
        <div class="trend-note">
          <span class="note-icon">i</span>
          <p>趋势只反映你记录下来的状态，不代表诊断结论，单日波动也不说明什么。</p>
        </div>
      </div>
    </div>

    <section class="panel diary-history">
      <div class="section-heading">
        <div><span class="section-kicker">你的文字</span><h2>日记记录</h2></div>
        <span class="muted" style="color:var(--muted);font-size:12px">仅自己可见</span>
      </div>
      <div v-if="diaries.length" class="diary-list">
        <article v-for="diary in diaries" :key="diary.id" class="diary-item">
          <div class="diary-item-score">{{ diary.score }}</div>
          <div>
            <strong><span class="tag" :class="diary.score >= 7 ? 'calm' : diary.score <= 5 ? 'warm' : 'focus'">{{ diary.emotionStatus }}</span></strong>
            <p>{{ diary.event || '没有留下文字，只记录了当下状态。' }}</p>
          </div>
          <time>{{ fullDate(diary.logDate) }}</time>
          <div class="diary-item-actions">
            <button type="button" @click="startEdit(diary)">编辑</button>
            <button type="button" @click="removeDiary(diary.id)">删除</button>
          </div>
        </article>
      </div>
      <div v-else class="empty-state">还没有日记。你可以从一个情绪和一个数字开始。</div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { createDiary, pageDiaries, updateDiary, deleteDiary, getDiaryTrend } from '../api'
import { toast } from '../utils/toast'

const emotions = ['平静', '焦虑', '低落', '愤怒', '疲惫', '开心']
const diaries = ref([])
const editingId = ref(null)
const trendDays = ref(7)
const trend = ref({ recordCount: 0, points: [] })
const saving = ref(false)
const error = ref('')

const form = reactive({
  emotionStatus: '平静',
  score: 7,
  event: '',
  sleepStatus: '一般',
  energyStatus: '中等'
})

const trendPoints = computed(() => trend.value.points || [])

async function load() {
  await loadDiaries()
  await loadTrend()
}

async function loadDiaries() {
  try {
    const page = await pageDiaries({ page: 1, pageSize: 50 })
    diaries.value = page?.records || []
  } catch (e) {
    diaries.value = []
  }
}

async function loadTrend() {
  try {
    trend.value = await getDiaryTrend(trendDays.value)
  } catch (e) {
    trend.value = { recordCount: 0, points: [] }
  }
}

function setTrendDays(days) {
  trendDays.value = days
  loadTrend()
}

async function saveDiary() {
  error.value = ''
  if (!form.emotionStatus) {
    error.value = '请选择主情绪'
    return
  }
  saving.value = true
  const payload = {
    emotionStatus: form.emotionStatus,
    score: form.score,
    event: form.event?.trim(),
    sleepStatus: form.sleepStatus,
    energyStatus: form.energyStatus
  }
  try {
    if (editingId.value) {
      await updateDiary(editingId.value, payload)
      toast('日记已更新，趋势也同步了')
    } else {
      await createDiary(payload)
      toast('今天的记录已保存')
    }
    resetForm()
    await load()
  } catch (e) {
    error.value = e.message || '保存失败'
  } finally {
    saving.value = false
  }
}

function startEdit(diary) {
  editingId.value = diary.id
  form.emotionStatus = diary.emotionStatus
  form.score = diary.score
  form.event = diary.event || ''
  form.sleepStatus = diary.sleepStatus || '一般'
  form.energyStatus = diary.energyStatus || '中等'
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function cancelEdit() {
  resetForm()
}

function resetForm() {
  editingId.value = null
  form.emotionStatus = '平静'
  form.score = 7
  form.event = ''
  form.sleepStatus = '一般'
  form.energyStatus = '中等'
  error.value = ''
}

async function removeDiary(id) {
  try {
    await deleteDiary(id)
    toast('这条日记已删除')
    await load()
  } catch (e) {
    toast(e.message || '删除失败')
  }
}

function shortDate(value) {
  if (!value) return ''
  return String(value).slice(5).replace('-', '/')
}

function fullDate(value) {
  if (!value) return ''
  return String(value).slice(5).replace('-', '月') + '日'
}

onMounted(load)
</script>
