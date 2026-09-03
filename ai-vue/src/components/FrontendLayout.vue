<template>
    <div class="frontend-layout">
        <!-- 无障碍：跳转到主要内容 -->
        <a href="#main-content" class="skip-link" aria-label="跳转到主要内容">跳到主要内容</a>
        <div class="navbar-container">
            <div class="brand-section">
                <el-image style="width: 50px; height: 50px" :src="iconUrl" alt="品牌logo" class="brand-logo" />
                <h1 class="brand-name">心理健康AI助手</h1>
            </div>
            <nav class="nav-section" aria-label="主导航" :class="{ 'nav-open': menuOpen }">
                <router-link to="/" class="nav-link" @click="closeMenu">首页</router-link>
                <router-link to="/consultation" class="nav-link" v-if="isLoggedIn" @click="closeMenu">AI咨询</router-link>
                <router-link to="/emotion-diary" class="nav-link" v-if="isLoggedIn" @click="closeMenu">情绪日记</router-link>
                <router-link to="/knowledge" class="nav-link" @click="closeMenu">知识库</router-link>
                <router-link to="/growth-plans" class="nav-link" @click="closeMenu">成长计划</router-link>
                <router-link to="/counseling" class="nav-link" @click="closeMenu">心理资源</router-link>
                <el-button v-if="isLoggedIn" class="logout-btn" @click="handleLogout">退出登录</el-button>
                <template v-else>
                    <router-link to="/auth/login" class="nav-link" @click="closeMenu">登录</router-link>
                    <router-link to="/auth/register" class="nav-link" @click="closeMenu">
                        <el-button type="primary">注册</el-button>
                    </router-link>
                </template>
            </nav>
            <!-- 移动端菜单开关 -->
            <button
                class="menu-toggle"
                :aria-expanded="menuOpen"
                aria-label="切换导航菜单"
                @click="menuOpen = !menuOpen"
            >
                <span class="menu-bar"></span>
                <span class="menu-bar"></span>
                <span class="menu-bar"></span>
            </button>
        </div>
        <main id="main-content" class="main-content">
            <router-view></router-view>
        </main>
        <footer class="footer-container">
            <div class="footer-bottom">
                <p>&copy; 2026 心理健康AI助手. All rights reserved.</p>
                <p class="footer-note">AI 支持不构成医疗建议，如处于紧急情况请拨打 120 / 12356</p>
            </div>
        </footer>
    </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { logout } from '@/api/admin'
import { useRouter } from 'vue-router'

const router = useRouter()

const iconUrl = new URL('@/assets/images/机器人.png', import.meta.url).href

const isLoggedIn = ref(false)
const menuOpen = ref(false)

const closeMenu = () => {
    menuOpen.value = false
}

// 登出：即使后端接口失败，也清除本地登录态并跳转
const handleLogout = () => {
    logout().finally(() => {
        // 清除缓存
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        // 跳转到登录页
        router.push('/auth/login')
    })
}

onMounted(() => {
   isLoggedIn.value = localStorage.getItem('token') !== null
})
</script>
<style scoped lang="scss">
.frontend-layout {
    background-color: #fff;
    min-height: 100vh;
    display: flex;
    flex-direction: column;

    .skip-link {
        position: absolute;
        left: -9999px;
        top: 0;
        background: #4A90E2;
        color: #fff;
        padding: 8px 16px;
        z-index: 1000;
        border-radius: 0 0 8px 0;

        &:focus {
            left: 0;
        }
    }

    .navbar-container {
        max-width: 1200px;
        width: 100%;
        margin: 0 auto;
        padding: 10px;
        display: flex;
        align-items: center;
        justify-content: space-between;
        position: relative;

        .brand-section {
            display: flex;
            align-items: center;

            .brand-name {
                margin-left: 10px;
                font-size: 24px;
                font-weight: 600;
                color: #333;
            }
        }

        .nav-section {
            display: flex;
            align-items: center;
            gap: 30px;
            flex-wrap: wrap;

            .nav-link {
                color: #4b5563;
                font-size: 16px;
                font-weight: 500;
                text-decoration: none;

                &:hover,
                &.router-link-active {
                    color: #4A90E2;
                }
            }
        }

        .menu-toggle {
            display: none;
            flex-direction: column;
            gap: 5px;
            background: none;
            border: none;
            cursor: pointer;
            padding: 8px;

            .menu-bar {
                width: 24px;
                height: 2px;
                background: #333;
                border-radius: 2px;
            }
        }
    }

    .main-content {
        flex: 1;
        width: 100%;
    }

    .footer-container {
        background: #1f2937;
        color: white;
        padding: 15px 0;
        margin-top: auto;

        .footer-bottom {
            max-width: 1200px;
            margin: 0 auto;
            padding: 0 10px;
            text-align: center;

            .footer-note {
                margin-top: 6px;
                font-size: 13px;
                color: #9ca3af;
            }
        }
    }

    // 移动端适配（P2-4 多端）
    @media (max-width: 860px) {
        .navbar-container {
            flex-wrap: wrap;

            .menu-toggle {
                display: flex;
            }

            .nav-section {
                display: none;
                width: 100%;
                flex-direction: column;
                align-items: flex-start;
                gap: 14px;
                padding: 12px 4px;

                &.nav-open {
                    display: flex;
                }
            }
        }

        .brand-name {
            font-size: 20px;
        }
    }
}
</style>
