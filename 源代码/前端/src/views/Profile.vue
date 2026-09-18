<template>
  <div class="profile-container">
    <el-row :gutter="20">
      <!-- 左侧：个人信息 + 权限清单 -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <h3>个人信息</h3>
          </template>
          <div class="user-info">
            <el-avatar :size="80" class="avatar">
              {{ userStore.nickname?.charAt(0) || userStore.username?.charAt(0) }}
            </el-avatar>
            <div class="info-list">
              <div class="info-item">
                <span class="label">用户名：</span>
                <span class="value" v-if="!editing">{{ userStore.username }}</span>
                <el-input v-else v-model="profileForm.username" size="small" style="width: 160px" />
              </div>
              <div class="info-item">
                <span class="label">昵称：</span>
                <span class="value" v-if="!editing">{{ userStore.nickname }}</span>
                <el-input v-else v-model="profileForm.nickname" size="small" style="width: 160px" />
              </div>
              <div class="info-item">
                <span class="label">角色：</span>
                <el-tag>{{ roleName }}</el-tag>
              </div>
              <div style="margin-top: 12px; text-align: center">
                <template v-if="!editing">
                  <el-button type="primary" size="small" text @click="startEditProfile">
                    <el-icon><Edit /></el-icon>编辑信息
                  </el-button>
                </template>
                <template v-else>
                  <el-button type="primary" size="small" :loading="profileLoading" @click="saveProfile">保存</el-button>
                  <el-button size="small" @click="cancelEdit">取消</el-button>
                </template>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 权限清单 -->
        <el-card style="margin-top: 20px">
          <template #header>
            <div class="perm-header">
              <h3>权限清单</h3>
              <el-tag size="small" type="info">{{ permissionCount }} 项</el-tag>
            </div>
          </template>
          <div v-loading="permLoading" class="perm-list">
            <template v-if="permMenuList.length">
              <div v-for="menu in permMenuList" :key="menu.id" class="perm-group">
                <div class="perm-group-title">
                  <el-icon size="16"><component :is="menu.icon || 'Folder'" /></el-icon>
                  <span>{{ menu.title }}</span>
                </div>
                <div v-if="menu.children?.length" class="perm-children">
                  <el-tag
                    v-for="child in menu.children"
                    :key="child.id"
                    size="small"
                    effect="plain"
                    class="perm-tag"
                  >
                    {{ child.title }}
                  </el-tag>
                </div>
              </div>
            </template>
            <el-empty v-else description="暂无权限" :image-size="60" />
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：修改密码 + 密码要求 -->
      <el-col :span="16">
        <el-card>
          <template #header>
            <h3>修改密码</h3>
          </template>
          <el-form
            ref="pwdFormRef"
            :model="pwdForm"
            :rules="pwdRules"
            label-width="100px"
            style="max-width: 500px"
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
              <el-button type="primary" :loading="loading" @click="handleChangePwd">
                修改密码
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card style="margin-top: 20px">
          <template #header>
            <h3>密码要求</h3>
          </template>
          <ul class="password-requirements">
            <li>密码长度至少8位</li>
            <li>包含大写字母 (A-Z)</li>
            <li>包含小写字母 (a-z)</li>
            <li>包含数字 (0-9)</li>
            <li>包含特殊字符 (!@#$%^&*)</li>
            <li>密码有效期90天，到期需更换</li>
          </ul>
        </el-card>
      </el-col>
    </el-row>
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
    // 清除用户状态和token，跳转到登录页
    userStore.logout()
    router.push('/login')
  } catch (error) {
    console.error('修改密码失败', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.profile-container {
  padding: 10px;
}

.user-info {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.avatar {
  margin-bottom: 20px;
  background: #409eff;
  font-size: 32px;
}

.info-list {
  width: 100%;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
}

.info-item:last-child {
  border-bottom: none;
}

.label {
  color: #909399;
}

.value {
  color: #303133;
  font-weight: 500;
}

h3 {
  margin: 0;
  font-size: 16px;
}

.password-requirements {
  padding-left: 20px;
  color: #606266;
  line-height: 2;
}

/* 权限清单 */
.perm-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.perm-header h3 {
  margin: 0;
}

.perm-list {
  max-height: 420px;
  overflow-y: auto;
}

.perm-group {
  margin-bottom: 14px;
}

.perm-group:last-child {
  margin-bottom: 0;
}

.perm-group-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.perm-children {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding-left: 22px;
}

.perm-tag {
  border-radius: 6px;
}
</style>
