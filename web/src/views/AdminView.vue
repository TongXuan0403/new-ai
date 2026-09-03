<template>
  <div>
    <div class="page-intro">
      <div>
        <span class="section-kicker">管理员空间</span>
        <h2>运营与安全概览</h2>
        <p>这里展示聚合指标和脱敏信息，不默认打开用户的私密原文。</p>
      </div>
      <span class="role-badge">管理员</span>
    </div>

    <div class="admin-tabs">
      <button v-for="tab in tabs" :key="tab.key" type="button"
              :class="{ active: activeTab === tab.key }" @click="activeTab = tab.key">{{ tab.label }}</button>
    </div>

    <!-- 概览 -->
    <div v-if="activeTab === 'overview'">
      <div class="metric-row">
        <div class="metric-card"><span>活跃用户</span><strong>{{ overview?.activeUsers ?? '-' }}</strong><p>普通用户</p></div>
        <div class="metric-card"><span>会话</span><strong>{{ overview?.totalSessions ?? '-' }}</strong><p>共 {{ overview?.sessionUsers ?? 0 }} 位用户使用</p></div>
        <div class="metric-card"><span>完成日记</span><strong>{{ overview?.totalDiaries ?? '-' }}</strong><p>共 {{ overview?.diaryUsers ?? 0 }} 位用户记录</p></div>
        <div class="metric-card warning"><span>风险待处理</span><strong>{{ overview?.riskPending ?? 0 }}</strong><p>仅展示脱敏摘要</p></div>
      </div>
      <div class="content-grid" style="grid-template-columns:repeat(2,minmax(0,1fr));margin-top:18px">
        <section class="panel">
          <div class="section-heading"><div><span class="section-kicker">内容</span><h2>知识库</h2></div></div>
          <dl class="data-list">
            <div><dt>已发布文章</dt><dd>{{ overview?.publishedArticles ?? 0 }}</dd></div>
            <div><dt>浏览量</dt><dd>{{ overview?.totalViews ?? 0 }}</dd></div>
            <div><dt>风险事件总数</dt><dd>{{ overview?.riskEvents ?? 0 }}</dd></div>
            <div><dt>风险等级分布</dt><dd>{{ levelSummary }}</dd></div>
          </dl>
        </section>
        <section class="panel">
          <div class="section-heading"><div><span class="section-kicker">说明</span><h2>默认脱敏</h2></div></div>
          <p style="font-size:13px;line-height:1.8">风险事件默认只展示脱敏摘要。查看事件详情、导出数据等敏感操作会写入审计日志，便于事后追溯。</p>
        </section>
      </div>
    </div>

    <!-- 风险事件 -->
    <div v-if="activeTab === 'risk'">
      <section class="panel table-panel">
        <div class="section-heading">
          <div><span class="section-kicker">安全审计</span><h2>风险事件中心</h2></div>
          <span class="muted" style="color:var(--muted);font-size:12px">默认脱敏</span>
        </div>
        <div class="risk-table-wrap">
          <table>
            <thead><tr><th>时间</th><th>等级</th><th>摘要</th><th>规则版本</th><th>状态</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="event in riskEvents" :key="event.id">
                <td>{{ formatDateTime(event.createdAt) }}</td>
                <td><span class="risk" :class="event.riskLevel >= 3 ? 'high' : event.riskLevel === 2 ? 'mid' : 'low'">{{ riskLevelLabel(event.riskLevel) }}</span></td>
                <td>{{ event.contentSummary || '-' }}</td>
                <td>{{ event.ruleVersion }}</td>
                <td>{{ event.status }}</td>
                <td>
                  <select v-model="event.status" @change="updateRiskStatus(event)" style="min-height:28px;min-width:80px;padding:0 6px;font-size:12px">
                    <option>待复核</option><option>处理中</option><option>已关闭</option>
                  </select>
                </td>
              </tr>
              <tr v-if="!riskEvents.length"><td colspan="6" style="text-align:center;color:var(--muted)">暂无风险事件</td></tr>
            </tbody>
          </table>
        </div>
        <div class="pagination">
          <span>第 {{ riskPage }} / {{ riskTotalPages }} 页</span>
          <button type="button" :disabled="riskPage <= 1" @click="loadRisk(riskPage - 1)">上一页</button>
          <button type="button" :disabled="riskPage >= riskTotalPages" @click="loadRisk(riskPage + 1)">下一页</button>
        </div>
      </section>
    </div>

    <!-- 危机资源 -->
    <div v-if="activeTab === 'resource'">
      <section class="panel">
        <div class="section-heading">
          <div><span class="section-kicker">危机资源</span><h2>前端展示资源</h2></div>
          <button class="primary-button small-button" type="button" @click="openResourceModal()">新增资源 <span>＋</span></button>
        </div>
        <div class="resource-config-grid">
          <div v-for="resource in resources" :key="resource.id">
            <strong>{{ resource.name }} <span v-if="!resource.enabled" style="color:var(--red)">（停用）</span></strong>
            <span>{{ resource.phone || '无电话' }} · {{ resource.description || '-' }}</span>
            <div style="margin-top:8px">
              <button class="status-button" type="button" @click="openResourceModal(resource)">编辑</button>
              <button class="status-button danger" type="button" @click="removeResource(resource)">删除</button>
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- 文章审核 -->
    <div v-if="activeTab === 'article'">
      <section class="panel">
        <div class="section-heading">
          <div><span class="section-kicker">内容审核</span><h2>文章状态</h2></div>
          <button class="primary-button small-button" type="button" @click="openArticleModal()">新建文章 <span>＋</span></button>
        </div>
        <div class="admin-article-list">
          <div v-for="article in articles" :key="article.id" class="admin-article">
            <div>
              <strong>{{ article.title }}</strong>
              <small>{{ article.categoryName }} · {{ statusLabel(article.status) }}<template v-if="article.auditRemark"> · {{ article.auditRemark }}</template></small>
            </div>
            <div style="display:flex;gap:6px">
              <button class="status-button" type="button" @click="toggleArticle(article)">{{ article.status === 'PUBLISHED' ? '下线' : '发布' }}</button>
              <button class="status-button danger" type="button" @click="removeArticle(article)">删除</button>
            </div>
          </div>
          <div v-if="!articles.length" class="empty-state">暂无文章</div>
        </div>
      </section>
    </div>

    <!-- 自助练习 -->
    <div v-if="activeTab === 'exercise'">
      <section class="panel">
        <div class="section-heading">
          <div><span class="section-kicker">内容维护</span><h2>自助练习库</h2></div>
          <button class="primary-button small-button" type="button" @click="openExerciseModal()">新建练习 <span>＋</span></button>
        </div>
        <div class="admin-article-list">
          <div v-for="exercise in exercises" :key="exercise.id" class="admin-article">
            <div>
              <strong>{{ exercise.title }}</strong>
              <small>{{ exercise.categoryName || '自助练习' }} · {{ exercise.minutes }} 分钟 · {{ exerciseStatusLabel(exercise.status) }}</small>
              <small v-if="exercise.tags" class="article-tags">{{ exercise.tags.split(',').map((t) => '#' + t.trim()).join(' ') }}</small>
            </div>
            <div style="display:flex;gap:6px">
              <button class="status-button" type="button" @click="toggleExercise(exercise)">{{ exercise.status === 'PUBLISHED' ? '下线' : '发布' }}</button>
              <button class="status-button" type="button" @click="openExerciseModal(exercise)">编辑</button>
              <button class="status-button danger" type="button" @click="removeExercise(exercise)">删除</button>
            </div>
          </div>
          <div v-if="!exercises.length" class="empty-state">暂无练习</div>
        </div>
      </section>
    </div>

    <!-- 配置版本 -->
    <div v-if="activeTab === 'config'">
      <section class="panel">
        <div class="section-heading">
          <div><span class="section-kicker">提示词 / 模型 / 规则</span><h2>系统配置版本</h2></div>
          <button class="primary-button small-button" type="button" @click="openConfigModal()">新建版本 <span>＋</span></button>
        </div>
        <p style="font-size:12px;color:var(--muted);margin-bottom:10px">每类配置仅一条生效；修改后创建草稿并「生效」，新的对话与风险检测将使用该版本，旧消息与风险事件保留当时版本号可追溯。</p>
        <div v-for="group in configTypeGroups" :key="group.type" class="config-group">
          <h4>{{ group.label }} <span class="muted" style="color:var(--muted);font-size:12px">当前生效：{{ activeLabel(group.type) }}</span></h4>
          <div class="config-list">
            <div v-for="version in group.items" :key="version.id" class="config-item" :class="version.status.toLowerCase()">
              <div>
                <strong>{{ version.version }}</strong>
                <span class="config-status" :class="version.status.toLowerCase()">{{ versionStatusLabel(version.status) }}</span>
                <small>{{ version.name }}<template v-if="version.remark"> · {{ version.remark }}</template></small>
                <pre v-if="version.content" class="config-preview">{{ version.content }}</pre>
              </div>
              <div style="display:flex;gap:6px;flex-wrap:wrap">
                <button v-if="version.status === 'DRAFT'" class="status-button primary" type="button" @click="openConfigModal(version)">编辑</button>
                <button v-if="version.status === 'DRAFT' || version.status === 'DISABLED'" class="status-button" type="button" @click="activateConfig(version)">生效</button>
                <button v-if="version.status === 'ACTIVE'" class="status-button" type="button" disabled style="opacity:.5">生效中</button>
                <button v-if="version.status !== 'ACTIVE'" class="status-button danger" type="button" @click="removeConfig(version)">删除</button>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- 对话反馈 -->
    <div v-if="activeTab === 'feedback'">
      <section class="panel table-panel">
        <div class="section-heading"><div><span class="section-kicker">体验</span><h2>对话反馈</h2></div></div>
        <div class="risk-table-wrap">
          <table>
            <thead><tr><th>时间</th><th>用户</th><th>帮助度</th><th>备注</th></tr></thead>
            <tbody>
              <tr v-for="item in feedbacks" :key="item.id">
                <td>{{ formatDateTime(item.createdAt) }}</td>
                <td>用户 {{ item.userId }}</td>
                <td>{{ helpfulnessLabel(item.helpfulness) }}</td>
                <td>{{ item.comment || '-' }}</td>
              </tr>
              <tr v-if="!feedbacks.length"><td colspan="4" style="text-align:center;color:var(--muted)">暂无反馈</td></tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>

    <!-- 审计日志 -->
    <div v-if="activeTab === 'audit'">
      <section class="panel table-panel">
        <div class="section-heading"><div><span class="section-kicker">追溯</span><h2>审计日志</h2></div></div>
        <div class="risk-table-wrap">
          <table>
            <thead><tr><th>时间</th><th>操作人</th><th>操作</th><th>对象</th><th>详情</th><th>IP</th></tr></thead>
            <tbody>
              <tr v-for="log in auditLogs" :key="log.id">
                <td>{{ formatDateTime(log.createdAt) }}</td>
                <td>{{ log.operatorId || '-' }}</td>
                <td>{{ log.action }}</td>
                <td>{{ log.targetType }} #{{ log.targetId || '-' }}</td>
                <td>{{ log.detail || '-' }}</td>
                <td>{{ log.ip || '-' }}</td>
              </tr>
              <tr v-if="!auditLogs.length"><td colspan="6" style="text-align:center;color:var(--muted)">暂无审计日志</td></tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>

    <!-- 资源编辑模态框 -->
    <div v-if="resourceModal" class="modal-mask" @click.self="resourceModal = null">
      <div class="modal">
        <h3>{{ resourceModal.id ? '编辑资源' : '新增资源' }}</h3>
        <div class="modal-field"><label>名称</label><input v-model="resourceForm.name" /></div>
        <div class="modal-field"><label>电话</label><input v-model="resourceForm.phone" /></div>
        <div class="modal-field"><label>类型</label>
          <select v-model="resourceForm.resourceType">
            <option>emergency</option><option>hotline</option><option>school</option><option>local</option>
          </select>
        </div>
        <div class="modal-field"><label>说明</label><textarea v-model="resourceForm.description"></textarea></div>
        <div class="modal-field"><label>地区/学校</label><input v-model="resourceForm.region" /></div>
        <label class="check-row"><input v-model="resourceForm.enabled" type="checkbox" /><span>启用</span></label>
        <div class="modal-actions">
          <button class="text-button" type="button" @click="resourceModal = null">取消</button>
          <button class="primary-button" type="button" @click="saveResource">保存</button>
        </div>
      </div>
    </div>

    <!-- 文章编辑模态框 -->
    <div v-if="articleModal" class="modal-mask" @click.self="articleModal = null">
      <div class="modal">
        <h3>{{ articleModal.id ? '编辑文章' : '新建文章' }}</h3>
        <div class="modal-field"><label>标题</label><input v-model="articleForm.title" /></div>
        <div class="modal-field"><label>分类</label>
          <select v-model="articleForm.categoryId">
            <option :value="1">压力</option><option :value="2">睡眠</option><option :value="3">关系</option><option :value="4">自助练习</option>
          </select>
        </div>
        <div class="modal-field"><label>摘要</label><textarea v-model="articleForm.summary"></textarea></div>
        <div class="modal-field"><label>正文（支持段落，空行分隔）</label><textarea v-model="articleForm.content" style="min-height:140px"></textarea></div>
        <div class="modal-field"><label>来源</label><input v-model="articleForm.source" /></div>
        <div class="modal-actions">
          <button class="text-button" type="button" @click="articleModal = null">取消</button>
          <button class="primary-button" type="button" @click="saveArticle">保存</button>
        </div>
      </div>
    </div>
    <!-- 练习编辑模态框 -->
    <div v-if="exerciseModal" class="modal-mask" @click.self="exerciseModal = null">
      <div class="modal">
        <h3>{{ exerciseModal.id ? '编辑练习' : '新建练习' }}</h3>
        <div class="modal-field"><label>名称</label><input v-model="exerciseForm.title" /></div>
        <div class="modal-field"><label>简介</label><textarea v-model="exerciseForm.summary"></textarea></div>
        <div class="modal-field"><label>练习步骤（每行一步）</label><textarea v-model="exerciseForm.content" style="min-height:140px"></textarea></div>
        <div class="modal-field"><label>预计时长（分钟）</label><input v-model.number="exerciseForm.minutes" type="number" min="1" max="60" /></div>
        <div class="modal-field"><label>标签（逗号分隔）</label><input v-model="exerciseForm.tags" placeholder="如：焦虑,呼吸" /></div>
        <div class="modal-actions">
          <button class="text-button" type="button" @click="exerciseModal = null">取消</button>
          <button class="primary-button" type="button" @click="saveExercise">保存</button>
        </div>
      </div>
    </div>

    <!-- 配置版本编辑模态框 -->
    <div v-if="configModal" class="modal-mask" @click.self="configModal = null">
      <div class="modal" style="max-width:560px">
        <h3>{{ configModal.id ? '编辑版本' : '新建版本' }}</h3>
        <div class="modal-field"><label>类型</label>
          <select v-model="configForm.configType" :disabled="!!configModal.id">
            <option value="PROMPT">PROMPT（提示词）</option>
            <option value="MODEL">MODEL（模型）</option>
            <option value="RISK_RULE">RISK_RULE（风险规则）</option>
          </select>
        </div>
        <div class="modal-field"><label>名称</label><input v-model="configForm.name" /></div>
        <div class="modal-field"><label>版本号（同类型内唯一）</label><input v-model="configForm.version" :disabled="!!configModal.id" /></div>
        <div class="modal-field"><label>内容</label>
          <textarea v-model="configForm.content" style="min-height:160px"
                    placeholder="PROMPT：系统提示词文本；MODEL：模型名；RISK_RULE：JSON {crisis:[...],harmOthers:[...],warning:[...],concern:[...]}" />
        </div>
        <div class="modal-field"><label>备注</label><input v-model="configForm.remark" /></div>
        <div class="modal-actions">
          <button class="text-button" type="button" @click="configModal = null">取消</button>
          <button class="primary-button" type="button" @click="saveConfig">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  adminOverview, adminRiskPage, adminRiskUpdateStatus,
  adminCrisisList, adminCreateCrisis, adminUpdateCrisis, adminDeleteCrisis,
  adminArticlePage, adminCreateArticle, adminUpdateArticle, adminDeleteArticle, adminUpdateArticleStatus,
  adminExercisePage, adminCreateExercise, adminUpdateExercise, adminDeleteExercise, adminUpdateExerciseStatus,
  adminConfigVersionPage, adminCreateConfigVersion, adminUpdateConfigVersion,
  adminActivateConfigVersion, adminDeleteConfigVersion,
  adminFeedbackPage, adminAuditPage
} from '../api'
import { toast } from '../utils/toast'

