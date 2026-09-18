<template>
  <div class="page-container">
    <el-card class="main-card">
      <template #header>
        <div class="card-header">
          <div class="card-title">
            <el-icon><UserFilled /></el-icon>
            <span>角色管理</span>
          </div>
          <el-button type="primary" @click="showAddDialog">
            <el-icon><Plus /></el-icon>新增角色
          </el-button>
        </div>
      </template>

      <el-table :data="roleList" border stripe v-loading="loading" class="data-table">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="roleName" label="角色名称" min-width="150" />
        <el-table-column prop="roleCode" label="角色编码" min-width="150" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="创建时间" width="180" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>编辑
            </el-button>
            <el-button type="success" link size="small" @click="handlePermission(row)">
              <el-icon><Setting /></el-icon>权限
            </el-button>
            <el-button
              type="danger"
              link size="small"
              @click="handleDelete(row)"
              :disabled="row.id <= 3"
            >
              <el-icon><Delete /></el-icon>删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" placeholder="请输入角色编码" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入角色描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 权限配置对话框 -->
    <el-dialog v-model="permissionVisible" title="菜单权限配置" width="500px">
      <div class="permission-header">
        <el-tag type="primary" effect="dark">{{ currentRole?.roleName }}</el-tag>
        <span class="permission-tip">勾选该角色可访问的菜单</span>
      </div>
      <div class="permission-content">
        <el-tree
          ref="menuTreeRef"
          :data="menuTree"
          :props="{ label: 'title', children: 'children' }"
          show-checkbox
          node-key="id"
          :default-checked-keys="checkedMenuIds"
          default-expand-all
        />
      </div>
      <template #footer>
        <el-button @click="permissionVisible = false">取消</el-button>
        <el-button type="primary" :loading="permissionLoading" @click="handleSavePermission">保存</el-button>
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
const permissionLoading = ref(false)
const dialogVisible = ref(false)
const permissionVisible = ref(false)
const isEdit = ref(false)
const dialogTitle = ref('')
const roleList = ref<any[]>([])
const menuTree = ref<any[]>([])
const checkedMenuIds = ref<number[]>([])
const currentRole = ref<any>(null)
const formRef = ref<FormInstance>()
const menuTreeRef = ref<any>(null)

const form = reactive({
  id: null as number | null,
  roleName: '',
  roleCode: '',
  description: ''
})

const formRules: FormRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

const fetchList = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/role/list')
    roleList.value = res.data || []
  } catch (error) {
    console.error('获取角色列表失败', error)
  } finally {
    loading.value = false
  }
}

const fetchMenuTree = async () => {
  try {
    const res: any = await request.get('/menu/tree')
    menuTree.value = res.data || []
  } catch (error) {
    console.error('获取菜单树失败', error)
  }
}

const showAddDialog = () => {
  isEdit.value = false
  dialogTitle.value = '新增角色'
  form.id = null
  form.roleName = ''
  form.roleCode = ''
  form.description = ''
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  isEdit.value = true
  dialogTitle.value = '编辑角色'
  form.id = row.id
  form.roleName = row.roleName
  form.roleCode = row.roleCode
  form.description = row.description
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (isEdit.value) {
      await request.put('/role', form)
      ElMessage.success('修改成功')
    } else {
      await request.post('/role', form)
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
  await ElMessageBox.confirm('确认删除该角色？', '提示', { type: 'warning' })
  try {
    await request.delete(`/role/${row.id}`)
    ElMessage.success('删除成功')
    fetchList()
  } catch (error) {
    console.error('删除失败', error)
  }
}

const handlePermission = async (row: any) => {
  currentRole.value = row
  checkedMenuIds.value = []
  
  try {
    const res: any = await request.get(`/menu/role/${row.id}`)
    checkedMenuIds.value = res.data || []
  } catch (error) {
    console.error('获取角色菜单失败', error)
  }
  
  permissionVisible.value = true
}

const handleSavePermission = async () => {
  if (!menuTreeRef.value || !currentRole.value) return

  const checkedKeys = menuTreeRef.value.getCheckedKeys()
  const halfCheckedKeys = menuTreeRef.value.getHalfCheckedKeys()
  const allMenuIds = [...checkedKeys, ...halfCheckedKeys]

  permissionLoading.value = true
  try {
    await request.post(`/role/${currentRole.value.id}/menus`, allMenuIds)
    ElMessage.success('权限配置成功')
    permissionVisible.value = false
  } catch (error) {
    console.error('保存权限失败', error)
  } finally {
    permissionLoading.value = false
  }
}

onMounted(() => {
  fetchList()
  fetchMenuTree()
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

.data-table {
  margin-bottom: 20px;
}

.permission-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 12px;
  background: var(--bg-color);
  border-radius: 8px;
}

.permission-tip {
  color: var(--text-secondary);
  font-size: 14px;
}

.permission-content {
  max-height: 400px;
  overflow-y: auto;
  padding: 8px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
}
</style>
