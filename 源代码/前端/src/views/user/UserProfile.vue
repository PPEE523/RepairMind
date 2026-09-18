<template>
  <div class="profile-page">
    <div class="profile-content">
      <!-- 页面标题 -->
      <section class="page-header">
        <h1 class="page-title">个人设置</h1>
        <p class="page-sub">管理您的账户信息、权限与密码</p>
      </section>

      <!-- 个人信息卡片 -->
      <section class="profile-card">
        <div class="card-header">
          <div class="card-header-left">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
            </svg>
            <h3>个人信息</h3>
          </div>
        </div>
        <div class="card-body profile-info-body">
          <div class="avatar-section">
            <div class="avatar-large">{{ (userStore.nickname || userStore.username || '?').charAt(0) }}</div>
            <div class="avatar-meta">
              <span class="avatar-name">{{ userStore.nickname || userStore.username }}</span>
              <span class="avatar-role">{{ roleName }}</span>
            </div>
          </div>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">用户名</span>
              <span class="info-value" v-if="!editing">{{ userStore.username }}</span>
              <el-input v-else v-model="profileForm.username" size="small" class="info-input" />
            </div>
            <div class="info-item">
              <span class="info-label">昵称</span>
              <span class="info-value" v-if="!editing">{{ userStore.nickname }}</span>
              <el-input v-else v-model="profileForm.nickname" size="small" class="info-input" />
            </div>
            <div class="info-item">
              <span class="info-label">角色</span>
              <el-tag size="small" effect="plain" type="primary">{{ roleName }}</el-tag>
            </div>
          </div>
          <div class="edit-profile-actions">
            <template v-if="!editing">
              <button class="edit-profile-btn" @click="startEditProfile">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                </svg>
                编辑信息
              </button>
            </template>
            <template v-else>
              <button class="edit-profile-btn save" :disabled="profileLoading" @click="saveProfile">
                {{ profileLoading ? '保存中...' : '保存' }}
              </button>
              <button class="edit-profile-btn cancel" @click="cancelEdit">取消</button>
            </template>
          </div>
        </div>
      </section>

      <!-- 权限清单 -->
      <section class="profile-card">
        <div class="card-header">
          <div class="card-header-left">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>
            </svg>
            <h3>权限清单</h3>
          </div>
          <span class="card-badge">{{ permissionCount }} 项</span>
        </div>
        <div class="card-body" v-loading="permLoading">
          <template v-if="permMenuList.length">
            <div v-for="menu in permMenuList" :key="menu.id" class="perm-group">
              <div class="perm-group-title">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/></svg>
                <span>{{ menu.title }}</span>
              </div>
              <div v-if="menu.children?.length" class="perm-children">
                <span
                  v-for="child in menu.children"
                  :key="child.id"
                  class="perm-pill"
                >
                  {{ child.title }}
                </span>
              </div>
            </div>
          </template>
          <div v-else class="empty-block">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>
            </svg>
            <span>暂无权限数据</span>
          </div>
        </div>
      </section>

      <!-- 修改密码 -->
      <section class="profile-card">
        <div class="card-header">
          <div class="card-header-left">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/><circle cx="12" cy="16" r="1"/>
            </svg>
            <h3>修改密码</h3>
          </div>
        </div>
        <div class="card-body">
          <el-form
            ref="pwdFormRef"
            :model="pwdForm"
            :rules="pwdRules"
            label-width="90px"
            class="pwd-form"
          >
            <el-form-item label="旧密码" prop="oldPassword">
              <el-input
                v-model="pwdForm.oldPassword"
                type="password"
                placeholder="请输入旧密码"
                show-password
              />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="pwdForm.newPassword"
                type="password"
                placeholder="请输入新密码"
                show-password
              />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="pwdForm.confirmPassword"
                type="password"
                placeholder="请再次输入新密码"
                show-password
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" @click="handleChangePwd" class="submit-btn">
                修改密码
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </section>

      <!-- 密码要求 -->
      <section class="profile-card">
        <div class="card-header">
          <div class="card-header-left">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/>
            </svg>
            <h3>密码要求</h3>
          </div>
        </div>
        <div class="card-body">
          <ul class="pwd-rules">
            <li><span class="rule-dot"></span>密码长度至少 8 位</li>
            <li><span class="rule-dot"></span>包含大写字母 (A-Z)</li>
            <li><span class="rule-dot"></span>包含小写字母 (a-z)</li>
            <li><span class="rule-dot"></span>包含数字 (0-9)</li>
            <li><span class="rule-dot"></span>包含特殊字符 (!@#$%^&amp;*)</li>
            <li><span class="rule-dot"></span>密码有效期 90 天，到期需更换</li>
          </ul>
        </div>
      </section>

      <!-- 退出登录 -->
      <div class="logout-section">
        <button class="logout-btn" @click="handleLogout">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/>
          </svg>
          退出登录
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'

const userStore = useUserStore()
const router = useRouter()
const loading = ref(false)
const pwdFormRef = ref<FormInstance>()

// ============ 编辑个人信息 ============
const editing = ref(false)
const profileLoading = ref(false)
const profileForm = reactive({
  username: '',
  nickname: ''
})

const startEditProfile = () => {
  profileForm.username = userStore.username
  profileForm.nickname = userStore.nickname || ''
  editing.value = true
}

const cancelEdit = () => {
  editing.value = false
}

const saveProfile = async () => {
  if (!profileForm.username.trim()) {
    ElMessage.warning('用户名不能为空')
    return
  }
  profileLoading.value = true
  try {
    await request.put('/auth/profile', {
      username: profileForm.username.trim(),
      nickname: profileForm.nickname.trim()
    })
    userStore.updateProfile({
      username: profileForm.username.trim(),
      nickname: profileForm.nickname.trim()
    })
    ElMessage.success('个人信息更新成功')
    editing.value = false
  } catch (error) {
    console.error('更新个人信息失败', error)
  } finally {
    profileLoading.value = false
  }
}

const roleName = computed(() => {
  const map: Record<number, string> = { 1: '系统管理员', 2: '资深检修工程师', 3: '实习检修人员' }
  return userStore.roleId ? map[userStore.roleId] || '未知角色' : '未知角色'
})

// ============ 权限清单 ============
const permMenuList = ref<any[]>([])
const permLoading = ref(false)

const permissionCount = computed(() => {
  let count = 0
  for (const menu of permMenuList.value) {
    count++
    if (menu.children?.length) {
      count += menu.children.length
    }
  }
  return count
})

const fetchPermissions = async () => {
  permLoading.value = true
  try {
    const res: any = await request.get('/auth/menu')
    permMenuList.value = res.data || []
  } catch (error) {
    console.error('获取权限清单失败', error)
    permMenuList.value = []
  } finally {
    permLoading.value = false
  }
}

onMounted(() => {
  fetchPermissions()
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule: any, value: string, callback: any) => {
  if (value !== pwdForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '密码至少8位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleChangePwd = async () => {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await request.post('/auth/change-password', {
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    userStore.logout()
    router.push('/user/login')
  } catch (error) {
    console.error('修改密码失败', error)
  } finally {
    loading.value = false
  }
}

const handleLogout = () => {
  userStore.logout()
  localStorage.removeItem('menus')
  ElMessage.success('已退出登录')
  router.push('/user/login')
}
</script>

<style scoped>
/* ============================================================
   Kimi 风格 — 用户端个人设置页面
   ============================================================ */
.profile-page {
  width: 100%;
  height: 100%;
  overflow-y: auto;
  padding: var(--space-6) 0;
}

.profile-content {
  max-width: 680px;
  width: 100%;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

/* ---- 页面标题 ---- */
.page-header {
  margin-bottom: var(--space-2);
}

.page-title {
  font-size: 24px;
  font-weight: var(--weight-bold);
  color: var(--text-primary);
  margin: 0 0 var(--space-1);
  letter-spacing: -0.02em;
}

.page-sub {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
  margin: 0;
}

/* ---- 卡片通用 ---- */
.profile-card {
  background: var(--surface-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-xs);
  transition: box-shadow var(--transition-base);
}

.profile-card:hover {
  box-shadow: var(--shadow-sm);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid var(--border-light);
}

.card-header-left {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--text-secondary);
}

.card-header-left h3 {
  margin: 0;
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  color: var(--text-primary);
  letter-spacing: -0.01em;
}

.card-badge {
  font-size: var(--text-xs);
  padding: 2px 10px;
  border-radius: var(--radius-full);
  background: var(--gray-100);
  color: var(--text-tertiary);
  font-weight: var(--weight-medium);
}

.card-body {
  padding: var(--space-5);
}

/* ---- 个人信息 ---- */
.profile-info-body {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.avatar-large {
  width: 72px;
  height: 72px;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, var(--primary-500), var(--primary-700));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: var(--weight-bold);
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);
}

.avatar-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.avatar-name {
  font-size: var(--text-lg);
  font-weight: var(--weight-semibold);
  color: var(--text-primary);
}

.avatar-role {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1px;
  background: var(--border-light);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: var(--space-4);
  background: var(--gray-50);
}

.info-label {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.info-value {
  font-size: var(--text-sm);
  color: var(--text-primary);
  font-weight: var(--weight-medium);
}

.info-input {
  width: 100%;
}

/* ---- 编辑个人信息按钮 ---- */
.edit-profile-actions {
  display: flex;
  gap: var(--space-2);
  margin-top: var(--space-3);
}

.edit-profile-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-1) var(--space-3);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: var(--surface-card);
  color: var(--text-secondary);
  font-size: var(--text-xs);
  font-weight: var(--weight-medium);
  cursor: pointer;
  transition: all var(--transition-fast);
  font-family: inherit;
}