const tabs = [
  { key: 'overview', label: '概览' },
  { key: 'risk', label: '风险事件中心' },
  { key: 'resource', label: '危机资源' },
  { key: 'article', label: '文章审核' },
  { key: 'exercise', label: '自助练习' },
  { key: 'config', label: '配置版本' },
  { key: 'feedback', label: '对话反馈' },
  { key: 'audit', label: '审计日志' }
]
const activeTab = ref('overview')
const overview = ref(null)
const riskEvents = ref([])
const riskPage = ref(1)
const riskTotalPages = ref(1)
const resources = ref([])
const articles = ref([])
const exercises = ref([])
const configVersions = ref([])
const feedbacks = ref([])
const auditLogs = ref([])

const resourceModal = ref(null)
const resourceForm = reactive({ id: null, name: '', phone: '', resourceType: 'hotline', description: '', region: '', enabled: true })
const articleModal = ref(null)
const articleForm = reactive({ id: null, title: '', categoryId: 1, summary: '', content: '', source: '' })
const exerciseModal = ref(null)
const exerciseForm = reactive({ id: null, title: '', summary: '', content: '', minutes: 5, tags: '' })
const configModal = ref(null)
const configForm = reactive({ id: null, configType: 'PROMPT', name: '', version: '', content: '', remark: '' })

