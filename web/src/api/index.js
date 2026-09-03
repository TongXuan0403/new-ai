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
export const listKnowledgeTags = () => request.get('/knowledge/tags')
export const listRecommendArticles = (limit) => request.get('/knowledge/recommend', { params: { limit } })

// ---------- 文章收藏（登录后） ----------
export const addFavorite = (articleId) => request.post(`/article-favorites/${articleId}`)
export const removeFavorite = (articleId) => request.delete(`/article-favorites/${articleId}`)
export const pageMyFavorites = (params) => request.get('/article-favorites', { params })
export const myFavoriteIds = () => request.get('/article-favorites/ids')

// ---------- 自助练习 ----------
export const listExercises = (params) => request.get('/exercises', { params })
export const getExercise = (id) => request.get(`/exercises/${id}`)
export const completeExercise = (id, payload) => request.post(`/exercises/${id}/complete`, payload)
export const myExerciseCompletions = () => request.get('/exercises/my/completions')

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

// ---------- 管理端 · 自助练习 ----------
export const adminExercisePage = (params) => request.get('/admin/exercises/page', { params })
export const adminCreateExercise = (payload) => request.post('/admin/exercises', payload)
export const adminUpdateExercise = (id, payload) => request.put(`/admin/exercises/${id}`, payload)
export const adminDeleteExercise = (id) => request.delete(`/admin/exercises/${id}`)
export const adminUpdateExerciseStatus = (id, status) => request.put(`/admin/exercises/${id}/status`, null, { params: { status } })

// ---------- 管理端 · 系统配置版本化 ----------
export const adminConfigVersionPage = (params) => request.get('/admin/system-config/versions', { params })
export const adminConfigVersionDetail = (id) => request.get(`/admin/system-config/versions/${id}`)
export const adminCreateConfigVersion = (payload) => request.post('/admin/system-config/versions', payload)
export const adminUpdateConfigVersion = (id, payload) => request.put(`/admin/system-config/versions/${id}`, payload)
export const adminActivateConfigVersion = (id) => request.post(`/admin/system-config/versions/${id}/activate`)
export const adminDisableConfigVersion = (id) => request.post(`/admin/system-config/versions/${id}/disable`)
export const adminDeleteConfigVersion = (id) => request.delete(`/admin/system-config/versions/${id}`)
