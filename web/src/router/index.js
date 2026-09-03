import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useConsentStore } from '../stores/consent'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../views/RegisterView.vue'),
    meta: { public: true, title: '注册' }
  },
  {
    path: '/consent',
    name: 'consent',
    component: () => import('../views/ConsentView.vue'),
    meta: { requiresAuth: true, title: '首次使用确认' }
  },
  {
    path: '/',
    component: () => import('../views/LayoutView.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', name: 'home', component: () => import('../views/HomeView.vue'), meta: { title: '首页' } },
      { path: 'chat', name: 'chat', component: () => import('../views/ChatView.vue'), meta: { requiresConsent: true, title: '倾诉对话' } },
      { path: 'diary', name: 'diary', component: () => import('../views/DiaryView.vue'), meta: { requiresConsent: true, title: '情绪日记' } },
      { path: 'knowledge', name: 'knowledge', component: () => import('../views/KnowledgeView.vue'), meta: { title: '知识库' } },
      { path: 'privacy', name: 'privacy', component: () => import('../views/PrivacyView.vue'), meta: { title: '隐私与数据' } },
      { path: 'admin', name: 'admin', component: () => import('../views/AdminView.vue'), meta: { requiresAdmin: true, title: '管理端' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  const consent = useConsentStore()

  if (to.meta.public) {
    if (auth.isLoggedIn && to.name === 'login') return { name: 'home' }
    return true
  }

  if (!auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  // 拉取同意状态（缓存 30 秒）
  if (to.meta.requiresConsent) {
    try {
      await consent.loadIfStale()
      if (!consent.complete) {
        return { name: 'consent' }
      }
    } catch (e) {
      // 拉取失败时允许继续，由接口层兜底
    }
  }

  if (to.meta.requiresAdmin && !auth.isAdmin) {
    return { name: 'home' }
  }

  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · Codex-AI 心理健康助手` : 'Codex-AI 心理健康助手'
})

export default router
