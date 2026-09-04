<template>
    <div class="appointments-page">
        <h2>预约与心理资源管理</h2>

        <el-tabs v-model="activeTab">
            <!-- 预约处理 -->
            <el-tab-pane label="预约处理" name="appointments">
                <div class="toolbar">
                    <el-input
                        v-model="query.keyword"
                        placeholder="搜索用户名 / 机构 / 原因"
                        clearable
                        style="width: 240px"
                        @keyup.enter="loadAppointments(1)"
                        @clear="loadAppointments(1)"
                    />
                    <el-select v-model="query.status" placeholder="状态筛选" clearable style="width: 130px" @change="loadAppointments(1)">
                        <el-option label="待处理" :value="0" />
                        <el-option label="已确认" :value="1" />
                        <el-option label="已取消" :value="2" />
                        <el-option label="已完成" :value="3" />
                    </el-select>
                    <el-button type="primary" @click="loadAppointments(1)">查询</el-button>
                </div>

                <el-table :data="appointments" border stripe v-loading="loading">
                    <el-table-column prop="userName" label="用户" width="100" />
                    <el-table-column prop="resourceName" label="预约机构" min-width="160" />
                    <el-table-column label="期望时间" min-width="140">
                        <template #default="{ row }">
                            {{ row.appointmentDate || '-' }} {{ row.appointmentTime || '' }}
                        </template>
                    </el-table-column>
                    <el-table-column prop="reason" label="原因" min-width="140" show-overflow-tooltip />
                    <el-table-column prop="contact" label="联系方式" min-width="120" show-overflow-tooltip />
                    <el-table-column label="状态" width="90">
                        <template #default="{ row }">
                            <el-tag :type="statusTag(row.status)" size="small">{{ statusName(row.status) }}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" width="180" fixed="right">
                        <template #default="{ row }">
                            <el-button v-if="row.status === 0" type="success" size="small" @click="handleStatus(row, 1)">确认</el-button>
                            <el-button v-if="row.status === 0 || row.status === 1" type="primary" size="small" @click="openProcess(row)">处理</el-button>
                        </template>
                    </el-table-column>
                </el-table>

                <el-pagination
                    class="pagination"
                    background
                    layout="total, prev, pager, next"
                    :total="total"
                    :page-size="query.size"
                    :current-page="query.currentPage"
                    @current-change="loadAppointments"
                />
            </el-tab-pane>

            <!-- 心理资源管理 -->
            <el-tab-pane label="心理资源管理" name="resources">
                <div class="toolbar">
                    <el-input
                        v-model="resourceKeyword"
                        placeholder="搜索资源名称 / 说明"
                        clearable
                        style="width: 240px"
                        @keyup.enter="loadResources"
                        @clear="loadResources"
                    />
                    <el-button type="primary" @click="openResourceDialog()">新增资源</el-button>
                </div>

                <el-table :data="resources" border stripe v-loading="resourceLoading">
                    <el-table-column prop="name" label="资源名称" min-width="170" />
                    <el-table-column label="类型" width="100">
                        <template #default="{ row }">
                            <el-tag :type="resourceTagType(row.resourceType)" size="small">{{ resourceTypeName(row.resourceType) }}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column prop="phone" label="电话" width="120" />
                    <el-table-column prop="workTime" label="服务时间" min-width="150" />
                    <el-table-column label="状态" width="90">
                        <template #default="{ row }">
                            <el-tag :type="row.enabled === 1 ? 'success' : 'info'" size="small">{{ row.enabled === 1 ? '启用' : '停用' }}</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" width="150" fixed="right">
                        <template #default="{ row }">
                            <el-button type="primary" size="small" @click="openResourceDialog(row)">编辑</el-button>
                            <el-button type="danger" size="small" @click="handleDeleteResource(row)">删除</el-button>
                        </template>
                    </el-table-column>
                </el-table>
            </el-tab-pane>
        </el-tabs>

        <!-- 处理预约弹窗 -->
        <el-dialog v-model="processDialog" title="处理预约" width="420px">
            <el-form label-width="70px">
                <el-form-item label="处理结果">
                    <el-radio-group v-model="processForm.status">
                        <el-radio :value="1">已确认</el-radio>
                        <el-radio :value="3">已完成</el-radio>
                        <el-radio :value="2">已取消</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="处理备注">
                    <el-input v-model="processForm.remark" type="textarea" :rows="3" placeholder="可填写确认时间或处理说明" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="processDialog = false">取消</el-button>
                <el-button type="primary" @click="submitProcess">保存</el-button>
            </template>
        </el-dialog>

        <!-- 新增/编辑资源弹窗 -->
        <el-dialog v-model="resourceDialog" :title="resourceForm.id ? '编辑资源' : '新增资源'" width="500px">
            <el-form ref="resourceFormRef" :model="resourceForm" :rules="resourceRules" label-width="80px">
                <el-form-item label="资源名称" prop="name">
                    <el-input v-model="resourceForm.name" placeholder="如：学校心理健康教育中心" />
                </el-form-item>
                <el-form-item label="类型" prop="resourceType">
                    <el-select v-model="resourceForm.resourceType" style="width: 100%">
                        <el-option label="校内资源" value="SCHOOL" />
                        <el-option label="求助热线" value="HOTLINE" />
                        <el-option label="本地资源" value="LOCAL" />
                    </el-select>
                </el-form-item>
                <el-form-item label="联系电话">
                    <el-input v-model="resourceForm.phone" />
                </el-form-item>
                <el-form-item label="地址">
                    <el-input v-model="resourceForm.address" />
                </el-form-item>
                <el-form-item label="服务时间">
                    <el-input v-model="resourceForm.workTime" placeholder="如：周一至周五 9:00-17:00" />
                </el-form-item>
                <el-form-item label="说明">
                    <el-input v-model="resourceForm.description" type="textarea" :rows="3" />
                </el-form-item>
                <el-form-item label="排序">
                    <el-input-number v-model="resourceForm.sortNo" :min="0" />
                </el-form-item>
                <el-form-item label="启用">
                    <el-switch v-model="resourceForm.enabled" :active-value="1" :inactive-value="0" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="resourceDialog = false">取消</el-button>
                <el-button type="primary" :loading="resourceSaving" @click="saveResource">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
    getAppointmentPage,
    updateAppointmentStatus,
    getAdminResources,
    createResource,
    updateResource,
    deleteResource
} from '@/api/admin'

