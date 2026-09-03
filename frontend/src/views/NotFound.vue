<template>
    <div class="not-found">
        <div class="code">404</div>
        <p class="text">抱歉，您访问的页面不存在</p>
        <el-button type="primary" @click="goHome">返回首页</el-button>
    </div>
</template>
<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
const goHome = () => {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')
    if (userInfo?.userType === 2) {
        router.push('/back/dashboard')
    } else {
        router.push('/')
    }
}
</script>
<style scoped lang="scss">
.not-found {
    height: 100vh;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 16px;
    background-color: #f1f4f6;
    .code {
        font-size: 80px;
        font-weight: 700;
        color: #4a9c8c;
    }
    .text {
        font-size: 16px;
        color: #6b7280;
        margin-bottom: 10px;
    }
}
</style>
