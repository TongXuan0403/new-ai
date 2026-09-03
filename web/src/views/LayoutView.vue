<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">C</div>
        <div><strong>Codex-AI</strong><span>心理健康助手</span></div>
      </div>
      <div class="sidebar-label">我的空间</div>
      <nav class="nav">
        <button class="nav-item" :class="{ active: route.name === 'home' }" @click="go('home')">
          <span class="nav-icon">⌂</span><span>首页</span>
        </button>
        <button class="nav-item" :class="{ active: route.name === 'chat' }" @click="go('chat')">
          <span class="nav-icon">◌</span><span>倾诉</span>
        </button>
        <button class="nav-item" :class="{ active: route.name === 'diary' }" @click="go('diary')">
          <span class="nav-icon">▤</span><span>情绪日记</span>
        </button>
        <button class="nav-item" :class="{ active: route.name === 'knowledge' }" @click="go('knowledge')">
          <span class="nav-icon">▧</span><span>知识库</span>
        </button>
        <button class="nav-item" :class="{ active: route.name === 'privacy' }" @click="go('privacy')">
          <span class="nav-icon">⊙</span><span>隐私与数据</span>
        </button>
        <button v-if="auth.isAdmin" class="nav-item" :class="{ active: route.name === 'admin' }" @click="go('admin')">
          <span class="nav-icon">▦</span><span>管理端</span>
        </button>
      </nav>
      <div class="sidebar-resource">
        <div class="resource-icon">!</div>
        <div><strong>需要立即帮助？</strong><p>联系现实支持，不要独自承担。</p></div>
        <div class="resource-links">
          <a href="tel:120">120</a><a href="tel:110">110</a><a href="tel:12356">12356</a>
        </div>
      </div>
      <div class="sidebar-footer">
        <span class="online-dot"></span><span>已登录 · {{ auth.displayName }}</span>
      </div>
    </aside>

    <main class="main">
      <header class="topbar">
        <div>
          <p class="eyebrow">CODEX / CARE</p>
          <h1>{{ pageTitle }}</h1>
        </div>
        <div class="topbar-actions">
          <button class="user-pill" type="button" @click="go('privacy')">
            <span class="avatar">{{ avatarText }}</span>
            <span>{{ auth.displayName }}</span>
            <span class="chevron">⌄</span>
          </button>
          <button class="text-button danger-link" type="button" @click="crisisOpen = true">需要紧急帮助</button>
          <button class="text-button" type="button" @click="handleLogout">退出</button>
        </div>
      </header>

      <router-view />

      <div v-if="crisisOpen" class="modal-mask" @click.self="crisisOpen = false">
        <div class="modal" style="max-width:520px">
          <div class="crisis-card" style="margin:0">
            <div class="crisis-leading">
              <span class="crisis-icon">!</span>
              <div>
                <strong>如果你正处于立即危险，请先联系现实支持</strong>
                <p>请确认自己暂时处于安全的地方，并让身边可信任的人陪着你。这里不会承诺已通知他人。</p>
              </div>
            </div>
            <div class="crisis-actions" style="padding-left:35px">
              <a class="crisis-call" href="tel:120">拨打 120</a>
              <a class="crisis-call" href="tel:110">拨打 110</a>
              <a class="crisis-call" href="tel:12356">拨打 12356</a>
              <button class="crisis-copy" type="button" @click="copyCrisis">复制求助信息</button>
            </div>
          </div>
          <div class="modal-actions">
            <button class="text-button" type="button" @click="crisisOpen = false">关闭</button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { toast } from '../utils/toast'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const crisisOpen = ref(false)
const pageTitle = computed(() => route.meta.title || '今天从哪里开始？')
const avatarText = computed(() => (auth.displayName || '同').charAt(0))

watch(
  () => route.name,
  () => {}
)

function go(name) {
  router.push({ name })
}

window.addEventListener('codex-open-crisis', () => {
  crisisOpen.value = true
})

async function handleLogout() {
  await auth.logout()
  router.replace('/login')
  toast('已安全退出')
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
</script>
