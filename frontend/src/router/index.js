import { createRouter, createWebHistory } from 'vue-router'
import BackendLayout from '@/components/BackendLayout.vue'
import AuthLayout from '@/components/AuthLayout.vue'
import FrontendLayout from '@/components/FrontendLayout.vue'


// 路由配置
const backendRoutes = [
    {
        path: '/back',
        redirect: '/back/dashboard',
        component: BackendLayout,
        children: [
            {
                path: 'dashboard',
                component: () => import('@/views/dashboard.vue'),
                meta: {
                    title: '数据分析',
                    icon: 'PieChart'
                }
            },
            {
                path: 'knowledge',
                component: () => import('@/views/knowledge.vue'),
                meta: {
                    title: '知识文章',
                    icon: 'ChatLineSquare'
                }
            },
            {
                path: 'consultations',
                component: () => import('@/views/consultations.vue'),
                meta: {
                    title: '咨询记录',
                    icon: 'Message'
                }
            },
            {
                path: 'emotional',
                component: () => import('@/views/emotional.vue'),
                meta: {
                    title: '情绪日志',
                    icon: 'User'
                }
            },
            {
                path: 'report',
                component: () => import('@/views/report.vue'),
                meta: {
                    title: '校园报告',
                    icon: 'DataLine'
                }
            },
            {
                path: 'appointments',
                component: () => import('@/views/appointments.vue'),
                meta: {
                    title: '预约管理',
                    icon: 'Calendar'
                }
            },
            {
                path: 'growth-plans',
                component: () => import('@/views/growthPlansAdmin.vue'),
                meta: {
                    title: '成长计划',
                    icon: 'TrendCharts'
                }
            }
        ]
    },
    {
        path: '/auth',
        component: AuthLayout,
        children: [
            {
                path: 'login',
                component: () => import('@/views/login.vue'),
                meta: {
                    title: '登录'
                }
            },
            {
                path: 'register',
                component: () => import('@/views/register.vue'),
                meta: {
                    title: '注册'
                }
            }
        ]
    }
]

const frontendRoutes = [
    {
        path: '/',
        component: FrontendLayout,
        children: [
            {
                path: '',
                component: () => import('@/views/home.vue')
            },
            {
                path: 'consultation',
                component: () => import('@/views/consultation.vue')
            },
            {
                path: 'emotion-diary',
                component: () => import('@/views/emotionDiary.vue')
            },
            {
                path: 'knowledge',
                component: () => import('@/views/frontendKnowledge.vue')
            },
            {
                path: 'knowledge/article/:id',
                component: () => import('@/views/articleDetail.vue'),
                props: true
            },
            {
                path: 'counseling',
                component: () => import('@/views/counseling.vue')
            },
            {
                path: 'growth-plans',
                component: () => import('@/views/growthPlans.vue')
            }
        ]
    },
    // 404 兜底，必须放在最后
    {
        path: '/:pathMatch(.*)*',
        component: () => import('@/views/NotFound.vue')
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes: [ ...backendRoutes, ...frontendRoutes]
})

// 安全读取本地用户信息，损坏或缺失时返回 null 并清理脏数据
const getLocalUserInfo = () => {
    try {
        const raw = localStorage.getItem('userInfo')
        return raw ? JSON.parse(raw) : null
    } catch (e) {
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        return null
    }
}

// 路由前置守卫
router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('token')
    // 当前用户是否登录
    if (token) {
        const userInfo = getLocalUserInfo()
        // 本地用户信息损坏/缺失时，按未登录处理
        if (!userInfo || userInfo.userType == null) {
            localStorage.removeItem('token')
            localStorage.removeItem('userInfo')
            if (to.path.startsWith('/back')) {
                return next('/auth/login')
            }
            return next()
        }
        // 如果是后台用户
        if (userInfo.userType == 2) {
            if (to.path.startsWith('/back')) {
                next()
            } else {
                next('/back/dashboard')
            }
        } else if (userInfo.userType == 1) {
            // 用户端账号只能访问前台路由
            if (to.path.startsWith('/back') || to.path.startsWith('/auth')) {
                next('/')
            } else {
                next()
            }
        } else {
            // 未知角色，放行避免导航挂起
            next()
        }
    } else {
        if (to.path.startsWith('/back')) {
            // 如果是访问后台页面，那么跳转到登录页
            next('/auth/login')
        } else {
            next()
        }
    }
})

export default router
