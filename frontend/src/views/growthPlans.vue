<template>
    <div class="growth-plans-page">
        <div class="page-header">
            <h2>主题化成长计划</h2>
            <p>由专业人员审核的阶段性自助成长计划，跟着节奏一步步来。</p>
        </div>

        <div class="toolbar">
            <el-radio-group v-model="theme" @change="loadPlans(1)">
                <el-radio-button value="">全部主题</el-radio-button>
                <el-radio-button value="情绪">情绪</el-radio-button>
                <el-radio-button value="压力">压力</el-radio-button>
                <el-radio-button value="睡眠">睡眠</el-radio-button>
                <el-radio-button value="人际">人际</el-radio-button>
            </el-radio-group>
        </div>

        <el-skeleton :loading="loading" animated>
            <div class="plan-grid">
                <el-card v-for="plan in plans" :key="plan.id" class="plan-card" shadow="hover" :body-style="{ padding: '18px' }">
                    <div class="plan-head">
                        <el-tag size="small" type="primary">{{ plan.theme || '综合' }}</el-tag>
                        <span v-if="plan.durationDays" class="duration">约 {{ plan.durationDays }} 天</span>
                    </div>
                    <h3 class="plan-title">{{ plan.title }}</h3>
                    <p class="plan-summary">{{ plan.summary }}</p>
                    <div class="plan-meta">
                        <span>审核：{{ plan.reviewer || '待审核' }}</span>
                        <span>浏览 {{ plan.viewCount }}</span>
                    </div>
                    <div v-if="plan.myProgress != null" class="my-progress">
                        <el-progress :percentage="plan.myProgress" :stroke-width="8" />
                    </div>
                    <div class="plan-actions">
                        <el-button type="primary" size="small" @click="openDetail(plan)">查看计划</el-button>
                        <el-button v-if="plan.myCompleted" size="small" type="success" disabled>已完成</el-button>
                    </div>
                </el-card>
                <el-empty v-if="!plans.length" description="暂无已发布的成长计划" />
            </div>
        </el-skeleton>

        <el-pagination
            v-if="total > 0"
            class="pagination"
            background
            layout="total, prev, pager, next"
            :total="total"
            :page-size="12"
            :current-page="query.currentPage"
            @current-change="loadPlans"
        />

        <!-- 计划详情弹窗 -->
        <el-dialog v-model="detailDialog" :title="currentPlan?.title" width="640px" top="5vh">
            <div v-if="currentPlan" class="plan-detail">
                <div class="detail-meta">
                    <el-tag size="small" type="primary">{{ currentPlan.theme || '综合' }}</el-tag>
                    <span v-if="currentPlan.durationDays">约 {{ currentPlan.durationDays }} 天</span>
                    <span>审核：{{ currentPlan.reviewer || '-' }}</span>
                    <span>浏览 {{ currentPlan.viewCount }}</span>
                </div>
                <div class="detail-content" v-html="renderContent(currentPlan.content)"></div>

                <div v-if="isLoggedIn" class="progress-box">
                    <h4>我的进度</h4>
                    <el-progress :percentage="myProgress" :stroke-width="10" />
                    <div class="progress-actions">
                        <el-slider v-model="myProgress" :step="10" :show-tooltip="true" style="flex: 1" @change="saveProgress" />
                        <el-button type="success" size="small" :disabled="myProgress === 100" @click="markComplete">标记完成</el-button>
                    </div>
                </div>
                <el-alert v-else type="info" :closable="false" show-icon title="登录后可记录并跟踪你的进度" />
            </div>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getGrowthPlans, getGrowthPlanDetail, updatePlanProgress } from '@/api/frontend'

const loading = ref(false)
const plans = ref([])
const total = ref(0)
const theme = ref('')
const query = reactive({ currentPage: 1, size: 12 })

const detailDialog = ref(false)
const currentPlan = ref(null)
const myProgress = ref(0)
const isLoggedIn = ref(false)

