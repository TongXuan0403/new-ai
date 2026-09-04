<template>
    <div class="report-page">
        <div class="page-header">
            <h2>匿名聚合校园心理健康报告</h2>
            <p class="report-tip">本报告基于全站匿名聚合数据生成，不含任何个人身份信息，生成时间：{{ report?.generatedAt?.replace('T', ' ') }}</p>
        </div>

        <el-row :gutter="16">
            <el-col :xs="12" :sm="8" :md="6" v-for="card in statCards" :key="card.label">
                <el-card class="stat-card" shadow="hover">
                    <p class="stat-label">{{ card.label }}</p>
                    <p class="stat-number">{{ card.value }}</p>
                </el-card>
            </el-col>
        </el-row>

        <el-alert
            v-if="report"
            :type="report.lowMoodRatio >= 20 ? 'warning' : 'success'"
            :closable="false"
            show-icon
            class="mood-alert"
            :title="`低情绪记录占比 ${report.lowMoodRatio}%`"
            :description="report.lowMoodRatio >= 20
                ? '低情绪（评分≤4）占比较高，建议加强心理健康宣导与支持资源触达。'
                : '低情绪记录占比较低，整体情绪状态平稳。'"
        />

        <el-row :gutter="16" style="margin-top: 16px;">
            <el-col :xs="24" :md="12">
                <el-card>
                    <template #header>情绪评分区间分布</template>
                    <div ref="moodChartRef" class="chart-box"></div>
                </el-card>
            </el-col>
            <el-col :xs="24" :md="12">
                <el-card>
                    <template #header>主导情绪分布</template>
                    <div ref="emotionChartRef" class="chart-box"></div>
                </el-card>
            </el-col>
        </el-row>

        <el-card style="margin-top: 16px;">
            <template #header>近 7 天活跃趋势</template>
            <div ref="trendChartRef" class="chart-box"></div>
        </el-card>
    </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { getCampusReport } from '@/api/admin'
import * as echarts from 'echarts'

const report = ref(null)

let moodChart = null
let emotionChart = null
let trendChart = null
const moodChartRef = ref(null)
const emotionChartRef = ref(null)
const trendChartRef = ref(null)

const statCards = computed(() => {
    const o = report.value?.overview
    if (!o) return []
    return [
        { label: '注册用户', value: o.totalUsers },
        { label: '咨询会话', value: o.totalSessions },
        { label: 'AI 对话消息', value: o.totalMessages },
        { label: '情绪日记', value: o.totalDiaries },
        { label: '7 日活跃用户', value: o.activeUsers7d },
        { label: '平均情绪分', value: o.avgMoodScore },
        { label: '成长计划', value: o.totalGrowthPlans },
        { label: '预约申请', value: o.totalAppointments }
    ]
})

const renderCharts = () => {
    if (!report.value) return

    // 情绪评分区间
    moodChart = echarts.init(moodChartRef.value)
    moodChart.setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: 40, right: 16, top: 30, bottom: 40 },
        xAxis: { type: 'category', data: report.value.moodDistribution.map((i) => i.name) },
        yAxis: { type: 'value', minInterval: 1 },
        series: [{
            type: 'bar',
            data: report.value.moodDistribution.map((i) => i.count),
            itemStyle: { color: '#5B8FF9', borderRadius: [4, 4, 0, 0] },
            barWidth: '46%'
        }]
    })

    // 主导情绪
    emotionChart = echarts.init(emotionChartRef.value)
    emotionChart.setOption({
        tooltip: { trigger: 'item' },
        legend: { bottom: 0, type: 'scroll' },
        series: [{
            type: 'pie',
            radius: ['38%', '64%'],
            center: ['50%', '44%'],
            data: report.value.emotionDistribution.map((i) => ({ name: i.name, value: i.count })),
            label: { formatter: '{b}: {c}' }
        }]
    })

    // 7 天趋势
    trendChart = echarts.init(trendChartRef.value)
    trendChart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['咨询会话', '情绪日记', '活跃用户'] },
        grid: { left: 40, right: 16, top: 40, bottom: 40 },
        xAxis: { type: 'category', data: report.value.dailyTrend.map((i) => i.date.slice(5)) },
        yAxis: { type: 'value', minInterval: 1 },
        series: [
            { name: '咨询会话', type: 'line', smooth: true, data: report.value.dailyTrend.map((i) => i.sessionCount) },
            { name: '情绪日记', type: 'line', smooth: true, data: report.value.dailyTrend.map((i) => i.diaryCount) },
            { name: '活跃用户', type: 'line', smooth: true, data: report.value.dailyTrend.map((i) => i.activeUsers) }
        ]
    })
}

const handleResize = () => {
    moodChart?.resize()
    emotionChart?.resize()
    trendChart?.resize()
}

onMounted(async () => {
    const res = await getCampusReport()
    report.value = res
    renderCharts()
    window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
    window.removeEventListener('resize', handleResize)
    moodChart?.dispose()
    emotionChart?.dispose()
    trendChart?.dispose()
})
</script>

<style scoped lang="scss">
.report-page {
    padding: 20px;

    .page-header {
        h2 { margin: 0 0 6px; color: #333; }
        .report-tip { margin: 0 0 16px; color: #9ca3af; font-size: 13px; }
    }

    .stat-card {
        margin-bottom: 16px;
        text-align: center;

        .stat-label { color: #9ca3af; font-size: 13px; margin: 0; }
        .stat-number { font-size: 28px; font-weight: 700; color: #1f2937; margin: 6px 0 0; }
    }

    .mood-alert {
        margin-top: 4px;
    }

    .chart-box {
        width: 100%;
        height: 300px;
    }
}
</style>
