<template>
  <div class="login-page">
    <!-- 品牌区 -->
    <div class="login-brand">
      <div class="login-logo">C</div>
      <h1>Cornerstone</h1>
      <p>
        文档约束驱动的多 AI 协作 Spring Cloud 基石<br />
        简洁 · 高级 · 易用 · 高性能
      </p>
    </div>

    <!-- 登录卡片 -->
    <el-card class="login-card" shadow="never">
      <div class="login-title">
        <h2>欢迎登录</h2>
        <p>使用你的账号进入管理后台</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        size="large"
        @keyup.enter="handleLogin"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            autocomplete="username"
            :prefix-icon="User"
          />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            autocomplete="current-password"
            show-password
            :prefix-icon="Lock"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-tip">
        测试账号 admin / admin123 · 部署与启动见 cornerstone-web/README.md
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import type { LoginForm } from '@/types/auth'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive<LoginForm>({
  username: '',
  password: '',
})

const rules: FormRules<LoginForm> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    // 防开放重定向：仅接受站内绝对路径（/ 开头且非 // 协议相对地址）
    const raw = (route.query.redirect as string) || '/'
    const redirect = raw.startsWith('/') && !raw.startsWith('//') ? raw : '/'
    router.push(redirect)
  } catch {
    // 登录失败提示已由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  position: relative;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 72px;
  background: var(--cs-login-bg);
  overflow: hidden;
}
/* 背景光晕：两个柔和的品牌色圆，营造纵深又不抢主体 */
.login-page::before,
.login-page::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}
.login-page::before {
  width: 560px;
  height: 560px;
  top: -180px;
  right: -140px;
  background: radial-gradient(circle, rgba(79, 70, 229, 0.32) 0%, transparent 65%);
}
.login-page::after {
  width: 480px;
  height: 480px;
  bottom: -200px;
  left: -120px;
  background: radial-gradient(circle, rgba(124, 58, 237, 0.22) 0%, transparent 65%);
}

/* ---------- 品牌区 ---------- */
.login-brand {
  position: relative;
  z-index: 1;
  color: #fff;
  max-width: 360px;
}
.login-logo {
  width: 54px;
  height: 54px;
  border-radius: 14px;
  background: var(--cs-gradient);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  font-weight: 700;
  margin-bottom: 22px;
  box-shadow: 0 10px 28px rgba(79, 70, 229, 0.45);
}
.login-brand h1 {
  margin: 0 0 12px;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 1px;
}
.login-brand p {
  margin: 0;
  color: rgba(255, 255, 255, 0.55);
  font-size: 14px;
  line-height: 1.9;
}

/* ---------- 登录卡片 ---------- */
.login-card {
  position: relative;
  z-index: 1;
  width: 400px;
  border-radius: 12px;
  box-shadow: 0 24px 56px -16px rgba(0, 0, 0, 0.5);
  padding: 10px 6px;
}
.login-title {
  text-align: center;
  margin-bottom: 26px;
}
.login-title h2 {
  margin: 0 0 6px;
  font-size: 22px;
  color: var(--cs-text);
}
.login-title p {
  margin: 0;
  color: var(--cs-text-secondary);
  font-size: 13px;
}
.login-btn {
  width: 100%;
  margin-top: 4px;
}
.login-tip {
  margin-top: 6px;
  color: var(--cs-text-placeholder);
  font-size: 12px;
  line-height: 1.7;
  text-align: center;
}

/* ---------- 窄屏适配（手机/平板）：品牌区置顶居中，卡片全宽 ---------- */
@media (max-width: 768px) {
  .login-page {
    gap: 28px;
    padding: 24px 16px;
    flex-direction: column;
    justify-content: center;
    overflow-y: auto;
  }
  .login-brand {
    text-align: center;
    max-width: 100%;
  }
  .login-logo {
    margin: 0 auto 16px;
  }
  .login-brand h1 {
    font-size: 24px;
  }
  .login-card {
    width: 100%;
    max-width: 400px;
  }
}
</style>