const renderContent = (content) => {
    if (!content) return ''
    // 简单将 Markdown 标题/列表转成安全 HTML，其余按段落展示
    return (content || '')
        .replace(/^### (.*)$/gm, '<h4>$1</h4>')
        .replace(/^## (.*)$/gm, '<h3>$1</h3>')
        .replace(/^# (.*)$/gm, '<h3>$1</h3>')
        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
        .replace(/^- (.*)$/gm, '<li>$1</li>')
        .replace(/^\d+\. (.*)$/gm, '<li>$1</li>')
        .replace(/((?:<li>.*<\/li>\n?)+)/g, '<ul>$1</ul>')
        .replace(/\n{2,}/g, '<br/><br/>')
        .replace(/\n/g, '<br/>')
}

const loadPlans = async (page = 1) => {
    loading.value = true
    try {
        query.currentPage = page
        const params = { ...query }
        if (theme.value) params.theme = theme.value
        const res = await getGrowthPlans(params)
        plans.value = res?.records || []
        total.value = res?.total || 0
    } finally {
        loading.value = false
    }
}

const openDetail = async (plan) => {
    const res = await getGrowthPlanDetail(plan.id)
    currentPlan.value = res
    myProgress.value = res?.myProgress || 0
    detailDialog.value = true
}

const saveProgress = async (value) => {
    if (!isLoggedIn.value) {
        ElMessage.warning('请先登录')
        return
    }
    await updatePlanProgress(currentPlan.value.id, { progress: value })
    ElMessage.success('进度已更新')
    loadPlans(query.currentPage)
}

const markComplete = async () => {
    await updatePlanProgress(currentPlan.value.id, { progress: 100 })
    myProgress.value = 100
    ElMessage.success('恭喜完成本计划！')
    loadPlans(query.currentPage)
}

onMounted(() => {
    isLoggedIn.value = localStorage.getItem('token') !== null
    loadPlans()
})
</script>

<style scoped lang="scss">
.growth-plans-page {
    max-width: 1100px;
    margin: 0 auto;
    padding: 24px 16px;

    .page-header {
        h2 { margin: 0 0 6px; color: #333; }
        p { margin: 0 0 18px; color: #6b7280; }
    }

    .toolbar { margin-bottom: 20px; }

    .plan-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
        gap: 16px;

        .plan-card {
            .plan-head {
                display: flex;
                align-items: center;
                gap: 10px;

                .duration { color: #9ca3af; font-size: 13px; }
            }

            .plan-title { margin: 10px 0 6px; font-size: 18px; color: #1f2937; }
            .plan-summary {
                color: #6b7280;
                font-size: 14px;
                line-height: 1.6;
                height: 44px;
                overflow: hidden;
                display: -webkit-box;
                -webkit-line-clamp: 2;
                -webkit-box-orient: vertical;
            }

            .plan-meta {
                display: flex;
                justify-content: space-between;
                color: #9ca3af;
                font-size: 13px;
                margin: 10px 0;
            }

            .my-progress { margin: 8px 0; }
            .plan-actions { text-align: right; }
        }
    }

    .pagination {
        margin-top: 20px;
        justify-content: flex-end;
    }

    .plan-detail {
        .detail-meta {
            display: flex;
            gap: 14px;
            color: #9ca3af;
            font-size: 13px;
            align-items: center;
            margin-bottom: 12px;
        }

        .detail-content {
            color: #374151;
            line-height: 1.8;
            font-size: 15px;

            :deep(h3) { margin: 14px 0 6px; color: #1f2937; }
            :deep(h4) { margin: 10px 0 4px; color: #1f2937; }
            :deep(li) { margin: 4px 0; }
            :deep(ul) { padding-left: 20px; }
        }

        .progress-box {
            margin-top: 20px;
            padding-top: 14px;
            border-top: 1px solid #e5e7eb;

            h4 { margin: 0 0 10px; color: #333; }

            .progress-actions {
                display: flex;
                gap: 14px;
                align-items: center;
                margin-top: 10px;
            }
        }
    }
}

@media (max-width: 640px) {
    .growth-plans-page .plan-grid {
        grid-template-columns: 1fr;
    }
}
</style>
