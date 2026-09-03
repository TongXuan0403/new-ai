<template>
    <div class="auth-wrapper">
        <!-- 左侧品牌展示区域 -->
        <div class="brand-section">
            <div class="brand-content">
                <h1>心理AI助手</h1>
                <p>每个深夜，每个焦虑的时刻，我们都在这里。不必独自承受，让心与心的连接温暖您的每一天</p>
                <div class="brand-icon">
                    <el-image :src="iconUrl" style="width: 120px;height: 120px;"></el-image>
                </div>
            </div>
        </div>
        <!-- 右侧表单区域 -->
        <div class="form-section">
            <div class="form-container">
                <router-view></router-view>
            </div>
        </div>
    </div>
</template>
<script setup>
const iconUrl = new URL('@/assets/images/robot-fill.png', import.meta.url).href
</script>
<style scoped lang="scss">
.auth-wrapper {
    display: flex;
    height: 100vh;
    min-height: 100vh;
    // 左侧品牌区域样式
    .brand-section {
        width: 50%;
        min-width: 0;
        background: linear-gradient(135deg, #4a9c8c 0%, #3a8a7c 100%);
        color: white;
        padding: 0 80px;
        display: flex;
        flex-direction: column;
        justify-content: center;

        .brand-content {
            max-width: 400px;

            h1 {
                font-size: 48px;
                font-weight: 700;
                margin-bottom: 20px;
            }

            p {
                font-size: 18px;
                line-height: 1.6;
                opacity: 0.9;
                margin-bottom: 40px;
            }

            .brand-icon {
                opacity: 0.2;
            }
        }
    }

    // 右侧表单区域样式
    .form-section {
        flex: 1;
        min-width: 0;
        display: flex;
        align-items: center;
        justify-content: center;
        background-color: #f1f4f6;

        .form-container {
            width: 384px;
            max-width: calc(100% - 32px);
        }
    }
}

// 窄屏（平板竖屏/手机）：隐藏品牌区，表单占满整屏
@media (max-width: 900px) {
    .auth-wrapper {
        .brand-section {
            display: none;
        }
        .form-section {
            flex: 1;
        }
    }
}
</style>