const configTypeGroups = computed(() => {
  const meta = [
    { type: 'PROMPT', label: '提示词' },
    { type: 'MODEL', label: '模型' },
    { type: 'RISK_RULE', label: '风险规则' }
  ]
  return meta.map((m) => ({
    ...m,
    items: configVersions.value.filter((v) => v.configType === m.type)
  }))
})

function activeLabel(type) {
  const active = configVersions.value.find((v) => v.configType === type && v.status === 'ACTIVE')
  return active ? active.version : '未配置（使用内置默认）'
}

const levelSummary = computed(() => {
  if (!overview.value?.riskByLevel) return '-'
  const map = overview.value.riskByLevel
  return ['level1', 'level2', 'level3'].map((key) => `${key.slice(-1)}级:${map[key] || 0}`).join(' · ')
})

onMounted(() => {
  loadOverview()
  loadRisk(1)
  loadResources()
  loadArticles()
  loadExercises()
  loadConfigs()
  loadFeedbacks()
  loadAuditLogs()
})

async function loadOverview() {
  try { overview.value = await adminOverview() } catch (e) { toast(e.message || '概览加载失败') }
}

async function loadRisk(page) {
  try {
    const result = await adminRiskPage({ page, pageSize: 10 })
    riskEvents.value = result?.records || []
    riskPage.value = Number(result?.current || 1)
    riskTotalPages.value = Math.max(1, Math.ceil(Number(result?.total || 0) / 10))
  } catch (e) {
    riskEvents.value = []
  }
}

