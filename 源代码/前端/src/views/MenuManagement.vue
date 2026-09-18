<template>
  <div class="menu-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <div class="title-group">
          <h2 class="page-title">菜单管理</h2>
          <span class="menu-count">{{ totalMenus }} 个菜单</span>
        </div>
      </div>
      <div class="header-actions">
        <el-button @click="toggleExpandAll" class="action-btn">
          <el-icon><Sort /></el-icon>
          {{ isAllExpanded ? '全部折叠' : '全部展开' }}
        </el-button>
        <el-button type="primary" @click="showAddDialog" class="action-btn primary-action">
          <el-icon><Plus /></el-icon>新增菜单
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, var(--primary-500), var(--primary-700))">
          <el-icon :size="22"><Menu /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ totalMenus }}</div>
          <div class="stat-label">菜单总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, var(--accent-400), var(--accent-600))">
          <el-icon :size="22"><Folder /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ topLevelCount }}</div>
          <div class="stat-label">顶级菜单</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, var(--success-400), var(--success-600))">
          <el-icon :size="22"><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ enabledCount }}</div>
          <div class="stat-label">已启用</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, var(--danger-400), var(--danger-600))">
          <el-icon :size="22"><Hide /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ disabledCount }}</div>
          <div class="stat-label">已禁用</div>
        </div>
      </div>
    </div>

    <!-- 树形表格 -->
    <el-card shadow="never" class="tree-card">
      <el-table
        ref="tableRef"
        :data="menuTree"
        row-key="id"
        :default-expand-all="isAllExpanded"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        border
        v-loading="loading"
        class="menu-table"
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="70" align="center">
          <template #default="{ row }">
            <span class="id-badge">{{ row.id }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="菜单名称" min-width="220">
          <template #default="{ row }">
            <div class="name-cell">
              <span class="menu-name">{{ row.name }}</span>
              <el-tag
                v-if="row.children && row.children.length"
                size="small"
                effect="plain"
                class="count-tag"
              >
                {{ row.children.length }} 子项
              </el-tag>
              <el-tag
                v-else-if="row.parentId && row.parentId !== 0"
                size="small"
                effect="plain"
                type="info"
                class="leaf-tag"
              >
                子菜单
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由路径" min-width="160">
          <template #default="{ row }">
            <code v-if="row.path" class="path-code">{{ row.path }}</code>
            <span v-else class="placeholder">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="component" label="组件路径" min-width="160">
          <template #default="{ row }">
            <code v-if="row.component" class="component-code">{{ row.component }}</code>
            <span v-else class="placeholder">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="icon" label="图标" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.icon" size="small" effect="plain" class="icon-tag">{{ row.icon }}</el-tag>
            <span v-else class="placeholder">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="70" align="center">
          <template #default="{ row }">
            <span class="sort-num">{{ row.sortOrder }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="visible" label="可见" width="80" align="center">
          <template #default="{ row }">
            <span :class="['status-dot', row.visible === 1 ? 'dot-success' : 'dot-muted']"></span>
            <span :class="row.visible === 1 ? 'text-success' : 'text-muted'">
              {{ row.visible === 1 ? '显示' : '隐藏' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <span :class="['status-dot', row.status === 1 ? 'dot-success' : 'dot-danger']"></span>
            <span :class="row.status === 1 ? 'text-success' : 'text-danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <div class="action-group">
              <el-button type="primary" link size="small" @click="handleEdit(row)">
                <el-icon><Edit /></el-icon>编辑
              </el-button>
              <el-button type="success" link size="small" @click="showAddChildDialog(row)">
                <el-icon><Plus /></el-icon>新增
              </el-button>
              <el-button type="danger" link size="small" @click="handleDelete(row)">
                <el-icon><Delete /></el-icon>删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="540px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px" class="menu-form">
        <el-form-item label="上级菜单">
          <el-tree-select
            v-model="form.parentId"
            :data="menuTreeOptions"
            :props="{ label: 'name', children: 'children', value: 'id' }"
            check-strictly
            clearable
            placeholder="留空则为顶级菜单"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="菜单名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入菜单名称" />
        </el-form-item>
        <el-form-item label="路由路径" prop="path">
          <el-input v-model="form.path" placeholder="如：/system/menu" />
        </el-form-item>
        <el-form-item label="组件路径">
          <el-input v-model="form.component" placeholder="如：views/System.vue" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="图标名称" />
        </el-form-item>
        <div class="form-row">
          <el-form-item label="排序" class="form-item-half">
            <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
          </el-form-item>
          <el-form-item label="可见" class="form-item-half">
            <el-switch v-model="form.visible" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </div>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Sort, Menu, Folder, CircleCheck, Hide } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const dialogTitle = ref('')
const menuTree = ref<any[]>([])
const isAllExpanded = ref(true)
const tableRef = ref()
const formRef = ref<FormInstance>()

const form = reactive({
  id: null as number | null,
  parentId: null as number | null,
  name: '',
  path: '',
  component: '',
  icon: '',
  sortOrder: 0,
  visible: 1,
  status: 1
})

const formRules: FormRules = {
  name: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  // path: [{ required: true, message: '请输入路由路径', trigger: 'blur' }]
}

const totalMenus = computed(() => {
  let count = 0
  const countNodes = (nodes: any[]) => {
    nodes.forEach(n => {
      count++
      if (n.children) countNodes(n.children)
    })
  }
  countNodes(menuTree.value)
  return count
})

const topLevelCount = computed(() => menuTree.value.length)

const enabledCount = computed(() => {
  let count = 0
  const countNodes = (nodes: any[]) => {
    nodes.forEach(n => {
      if (n.status === 1) count++
      if (n.children) countNodes(n.children)
    })
  }
  countNodes(menuTree.value)
  return count
})

const disabledCount = computed(() => totalMenus.value - enabledCount.value)

const flatMenuList = ref<any[]>([])

const buildTree = (list: any[], parentId: number): any[] => {
  return list
    .filter(item => item.parentId === parentId)
    .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
    .map(item => ({
      ...item,
      children: buildTree(list, item.id)
    }))
}

const menuTreeOptions = computed(() => buildTree(flatMenuList.value, 0))

const toggleExpandAll = () => {
  isAllExpanded.value = !isAllExpanded.value
  const data = menuTree.value
  menuTree.value = []
  nextTick(() => {
    menuTree.value = data
  })
}

const fetchList = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/menu/list')
    flatMenuList.value = res.data || []
    menuTree.value = buildTree(flatMenuList.value, 0)
  } catch (error) {
    console.error('获取菜单列表失败', error)
  } finally {
    loading.value = false
  }
}

const showAddDialog = () => {
  isEdit.value = false
  dialogTitle.value = '新增菜单'
  form.id = null
  form.parentId = null
  form.name = ''
  form.path = ''
  form.component = ''
  form.icon = ''
  form.sortOrder = 0
  form.visible = 1
  form.status = 1
  dialogVisible.value = true
}

const showAddChildDialog = (parent: any) => {
  isEdit.value = false
  dialogTitle.value = `新增子菜单 — ${parent.name}`
  form.id = null
  form.parentId = parent.id
  form.name = ''
  form.path = ''
  form.component = ''
  form.icon = ''
  form.sortOrder = 0
  form.visible = 1
  form.status = 1
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  isEdit.value = true
  dialogTitle.value = '编辑菜单'
  form.id = row.id
  form.parentId = row.parentId === 0 ? null : row.parentId
  form.name = row.name
  form.path = row.path
  form.component = row.component || ''
  form.icon = row.icon || ''
  form.sortOrder = row.sortOrder
  form.visible = row.visible
  form.status = row.status
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const data = { ...form, parentId: form.parentId || 0 }
    if (isEdit.value) {
      await request.put('/menu', data)
      ElMessage.success('修改成功')
    } else {
      await request.post('/menu', data)
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
  if (row.children && row.children.length > 0) {
    ElMessage.warning('请先删除子菜单')
    return
  }
  await ElMessageBox.confirm(`确认删除菜单「${row.name}」？`, '确认删除', { type: 'warning' })
  try {
    await request.delete(`/menu/${row.id}`)
    ElMessage.success('删除成功')
    fetchList()
  } catch (error) {
    console.error('删除失败', error)
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.menu-page {
  padding: 0;
  animation: pageIn 0.35s var(--ease-out-expo);
}

@keyframes pageIn {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ============ 页面头部 ============ */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-6);
}

.title-group {
  display: flex;
  align-items: baseline;
  gap: var(--space-3);
}

.page-title {
  font-size: var(--text-2xl);
  font-weight: var(--weight-bold);
  color: var(--text-primary);
  margin: 0;
  letter-spacing: var(--tracking-tight);
}

.menu-count {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  background: var(--gray-100);
  padding: 3px 10px;
  border-radius: var(--radius-full);
  font-weight: var(--weight-medium);
  letter-spacing: var(--tracking-wide);
}

.header-actions {
  display: flex;
  gap: var(--space-3);
}

.action-btn {
  border-radius: var(--radius-md);
  font-weight: var(--weight-medium);
}

.primary-action {
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25);
}

/* ============ 统计卡片 ============ */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-6);
}