.edit-profile-btn:hover {
  color: var(--primary-600);
  border-color: var(--primary-200);
  background: var(--primary-50);
}

.edit-profile-btn.save {
  background: var(--primary-600);
  color: #fff;
  border-color: var(--primary-600);
}

.edit-profile-btn.save:hover {
  background: var(--primary-700);
}

.edit-profile-btn.save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.edit-profile-btn.cancel {
  color: var(--text-tertiary);
}

.edit-profile-btn.cancel:hover {
  color: var(--text-primary);
}

/* ---- 权限清单 ---- */
.perm-group {
  margin-bottom: var(--space-4);
}

.perm-group:last-child {
  margin-bottom: 0;
}

.perm-group-title {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  color: var(--text-primary);
  margin-bottom: var(--space-2);
}

.perm-group-title svg {
  color: var(--text-tertiary);
  flex-shrink: 0;
}

.perm-children {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
  padding-left: 22px;
}

.perm-pill {
  padding: 3px 10px;
  font-size: var(--text-xs);
  color: var(--text-secondary);
  background: var(--gray-50);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-full);
  white-space: nowrap;
  transition: all var(--transition-fast);
}

.perm-pill:hover {
  color: var(--primary-600);
  border-color: var(--primary-200);
  background: var(--primary-50);
}