async function updateRiskStatus(event) {
  try {
    await adminRiskUpdateStatus(event.id, { status: event.status })
    toast('处理状态已更新')
    loadOverview()
  } catch (e) {
    toast(e.message || '更新失败')
    loadRisk(riskPage.value)
  }
}

async function loadResources() {
  try { resources.value = await adminCrisisList() || [] } catch (e) { resources.value = [] }
}

function openResourceModal(resource) {
  if (resource) {
    Object.assign(resourceForm, { id: resource.id, name: resource.name, phone: resource.phone, resourceType: resource.resourceType, description: resource.description, region: resource.region, enabled: resource.enabled })
  } else {
    Object.assign(resourceForm, { id: null, name: '', phone: '', resourceType: 'hotline', description: '', region: '', enabled: true })
  }
  resourceModal.value = true
}

async function saveResource() {
  if (!resourceForm.name) { toast('请填写资源名称'); return }
  const payload = {
    name: resourceForm.name,
    phone: resourceForm.phone,
    resourceType: resourceForm.resourceType,
    description: resourceForm.description,
    region: resourceForm.region,
    enabled: resourceForm.enabled
  }
  try {
    if (resourceForm.id) {
      await adminUpdateCrisis(resourceForm.id, payload)
    } else {
      await adminCreateCrisis(payload)
    }
    resourceModal.value = null
    toast('资源已保存')
    loadResources()
  } catch (e) {
    toast(e.message || '保存失败')
  }
}

