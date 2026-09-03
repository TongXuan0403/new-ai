<template>
    <div class="growth-plans-admin">
        <div class="toolbar">
            <el-select v-model="status" placeholder="状态筛选" clearable style="width: 130px" @change="loadPlans(1)">
                <el-option label="草稿" :value="0" />
                <el-option label="已发布" :value="1" />
                <el-option label="已下线" :value="2" />
            </el-select>
            <el-button type="primary" @click="openDialog()">新增计划</el-button>
        </div>

        <el-table :data="plans" border stripe v-loading="loading">
            <el-table-column prop="title" label="计划标题" min-width="200" show-overflow-tooltip />
            <el-table-column prop="theme" label="主题" width="90" />
            <el-table-column prop="durationDays" label="周期(天)" width="90" />
            <el-table-column prop="reviewer" label="审核人" width="100" />
            <el-table-column label="状态" width="90">
                <template #default="{ row }">
                    <el-tag :type="statusTag(row.status)" size="small">{{ statusName(row.status) }}</el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="viewCount" label="浏览" width="80" />
            <el-table-column label="审核时间" width="150">
                <template #default="{ row }">
                    {{ row.reviewedAt ? row.reviewedAt.replace('T', ' ').slice(0, 16) : '-' }}
                </template>
            </el-table-column>
            <el-table-column label="操作" width="230" fixed="right">
                <template #default="{ row }">
                    <el-button v-if="row.status !== 1" type="success" size="small" @click="changeStatus(row, 1)">发布</el-button>
                    <el-button v-if="row.status === 1" type="warning" size="small" @click="changeStatus(row, 2)">下线</el-button>
                    <el-button type="primary" size="small" @click="openDialog(row)">编辑</el-button>
                    <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
                </template>
            </el-table-column>
        </el-table>

        <el-pagination
            class="pagination"
            background
            layout="total, prev, pager, next"
            :total="total"
            :page-size="10"
            :current-page="query.currentPage"
            @current-change="loadPlans"
        />

        <!-- 新增/编辑弹窗 -->
        <el-dialog v-model="dialog" :title="form.id ? '编辑计划' : '新增计划'" width="640px" top="5vh">
            <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
                <el-form-item label="计划标题" prop="title">
                    <el-input v-model="form.title" />
                </el-form-item>
                <el-form-item label="主题" prop="theme">
                    <el-select v-model="form.theme" style="width: 100%">
                        <el-option label="情绪" value="情绪" />
                        <el-option label="压力" value="压力" />
                        <el-option label="睡眠" value="睡眠" />
                        <el-option label="人际" value="人际" />
                        <el-option label="综合" value="综合" />
                    </el-select>
                </el-form-item>
                <el-form-item label="建议周期">
                    <el-input-number v-model="form.durationDays" :min="1" :max="365" />
                    <span class="unit">天</span>
                </el-form-item>
                <el-form-item label="摘要">
                    <el-input v-model="form.summary" type="textarea" :rows="2" />
                </el-form-item>
                <el-form-item label="计划正文">
                    <el-input
                        v-model="form.content"
                        type="textarea"
                        :rows="10"
                        placeholder="支持 Markdown：标题用 # ，列表用 - 或 1. ，加粗用 **文本**"
                    />
                </el-form-item>
                <el-form-item label="审核人">
                    <el-input v-model="form.reviewer" placeholder="专业审核人姓名或机构" />
                </el-form-item>
                <el-form-item label="初始状态">
                    <el-radio-group v-model="form.status">
                        <el-radio :value="0">草稿</el-radio>
                        <el-radio :value="1">直接发布</el-radio>
                    </el-radio-group>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialog = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="save">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminGrowthPlans, createGrowthPlan, updateGrowthPlan, updateGrowthPlanStatus, deleteGrowthPlan } from '@/api/admin'

const loading = ref(false)
const plans = ref([])
const total = ref(0)
const status = ref(null)
const query = reactive({ currentPage: 1, size: 10 })

const dialog = ref(false)
const saving = ref(false)
const formRef = ref(null)
const form = reactive({ id: null, title: '', theme: '情绪', durationDays: 7, summary: '', content: '', reviewer: '', status: 0 })

const rules = {
    title: [{ required: true, message: '请输入计划标题', trigger: 'blur' }]
}

const statusName = (s) => ({ 0: '草稿', 1: '已发布', 2: '已下线' }[s] || '未知')
const statusTag = (s) => ({ 0: 'info', 1: 'success', 2: 'warning' }[s] || 'info')

const loadPlans = async (page = 1) => {
    loading.value = true
    try {
        query.currentPage = page
        const params = { ...query }
        if (status.value != null) params.status = status.value
        const res = await getAdminGrowthPlans(params)
        plans.value = res.data?.records || []
        total.value = res.data?.total || 0
    } finally {
        loading.value = false
    }
}

const openDialog = (row) => {
    if (row) {
        Object.assign(form, row)
    } else {
        Object.assign(form, { id: null, title: '', theme: '情绪', durationDays: 7, summary: '', content: '', reviewer: '', status: 0 })
    }
    dialog.value = true
}

const save = async () => {
    await formRef.value.validate()
    saving.value = true
    try {
        if (form.id) {
            await updateGrowthPlan(form.id, form)
        } else {
            await createGrowthPlan(form)
        }
        ElMessage.success('保存成功')
        dialog.value = false
        loadPlans(query.currentPage)
    } finally {
        saving.value = false
    }
}

const changeStatus = async (row, newStatus) => {
    await updateGrowthPlanStatus(row.id, { status: newStatus })
    ElMessage.success(newStatus === 1 ? '已发布' : '已下线')
    loadPlans(query.currentPage)
}

const handleDelete = async (row) => {
    await ElMessageBox.confirm(`确定删除计划「${row.title}」吗？`, '删除确认', { type: 'warning' })
    await deleteGrowthPlan(row.id)
    ElMessage.success('已删除')
    loadPlans(query.currentPage)
}

onMounted(() => {
    loadPlans()
})
</script>

<style scoped lang="scss">
.growth-plans-admin {
    padding: 20px;

    .toolbar {
        display: flex;
        gap: 12px;
        margin-bottom: 16px;
    }

    .pagination {
        margin-top: 16px;
        justify-content: flex-end;
    }

    .unit {
        margin-left: 8px;
        color: #9ca3af;
    }
}
</style>
