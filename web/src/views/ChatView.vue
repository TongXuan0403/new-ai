<template>
  <div class="chat-layout">
    <aside class="session-list">
      <div class="section-heading compact">
        <div><span class="section-kicker">你的记录</span><h2>会话</h2></div>
        <button class="icon-button" type="button" title="新建会话" aria-label="新建会话" @click="newSession">＋</button>
      </div>
      <div class="session-items">
        <div v-for="session in sessions" :key="session.id" class="session-item" :class="{ active: session.id === activeSessionId }">
          <button class="session-select" style="width:100%;text-align:left;background:none;border:0;padding:0" type="button" @click="selectSession(session.id)">
            <strong>{{ session.sessionTitle }}</strong>
            <span>{{ formatSessionTime(session.startedAt) }} · {{ session.id }}</span>
          </button>
          <button class="session-delete" type="button" title="删除会话" @click.stop="removeSession(session.id)">×</button>
        </div>
        <div v-if="!sessions.length" class="empty-state" style="padding:20px 12px">还没有会话，点“＋”开始。</div>
      </div>
    </aside>

    <div class="chat-panel">
      <div class="chat-header">
        <div>
          <span class="section-kicker">AI 心理支持</span>
          <strong>{{ activeSession?.sessionTitle || '新的倾诉' }}</strong>
        </div>
        <button class="text-button danger-link" type="button" @click="openCrisis">需要紧急帮助</button>
      </div>

      <div v-if="crisisShown" class="crisis-card">
        <div class="crisis-leading">
          <span class="crisis-icon">!</span>
          <div>
            <strong>如果你正处于立即危险，请先联系现实支持</strong>
            <p>请确认自己暂时处于安全的地方，并让身边可信任的人陪着你。这里不会承诺已通知他人。</p>
          </div>
        </div>
        <div class="crisis-actions">
          <a class="crisis-call" href="tel:120">拨打 120</a>
          <a class="crisis-call" href="tel:110">拨打 110</a>
          <a class="crisis-call" href="tel:12356">拨打 12356</a>
          <button class="crisis-copy" type="button" @click="copyCrisis">复制求助信息</button>
        </div>
      </div>

      <div v-if="!hasUserMessage" class="prompt-chips">
        <button type="button" @click="usePrompt('我现在很焦虑')">我现在很焦虑</button>
        <button type="button" @click="usePrompt('我想把一件事说清楚')">我想把一件事说清楚</button>
        <button type="button" @click="usePrompt('我睡不着')">我睡不着</button>
      </div>

      <div ref="messagesEl" class="messages">
        <div v-for="message in activeMessages" :key="message.key" class="message" :class="message.senderType === 1 ? 'user' : 'ai'">
          <p>{{ message.content }}</p>
          <span v-if="message.createdAt" class="message-meta">{{ formatTime(message.createdAt) }}</span>
        </div>
        <div v-if="typing" class="message ai">
          <p><span class="typing-dots"><i></i><i></i><i></i></span></p>
        </div>
      </div>

      <div class="composer-wrap">
        <form class="composer" @submit.prevent="send">
          <textarea v-model="draft" rows="1" maxlength="2000" placeholder="输入你想说的话，2000 字以内" autocomplete="off" @keydown.enter.exact.prevent="send" @input="autoGrow"></textarea>
          <button class="primary-button send-button" type="submit" :disabled="sending || !draft.trim()">发送 <span>↗</span></button>
        </form>
        <div class="composer-meta">
          <span>你可以随时停下来。请不要输入身份证号、密码等不必要的个人信息。</span>
          <span>{{ draft.length }} / 2000</span>
        </div>
      </div>

      <div v-if="feedbackVisible" class="feedback-bar">
        <span>这次回应对你有帮助吗？</span>
        <button type="button" @click="sendFeedback(1)">有帮助</button>
        <button type="button" @click="sendFeedback(2)">一般</button>
        <button type="button" @click="sendFeedback(3)">没帮助</button>
        <button class="text-button" type="button" @click="$router.push({ name: 'diary' })">记录本次感受</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { listSessions, getSession, startSession, deleteSession, sendChatMessage, submitFeedback } from '../api'
import { toast } from '../utils/toast'

const sessions = ref([])
const activeSessionId = ref(null)
const draft = ref('')
const sending = ref(false)
const typing = ref(false)
const feedbackVisible = ref(false)
const crisisShown = ref(false)
const messagesEl = ref(null)

const activeSession = computed(() => sessions.value.find((s) => s.id === activeSessionId.value) || null)
const activeMessages = computed(() => activeSession.value?.messages || [])
const hasUserMessage = computed(() => activeMessages.value.some((m) => m.senderType === 1))

async function loadSessions() {
  try {
    const list = await listSessions()
    sessions.value = (list || []).map((s) => ({ ...s, messages: [] }))
    if (activeSessionId.value && sessions.value.some((s) => s.id === activeSessionId.value)) {
      await loadSessionMessages(activeSessionId.value)
    } else if (sessions.value.length) {
      activeSessionId.value = sessions.value[0].id
      await loadSessionMessages(activeSessionId.value)
    }
  } catch (e) {
    sessions.value = []
    toast(e.message || '会话加载失败')
  }
}

async function loadSessionMessages(sessionId) {
  try {
    const detail = await getSession(sessionId)
    const target = sessions.value.find((s) => s.id === sessionId)
    if (target) {
      target.sessionTitle = detail.sessionTitle
      target.riskLevel = detail.riskLevel
      target.messages = (detail.messages || []).map((m) => ({
        key: `${m.id}-${m.senderType}`,
        senderType: m.senderType,
        content: m.content,
        createdAt: m.createdAt
      }))
      crisisShown.value = Number(detail.riskLevel || 0) >= 3
    }
  } catch (e) {
    toast(e.message || '会话详情加载失败')
  }
}

