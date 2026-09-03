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
      <h2>创建账号</h2>
      <p class="subtitle">注册只需用户名和密码，后续可按需要补充资料。</p>
      <form @submit.prevent="handleRegister">
        <div class="auth-field">
          <label for="username">用户名 <em style="color:var(--green);font-style:normal;font-size:11px">3-50 位字母、数字或下划线</em></label>
          <input id="username" v-model.trim="form.username" autocomplete="username" placeholder="例如：lin_chen" />
        </div>
        <div class="auth-field">
          <label for="password">密码 <em style="color:var(--green);font-style:normal;font-size:11px">至少 6 位</em></label>
          <input id="password" v-model="form.password" type="password" autocomplete="new-password" placeholder="请输入密码" />
        </div>
        <div class="auth-field">
          <label for="nickname">昵称 <em style="color:#a1aca6;font-style:normal;font-size:11px">可选</em></label>
          <input id="nickname" v-model.trim="form.nickname" placeholder="给自己一个称呼" />
        </div>
        <p v-if="error" class="auth-error">{{ error }}</p>
        <button class="primary-button" type="submit" :disabled="loading">
          {{ loading ? '创建中…' : '注册并进入' }}
        </button>
      </form>
      <p class="auth-switch">已有账号？<router-link to="/login">去登录</router-link></p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

const form = reactive({ username: '', password: '', nickname: '' })
const error = ref('')
const loading = ref(false)

async function handleRegister() {
  if (!form.username || !form.password) {
    error.value = '请填写用户名和密码'
    return
  }
  if (!/^[a-zA-Z0-9_]+$/.test(form.username)) {
    error.value = '用户名只能包含字母、数字和下划线'
    return
  }
  error.value = ''
  loading.value = true
  try {
    await auth.register({ username: form.username, password: form.password, nickname: form.nickname })
    router.replace('/')
  } catch (e) {
    error.value = e.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>
