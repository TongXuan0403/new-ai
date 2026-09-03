<template>
  <div class="auth-shell">
    <div class="auth-card">
      <div class="auth-brand">
        <div class="brand-mark">C</div>
        <div>
          <strong style="font-size:17px">Codex-AI</strong>
          <span style="display:block;color:var(--muted);font-size:12px;margin-top:3px">心理健康助手</span>
        </div>
      </div>
      <h2>欢迎回来</h2>
      <p class="subtitle">登录后继续你的情绪记录与倾诉。</p>
      <form @submit.prevent="handleLogin">
        <div class="auth-field">
          <label for="username">用户名</label>
          <input id="username" v-model.trim="form.username" autocomplete="username" placeholder="请输入用户名" />
        </div>
        <div class="auth-field">
          <label for="password">密码</label>
          <input id="password" v-model="form.password" type="password" autocomplete="current-password" placeholder="请输入密码" />
        </div>
        <p v-if="error" class="auth-error">{{ error }}</p>
        <button class="primary-button" type="submit" :disabled="loading">
          {{ loading ? '登录中…' : '登录' }}
        </button>
      </form>
      <p class="auth-switch">还没有账号？<router-link to="/register">注册一个</router-link></p>
      <p class="auth-switch" style="margin-top:6px;font-size:11px">演示账号：admin / Admin@123 · demo / Demo@123</p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const form = reactive({ username: '', password: '' })
const error = ref('')
const loading = ref(false)

async function handleLogin() {
  if (!form.username || !form.password) {
    error.value = '请输入用户名和密码'
    return
  }
  error.value = ''
  loading.value = true
  try {
    await auth.login({ username: form.username, password: form.password })
    router.replace(route.query.redirect || '/')
  } catch (e) {
    error.value = e.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>