const activeTab = ref('appointments')

// 预约处理
const loading = ref(false)
const appointments = ref([])
const total = ref(0)
const query = reactive({ keyword: '', status: null, currentPage: 1, size: 10 })
const processDialog = ref(false)
const processForm = reactive({ id: null, status: 1, remark: '' })

const statusName = (s) => ({ 0: '待处理', 1: '已确认', 2: '已取消', 3: '已完成' }[s] || '未知')
const statusTag = (s) => ({ 0: 'warning', 1: 'success', 2: 'info', 3: 'primary' }[s] || 'info')

const loadAppointments = async (page = 1) => {
    loading.value = true
    try {
        query.currentPage = page
        const res = await getAppointmentPage(query)
        appointments.value = res?.records || []
        total.value = res?.total || 0
    } finally {
        loading.value = false
    }
}

const handleStatus = async (row, status) => {
    await updateAppointmentStatus(row.id, { status })
    ElMessage.success('操作成功')
    loadAppointments(query.currentPage)
}

const openProcess = (row) => {
    processForm.id = row.id
    processForm.status = row.status === 1 ? 3 : 1
    processForm.remark = ''
    processDialog.value = true
}

const submitProcess = async () => {
    await updateAppointmentStatus(processForm.id, { status: processForm.status, remark: processForm.remark })
    ElMessage.success('已保存处理结果')
    processDialog.value = false
    loadAppointments(query.currentPage)
}

// 资源管理
const resourceLoading = ref(false)
const resources = ref([])
const resourceKeyword = ref('')
const resourceDialog = ref(false)
const resourceSaving = ref(false)
const resourceFormRef = ref(null)
const resourceForm = reactive({ id: null, name: '', resourceType: 'SCHOOL', phone: '', address: '', workTime: '', description: '', sortNo: 0, enabled: 1 })

const resourceRules = {
    name: [{ required: true, message: '请输入资源名称', trigger: 'blur' }]
}

const resourceTypeName = (t) => ({ SCHOOL: '校内资源', HOTLINE: '求助热线', LOCAL: '本地资源' }[t] || '其他')
const resourceTagType = (t) => (t === 'HOTLINE' ? 'danger' : t === 'SCHOOL' ? 'primary' : 'success')

const loadResources = async () => {
    resourceLoading.value = true
    try {
        const res = await getAdminResources(resourceKeyword.value)
        resources.value = res || []
    } finally {
        resourceLoading.value = false
    }
}

const openResourceDialog = (row) => {
    if (row) {
        Object.assign(resourceForm, row)
    } else {
        Object.assign(resourceForm, { id: null, name: '', resourceType: 'SCHOOL', phone: '', address: '', workTime: '', description: '', sortNo: 0, enabled: 1 })
    }
    resourceDialog.value = true
}

const saveResource = async () => {
    await resourceFormRef.value.validate()
    resourceSaving.value = true
    try {
        if (resourceForm.id) {
            await updateResource(resourceForm.id, resourceForm)
        } else {
            await createResource(resourceForm)
        }
        ElMessage.success('保存成功')
        resourceDialog.value = false
        loadResources()
    } finally {
        resourceSaving.value = false
    }
}

const handleDeleteResource = async (row) => {
    await ElMessageBox.confirm(`确定删除资源「${row.name}」吗？`, '删除确认', { type: 'warning' })
    await deleteResource(row.id)
    ElMessage.success('已删除')
    loadResources()
}

onMounted(() => {
    loadAppointments()
    loadResources()
})
</script>

<style scoped lang="scss">
.appointments-page {
    padding: 20px;

    h2 { margin-top: 0; color: #333; }

    .toolbar {
        display: flex;
        gap: 12px;
        margin-bottom: 16px;
        flex-wrap: wrap;
    }

    .pagination {
        margin-top: 16px;
        justify-content: flex-end;
    }
}
</style>
