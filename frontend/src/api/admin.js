import service from '@/utils/request'

export function login(data) {
    return service.post('/user/login', data)
}


export function categoryTree() {
    return service.get('/knowledge/category/tree')
}

export function articlePage(params) {
     return service.get('/knowledge/article/page', { params })
}

export function uploadFile(file, businessInfo) {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('businessType', 'ARTICLE')
    formData.append('businessId', businessInfo.businessId)
    formData.append('businessField', 'cover')

    return service.post('/file/upload', formData, {
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    })
}


export function createArticle(data) {
    return service.post('/knowledge/article', data)
}

export function getArticleDetail(id) {
    return service.get(`/knowledge/article/${id}`)
}


export function updateArticle(id, data) {
    return service.put(`/knowledge/article/${id}`, data)
}

export function changeArticleStatus(id, data) {
    return service.put(`/knowledge/article/${id}/status`, data)
}

export function deleteArticle(id) {
    return service.delete(`/knowledge/article/${id}`)
}

export function getConsultationPage(params) {
    return service.get('/psychological-chat/sessions', { params })
}

export function getSessionDetail(sessionId) {
    return service.get(`/psychological-chat/sessions/${sessionId}/messages`)
}

export function getEmotionalPage(params) {
    return service.get('/emotion-diary/admin/page', { params })
}

export function deleteEmotional(id) {
    return service.delete(`/emotion-diary/admin/${id}`)
}

export function getAnalyticsOverview() {
    return service.get(`/data-analytics/overview`)
}

export function logout() {
    return service.post('/user/logout')
}

// ---------- P2：匿名校园报告 ----------

export function getCampusReport() {
    return service.get('/data-analytics/campus-report')
}

// ---------- P2：心理资源管理 ----------

export function getAdminResources(keyword) {
    return service.get('/counseling/resources/admin/list', { params: { keyword } })
}

export function createResource(data) {
    return service.post('/counseling/resources', data)
}

export function updateResource(id, data) {
    return service.put(`/counseling/resources/${id}`, data)
}

export function deleteResource(id) {
    return service.delete(`/counseling/resources/${id}`)
}

// ---------- P2：预约管理 ----------

export function getAppointmentPage(params) {
    return service.get('/counseling/appointments/admin/page', { params })
}

export function updateAppointmentStatus(id, data) {
    return service.put(`/counseling/appointments/${id}/status`, data)
}

// ---------- P2：成长计划管理 ----------

export function getAdminGrowthPlans(params) {
    return service.get('/growth-plan/page', { params })
}

export function createGrowthPlan(data) {
    return service.post('/growth-plan', data)
}

export function updateGrowthPlan(id, data) {
    return service.put(`/growth-plan/${id}`, data)
}

export function updateGrowthPlanStatus(id, data) {
    return service.put(`/growth-plan/${id}/status`, data)
}

export function deleteGrowthPlan(id) {
    return service.delete(`/growth-plan/${id}`)
}

// ---------- 知识库文档导入（拖拽/选择上传，后端识取文档内容） ----------

export function importDocument(file) {
    const formData = new FormData()
    formData.append('file', file)
    return service.post('/knowledge/article/import', formData, {
        headers: {
            'Content-Type': 'multipart/form-data'
        },
        timeout: 60000
    })
}