.stat-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  display: flex;
  align-items: center;
  gap: var(--space-4);
  transition: all var(--transition-normal);
  box-shadow: var(--shadow-xs);
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
  border-color: var(--primary-100);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.stat-value {
  font-size: var(--text-2xl);
  font-weight: var(--weight-bold);
  color: var(--text-primary);
  font-family: var(--font-mono);
  line-height: 1;
}

.stat-label {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  margin-top: 4px;
  font-weight: var(--weight-medium);
  letter-spacing: var(--tracking-wide);
}

/* ============ 树形卡片 ============ */
.tree-card {
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
}

.tree-card :deep(.el-card__body) {
  padding: 0;
}

/* ============ 表格样式 ============ */
.menu-table {
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.menu-table :deep(th.el-table__cell) {
  background: var(--gray-50) !important;
  color: var(--text-secondary);
  font-weight: var(--weight-semibold);
  font-size: var(--text-xs);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  padding: 14px 0;
}

.menu-table :deep(td.el-table__cell) {
  padding: 10px 0;
}

.menu-table :deep(.el-table__row:hover > td) {
  background: var(--primary-50) !important;
}

/* ID 标签 */
.id-badge {
  display: inline-block;
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: var(--weight-medium);
  color: var(--text-secondary);
  background: var(--gray-100);
  padding: 1px 6px;
  border-radius: var(--radius-xs);
}

/* 菜单名称 */
.name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.menu-name {
  font-weight: var(--weight-medium);
  color: var(--text-primary);
  font-size: var(--text-sm);
}

.count-tag {
  background: var(--primary-50);
  border-color: var(--primary-200);
  color: var(--primary-600);
  font-size: 11px;
}

.leaf-tag {
  font-size: 11px;
}

/* 路径代码样式 */
.path-code,
.component-code {
  display: inline-block;
  font-family: var(--font-mono);
  font-size: 12px;
  background: var(--gray-50);
  color: var(--text-secondary);
  padding: 2px 8px;
  border-radius: var(--radius-xs);
  border: 1px solid var(--border-light);
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.path-code {
  color: var(--primary-700);
  background: var(--primary-50);
  border-color: var(--primary-100);
}

.icon-tag {
  font-family: var(--font-mono);
  font-size: 11px;
}

.sort-num {
  font-family: var(--font-mono);
  font-weight: var(--weight-medium);
  color: var(--text-secondary);
}

.placeholder {
  color: var(--gray-300);
}

/* 状态指示点 */
.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
}

.dot-success {
  background: var(--success-500);
  box-shadow: 0 0 6px rgba(34, 197, 94, 0.4);
}

.dot-muted {
  background: var(--gray-300);
}

.dot-danger {
  background: var(--danger-400);
  box-shadow: 0 0 6px rgba(239, 68, 68, 0.3);
}

.text-success {
  color: var(--success-600);
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
}

.text-muted {
  color: var(--gray-400);
  font-size: var(--text-sm);
}

.text-danger {
  color: var(--danger-500);
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
}

/* 操作按钮组 */
.action-group {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

/* ============ 对话框表单 ============ */
.menu-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.form-row {
  display: flex;
  gap: var(--space-5);
}

.form-item-half {
  flex: 1;
}

/* ============ 响应式 ============ */
@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-3);
  }
}
</style>
