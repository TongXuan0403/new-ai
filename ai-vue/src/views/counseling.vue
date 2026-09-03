<template>
    <div class="counseling-page">
        <div class="page-header">
            <h2>心理中心预约与转介</h2>
            <p>当你需要专业支持时，可以预约学校心理中心，或直接拨打热线电话。</p>
        </div>

        <el-alert
            type="warning"
            :closable="false"
            show-icon
            title="紧急情况请立即求助"
            description="如处于立即危险或正在自伤，请立即拨打 120 或 110，不要等待线上回复。"
            class="urgent-alert"
        />

        <el-skeleton :loading="loading" animated>
            <div class="resource-grid">
                <el-card
                    v-for="res in resources"
                    :key="res.id"
                    class="resource-card"
                    shadow="hover"
                    :body-style="{ padding: '18px' }"
                >
                    <div class="resource-head">
                        <el-tag :type="tagType(res.resourceType)" size="small">{{ typeName(res.resourceType) }}</el-tag>
                        <h3 class="resource-name">{{ res.name }}</h3>
                    </div>
                    <div class="resource-info">
                        <p v-if="res.phone"><span class="label">电话：</span><a :href="`tel:${res.phone}`">{{ res.phone }}</a></p>
                        <p v-if="res.address"><span class="label">地址：</span>{{ res.address }}</p>
                        <p v-if="res.workTime"><span class="label">服务时间：</span>{{ res.workTime }}</p>
                        <p v-if="res.description" class="resource-desc">{{ res.description }}</p>
                    </div>
                    <div class="resource-actions">
                        <el-button type="primary" @click="openAppointment(res)">预约咨询</el-button>
                    </div>
                </el-card>
                <el-empty v-if="!resources.length" description="暂无可用心理资源" />
            </div>
        </el-skeleton>

        <!-- 我的预约 -->
        <div class="my-appointments">
            <h3>我的预约</h3>
            <el-table v-if="myAppointments.length" :data="myAppointments" border stripe>
                <el-table-column prop="resourceName" label="预约机构" min-width="160" />
                <el-table-column label="期望时间" min-width="140">
                    <template #default="{ row }">
                        {{ row.appointmentDate || '-' }} {{ row.appointmentTime || '' }}
                    </template>
                </el-table-column>
                <el-table-column prop="reason" label="原因" min-width="160" show-overflow-tooltip />
                <el-table-column label="状态" width="100">
                    <template #default="{ row }">
                        <el-tag :type="statusTag(row.status)" size="small">{{ statusName(row.status) }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="100">
                    <template #default="{ row }">
                        <el-button
                            v-if="row.status === 0 || row.status === 1"
                            type="danger"
                            size="small"
                            @click="handleCancel(row)"
                        >取消</el-button>
                    </template>
                </el-table-column>
            </el-table>
            <el-empty v-else description="还没有预约记录" />
        </div>

        <!-- 预约弹窗 -->
        <el-dialog v-model="appointmentDialog" :title="`预约：${currentResource?.name || ''}`" width="480px">
            <el-form ref="formRef" :model="appointmentForm" :rules="appointmentRules" label-width="90px">
                <el-form-item label="期望日期" prop="appointmentDate">
                    <el-date-picker
                        v-model="appointmentForm.appointmentDate"
                        type="date"
                        placeholder="选择日期"
                        value-format="YYYY-MM-DD"
                        :disabled-date="disabledDate"
                        style="width: 100%"
                    />
                </el-form-item>
                <el-form-item label="期望时段" prop="appointmentTime">
                    <el-select v-model="appointmentForm.appointmentTime" placeholder="选择时段" style="width: 100%">
                        <el-option label="上午（9:00-12:00）" value="上午" />
                        <el-option label="下午（14:00-17:30）" value="下午" />
                        <el-option label="不指定" value="不指定" />
                    </el-select>
                </el-form-item>
                <el-form-item label="预约原因" prop="reason">
                    <el-input v-model="appointmentForm.reason" type="textarea" :rows="3" placeholder="简单说明希望获得哪些支持（可选）" />
                </el-form-item>
                <el-form-item label="联系方式" prop="contact">
                    <el-input v-model="appointmentForm.contact" placeholder="电话或微信，便于中心联系你（可选）" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="appointmentDialog = false">取消</el-button>
                <el-button type="primary" :loading="submitting" @click="submitAppointment">提交预约</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCounselingResources, createAppointment, getMyAppointments, cancelAppointment } from '@/api/frontend'

