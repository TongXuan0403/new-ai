<template>
  <div class="consent-shell">
    <div class="consent-card">
      <span class="section-kicker">第一次见面</span>
      <h2>先确认，我们如何陪你使用</h2>
      <p class="lead">这里是一个心理支持与自助管理工具。它可以帮你表达、梳理和记录，但不能替代医生、心理咨询师或紧急救援。</p>
      <form @submit.prevent="handleAgree">
        <label class="check-row">
          <input v-model="ageCheck" type="checkbox" />
          <span>我确认自己已满 18 周岁。</span>
        </label>
        <label class="check-row">
          <input v-model="privacyCheck" type="checkbox" />
          <span>我已阅读并同意隐私政策，了解心理对话和日记按敏感个人信息处理。</span>
        </label>
        <label class="check-row">
          <input v-model="boundaryCheck" type="checkbox" />
          <span>我理解这不是诊断、治疗或紧急救援服务。</span>
        </label>
        <p v-if="error" class="form-error">{{ error }}</p>
        <button class="primary-button" type="submit" :disabled="loading">
          {{ loading ? '确认中…' : '确认并进入 →' }}
        </button>
      </form>
      <p style="margin:18px 0 0;font-size:11px;color:var(--muted)">
        演示版本：隐私政策 privacy-v1.0 · 敏感信息说明 sensitive-v1.0 · 产品边界 boundary-v1.0
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useConsentStore } from '../stores/consent'

const router = useRouter()
const consent = useConsentStore()

const ageCheck = ref(false)
const privacyCheck = ref(false)
const boundaryCheck = ref(false)
const error = ref('')
const loading = ref(false)

async function handleAgree() {
  if (!ageCheck.value || !privacyCheck.value || !boundaryCheck.value) {
    error.value = '请完成三项确认后继续'
    return
  }
  error.value = ''
  loading.value = true
  try {
    await consent.submit({
      ageConfirmed: true,
      privacyPolicyVersion: consent.privacyPolicyVersion || 'privacy-v1.0',
      sensitiveInfoVersion: consent.sensitiveInfoVersion || 'sensitive-v1.0',
      productBoundaryVersion: consent.productBoundaryVersion || 'boundary-v1.0'
    })
    router.replace('/')
  } catch (e) {
    error.value = e.message || '确认失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>