async function removeResource(resource) {
  try {
    await adminDeleteCrisis(resource.id)
    toast('资源已删除')
    loadResources()
  } catch (e) {
    toast(e.message || '删除失败')
  }
}

async function loadArticles() {
  try {
    const result = await adminArticlePage({ page: 1, pageSize: 50 })
    articles.value = result?.records || []
  } catch (e) {
    articles.value = []
  }
}

function openArticleModal(article) {
  if (article) {
    Object.assign(articleForm, { id: article.id, title: article.title, categoryId: Number(article.categoryId || 1), summary: article.summary || '', content: article.content || '', source: article.source || '' })
  } else {
    Object.assign(articleForm, { id: null, title: '', categoryId: 1, summary: '', content: '', source: '运营团队' })
  }
  articleModal.value = true
}

async function saveArticle() {
  if (!articleForm.title) { toast('请填写标题'); return }
  const payload = {
    categoryId: Number(articleForm.categoryId),
    title: articleForm.title,
    summary: articleForm.summary,
    content: articleForm.content,
    source: articleForm.source,
    minutes: 5,
    status: 'DRAFT'
  }
  try {
    if (articleForm.id) {
      await adminUpdateArticle(articleForm.id, payload)
    } else {
      await adminCreateArticle(payload)
    }
    articleModal.value = null
    toast('文章已保存')
    loadArticles()
  } catch (e) {
    toast(e.message || '保存失败')
  }
}