const loading = ref(false)
const resources = ref([])
const myAppointments = ref([])
const appointmentDialog = ref(false)
const submitting = ref(false)
const currentResource = ref(null)
const formRef = ref(null)

const appointmentForm = reactive({
    resourceId: null,
    appointmentDate: '',
    appointmentTime: '',
    reason: '',
    contact: ''
})

const appointmentRules = {
    appointmentDate: [{ required: true, message: '请选择期望日期', trigger: 'change' }]
}

const typeName = (t) => ({ SCHOOL: '校内资源', HOTLINE: '求助热线', LOCAL: '本地资源' }[t] || '其他')
const tagType = (t) => (t === 'HOTLINE' ? 'danger' : t === 'SCHOOL' ? 'primary' : 'success')
const statusName = (s) => ({ 0: '待处理', 1: '已确认', 2: '已取消', 3: '已完成' }[s] || '未知')
const statusTag = (s) => ({ 0: 'warning', 1: 'success', 2: 'info', 3: 'primary' }[s] || 'info')

const disabledDate = (date) => date.getTime() < Date.now() - 86400000

const openAppointment = (res) => {
    if (!localStorage.getItem('token')) {
        ElMessage.warning('请先登录后再提交预约')
        return
    }
    currentResource.value = res
    appointmentForm.resourceId = res.id
    appointmentForm.appointmentDate = ''
    appointmentForm.appointmentTime = ''
    appointmentForm.reason = ''
    appointmentForm.contact = ''
    appointmentDialog.value = true
}

const submitAppointment = async () => {
    await formRef.value.validate()
    submitting.value = true
    try {
        await createAppointment(appointmentForm)
        ElMessage.success('预约提交成功，等待心理中心确认')
        appointmentDialog.value = false
        loadMyAppointments()
    } finally {
        submitting.value = false
    }
}

const handleCancel = async (row) => {
    await ElMessageBox.confirm(`确定取消对「${row.resourceName}」的预约吗？`, '取消预约', { type: 'warning' })
    await cancelAppointment(row.id)
    ElMessage.success('已取消预约')
    loadMyAppointments()
}

const loadResources = async () => {
    loading.value = true
    try {
        const res = await getCounselingResources()
        resources.value = res.data || []
    } finally {
        loading.value = false
    }
}

const loadMyAppointments = async () => {
    if (!localStorage.getItem('token')) return
    const res = await getMyAppointments()
    myAppointments.value = res.data || []
}

onMounted(() => {
    loadResources()
    loadMyAppointments()
})
</script>

<style scoped lang="scss">
.counseling-page {
    max-width: 1100px;
    margin: 0 auto;
    padding: 24px 16px;

    .page-header {
        h2 { margin: 0 0 6px; color: #333; }
        p { margin: 0 0 18px; color: #6b7280; }
    }

    .urgent-alert {
        margin-bottom: 20px;
    }

    .resource-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
        gap: 16px;

        .resource-card {
            .resource-head {
                display: flex;
                align-items: center;
                gap: 8px;

                .resource-name { margin: 0; font-size: 17px; color: #1f2937; }
            }

            .resource-info {
                margin: 12px 0;
                color: #4b5563;
                font-size: 14px;

                p { margin: 6px 0; }
                .label { color: #9ca3af; }
                .resource-desc { color: #6b7280; line-height: 1.6; }
            }

            .resource-actions { text-align: right; }
        }
    }

    .my-appointments {
        margin-top: 36px;

        h3 { color: #333; margin-bottom: 12px; }
    }
}

@media (max-width: 640px) {
    .counseling-page .resource-grid {
        grid-template-columns: 1fr;
    }
}
</style>
