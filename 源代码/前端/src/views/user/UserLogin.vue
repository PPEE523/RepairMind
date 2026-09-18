<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <div class="login-brand">
      <div class="brand-content">
        <div class="brand-logo">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/>
          </svg>
        </div>
        <h1 class="brand-title">智能设备检修系统</h1>
        <p class="brand-desc">
          基于 AI 知识库的智能设备故障诊断与标准化检修作业辅助平台
        </p>
        <div class="brand-features">
          <div class="bf-item">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
            <span>多模态智能故障检索</span>
          </div>
          <div class="bf-item">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
            <span>标准化分步检修指引</span>
          </div>
          <div class="bf-item">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
            <span>历史案例知识库沉淀</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧登录表单 -->
    <div class="login-form-side">
      <div class="form-container">
        <div class="form-header">
          <h2>欢迎回来</h2>
          <p>登录您的账户以继续使用</p>
        </div>

        <form class="login-form" @submit.prevent="handleLogin">
          <!-- 用户名 -->
          <div class="field-group">
            <label class="field-label" for="username">用户名</label>
            <div class="field-input-wrap" :class="{ error: errors.username }">
              <svg class="field-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
              </svg>
              <input
                id="username"
                v-model="loginForm.username"
                type="text"
                class="field-input"
                placeholder="请输入用户名"
                autocomplete="username"
                @input="errors.username = ''"
              />
            </div>
            <span v-if="errors.username" class="field-error">{{ errors.username }}</span>
          </div>

          <!-- 密码 -->
          <div class="field-group">
            <label class="field-label" for="password">密码</label>
            <div class="field-input-wrap" :class="{ error: errors.password }">
              <svg class="field-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>
              </svg>
              <input
                id="password"
                v-model="loginForm.password"
                :type="showPwd ? 'text' : 'password'"
                class="field-input"
                placeholder="请输入密码"
                autocomplete="current-password"
                @input="errors.password = ''"
              />
              <button type="button" class="pwd-toggle" @click="showPwd = !showPwd" tabindex="-1">
                <svg v-if="!showPwd" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/><path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
              </button>
            </div>
            <span v-if="errors.password" class="field-error">{{ errors.password }}</span>
          </div>

          <!-- 错误提示 -->
          <div v-if="loginError" class="login-error">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
            {{ loginError }}
          </div>

          <!-- 提交 -->
          <button type="submit" class="submit-btn" :disabled="loading">
            <span v-if="loading" class="btn-spinner"></span>
            <span v-else>登 录</span>
          </button>
        </form>

        <!-- 底部链接 -->
        <div class="form-footer">
          <span>管理员入口</span>
          <router-link to="/login" class="admin-link">
            前往管理后台
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="m9 18 6-6-6-6"/>
            </svg>
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const showPwd = ref(false)
const loginError = ref('')

const loginForm = reactive({
  username: '',
  password: ''
})

const errors = reactive({
  username: '',
  password: ''
})

const validate = (): boolean => {
  let valid = true
  if (!loginForm.username.trim()) {
    errors.username = '请输入用户名'
    valid = false
  } else if (loginForm.username.trim().length < 3) {
    errors.username = '用户名至少 3 位'
    valid = false
  }

  if (!loginForm.password) {
    errors.password = '请输入密码'
    valid = false
  } else if (loginForm.password.length < 6) {
    errors.password = '密码至少 6 位'
    valid = false
  }
  return valid
}

const handleLogin = async () => {
  loginError.value = ''
  if (!validate()) return

  loading.value = true
  try {
    const res: any = await login({
      username: loginForm.username.trim(),
      password: loginForm.password
    })
    userStore.setLoginInfo(res.data)
    localStorage.removeItem('menus')
    ElMessage.success('登录成功')
    router.push('/user/home')
  } catch (error: any) {
    const msg = error?.response?.data?.msg || error?.message || '登录失败，请检查用户名和密码'
    loginError.value = msg
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  min-height: 100vh;
  min-height: 100dvh;
}

/* ============================================================
   左侧品牌区
   ============================================================ */
.login-brand {
  display: none;
  flex: 1;
  background: linear-gradient(165deg, #1e3a8a 0%, #2563eb 40%, #1d4ed8 100%);
  position: relative;
  overflow: hidden;
}

.login-brand::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 15% 85%, rgba(249, 115, 22, 0.15), transparent 55%),
    radial-gradient(ellipse at 85% 15%, rgba(59, 130, 246, 0.25), transparent 55%),
    radial-gradient(ellipse at 50% 50%, rgba(255, 255, 255, 0.03), transparent 70%);
}