.empty-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-8) 0;
  color: var(--text-tertiary);
  font-size: var(--text-sm);
}

/* ---- 修改密码表单 ---- */
.pwd-form {
  max-width: 420px;
}

.pwd-form :deep(.el-form-item) {
  margin-bottom: var(--space-4);
}

.pwd-form :deep(.el-form-item__label) {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  font-weight: var(--weight-medium);
}

.submit-btn {
  width: 100%;
  height: 40px;
  font-weight: var(--weight-medium);
  border-radius: var(--radius-md);
}

/* ---- 密码要求 ---- */
.pwd-rules {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.pwd-rules li {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  color: var(--text-secondary);
  line-height: var(--leading-normal);
}

.rule-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--primary-300);
  flex-shrink: 0;
}

/* ---- 退出登录 ---- */
.logout-section {
  padding-top: var(--space-8);
}

.logout-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  width: 100%;
  padding: var(--space-3) var(--space-5);
  border: none;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--primary-600), var(--primary-700));
  color: #fff;
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  cursor: pointer;
  transition: all var(--transition-base);
  font-family: inherit;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25);
}

.logout-btn:hover {
  background: linear-gradient(135deg, var(--primary-700), var(--primary-800));
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.35);
  transform: translateY(-1px);
}

.logout-btn:active {
  transform: scale(0.98);
}

/* ---- 响应式 ---- */
@media (max-width: 640px) {
  .profile-page {
    padding: var(--space-3) 0 var(--space-12);
  }

  .profile-content {
    gap: var(--space-4);
  }

  .page-title {
    font-size: 20px;
  }

  .avatar-large {
    width: 56px;
    height: 56px;
    font-size: 22px;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .pwd-form {
    max-width: 100%;
  }

  .logout-section {
    padding-top: var(--space-6);
  }
}
</style>
