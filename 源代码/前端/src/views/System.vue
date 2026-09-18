<template>
  <div class="system-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="title-group">
        <h2 class="page-title">系统配置</h2>
        <span class="page-desc">管理AI平台、日志与反馈数据</span>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="system-tabs">
      <!-- AI平台配置 -->
      <el-tab-pane name="dify">
        <template #label>
          <div class="tab-label">
            <el-icon><Connection /></el-icon>
            <span>AI平台配置</span>
          </div>
        </template>

        <div class="tab-content">
          <!-- 统计卡片 -->
          <div class="stats-row">
            <div class="stat-card">
              <div class="stat-icon" style="background: linear-gradient(135deg, var(--primary-500), var(--primary-700))">
                <el-icon :size="20"><Cpu /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ modelConfigList.length }}</div>
                <div class="stat-label">配置总数</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon" style="background: linear-gradient(135deg, var(--success-400), var(--success-600))">
                <el-icon :size="20"><CircleCheck /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ enabledModelCount }}</div>
                <div class="stat-label">已启用</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon" style="background: linear-gradient(135deg, var(--gray-400), var(--gray-600))">
                <el-icon :size="20"><Warning /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ modelConfigList.length - enabledModelCount }}</div>
                <div class="stat-label">未启用</div>
              </div>
            </div>
          </div>

          <el-card shadow="never" class="content-card">
            <template #header>
              <div class="card-header">
                <div class="card-title">
                  <el-icon><Setting /></el-icon>
                  <span>AI平台配置管理</span>
                </div>
                <el-button type="primary" @click="showAddModelDialog" class="add-btn">
                  <el-icon><Plus /></el-icon>新增配置
                </el-button>
              </div>
            </template>
            <el-table :data="modelConfigList" border class="data-table" style="width: 100%">
              <el-table-column prop="id" label="ID" width="60" align="center">
                <template #default="{ row }">
                  <span class="id-badge">{{ row.id }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="difyBaseUrl" label="API地址" min-width="200">
                <template #default="{ row }">
                  <code class="url-code">{{ row.difyBaseUrl }}</code>
                </template>
              </el-table-column>
              <el-table-column prop="difyAppApiKey" label="App API Key" width="200">
                <template #default="{ row }">
                  <el-input v-model="row.difyAppApiKey" type="password" show-password size="small" />
                </template>
              </el-table-column>
              <el-table-column prop="difyDatasetApiKey" label="Dataset API Key" width="200">
                <template #default="{ row }">
                  <el-input v-model="row.difyDatasetApiKey" type="password" show-password size="small" />
                </template>
              </el-table-column>
              <el-table-column prop="difyDatasetId" label="知识库ID" width="180">
                <template #default="{ row }">
                  <code v-if="row.difyDatasetId" class="dataset-id-code">{{ row.difyDatasetId }}</code>
                  <span v-else class="placeholder">—</span>
                </template>
              </el-table-column>
              <el-table-column prop="enable" label="状态" width="90" align="center">
                <template #default="{ row }">
                  <el-switch
                    v-model="row.enable"
                    :active-value="1"
                    :inactive-value="0"
                    @change="handleToggleEnable(row)"
                  />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="140" align="center" fixed="right">
                <template #default="{ row }">
                  <div class="action-group">
                    <el-button type="primary" link size="small" @click="editModelConfig(row)">
                      <el-icon><Edit /></el-icon>编辑
                    </el-button>
                    <el-button type="danger" link size="small" @click="deleteModelConfig(row.id)">
                      <el-icon><Delete /></el-icon>删除
                    </el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </div>
      </el-tab-pane>

      <!-- 操作日志 -->
      <el-tab-pane name="log">
        <template #label>
          <div class="tab-label">
            <el-icon><Document /></el-icon>
            <span>操作日志</span>
          </div>
        </template>

        <div class="tab-content">
          <el-card shadow="never" class="content-card">
            <template #header>
              <div class="card-header">
                <div class="card-title">
                  <el-icon><Document /></el-icon>
                  <span>操作日志</span>
                  <span class="log-count">共 {{ logTotal }} 条记录</span>
                </div>
                <div class="card-actions">
                  <el-button
                    v-if="userStore.isAdmin()"
                    type="danger"
                    plain
                    size="small"
                    :icon="Delete"
                    :disabled="selectedLogIds.length === 0"
                    @click="handleBatchDelete"
                  >
                    批量删除 ({{ selectedLogIds.length }})
                  </el-button>
                  <el-button
                    v-if="userStore.isAdmin()"
                    type="danger"
                    plain
                    size="small"
                    :icon="Delete"
                    @click="handleClearLogs"
                  >
                    清空全部
                  </el-button>
                </div>
              </div>
            </template>
            <el-table
              :data="logList"
              border
              class="data-table"
              style="width: 100%"
              @selection-change="handleLogSelectionChange"
            >
              <el-table-column v-if="userStore.isAdmin()" type="selection" width="50" align="center" />
              <el-table-column prop="id" label="ID" width="70" align="center">
                <template #default="{ row }">
                  <span class="id-badge">{{ row.id }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="nickname" label="用户" width="100" align="center">
                <template #default="{ row }">
                  <span>{{ row.nickname || row.userId }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="operType" label="操作类型" width="110" align="center">
                <template #default="{ row }">
                  <el-tag
                    :type="operTypeTag(row.operType)"
                    size="small"
                    effect="plain"
                    class="oper-tag"
                  >
                    {{ row.operType }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="content" label="操作内容" min-width="300" show-overflow-tooltip>
                <template #default="{ row }">
                  <code class="content-code">{{ row.content }}</code>
                </template>
              </el-table-column>
              <el-table-column prop="ipAddress" label="IP地址" width="140">
                <template #default="{ row }">
                  <span class="ip-text">{{ row.ipAddress }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="operTime" label="操作时间" width="180" align="center">
                <template #default="{ row }">
                  <span class="time-text">{{ row.operTime }}</span>
                </template>
              </el-table-column>
              <el-table-column v-if="userStore.isAdmin()" label="操作" width="100" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button
                    type="danger"
                    link
                    size="small"
                    :icon="Delete"
                    @click="handleDeleteLog(row)"
                  >
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="pagination-container">
              <el-pagination
                v-model:current-page="logPageNum"
                v-model:page-size="logPageSize"
                :total="logTotal"
                :page-sizes="[10, 20, 50, 100]"
                layout="total, sizes, prev, pager, next, jumper"
                @size-change="fetchLogs"
                @current-change="fetchLogs"
              />
            </div>
          </el-card>
        </div>
      </el-tab-pane>

      <!-- AI反馈统计 -->
      <el-tab-pane name="feedback">
        <template #label>
          <div class="tab-label">
            <el-icon><DataAnalysis /></el-icon>
            <span>AI反馈统计</span>
          </div>
        </template>

        <div class="tab-content">
          <!-- 反馈统计卡片 -->
          <div class="stats-row">
            <div class="stat-card">
              <div class="stat-icon" style="background: linear-gradient(135deg, var(--primary-500), var(--primary-700))">
                <el-icon :size="20"><ChatDotRound /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ feedbackStats.totalFeedback }}</div>
                <div class="stat-label">总反馈数</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon" style="background: linear-gradient(135deg, var(--success-400), var(--success-600))">
                <el-icon :size="20"><Select /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ feedbackStats.correctCount }}</div>
                <div class="stat-label">正确数</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon" style="background: linear-gradient(135deg, var(--danger-400), var(--danger-600))">
                <el-icon :size="20"><CloseBold /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ feedbackStats.errorCount }}</div>
                <div class="stat-label">错误数</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon" style="background: linear-gradient(135deg, var(--accent-400), var(--accent-600))">
                <el-icon :size="20"><TrendCharts /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ feedbackStats.correctRate }}<span class="stat-suffix">%</span></div>
                <div class="stat-label">正确率</div>
              </div>
            </div>
          </div>

          <el-card shadow="never" class="content-card">
            <template #header>
              <div class="card-header">
                <div class="card-title">
                  <el-icon><List /></el-icon>
                  <span>反馈记录</span>
                </div>
              </div>
            </template>
            <el-table :data="feedbackList" border class="data-table" style="width: 100%">
              <el-table-column prop="id" label="ID" width="70" align="center">
                <template #default="{ row }">
                  <span class="id-badge">{{ row.id }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="userId" label="用户ID" width="90" align="center" />
              <el-table-column prop="rating" label="评分" width="100" align="center">
                <template #default="{ row }">
                  <span :class="['status-dot', row.rating === 1 ? 'dot-success' : 'dot-danger']"></span>
                  <el-tag :type="row.rating === 1 ? 'success' : 'danger'" size="small" effect="plain">
                    {{ row.rating === 1 ? '正确' : '错误' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="feedbackType" label="反馈类型" width="120" align="center" />
              <el-table-column prop="feedbackContent" label="反馈内容" show-overflow-tooltip />
              <el-table-column prop="createTime" label="反馈时间" width="180" align="center">
                <template #default="{ row }">
                  <span class="time-text">{{ row.createTime }}</span>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </div>
      </el-tab-pane>

      <!-- 上传配置 -->
      <el-tab-pane name="upload">
        <template #label>
          <div class="tab-label">
            <el-icon><Upload /></el-icon>
            <span>上传配置</span>
          </div>
        </template>

        <div class="tab-content">
          <el-card shadow="never" class="content-card">
            <template #header>
              <div class="card-header">
                <div class="card-title">
                  <el-icon><Upload /></el-icon>
                  <span>对话上传开关配置</span>
                </div>
              </div>
            </template>
            <el-form label-width="160px" class="upload-config-form">
              <el-form-item label="图片上传">
                <el-switch v-model="uploadConfigForm.enableImageUpload" active-text="开启" inactive-text="关闭" />
              </el-form-item>
              <el-form-item label="允许的图片格式">
                <el-input v-model="uploadConfigForm.allowedImageTypes" placeholder="逗号分隔，如：jpg,jpeg,png,gif,webp,svg" />
              </el-form-item>
              <el-form-item label="视频上传">
                <el-switch v-model="uploadConfigForm.enableVideoUpload" active-text="开启" inactive-text="关闭" />
              </el-form-item>
              <el-form-item label="允许的视频格式">
                <el-input v-model="uploadConfigForm.allowedVideoTypes" placeholder="逗号分隔，如：mp4,mov,mpeg,webm" />
              </el-form-item>
              <el-form-item label="音频上传">
                <el-switch v-model="uploadConfigForm.enableAudioUpload" active-text="开启" inactive-text="关闭" />
              </el-form-item>
              <el-form-item label="允许的音频格式">
                <el-input v-model="uploadConfigForm.allowedAudioTypes" placeholder="逗号分隔，如：mp3,m4a,wav,webm,mpga" />
              </el-form-item>
              <el-form-item label="文件上传">
                <el-switch v-model="uploadConfigForm.enableFileUpload" active-text="开启" inactive-text="关闭" />
              </el-form-item>
              <el-form-item label="允许的文件格式">
                <el-input v-model="uploadConfigForm.allowedFileTypes" placeholder="逗号分隔，如：txt,md,pdf,xls,xlsx,doc,docx" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="saveUploadConfig" :loading="uploadConfigLoading">保存配置</el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 新增/编辑AI平台配置对话框 -->
    <el-dialog
      v-model="modelDialogVisible"
      :title="isEditMode ? '编辑AI平台配置' : '新增AI平台配置'"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form :model="modelForm" label-width="140px" :rules="modelRules" ref="modelFormRef" class="model-form">
        <el-form-item label="API地址" prop="difyBaseUrl">
          <el-input v-model="modelForm.difyBaseUrl" placeholder="例如：http://192.168.110.107" />
        </el-form-item>
        <el-form-item label="App API Key" prop="difyAppApiKey">
          <el-input v-model="modelForm.difyAppApiKey" type="password" show-password placeholder="请输入App API Key" />
        </el-form-item>
        <el-form-item label="Dataset API Key" prop="difyDatasetApiKey">
          <el-input v-model="modelForm.difyDatasetApiKey" type="password" show-password placeholder="请输入Dataset API Key" />
        </el-form-item>
        <el-form-item label="知识库ID" prop="difyDatasetId">
          <el-input v-model="modelForm.difyDatasetId" placeholder="请输入默认知识库ID" />
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="modelForm.enable" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modelDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveModelConfig" :loading="modelLoading">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Edit, Delete, Setting, Connection, Cpu, CircleCheck, Warning,
  Document, DataAnalysis, ChatDotRound, Select, CloseBold, TrendCharts, List, Upload
} from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const activeTab = ref('dify')

const logList = ref<any[]>([])
const logPageNum = ref(1)
const logPageSize = ref(10)
const logTotal = ref(0)
const selectedLogIds = ref<number[]>([])

// AI平台配置管理
const modelConfigList = ref<any[]>([])
const modelDialogVisible = ref(false)
const isEditMode = ref(false)
const modelLoading = ref(false)
const modelFormRef = ref()

const enabledModelCount = computed(() => modelConfigList.value.filter(m => m.enable === 1).length)

const modelForm = reactive({
  id: 0,
  difyBaseUrl: '',
  difyAppApiKey: '',
  difyDatasetApiKey: '',
  difyDatasetId: '',
  enable: 1
})

const modelRules = {
  difyBaseUrl: [{ required: true, message: '请输入AI URL地址', trigger: 'blur' }],
  difyAppApiKey: [{ required: true, message: '请输入App API Key', trigger: 'blur' }],
  difyDatasetApiKey: [{ required: true, message: '请输入Dataset API Key', trigger: 'blur' }]
}

// AI反馈统计
const feedbackStats = reactive({
  totalFeedback: 0,
  correctCount: 0,
  errorCount: 0,
  correctRate: 0
})
const feedbackList = ref<any[]>([])

const operTypeTag = (type: string) => {
  const map: Record<string, string> = {
    GET: 'success',
    POST: 'primary',
    PUT: 'warning',
    DELETE: 'danger',
    PATCH: 'warning'
  }
  return map[type] || 'info'
}

const fetchLogs = async () => {
  try {
    const res: any = await request.get('/system/log', {
      params: {
        pageNum: logPageNum.value,
        pageSize: logPageSize.value
      }
    })
    if (res.data) {
      logList.value = res.data.records || []
      logTotal.value = res.data.total || 0
    }
  } catch (error) {
    console.error('获取日志失败', error)
  }
}

// 表格选择变化
const handleLogSelectionChange = (selection: any[]) => {
  selectedLogIds.value = selection.map(item => item.id)
}

// 删除单条日志
const handleDeleteLog = (row: any) => {
  ElMessageBox.confirm(
    `确定要删除该条操作日志吗？（ID: ${row.id}）`,
    '删除确认',
    { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
  ).then(async () => {
    try {
      await request.delete(`/system/log/${row.id}`)
      ElMessage.success('删除成功')
      // 若当前页删除后无数据，回到上一页
      if (logList.value.length === 1 && logPageNum.value > 1) {
        logPageNum.value--
      }
      fetchLogs()
    } catch (error) {
      console.error('删除日志失败', error)
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

// 批量删除日志
const handleBatchDelete = () => {
  if (selectedLogIds.value.length === 0) {
    ElMessage.warning('请先选择要删除的日志')
    return
  }
  const selectedCount = selectedLogIds.value.length
  const isAllCurrentPage = selectedCount === logList.value.length
  ElMessageBox.confirm(
    `确定要删除选中的 ${selectedCount} 条操作日志吗？`,
    '批量删除确认',
    { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
  ).then(async () => {
    try {
      await request.delete('/system/log/batch', { data: selectedLogIds.value })
      ElMessage.success('批量删除成功')
      selectedLogIds.value = []
      // 若当前页全部删除且不是第一页，回到上一页
      if (isAllCurrentPage && logPageNum.value > 1) {
        logPageNum.value--
      }
      fetchLogs()
    } catch (error) {
      console.error('批量删除日志失败', error)
      ElMessage.error('批量删除失败')
    }
  }).catch(() => {})
}

// 清空所有日志
const handleClearLogs = () => {
  ElMessageBox.confirm(
    '确定要清空所有操作日志吗？此操作不可恢复！',
    '清空确认',
    { type: 'error', confirmButtonText: '确定清空', cancelButtonText: '取消' }
  ).then(async () => {
    try {
      await request.delete('/system/log/clear')
      ElMessage.success('已清空所有操作日志')
      logPageNum.value = 1
      fetchLogs()
    } catch (error) {
      console.error('清空日志失败', error)
      ElMessage.error('清空失败')
    }
  }).catch(() => {})
}

const fetchModelConfigs = async () => {
  try {
    const res: any = await request.get('/system/model')
    modelConfigList.value = res.data || []
  } catch (error) {
    console.error('获取AI平台配置失败', error)
  }
}

const fetchFeedbackStats = async () => {
  try {
    const res: any = await request.get('/ai/feedback/stats')
    if (res.data) {
      Object.assign(feedbackStats, res.data)
    }
  } catch (error) {
    console.error('获取AI反馈统计失败', error)
  }
}

const fetchFeedbackList = async () => {
  try {
    const res: any = await request.get('/ai/feedback/list', { params: { pageNum: 1, pageSize: 50 } })
    feedbackList.value = res.data || []
  } catch (error) {
    console.error('获取AI反馈列表失败', error)
  }
}

const showAddModelDialog = () => {
  isEditMode.value = false
  modelForm.id = 0
  modelForm.difyBaseUrl = ''
  modelForm.difyAppApiKey = ''
  modelForm.difyDatasetApiKey = ''
  modelForm.difyDatasetId = ''
  modelForm.enable = 1
  modelDialogVisible.value = true
}

const editModelConfig = (row: any) => {
  isEditMode.value = true
  modelForm.id = row.id
  modelForm.difyBaseUrl = row.difyBaseUrl || ''
  modelForm.difyAppApiKey = row.difyAppApiKey || ''
  modelForm.difyDatasetApiKey = row.difyDatasetApiKey || ''
  modelForm.difyDatasetId = row.difyDatasetId || ''
  modelForm.enable = row.enable
  modelDialogVisible.value = true
}

const saveModelConfig = async () => {
  if (!modelFormRef.value) return

  await modelFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return

    modelLoading.value = true
    try {
      await request.post('/system/model', modelForm)
      ElMessage.success('保存成功')
      modelDialogVisible.value = false
      fetchModelConfigs()
    } catch (error) {
      ElMessage.error('保存失败')
      console.error('保存AI平台配置失败', error)
    } finally {
      modelLoading.value = false
    }
  })
}

const handleToggleEnable = async (row: any) => {
  try {
    await request.put(`/system/model/${row.id}/toggle/${row.enable}`)
    ElMessage.success('状态切换成功')
    fetchModelConfigs()
  } catch (error) {
    ElMessage.error('状态切换失败')
    console.error('切换状态失败', error)
  }
}

const deleteModelConfig = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除该配置吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await request.delete(`/system/model/${id}`)
    ElMessage.success('删除成功')
    fetchModelConfigs()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
      console.error('删除AI平台配置失败', error)
    }
  }
}

// 监听tab切换，加载对应数据
watch(activeTab, (newVal) => {
  if (newVal === 'dify') {
    fetchModelConfigs()
  } else if (newVal === 'log') {
    fetchLogs()
  } else if (newVal === 'feedback') {
    fetchFeedbackStats()
    fetchFeedbackList()
  } else if (newVal === 'upload') {
    fetchUploadConfig()
  }
})

// ========== 上传配置管理 ==========
const uploadConfigForm = reactive({
  enableImageUpload: true,
  enableVideoUpload: true,
  enableAudioUpload: true,
  enableFileUpload: true,
  allowedImageTypes: '',
  allowedVideoTypes: '',
  allowedAudioTypes: '',
  allowedFileTypes: 'txt,md,pdf,xls,xlsx,doc,docx'
})
const uploadConfigLoading = ref(false)

const fetchUploadConfig = async () => {
  try {
    const res: any = await request.get('/system/upload-config')
    if (res.data) {
      Object.assign(uploadConfigForm, res.data)
    }
  } catch (error) {
    console.error('获取上传配置失败', error)
  }
}

const saveUploadConfig = async () => {
  uploadConfigLoading.value = true
  try {
    await request.post('/system/upload-config', {
      enableImageUpload: uploadConfigForm.enableImageUpload,
      enableVideoUpload: uploadConfigForm.enableVideoUpload,
      enableAudioUpload: uploadConfigForm.enableAudioUpload,
      enableFileUpload: uploadConfigForm.enableFileUpload,
      allowedImageTypes: uploadConfigForm.allowedImageTypes,
      allowedVideoTypes: uploadConfigForm.allowedVideoTypes,
      allowedAudioTypes: uploadConfigForm.allowedAudioTypes,
      allowedFileTypes: uploadConfigForm.allowedFileTypes
    })
    ElMessage.success('上传配置保存成功')
  } catch (error) {
    ElMessage.error('保存失败')
    console.error('保存上传配置失败', error)
  } finally {
    uploadConfigLoading.value = false
  }
}

onMounted(() => {
  fetchModelConfigs()
  fetchLogs()
})
</script>

<style scoped>
.system-page {
  padding: 0;
  animation: pageIn 0.35s var(--ease-out-expo);
}

@keyframes pageIn {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ============ 页面头部 ============ */
.page-header {
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

.page-desc {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
  font-weight: var(--weight-medium);
}

/* ============ Tab 样式 ============ */
.system-tabs :deep(.el-tabs__header) {
  margin-bottom: var(--space-6);
}

.system-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: var(--border-light);
}

.system-tabs :deep(.el-tabs__item) {
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  color: var(--text-secondary);
  padding: 0 20px;
  height: 44px;
  line-height: 44px;
}

.system-tabs :deep(.el-tabs__item.is-active) {
  color: var(--primary-600);
  font-weight: var(--weight-semibold);
}

.system-tabs :deep(.el-tabs__active-bar) {
  height: 2px;
  border-radius: 1px;
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
}

.tab-content {
  animation: tabIn 0.25s var(--ease-out-quart);
}

@keyframes tabIn {
  from { opacity: 0; transform: translateX(-8px); }
  to { opacity: 1; transform: translateX(0); }
}

/* ============ 统计卡片 ============ */
.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
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
  width: 44px;
  height: 44px;
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

.stat-suffix {
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  color: var(--text-tertiary);
  margin-left: 2px;
}

.stat-label {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  margin-top: 4px;
  font-weight: var(--weight-medium);
  letter-spacing: var(--tracking-wide);
}

/* ============ 内容卡片 ============ */
.content-card {
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
}

.content-card :deep(.el-card__header) {
  padding: var(--space-4) var(--space-5);
  background: var(--gray-50);
  border-bottom: 1px solid var(--border-light);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--text-primary);
}

.log-count {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  background: var(--gray-100);
  padding: 2px 8px;
  border-radius: var(--radius-full);
  margin-left: var(--space-2);
  font-weight: var(--weight-medium);
}

.card-actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.add-btn {
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25);
}

/* ============ 表格样式 ============ */
.data-table {
  border-radius: var(--radius-md);
  overflow: hidden;
}

.data-table :deep(th.el-table__cell) {
  background: var(--gray-50) !important;
  color: var(--text-secondary);
  font-weight: var(--weight-semibold);
  font-size: var(--text-xs);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  padding: 12px 0;
}

.data-table :deep(td.el-table__cell) {
  padding: 10px 0;
}

.data-table :deep(.el-table__row:hover > td) {
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

/* URL 代码 */
.url-code {
  display: inline-block;
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--primary-700);
  background: var(--primary-50);
  padding: 2px 8px;
  border-radius: var(--radius-xs);
  border: 1px solid var(--primary-100);
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dataset-id-code {
  display: inline-block;
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--text-secondary);
  background: var(--gray-50);
  padding: 2px 6px;
  border-radius: var(--radius-xs);
  border: 1px solid var(--border-light);
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 操作类型标签 */
.oper-tag {
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: var(--weight-semibold);
  letter-spacing: 0.03em;
}

/* 内容代码 */
.content-code {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--text-secondary);
}

.ip-text {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--text-secondary);
}

.time-text {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  font-family: var(--font-mono);
  font-size: 12px;
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

.dot-danger {
  background: var(--danger-400);
  box-shadow: 0 0 6px rgba(239, 68, 68, 0.3);
}

/* 操作按钮组 */
.action-group {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

/* 分页 */
.pagination-container {
  display: flex;
  justify-content: flex-end;
  padding: var(--space-4) 0 0;
}

/* ============ 对话框表单 ============ */
.model-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

/* ============ 上传配置表单 ============ */
.upload-config-form :deep(.el-form-item) {
  margin-bottom: 24px;
}

.config-hint {
  margin-left: 12px;
  font-size: 12px;
  color: var(--text-tertiary);
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
}
</style>