.brand-content {
  position: relative;
  z-index: 1;
  padding: var(--space-16);
  display: flex;
  flex-direction: column;
  justify-content: center;
  height: 100%;
  max-width: 480px;
  margin: 0 auto;
}

.brand-logo {
  width: 72px;
  height: 72px;
  background: rgba(255, 255, 255, 0.10);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--space-8);
  backdrop-filter: blur(12px);
  transition: transform var(--transition-base);
}

.brand-logo:hover {
  transform: scale(1.04);
}

.brand-title {
  font-size: var(--text-3xl);
  font-weight: var(--weight-bold);
  color: #fff;
  margin: 0 0 var(--space-4);
  letter-spacing: -0.02em;
  line-height: var(--leading-tight);
}

.brand-desc {
  font-size: var(--text-base);
  color: rgba(255, 255, 255, 0.68);
  margin: 0 0 var(--space-10);
  line-height: var(--leading-relaxed);
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.bf-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  color: rgba(255, 255, 255, 0.78);
  font-size: var(--text-sm);
  transition: color var(--transition-base);
}

.bf-item:hover {
  color: #fff;
}

.bf-item svg {
  flex-shrink: 0;
  color: #f97316;
}

/* ============================================================
   右侧登录表单
   ============================================================ */
.login-form-side {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--surface-card);
  padding: var(--space-6);
}

.form-container {
  width: 100%;
  max-width: 400px;
}

.form-header {
  margin-bottom: var(--space-8);
}

.form-header h2 {
  font-size: var(--text-2xl);
  font-weight: var(--weight-bold);
  color: var(--text-primary);
  margin: 0 0 var(--space-2);
  letter-spacing: var(--tracking-tight);
}

.form-header p {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  margin: 0;
}

/* 表单字段 */
.login-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.field-label {
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  color: var(--text-primary);
}

.field-input-wrap {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  border: 1.5px solid var(--border-base);
  border-radius: var(--radius-sm);
  padding: 0 var(--space-3);
  transition: all var(--transition-base);
  background: var(--surface-card);
}

.field-input-wrap:focus-within {
  border-color: var(--primary-400);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.08);
}

.field-input-wrap.error {
  border-color: var(--color-danger);
}

.field-input-wrap.error:focus-within {
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.08);
}

.field-icon {
  color: var(--text-tertiary);
  flex-shrink: 0;
}

.field-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: var(--text-base);
  color: var(--text-primary);
  padding: var(--space-3) 0;
  min-width: 0;
}

.field-input::placeholder {
  color: var(--text-tertiary);
}

.pwd-toggle {
  border: none;
  background: transparent;
  color: var(--text-tertiary);
  cursor: pointer;
  padding: var(--space-1);
  border-radius: var(--radius-xs);
  display: flex;
  transition: color var(--transition-base);
}

.pwd-toggle:hover {
  color: var(--text-secondary);
}

.field-error {
  font-size: var(--text-xs);
  color: var(--color-danger);
}

/* 登录错误 */
.login-error {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3);
  background: var(--color-danger-light);
  border: 1px solid rgba(239, 68, 68, 0.15);
  border-radius: var(--radius-sm);
  font-size: var(--text-sm);
  color: var(--color-danger-600);
}

/* 提交按钮 */
.submit-btn {
  width: 100%;
  height: 48px;
  border: none;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--primary-600), var(--primary-700));
  color: #fff;
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  cursor: pointer;
  transition: all var(--transition-base);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: var(--space-2);
  letter-spacing: 0.3em;
}

.submit-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, var(--primary-700), var(--primary-800));
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.25);
}

.submit-btn:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: none;
}

.submit-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

/* 加载旋转 */
.btn-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 页脚 */
.form-footer {
  margin-top: var(--space-8);
  padding-top: var(--space-6);
  border-top: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}

.admin-link {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-xs);
  font-weight: var(--weight-medium);
  color: var(--text-link);
  text-decoration: none;
  transition: gap var(--transition-base);
}

.admin-link:hover {
  gap: var(--space-2);
}

/* ============================================================
   响应式
   ============================================================ */
@media (min-width: 768px) {
  .login-brand {
    display: flex;
  }

  .login-form-side {
    flex: 1;
  }
}

@media (max-width: 767px) {
  .login-page {
    flex-direction: column;
  }

  .form-container {
    max-width: 100%;
  }
}
</style>
