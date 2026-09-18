<template>
  <div class="login-page">
    <!-- 背景装饰 -->
    <div class="bg-pattern"></div>
    <div class="bg-grid"></div>
    
    <div class="login-container">
      <!-- 左侧品牌区 -->
      <div class="brand-section">
        <div class="brand-content">
          <div class="brand-icon">
            <el-icon size="48"><Tools /></el-icon>
          </div>
          <h1 class="brand-title">智能设备检修系统</h1>
          <p class="brand-subtitle">Equipment Repair & Maintenance System</p>
          
          <div class="brand-features">
            <div class="feature">
              <div class="feature-icon">
                <el-icon><Search /></el-icon>
              </div>
              <div class="feature-text">
                <h4>智能故障检索</h4>
                <p>AI驱动的多模态故障诊断</p>
              </div>
            </div>
            <div class="feature">
              <div class="feature-icon">
                <el-icon><Document /></el-icon>
              </div>
              <div class="feature-text">
                <h4>标准化作业</h4>
                <p>规范化检修流程指引</p>
              </div>
            </div>
            <div class="feature">
              <div class="feature-icon">
                <el-icon><DataAnalysis /></el-icon>
              </div>
              <div class="feature-text">
                <h4>知识沉淀</h4>
                <p>维修经验智能管理</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧登录区 -->
      <div class="login-section">
        <div class="login-form-container">
          <div class="login-header">
            <h2>欢迎回来</h2>
            <p>请登录您的账号</p>
          </div>

          <el-form
            ref="loginFormRef"
            :model="loginForm"
            :rules="loginRules"
            class="login-form"
            @keyup.enter="handleLogin"
          >
            <el-form-item prop="username">
              <el-input
                v-model="loginForm.username"
                placeholder="请输入用户名"
                size="large"
                class="login-input"
              >
                <template #prefix>
                  <el-icon><User /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                size="large"
                show-password
                class="login-input"
              >
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <div class="form-options">
              <el-checkbox v-model="rememberMe">记住我</el-checkbox>
              <el-link type="primary" :underline="false">忘记密码？</el-link>
            </div>

            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :loading="loading"
                class="login-btn"
                @click="handleLogin"
              >
                {{ loading ? '登录中...' : '登 录' }}
              </el-button>
            </el-form-item>
          </el-form>

          <div class="login-footer">
            <span>默认账号：admin / Admin@123456</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { login } from '@/api'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const loginFormRef = ref<FormInstance>()
const loading = ref(false)
const rememberMe = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const loginRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 个字符', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  const valid = await loginFormRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res: any = await login(loginForm)
    userStore.setLoginInfo(res.data)
    localStorage.removeItem('menus')
    
    if (rememberMe.value) {
      localStorage.setItem('rememberedUser', loginForm.username)
    } else {
      localStorage.removeItem('rememberedUser')
    }

    ElMessage.success('登录成功')
    await nextTick()

    if (res.data.passwordExpired) {
      ElMessageBox.confirm(
        '您的密码已超过90天未更换，建议立即修改密码。',
        '密码过期提醒',
        {
          confirmButtonText: '去修改',
          cancelButtonText: '稍后再说',
          type: 'warning'
        }
      ).then(() => {
        router.push('/profile')
      }).catch(() => {
        router.push('/')
      })
    } else {
      router.push('/')
    }
  } catch (error) {
    console.error('登录失败', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  width: 100%;
  height: 100vh;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0f172a 100%);
}

/* 背景装饰 */
.bg-pattern {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    radial-gradient(circle at 25% 25%, rgba(59, 130, 246, 0.15) 0%, transparent 50%),
    radial-gradient(circle at 75% 75%, rgba(249, 115, 22, 0.1) 0%, transparent 50%);
}

.bg-grid {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    linear-gradient(rgba(255, 255, 255, 0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.02) 1px, transparent 1px);
  background-size: 50px 50px;
}

/* 登录容器 */
.login-container {
  position: relative;
  z-index: 1;
  display: flex;
  width: 100%;
  height: 100%;
}

/* 品牌区 */
.brand-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px;
}

.brand-content {
  max-width: 480px;
}

.brand-icon {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, var(--primary-500) 0%, var(--primary-700) 100%);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin-bottom: 32px;
  box-shadow: 0 8px 32px rgba(37, 99, 235, 0.3);
}

.brand-title {
  font-size: 42px;
  font-weight: 700;
  color: white;
  margin: 0 0 8px 0;
  letter-spacing: -0.5px;
}

.brand-subtitle {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.6);
  margin: 0 0 48px 0;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.feature {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.feature-icon {
  width: 48px;
  height: 48px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary-400);
  flex-shrink: 0;
}

.feature-text h4 {
  font-size: 16px;
  font-weight: 600;
  color: white;
  margin: 0 0 4px 0;
}

.feature-text p {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  margin: 0;
}

/* 登录区 */
.login-section {
  width: 520px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  box-shadow: -20px 0 60px rgba(0, 0, 0, 0.2);
}

.login-form-container {
  width: 100%;
  max-width: 380px;
  padding: 40px;
}

.login-header {
  margin-bottom: 40px;
}

.login-header h2 {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 8px 0;
}

.login-header p {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

/* 表单样式 */
.login-form {
  width: 100%;
}

.login-input :deep(.el-input__wrapper) {
  padding: 8px 16px;
  border-radius: 10px;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--primary-600) 0%, var(--primary-800) 100%);
  border: none;
  box-shadow: 0 4px 16px rgba(37, 99, 235, 0.3);
  transition: all 0.3s ease;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.4);
}

.login-btn:active {
  transform: translateY(0);
}

/* 底部提示 */
.login-footer {
  margin-top: 32px;
  text-align: center;
  padding-top: 24px;
  border-top: 1px solid var(--border-light);
}

.login-footer span {
  font-size: 12px;
  color: var(--text-tertiary);
  background: var(--gray-50);
  padding: 6px 12px;
  border-radius: 6px;
}

/* 响应式 */
@media (max-width: 1024px) {
  .brand-section {
    display: none;
  }
  
  .login-section {
    width: 100%;
  }
}
</style>
