import request from './request'

// ---------- 用户 ----------
export const login = (payload) => request.post('/user/login', payload)
export const register = (payload) => request.post('/user/add', payload)
export const logout = () => request.post('/user/logout')

// ---------- 同意 ----------
export const getConsentStatus = () => request.get('/consents/current')
export const submitConsent = (payload) => request.post('/consents', payload)
export const revokeConsent = () => request.post('/consents/revoke')

// ---------- 隐私与数据 ----------
export const getPrivacyProfile = () => request.get('/user/privacy/profile')
export const exportData = () => request.post('/user/privacy/export')
export const submitDeletion = (reason) => request.post('/user/privacy/deletion-request', { reason })
export const getDeletionStatus = () => request.get('/user/privacy/deletion-request')
export const cancelDeletion = () => request.post('/user/privacy/deletion-request/cancel')

// ---------- 情绪日记 ----------
export const createDiary = (payload) => request.post('/emotion-diary', payload)
export const pageDiaries = (params) => request.get('/emotion-diary/page', { params })
export const getDiary = (id) => request.get(`/emotion-diary/${id}`)
export const updateDiary = (id, payload) => request.put(`/emotion-diary/${id}`, payload)
export const deleteDiary = (id) => request.delete(`/emotion-diary/${id}`)
export const getDiaryTrend = (days) => request.get('/emotion-diary/trend', { params: { days } })

// ---------- 会话与消息 ----------
export const startSession = (payload) => request.post('/psychological-chat/session/start', payload)
export const listSessions = (keyword) => request.get('/psychological-chat/sessions', { params: { keyword } })
export const getSession = (sessionId) => request.get(`/psychological-chat/sessions/${sessionId}`)
export const deleteSession = (sessionId) => request.delete(`/psychological-chat/sessions/${sessionId}`)
export const sendChatMessage = (payload) => request.post('/psychological-chat/messages', payload)

// ---------- 知识库（公开） ----------
export const listKnowledge = (params) => request.get('/knowledge', { params })
export const getArticle = (id) => request.get(`/knowledge/article/${id}`)

// ---------- 危机资源（公开启用） ----------
export const listCrisisResources = () => request.get('/crisis-resources')

// ---------- 对话反馈 ----------
export const submitFeedback = (payload) => request.post('/chat-feedback', payload)

// ---------- 管理端 ----------
export const adminOverview = () => request.get('/admin/data-analytics/overview')
export const adminArticlePage = (params) => request.get('/admin/knowledge/article/page', { params })
export const adminCreateArticle = (payload) => request.post('/admin/knowledge/article', payload)
export const adminUpdateArticle = (id, payload) => request.put(`/admin/knowledge/article/${id}`, payload)
export const adminDeleteArticle = (id) => request.delete(`/admin/knowledge/article/${id}`)
export const adminUpdateArticleStatus = (id, payload) => request.put(`/admin/knowledge/article/${id}/status`, payload)
export const adminRiskPage = (params) => request.get('/admin/risk-events/page', { params })
export const adminRiskUpdateStatus = (id, payload) => request.put(`/admin/risk-events/${id}/status`, payload)
export const adminCrisisList = () => request.get('/admin/crisis-resources')
export const adminCreateCrisis = (payload) => request.post('/admin/crisis-resources', payload)
export const adminUpdateCrisis = (id, payload) => request.put(`/admin/crisis-resources/${id}`, payload)
export const adminDeleteCrisis = (id) => request.delete(`/admin/crisis-resources/${id}`)
export const adminFeedbackPage = (params) => request.get('/admin/chat-feedback/page', { params })
export const adminAuditPage = (params) => request.get('/admin/audit-logs/page', { params })