async function newSession() {
  try {
    const detail = await startSession({ title: '新的倾诉' })
    const session = {
      id: detail.id,
      sessionTitle: detail.sessionTitle,
      startedAt: detail.startedAt,
      riskLevel: 0,
      messages: (detail.messages || []).map((m) => ({
        key: `${m.id}-${m.senderType}`, senderType: m.senderType, content: m.content, createdAt: m.createdAt
      }))
    }
    sessions.value.unshift(session)
    activeSessionId.value = session.id
    crisisShown.value = false
    feedbackVisible.value = false
    toast('新的倾诉已开始')
  } catch (e) {
    toast(e.message || '新建会话失败')
  }
}

async function selectSession(id) {
  activeSessionId.value = id
  crisisShown.value = false
  feedbackVisible.value = false
  await loadSessionMessages(id)
}

async function removeSession(id) {
  try {
    await deleteSession(id)
  } catch (e) {
    // 继续本地移除
  }
  sessions.value = sessions.value.filter((s) => s.id !== id)
  if (activeSessionId.value === id) {
    activeSessionId.value = sessions.value[0]?.id || null
    if (activeSessionId.value) await loadSessionMessages(activeSessionId.value)
  }
  toast('会话已删除')
}

function usePrompt(text) {
  draft.value = text
}

function autoGrow(e) {
  e.target.style.height = 'auto'
  e.target.style.height = `${Math.min(e.target.scrollHeight, 130)}px`
}

function pushMessage(senderType, content) {
  const target = sessions.value.find((s) => s.id === activeSessionId.value)
  if (!target) return
  target.messages.push({
    key: `${Date.now()}-${senderType}-${Math.random().toString(36).slice(2, 6)}`,
    senderType,
    content,
    createdAt: new Date().toISOString()
  })
  scrollToBottom()
}

async function send() {
  const content = draft.value.trim()
  if (!content || sending.value || !activeSessionId.value) return
  if (!activeSessionId.value) {
    toast('请先新建或选择一个会话')
    return
  }
  draft.value = ''
  pushMessage(1, content)
  sending.value = true
  typing.value = true
  feedbackVisible.value = false

  let assistantMessageId = null
  try {
    const result = await sendChatMessage({
      sessionId: activeSessionId.value,
      content,
      model: undefined
    })
    assistantMessageId = result.assistantMessageId
    const riskLevel = Number(result.riskLevel || 0)
    if (riskLevel >= 3) {
      crisisShown.value = true
    }
    // 流式渲染
    const full = await streamAssistant(activeSessionId.value, assistantMessageId)
    typing.value = false
    pushMessage(2, full)
    feedbackVisible.value = true
    // 更新标题（服务端可能已改）
    const target = sessions.value.find((s) => s.id === activeSessionId.value)
    if (target && target.sessionTitle === '新的倾诉') {
      target.sessionTitle = '正在梳理的一件事'
    }
  } catch (e) {
    typing.value = false
    toast(e.message || '发送失败，请重试')
  } finally {
    sending.value = false
    typing.value = false
  }
}

async function streamAssistant(sessionId, assistantMessageId) {
  const token = localStorage.getItem('codex-ai-token') || ''
  const url = `/api/psychological-chat/stream?sessionId=${encodeURIComponent(sessionId)}&assistantMessageId=${assistantMessageId}`
  const resp = await fetch(url, { headers: { Authorization: `Bearer ${token}` } })
  if (!resp.ok || !resp.body) {
    throw new Error('流式响应不可用')
  }
  const reader = resp.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let full = ''
  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const blocks = buffer.split('\n\n')
    buffer = blocks.pop() || ''
    for (const block of blocks) {
      const dataLine = block.split('\n').find((line) => line.startsWith('data:'))
      if (!dataLine) continue
      const raw = dataLine.slice(5).trim()
      if (!raw) continue
      try {
        const data = JSON.parse(raw)
        if (data.content) {
          full += data.content
          // 更新最后一条 AI 消息为增量
          const target = sessions.value.find((s) => s.id === activeSessionId.value)
          if (target && target.messages.length) {
            const last = target.messages[target.messages.length - 1]
            if (last && last.senderType === 2) {
              last.content = full
            } else {
              pushMessage(2, full)
            }
          }
          scrollToBottom()
        }
      } catch (e) {
        // 忽略无法解析的片段
      }
    }
  }
  return full || '(未收到回复)'
}

async function sendFeedback(helpfulness) {
  try {
    await submitFeedback({
      sessionId: activeSessionId.value,
      helpfulness
    })
    feedbackVisible.value = false
    toast('感谢你的反馈，它会帮助改进回应质量')
  } catch (e) {
    toast(e.message || '反馈提交失败')
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesEl.value) {
      messagesEl.value.scrollTop = messagesEl.value.scrollHeight
    }
  })
}

function formatTime(value) {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return ''
  return new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit' }).format(d)
}

function formatSessionTime(value) {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return ''
  return new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(d)
}

async function copyCrisis() {
  const text = '如果你正处于立即危险，请拨打 120、110 或 12356，并联系身边可信任的人陪同处理。'
  try {
    await navigator.clipboard.writeText(text)
    toast('求助信息已复制')
  } catch {
    toast('复制失败，请直接拨打 120、110 或 12356')
  }
}

function openCrisis() {
  window.dispatchEvent(new CustomEvent('codex-open-crisis'))
}

onMounted(loadSessions)
</script>
