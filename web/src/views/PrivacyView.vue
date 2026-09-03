<template>
  <div>
    <div class="page-intro">
      <div>
        <span class="section-kicker">我的账户</span>
        <h2>隐私与数据</h2>
        <p>了解数据如何被使用，也可以随时管理自己的授权和记录。</p>
      </div>
    </div>

    <div class="privacy-grid">
      <section class="panel">
        <div class="section-heading">
          <div><span class="section-kicker">当前状态</span><h2>授权记录</h2></div>
          <span class="status-chip" :class="profile?.consentComplete ? 'success' : ''">{{ profile?.consentComplete ? '已完成' : '未完成' }}</span>
        </div>
        <dl class="data-list">
          <div><dt>年龄确认</dt><dd>{{ profile?.ageConfirmed ? '已确认年满 18 周岁' : '未确认' }}</dd></div>
          <div><dt>隐私政策</dt><dd>{{ profile?.privacyPolicyVersion || '-' }} · {{ profile?.consentComplete ? '已同意' : '待确认' }}</dd></div>
          <div><dt>敏感信息说明</dt><dd>{{ profile?.sensitiveInfoVersion || '-' }} · {{ profile?.consentComplete ? '已同意' : '待确认' }}</dd></div>
          <div><dt>产品边界说明</dt><dd>{{ profile?.productBoundaryVersion || '-' }} · {{ profile?.consentComplete ? '已确认' : '待确认' }}</dd></div>
        </dl>
        <button class="secondary-button" type="button" @click="handleRevoke">撤回非必要授权</button>
      </section>

      <section class="panel">
        <div class="section-heading">
          <div><span class="section-kicker">数据管理</span><h2>带走或删除</h2></div>
        </div>
        <div class="privacy-actions">
          <button class="privacy-action" type="button" @click="handleExport">
            <span class="action-symbol">↓</span>
            <span><strong>导出我的数据</strong><small>下载会话、日记和授权记录的 JSON 文件</small></span>
            <span>→</span>
          </button>
          <button class="privacy-action danger-action" type="button" @click="handleDelete">
            <span class="action-symbol">×</span>
            <span><strong>申请删除账号与数据</strong><small>申请进入可审计的处理流程</small></span>
            <span>→</span>
          </button>
        </div>
        <div v-if="profile?.deletionRequested" class="deletion-status">
          <strong>删除申请状态：{{ profile.deletionStatus }}</strong>
          <p style="margin:6px 0 0">正式系统会保留必要的审计记录，并在处理完成后通知你。</p>
          <button v-if="profile.deletionStatus !== '已完成' && profile.deletionStatus !== '处理中'" class="text-button" type="button" @click="handleCancelDeletion">取消申请</button>
        </div>
      </section>
    </div>

    <section class="panel privacy-note">
      <span class="note-icon">i</span>
      <div>
        <strong>我们只收集实现功能所必需的信息</strong>
        <p>心理对话、情绪日记和风险事件按敏感个人信息进行设计。后台默认只查看聚合数据和脱敏摘要，不以运营便利为由浏览私密原文。</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getPrivacyProfile, exportData, submitDeletion, cancelDeletion } from '../api'
import { toast } from '../utils/toast'

const router = useRouter()
const profile = ref(null)

async function load() {
  try {
    profile.value = await getPrivacyProfile()
  } catch (e) {
    profile.value = null
  }
}

async function handleRevoke() {
  try {
    await submitDeletionForRevoke()
  } catch (e) {
    toast(e.message || '操作失败')
  }
}

async function submitDeletionForRevoke() {
  const { revokeConsent } = await import('../api')
  await revokeConsent()
  toast('非必要授权已撤回，重新使用前需要再次确认')
  router.replace('/consent')
}

async function handleExport() {
  try {
    const result = await exportData()
    const json = result?.content || JSON.stringify({})
    const blob = new Blob([json], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = `codex-ai-data-${new Date().toISOString().slice(0, 10)}.json`
    anchor.click()
    URL.revokeObjectURL(url)
    toast('数据导出已开始下载')
  } catch (e) {
    toast(e.message || '导出失败')
  }
}

async function handleDelete() {
  if (profile.value?.deletionRequested) {
    toast('删除申请正在处理中')
    return
  }
  try {
    await submitDeletion('')
    toast('删除申请已提交，状态可在这里查看')
    await load()
  } catch (e) {
    toast(e.message || '提交失败')
  }
}

async function handleCancelDeletion() {
  try {
    await cancelDeletion()
    toast('删除申请已取消')
    await load()
  } catch (e) {
    toast(e.message || '取消失败')
  }
}

onMounted(load)
</script>
