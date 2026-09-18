<template>
  <div class="page-container">
    <el-card class="main-card">
      <template #header>
        <div class="card-header">
          <div class="card-title">
            <el-icon><Checked /></el-icon>
            <span>案例审核</span>
          </div>
        </div>
      </template>

      <!-- 筛选条件 -->
      <div class="search-bar">
        <el-form :inline="true" :model="queryParams">
          <el-form-item label="审核状态">
            <el-select v-model="queryParams.auditStatus" placeholder="全部" clearable style="width: 120px">
              <el-option label="全部" :value="undefined" />
              <el-option label="待审核" :value="0" />
              <el-option label="已通过" :value="1" />
              <el-option label="已驳回" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="fetchList">查询</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 案例列表 -->
      <el-table :data="auditList" border stripe v-loading="loading" class="data-table">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="title" label="案例标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="equipModel" label="设备型号" width="140" />
        <el-table-column prop="uploadUserName" label="上传人" width="100" align="center" />
        <el-table-column prop="auditStatus" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.auditStatus)">
              {{ getStatusText(row.auditStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="知识库文档" width="140" align="center">
          <template #default="{ row }">
            <template v-if="row.auditStatus !== 1">
              <el-tag type="info" size="small">-</el-tag>
            </template>
            <template v-else-if="docStatusMap[row.id] === undefined">
              <el-tag type="warning" size="small">检测中...</el-tag>
            </template>
            <template v-else-if="docStatusMap[row.id]?.exists">
              <el-tag type="success" size="small">文档正常</el-tag>
            </template>
            <template v-else>
              <el-tooltip :content="docStatusMap[row.id]?.reason || '未知'" placement="top">
                <el-tag type="danger" size="small">文档已删除</el-tag>
              </el-tooltip>
            </template>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="上传时间" width="170" align="center" :formatter="formatDateTime" />
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <template v-if="row.auditStatus === 0">
              <el-button type="primary" link size="small" @click="handleAudit(row)">
                <el-icon><Edit /></el-icon>审核
              </el-button>
            </template>
            <template v-if="userStore.isAdmin() && ((docStatusMap[row.id] && !docStatusMap[row.id].exists && row.auditStatus === 1) || row.auditStatus === 2)">
              <el-button type="danger" link size="small" @click="handleDelete(row)">
                <el-icon><Delete /></el-icon>删除
              </el-button>
            </template>
            <el-button type="primary" link size="small" @click="handleView(row)">
              <el-icon><View /></el-icon>查看
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
          layout="total, sizes, prev, pager, next"
          @size-change="fetchList"
          @current-change="fetchList"
        />
      </div>
    </el-card>

    <!-- 审核对话框 -->
    <el-dialog v-model="auditDialogVisible" title="案例审核" width="1100px" top="3vh">
      <el-alert title="审核时可修改案例内容，修改后将同步更新到知识库" type="info" :closable="false" style="margin-bottom: 16px" />

      <div class="audit-layout">
        <!-- 左侧：相似度检测结果 -->
        <div class="audit-left">
          <div class="panel-title">检测结果</div>

          <!-- 相似度检测 -->
          <div v-if="similarityResult" class="similarity-section">
            <div class="similarity-header">知识库相似度检测</div>
            <div class="similarity-bar-wrapper">
              <div class="similarity-bar">
                <div
                  class="similarity-fill"
                  :style="{ width: (similarityResult.similarity * 100) + '%' }"
                  :class="{
                    'high-similarity': similarityResult.level === 'high',
                    'medium-similarity': similarityResult.level === 'medium'
                  }"
                />
              </div>
              <span class="similarity-score" :class="{
                'score-high': similarityResult.level === 'high',
                'score-medium': similarityResult.level === 'medium'
              }">{{ (similarityResult.similarity * 100).toFixed(1) }}%</span>
            </div>
            <div class="similarity-msg" :class="{
              'msg-high': similarityResult.level === 'high',
              'msg-medium': similarityResult.level === 'medium'
            }">
              {{ similarityResult.message }}
            </div>
            <!-- 展示匹配到的文档分段 -->
            <div v-if="similarityResult.topDocuments && similarityResult.topDocuments.length > 0" class="matched-docs">
              <div class="matched-docs-title">
                匹配到 {{ similarityResult.topDocuments.length }} 个相似分段：
              </div>
              <div v-for="(doc, idx) in similarityResult.topDocuments" :key="idx" class="matched-doc-item">
                <div class="matched-doc-header">
                  <el-tag size="small" :type="getDocTagType(doc.score)">{{ (doc.score * 100).toFixed(1) }}%</el-tag>
                  <span class="doc-name" :title="doc.documentName">{{ doc.documentName || '未知文档' }}</span>
                  <span class="segment-label">分段 {{ idx + 1 }}</span>
                </div>
                <div v-if="doc.segmentContent" class="segment-content">
                  <div class="segment-content-label">匹配内容：</div>
                  <div class="segment-content-text">{{ truncateText(doc.segmentContent, 150) }}</div>
                </div>
              </div>
            </div>
          </div>
          <div v-else-if="similarityLoading" class="similarity-loading">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>正在检测案例相似度...</span>
          </div>
        </div>

        <!-- 右侧：表达内容（可编辑表单） -->
        <div class="audit-right">
          <div class="panel-title">案例内容</div>
          <el-form :model="auditForm" label-width="80px" class="audit-form">
            <el-form-item label="案例标题">
              <el-input v-model="auditForm.title" />
            </el-form-item>
            <el-form-item label="设备型号">
              <el-input v-model="auditForm.equipModel" />
            </el-form-item>
            <el-form-item label="故障描述">
              <el-input v-model="auditForm.symptom" type="textarea" :rows="3" />
            </el-form-item>
            <el-form-item label="解决方案">
              <el-input v-model="auditForm.solution" type="textarea" :rows="3" />
            </el-form-item>
            <el-form-item label="审核备注">
              <el-input v-model="auditForm.remark" type="textarea" :rows="2" placeholder="驳回时请填写原因" />
            </el-form-item>
            <el-form-item v-if="auditImageList.length > 0" label="案例图片">
              <div class="audit-images">
                <el-image
                  v-for="(url, idx) in auditImageList"
                  :key="idx"
                  :src="url"
                  :preview-src-list="auditImageList"
                  :initial-index="idx"
                  fit="cover"
                  class="audit-image-item"
                  preview-teleported
                />
              </div>
            </el-form-item>
          </el-form>
        </div>
      </div>

      <template #footer>
        <el-button @click="auditDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="handleReject">驳回</el-button>
        <el-button type="success" @click="handleApprove">通过并入库</el-button>
      </template>
    </el-dialog>

    <!-- 查看详情对话框 -->
    <el-dialog v-model="viewDialogVisible" title="案例详情" width="700px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="案例标题">{{ currentCase?.title }}</el-descriptions-item>
        <el-descriptions-item label="设备型号">{{ currentCase?.equipModel }}</el-descriptions-item>
        <el-descriptions-item label="上传人">{{ currentCase?.uploadUserName }}</el-descriptions-item>
        <el-descriptions-item label="审核状态">
          <el-tag :type="getStatusType(currentCase?.auditStatus)">
            {{ getStatusText(currentCase?.auditStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="上传时间" :span="2">{{ formatTime(currentCase?.createTime) }}</el-descriptions-item>
        <el-descriptions-item v-if="currentCase?.auditRemark" label="审核备注" :span="2">
          {{ currentCase.auditRemark }}
        </el-descriptions-item>
      </el-descriptions>
      <div class="detail-content">
        <h4>故障描述：</h4>
        <div class="content-text">{{ currentCase?.symptom || currentCase?.content }}</div>
        <h4 style="margin-top: 16px">解决方案：</h4>
        <div class="content-text">{{ currentCase?.solution || '未填写' }}</div>
        <template v-if="viewImageList.length > 0">
          <h4 style="margin-top: 16px">案例图片：</h4>
          <div class="audit-images">
            <el-image
              v-for="(url, idx) in viewImageList"
              :key="idx"
              :src="url"
              :preview-src-list="viewImageList"
              :initial-index="idx"
              fit="cover"
              class="audit-image-item"
              preview-teleported
            />
          </div>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { checkSimilarity } from '@/api'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const loading = ref(false)
const auditDialogVisible = ref(false)
const viewDialogVisible = ref(false)
const auditList = ref<any[]>([])
const currentCase = ref<any>(null)
const total = ref(0)
const docStatusMap = ref<Record<number, { exists: boolean; reason: string }>>({})
const similarityLoading = ref(false)
const similarityResult = ref<{
  similarity: number
  level: string
  isDuplicate: boolean
  message: string
  topDocuments: Array<{ documentId: string; documentName: string; score: number; segmentContent: string }>
} | null>(null)

const queryParams = reactive({
  auditStatus: undefined as number | undefined,
  pageNum: 1,
  pageSize: 10
})

const auditForm = reactive({
  knowledgeId: null as number | null,
  title: '',
  content: '',
  symptom: '',
  solution: '',
  equipModel: '',
  remark: '',
  images: '' as string
})

const auditImageList = computed(() => {
  try {
    const arr = JSON.parse(auditForm.images)
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
})

const viewImageList = computed(() => {
  const raw = currentCase.value?.images
  if (!raw) return []
  try {
    const arr = JSON.parse(raw)
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
})

const getStatusType = (status: number) => {
  const map: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'danger' }
  return map[status] || 'info'
}

const getStatusText = (status: number) => {
  const map: Record<number, string> = { 0: '待审核', 1: '已通过', 2: '已驳回' }
  return map[status] || '未知'
}

const getDocTagType = (score: number) => {
  if (score > 0.8) return 'danger'
  if (score > 0.6) return 'warning'
  return 'info'
}

const truncateText = (text: string, maxLen: number) => {
  if (!text) return ''
  return text.length > maxLen ? text.substring(0, maxLen) + '...' : text
}

const formatDateTime = (row: any) => {
  if (!row.createTime) return ''
  return formatTime(row.createTime)
}

const formatTime = (time: any) => {
  if (!time) return ''
  const d = new Date(time)
  if (isNaN(d.getTime())) return ''
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const fetchList = async () => {
  loading.value = true
  try {
    const params: any = { pageNum: queryParams.pageNum, pageSize: queryParams.pageSize }
    if (queryParams.auditStatus !== undefined) {
      params.auditStatus = queryParams.auditStatus
    }
    const res: any = await request.get('/knowledge/audit/list', { params })
    auditList.value = res.data?.records || []
    total.value = res.data?.total || 0
    // 加载完成后检查已通过案例的文档状态
    checkDocumentStatus()
  } catch (error) {
    console.error('获取列表失败', error)
  } finally {
    loading.value = false
  }
}

const checkDocumentStatus = async () => {
  // 筛选出已通过且有 documentId 的案例
  const toCheck = auditList.value.filter(
    (item: any) => item.auditStatus === 1
  ).map((item: any) => item.id)
  if (toCheck.length === 0) return
  try {
    const res: any = await request.post('/knowledge/check-documents', toCheck)
    if (res.data) {
      docStatusMap.value = { ...docStatusMap.value, ...res.data }
    }
  } catch (error) {
    console.error('检查文档状态失败', error)
  }
}

const handleView = (row: any) => {
  currentCase.value = row
  viewDialogVisible.value = true
}

const handleAudit = (row: any) => {
  currentCase.value = row
  auditForm.knowledgeId = row.id
  auditForm.title = row.title
  auditForm.content = row.content
  auditForm.symptom = row.symptom || ''
  auditForm.solution = row.solution || ''
  auditForm.equipModel = row.equipModel
  auditForm.remark = ''
  auditForm.images = row.images || ''
  auditDialogVisible.value = true

  // 打开弹窗后自动检测相似度
  checkCaseSimilarity(row)
}

const checkCaseSimilarity = async (row: any) => {
  similarityResult.value = null
  similarityLoading.value = true
  try {
    const res: any = await checkSimilarity({
      symptom: row.symptom || row.content || '',
      solution: row.solution || '',
      equipModel: row.equipModel || ''
    })
    if (res.data) {
      similarityResult.value = res.data
    }
  } catch (error) {
    console.error('相似度检测失败', error)
  } finally {
    similarityLoading.value = false
  }
}

const handleApprove = async () => {
  await ElMessageBox.confirm('确认通过该案例审核？通过后将上传到默认知识库', '审核确认', {
    type: 'success'
  })
  
  try {
    await request.post('/knowledge/audit', {
      knowledgeId: auditForm.knowledgeId,
      auditStatus: 1,
      remark: auditForm.remark,
      title: auditForm.title,
      content: auditForm.content,
      symptom: auditForm.symptom,
      solution: auditForm.solution,
      equipModel: auditForm.equipModel
    })
    ElMessage.success('审核通过，案例已上传到知识库')
    auditDialogVisible.value = false
    fetchList()
  } catch (error) {
    console.error('审核失败', error)
  }
}

const handleReject = async () => {
  if (!auditForm.remark) {
    ElMessage.warning('请填写驳回理由')
    return
  }
  
  try {
    await request.post('/knowledge/audit', {
      knowledgeId: auditForm.knowledgeId,
      auditStatus: 2,
      remark: auditForm.remark,
      title: auditForm.title,
      content: auditForm.content,
      symptom: auditForm.symptom,
      solution: auditForm.solution,
      equipModel: auditForm.equipModel
    })
    ElMessage.success('已驳回')
    auditDialogVisible.value = false
    fetchList()
  } catch (error) {
    console.error('驳回失败', error)
  }
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(
      `确认删除案例「${row.title}」？知识库中对应的文档已不存在，删除后将不可恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
    await request.delete(`/knowledge/${row.id}`)
    ElMessage.success('案例已删除')
    fetchList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败', error)
    }
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

.detail-content {
  margin-top: 20px;
  padding: 16px;
  background: var(--bg-color);
  border-radius: 8px;
}

.detail-content h4 {
  margin-bottom: 12px;
  color: var(--text-primary);
}

.content-text {
  line-height: 1.8;
  white-space: pre-wrap;
  color: var(--text-regular);
}

/* 审核弹窗左右布局 */
.audit-layout {
  display: flex;
  gap: 20px;
  min-height: 320px;
  max-height: 55vh;
}

.audit-left {
  flex: 0 0 46%;
  overflow-y: auto;
}

.audit-right {
  flex: 1;
  overflow-y: auto;
}

.panel-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 2px solid #e5e7eb;
  display: flex;
  align-items: center;
  gap: 6px;
}

.panel-title::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 16px;
  background: #6366f1;
  border-radius: 2px;
}

/* 审核表单样式调整 */
.audit-form :deep(.el-form-item) {
  margin-bottom: 12px;
}

.audit-form :deep(.el-form-item__label) {
  font-size: 13px;
  font-weight: 600;
}

/* 相似度检测区域 */
.similarity-section {
  background: var(--bg-color);
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}

.similarity-header {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 12px;
}

.similarity-bar-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.similarity-bar {
  flex: 1;
  height: 10px;
  background: #e5e7eb;
  border-radius: 5px;
  overflow: hidden;
}

.similarity-fill {
  height: 100%;
  background: #22c55e;
  border-radius: 5px;
  transition: width 0.6s ease;
}

.similarity-fill.high-similarity {
  background: #ef4444;
}

.similarity-fill.medium-similarity {
  background: #f59e0b;
}

.similarity-score {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  min-width: 60px;
  text-align: right;
}

.similarity-score.score-high {
  color: #ef4444;
}

.similarity-score.score-medium {
  color: #f59e0b;
}

.similarity-msg {
  font-size: 13px;
  color: var(--text-secondary);
}

.similarity-msg.msg-high {
  color: #ef4444;
  font-weight: 500;
}

.similarity-msg.msg-medium {
  color: #f59e0b;
  font-weight: 500;
}

.similarity-msg.msg-medium {
  color: #f59e0b;
  font-weight: 500;
}

/* 匹配到的文档分段列表 */
.matched-docs {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e5e7eb;
}

.matched-docs-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.matched-doc-item {
  padding: 8px;
  margin-bottom: 6px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.matched-doc-item:last-child {
  margin-bottom: 0;
}

.matched-doc-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.doc-name {
  color: var(--text-primary);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.segment-label {
  font-size: 11px;
  color: var(--text-secondary);
  background: #f3f4f6;
  padding: 1px 6px;
  border-radius: 3px;
  flex-shrink: 0;
}

/* 分段内容 */
.segment-content {
  margin-top: 8px;
}

.segment-content-label {
  font-size: 11px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.segment-content-text {
  font-size: 12px;
  line-height: 1.6;
  color: var(--text-regular);
  background: #f9fafb;
  border-left: 3px solid #6366f1;
  padding: 8px 12px;
  border-radius: 0 4px 4px 0;
  white-space: pre-wrap;
  word-break: break-all;
}


.similarity-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px;
  color: var(--text-secondary);
  font-size: 13px;
}

.audit-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.audit-image-item {
  width: 100px;
  height: 100px;
  border-radius: 6px;
  border: 1px solid var(--border-color, #e4e7ed);
  cursor: pointer;
}
</style>
