<template>
  <div class="page-container">
    <el-card class="main-card">
      <template #header>
        <div class="card-header">
          <div class="card-title">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </div>
          <el-button type="primary" @click="showAddDialog">
            <el-icon><Plus /></el-icon>新增用户
          </el-button>
        </div>
      </template>

      <div class="search-bar">
        <el-form :inline="true" :model="queryParams">
          <el-form-item label="用户名">
            <el-input
              v-model="queryParams.username"
              placeholder="请输入用户名"
              clearable
              @keyup.enter="fetchList"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="fetchList">查询</el-button>
            <el-button @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table :data="userList" border stripe v-loading="loading" class="data-table">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="roleId" label="角色" width="140" align="center">
          <template #default="{ row }">
            <el-tag :type="getRoleType(row.roleId)" effect="plain">
              {{ getRoleName(row.roleId) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="dark">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>编辑
            </el-button>
            <el-button type="warning" link size="small" @click="handleResetPwd(row)">
              <el-icon><Key /></el-icon>重置密码
            </el-button>
            <el-button
              :type="row.status === 1 ? 'danger' : 'success'"
              link size="small"
              @click="handleToggleStatus(row)"
            >
              <el-icon><Switch /></el-icon>{{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button
              type="danger"
              link size="small"
              @click="handleDelete(row)"
              :disabled="row.roleId === 1"
            >
              <el-icon><Delete /></el-icon>删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchList"
          @current-change="fetchList"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="form.roleId" placeholder="请选择角色" style="width: 100%">
            <el-option label="系统管理员" :value="1" />
            <el-option label="资深检修工程师" :value="2" />
            <el-option label="实习检修人员" :value="3" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码对话框 -->
    <el-dialog v-model="resetPwdVisible" title="重置密码" width="400px">
      <el-form ref="resetPwdFormRef" :model="resetPwdForm" :rules="resetPwdRules" label-width="80px">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="resetPwdForm.newPassword" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetPwdLoading" @click="confirmResetPwd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import request from '@/utils/request'

const formatDateTime = (time: string) => {
  if (!time) return '-'
  return time.substring(0, 19).replace('T', ' ')
}

const loading = ref(false)
const submitLoading = ref(false)
const resetPwdLoading = ref(false)
const dialogVisible = ref(false)
const resetPwdVisible = ref(false)
const isEdit = ref(false)
const dialogTitle = ref('')
const userList = ref<any[]>([])
const total = ref(0)
const formRef = ref<FormInstance>()
const resetPwdFormRef = ref<FormInstance>()
const currentUserId = ref<number | null>(null)

const queryParams = reactive({
  username: '',
  pageNum: 1,
  pageSize: 10
})

const form = reactive({
  id: null as number | null,
  username: '',
  password: '',
  nickname: '',
  roleId: null as number | null
})

const resetPwdForm = reactive({
  newPassword: ''
})

const formRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码至少8位', trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const resetPwdRules: FormRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '密码至少8位', trigger: 'blur' }
  ]
}

const getRoleType = (roleId: number) => {
  const map: Record<number, string> = { 1: 'danger', 2: 'warning', 3: 'info' }
  return map[roleId] || 'info'
}

const getRoleName = (roleId: number) => {
  const map: Record<number, string> = { 1: '系统管理员', 2: '资深检修工程师', 3: '实习检修人员' }
  return map[roleId] || '未知'
}

const fetchList = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/user/list', { params: queryParams })
    userList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('获取用户列表失败', error)
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  queryParams.username = ''
  queryParams.pageNum = 1
  fetchList()
}

const showAddDialog = () => {
  isEdit.value = false
  dialogTitle.value = '新增用户'
  form.id = null
  form.username = ''
  form.password = ''
  form.nickname = ''
  form.roleId = null
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  isEdit.value = true
  dialogTitle.value = '编辑用户'
  form.id = row.id
  form.username = row.username
  form.nickname = row.nickname
  form.roleId = row.roleId
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (isEdit.value) {
      await request.put('/user', form)
      ElMessage.success('修改成功')
    } else {
      await request.post('/user', form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchList()
  } catch (error) {
    console.error('操作失败', error)
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (row: any) => {
  await ElMessageBox.confirm('确认删除该用户？', '提示', { type: 'warning' })
  try {
    await request.delete(`/user/${row.id}`)
    ElMessage.success('删除成功')
    fetchList()
  } catch (error) {
    console.error('删除失败', error)
  }
}

const handleToggleStatus = async (row: any) => {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'
  await ElMessageBox.confirm(`确认${action}该用户？`, '提示', { type: 'warning' })
  try {
    await request.put(`/user/${row.id}/status/${newStatus}`)
    ElMessage.success(`${action}成功`)
    fetchList()
  } catch (error) {
    console.error(`${action}失败`, error)
  }
}

const handleResetPwd = (row: any) => {
  currentUserId.value = row.id
  resetPwdForm.newPassword = ''
  resetPwdVisible.value = true
}

const confirmResetPwd = async () => {
  const valid = await resetPwdFormRef.value?.validate().catch(() => false)
  if (!valid) return

  resetPwdLoading.value = true
  try {
    await request.put(`/user/${currentUserId.value}/reset-password`, resetPwdForm.newPassword, {
      headers: { 'Content-Type': 'text/plain' }
    })
    ElMessage.success('密码重置成功')
    resetPwdVisible.value = false
  } catch (error) {
    console.error('重置密码失败', error)
  } finally {
    resetPwdLoading.value = false
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.page-container {
  padding: 0;
}

.main-card {
  min-height: calc(100vh - 140px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.search-bar {
  margin-bottom: 20px;
  padding: 16px;
  background: var(--bg-color);
  border-radius: 8px;
}

.data-table {
  margin-bottom: 20px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0;
}
</style>