async function toggleArticle(article) {
  const next = article.status === 'PUBLISHED' ? 'OFFLINE' : 'PUBLISHED'
  try {
    await adminUpdateArticleStatus(article.id, { status: next, auditRemark: next === 'PUBLISHED' ? '审核通过' : undefined })
    toast(next === 'PUBLISHED' ? '文章已发布到知识库' : '文章已下线，学生端不可见')
    loadArticles()
    loadOverview()
  } catch (e) {
    toast(e.message || '操作失败')
  }
}

async function removeArticle(article) {
  try {
    await adminDeleteArticle(article.id)
    toast('文章已删除')
    loadArticles()
  } catch (e) {
    toast(e.message || '删除失败')
  }
}

async function loadFeedbacks() {
  try {
    const result = await adminFeedbackPage({ page: 1, pageSize: 20 })
    feedbacks.value = result?.records || []
  } catch (e) {
    feedbacks.value = []
  }
}

// ---------- 自助练习管理 ----------
async function loadExercises() {
  try {
    const result = await adminExercisePage({ page: 1, pageSize: 50 })
    exercises.value = result?.records || []
  } catch (e) {
    exercises.value = []
  }
}

function openExerciseModal(exercise) {
  if (exercise) {
    Object.assign(exerciseForm, {
      id: exercise.id, title: exercise.title, summary: exercise.summary || '',
      content: exercise.content || '', minutes: exercise.minutes || 5, tags: exercise.tags || ''
    })
  } else {
    Object.assign(exerciseForm, { id: null, title: '', summary: '', content: '', minutes: 5, tags: '' })
  }
  exerciseModal.value = true
}

