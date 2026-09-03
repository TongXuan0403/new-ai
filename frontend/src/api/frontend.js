import service from '@/utils/request'

export const register = (data) => {
    return service.post('/user/add', data)
}

export const startSession = (data) => {
    return service.post('/psychological-chat/session/start', data)
}

export const getSessionList = (params) => {
    return service.get('/psychological-chat/sessions', { params })
}

export const deleteSession = (sessionId) => {
    return service.delete(`/psychological-chat/sessions/${sessionId}`)
}

export const getSessionDetail = (sessionId) => {
    return service.get(`/psychological-chat/sessions/${sessionId}/messages`)
}

export const getSessionEmotion = (sessionId) => {
    return service.get(`/psychological-chat/session/${sessionId}/emotion`)
}

export const addEmotionDiary = (data) => {
    return service.post('/emotion-diary', data)
}

export const getKnowledgeList = (params) => {
    return service.get('/knowledge/article/page', { params })
}

export const getKnowledgeDetail = (articleId) => {
    return service.get(`/knowledge/article/${articleId}`)
}

// ---------- P2：心理中心预约/转介 ----------

export const getCounselingResources = () => {
    return service.get('/counseling/resources')
}

export const createAppointment = (data) => {
    return service.post('/counseling/appointments', data)
}

export const getMyAppointments = () => {
    return service.get('/counseling/appointments/my')
}

export const cancelAppointment = (id) => {
    return service.delete(`/counseling/appointments/${id}`)
}

// ---------- P2：主题化成长计划 ----------

export const getGrowthPlans = (params) => {
    return service.get('/growth-plan/page', { params })
}

export const getGrowthPlanDetail = (id) => {
    return service.get(`/growth-plan/${id}`)
}

export const getMyPlans = () => {
    return service.get('/growth-plan/my')
}

export const updatePlanProgress = (id, data) => {
    return service.put(`/growth-plan/${id}/progress`, data)
}