async function saveExercise() {
  if (!exerciseForm.title) { toast('请填写练习名称'); return }
  const payload = {
    categoryId: 4,
    title: exerciseForm.title,
    summary: exerciseForm.summary,
    content: exerciseForm.content,
    minutes: Number(exerciseForm.minutes) || 5,
    tags: exerciseForm.tags,
    sortOrder: 0,
    status: 'DRAFT'
  }
  try {
    if (exerciseForm.id) {
      await adminUpdateExercise(exerciseForm.id, payload)
    } else {
      await adminCreateExercise(payload)
    }
    exerciseModal.value = null
    toast('练习已保存')
    loadExercises()
  } catch (e) {
    toast(e.message || '保存失败')
  }
}

async function toggleExercise(exercise) {
  const next = exercise.status === 'PUBLISHED' ? 'OFFLINE' : 'PUBLISHED'
  try {
    await adminUpdateExerciseStatus(exercise.id, next)
    toast(next === 'PUBLISHED' ? '练习已发布到学生端' : '练习已下线')
    loadExercises()
    loadOverview()
  } catch (e) {
    toast(e.message || '操作失败')
  }
}

async function removeExercise(exercise) {
  try {
    await adminDeleteExercise(exercise.id)
    toast('练习已删除')
    loadExercises()
  } catch (e) {
    toast(e.message || '删除失败')
  }
}

// ---------- 配置版本管理 ----------
async function loadConfigs() {
  try {
    const result = await adminConfigVersionPage({ page: 1, pageSize: 100 })
    configVersions.value = result?.records || []
  } catch (e) {
    configVersions.value = []
  }
}

function openConfigModal(version) {
  if (version) {
    Object.assign(configForm, {
      id: version.id, configType: version.configType, name: version.name,
      version: version.version, content: version.content || '', remark: version.remark || ''
    })
  } else {
    Object.assign(configForm, { id: null, configType: 'PROMPT', name: '', version: '', content: '', remark: '' })
  }
  configModal.value = true
}

async function saveConfig() {
  if (!configForm.name || !configForm.version) { toast('请填写名称与版本号'); return }
  const payload = {
    configType: configForm.configType,
    name: configForm.name,
    version: configForm.version,
    content: configForm.content,
    remark: configForm.remark
  }
  try {
    if (configForm.id) {
      await adminUpdateConfigVersion(configForm.id, payload)
    } else {
      await adminCreateConfigVersion(payload)
    }
    configModal.value = null
    toast('版本已保存（草稿）')
    loadConfigs()
  } catch (e) {
    toast(e.message || '保存失败')
  }
}

async function activateConfig(version) {
  try {
    await adminActivateConfigVersion(version.id)
    toast(`版本 ${version.version} 已生效`)
    loadConfigs()
  } catch (e) {
    toast(e.message || '生效失败')
  }
}

async function removeConfig(version) {
  try {
    await adminDeleteConfigVersion(version.id)
    toast('版本已删除')
    loadConfigs()
  } catch (e) {
    toast(e.message || '删除失败')
  }
}

async function loadAuditLogs() {
  try {
    const result = await adminAuditPage({ page: 1, pageSize: 20 })
    auditLogs.value = result?.records || []
  } catch (e) {
    auditLogs.value = []
  }
}

function riskLevelLabel(level) {
  return level === 3 ? '危机' : level === 2 ? '预警' : '关注'
}
function statusLabel(status) {
  return { DRAFT: '草稿', PENDING_REVIEW: '待审', PUBLISHED: '已发布', REJECTED: '已驳回', OFFLINE: '已下线' }[status] || status
}
function exerciseStatusLabel(status) {
  return { DRAFT: '草稿', PUBLISHED: '已发布', OFFLINE: '已下线' }[status] || status
}
function versionStatusLabel(status) {
  return { DRAFT: '草稿', ACTIVE: '生效中', DISABLED: '已停用' }[status] || status
}
function helpfulnessLabel(value) {
  return { 1: '有帮助', 2: '一般', 3: '没帮助' }[value] || value
}
function formatDateTime(value) {
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return String(value)
  return new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(d)
}
</script>
