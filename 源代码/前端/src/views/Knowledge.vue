<template>
  <div class="knowledge-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h1>知识库管理</h1>
        <p>查看和管理知识库文档，支持数据检索</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" plain class="header-btn" @click="showUploadDialog = true">
          <el-icon><Upload /></el-icon>上传文档
        </el-button>
      </div>
    </div>

    <!-- 知识库选择 -->
    <div class="dataset-section">
      <div class="dataset-scroll">
        <div
          v-for="dataset in datasetList"
          :key="dataset.id"
          class="dataset-card"
          :class="{ active: selectedDataset === dataset.id }"
          @click="selectDataset(dataset)"
        >
          <div class="dataset-icon">
            <el-icon size="22"><Collection /></el-icon>
          </div>
          <div class="dataset-info">
            <h3>{{ dataset.name }}</h3>
            <div class="dataset-meta">
              <span><el-icon><Document /></el-icon> {{ dataset.document_count }} 篇</span>
              <span><el-icon><Edit /></el-icon> {{ formatWordCount(dataset.word_count) }} 词</span>
            </div>
          </div>
          <el-button
            v-if="canDelete"
            class="dataset-edit-btn"
            link
            size="small"
            @click.stop="showEditDatasetDialog(dataset)"
          >
            <el-icon><Setting /></el-icon>
          </el-button>
          <div class="dataset-check" v-if="selectedDataset === dataset.id">
            <el-icon><CircleCheck /></el-icon>
          </div>
        </div>
      </div>
    </div>

    <!-- 主内容区 - Tab 切换 -->
    <div class="main-content" v-if="selectedDataset">
      <el-tabs v-model="activeTab" class="knowledge-tabs">
        <!-- 文档管理 Tab -->
        <el-tab-pane label="文档管理" name="documents">
          <template #label>
            <span class="tab-label"><el-icon><Document /></el-icon>文档管理</span>
          </template>
          <div class="tab-content">
            <div class="doc-toolbar">
              <el-input
                v-model="searchKeyword"
                placeholder="搜索文档..."
                prefix-icon="Search"
                clearable
                style="width: 260px"
              />
              <el-tag type="info" effect="plain" size="small">{{ pagination.total }} 篇文档</el-tag>
            </div>

            <div class="document-grid" v-loading="loading">
              <div
                v-for="doc in filteredDocuments"
                :key="doc.id"
                class="document-card"
              >
                <div class="doc-icon">
                  <el-icon size="20"><Document /></el-icon>
                </div>
                <div class="doc-info">
                  <h4>{{ doc.name }}</h4>
                  <div class="doc-meta">
                    <span>{{ doc.word_count }} 词</span>
                    <span>命中 {{ doc.hit_count }} 次</span>
                  </div>
                </div>
                <div class="doc-status">
                  <el-tag :type="getStatusType(doc.display_status)" size="small">
                    {{ getStatusText(doc.display_status) }}
                  </el-tag>
                </div>
                <div class="doc-actions">
                  <el-button type="primary" link size="small" @click="handleViewSegments(doc)">详情</el-button>
                  <el-button type="success" link size="small" @click="handlePreview(doc)">预览</el-button>
                  <el-button type="warning" link size="small" @click="handleDownload(doc)">下载</el-button>
                  <el-button v-if="canDelete" type="danger" link size="small" @click="handleDelete(doc)">删除</el-button>
                </div>
              </div>

              <div v-if="filteredDocuments.length === 0 && !loading" class="empty-state">
                <el-icon size="48"><Document /></el-icon>
                <p>暂无文档</p>
              </div>
            </div>

            <div class="pagination-wrapper">
              <el-pagination
                v-model:current-page="pagination.page"
                v-model:page-size="pagination.limit"
                :total="pagination.total"
                :page-sizes="[10, 20, 50]"
                layout="total, sizes, prev, pager, next"
                @size-change="fetchDocuments"
                @current-change="fetchDocuments"
              />
            </div>
          </div>
        </el-tab-pane>

        <!-- 数据检索 Tab -->
        <el-tab-pane label="数据检索" name="retrieve">
          <template #label>
            <span class="tab-label"><el-icon><Search /></el-icon>数据检索</span>
          </template>
          <div class="tab-content">
            <div class="retrieve-section">
              <div class="retrieve-input-area">
                <div class="search-bar">
                  <div class="search-input-wrapper">
                    <el-icon class="search-input-icon"><Search /></el-icon>
                    <input
                      v-model="retrieveQuery"
                      class="search-input"
                      placeholder="输入检索关键词，在当前知识库中搜索相关内容..."
                      @keyup.enter="handleRetrieve"
                    />
                    <el-icon
                      v-if="retrieveQuery"
                      class="search-clear-icon"
                      @click="retrieveQuery = ''"
                    ><CircleClose /></el-icon>
                  </div>
                  <el-button
                    type="primary"
                    size="large"
                    :loading="retrieveLoading"
                    class="search-btn"
                    @click="handleRetrieve"
                  >
                    <el-icon><Search /></el-icon>
                    <span>开始检索</span>
                  </el-button>
                </div>
                <div class="retrieve-options">
                  <div class="option-item">
                    <el-icon class="option-icon"><List /></el-icon>
                    <span class="retrieve-label">返回结果数</span>
                    <el-slider v-model="retrieveTopK" :min="1" :max="20" :step="1" class="option-slider" />
                    <span class="option-value">{{ retrieveTopK }} 条</span>
                  </div>
                </div>
              </div>

              <div class="retrieve-results" v-loading="retrieveLoading">
                <div v-if="retrieveResults.length > 0" class="results-header">
                  <span class="results-count">找到 {{ retrieveResults.length }} 条相关结果</span>
                  <el-button text size="small" @click="retrieveResults = []">
                    <el-icon><Delete /></el-icon>清空结果
                  </el-button>
                </div>

                <div v-for="(record, index) in retrieveResults" :key="index" class="result-card">
                  <div class="result-header">
                    <div class="result-rank">
                      <span class="rank-num">{{ index + 1 }}</span>
                    </div>
                    <div class="result-meta">
                      <el-tag size="small" type="primary">相似度 {{ (record.score * 100).toFixed(1) }}%</el-tag>
                      <span v-if="record.document?.name" class="result-doc-name">
                        <el-icon><Document /></el-icon>{{ record.document.name }}
                      </span>
                    </div>
                  </div>
                  <div class="result-content">{{ record.content }}</div>
                  <div v-if="record.keywords && record.keywords.length" class="result-keywords">
                    <el-tag v-for="kw in record.keywords" :key="kw" size="small" type="info" effect="plain" class="kw-tag">
                      {{ kw }}
                    </el-tag>
                  </div>
                </div>

                <div v-if="retrieveSearched && retrieveResults.length === 0 && !retrieveLoading" class="empty-state">
                  <el-icon size="48"><Search /></el-icon>
                  <p>未找到相关内容</p>
                  <p class="empty-hint">请尝试更换关键词或调整检索参数</p>
                </div>

                <div v-if="!retrieveSearched && retrieveResults.length === 0" class="retrieve-placeholder">
                  <el-icon size="64" color="#cbd5e1"><Search /></el-icon>
                  <h3>知识库数据检索</h3>
                  <p>输入关键词，从当前知识库中检索最相关的内容片段</p>
                  <div class="retrieve-tips">
                    <div class="tip-item">
                      <el-icon><InfoFilled /></el-icon>
                      <span>支持自然语言查询</span>
                    </div>
                    <div class="tip-item">
                      <el-icon><InfoFilled /></el-icon>
                      <span>基于向量相似度匹配</span>
                    </div>
                    <div class="tip-item">
                      <el-icon><InfoFilled /></el-icon>
                      <span>可调整返回结果数量</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 未选择提示 -->
    <div v-if="!selectedDataset" class="select-prompt">
      <el-icon size="64"><FolderOpened /></el-icon>
      <h3>请选择知识库</h3>
      <p>选择上方知识库后即可管理文档和检索数据</p>
    </div>

    <!-- 编辑知识库对话框 -->
    <el-dialog v-model="showEditDsDialog" title="编辑知识库" width="480px">
      <el-form label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="editDsForm.name" placeholder="知识库名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="editDsForm.description"
            type="textarea"
            :rows="3"
            placeholder="知识库描述（可选）"
            maxlength="256"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDsDialog = false">取消</el-button>
        <el-button type="primary" :loading="editDsSaving" @click="saveDataset">确认更新</el-button>
      </template>
    </el-dialog>

    <!-- 上传对话框 -->
    <el-dialog v-model="showUploadDialog" title="上传文档" width="500px">
      <el-form label-width="100px">
        <el-form-item label="目标知识库" required>
          <el-select v-model="uploadDatasetId" placeholder="请选择" style="width: 100%">
            <el-option v-for="d in datasetList" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="上传文件">
          <el-upload
            drag
            action="#"
            :auto-upload="false"
            :on-change="handleFileChange"
            :limit="5"
            accept=".docx,.pdf,.csv,.html,.xlsx,.txt,.md"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽或点击上传</div>
            <template #tip>
              <div class="el-upload__tip">支持 DOCX/PDF/CSV/HTML/XLSX/TXT/MD，单个≤15MB</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">上传</el-button>
      </template>
    </el-dialog>

    <!-- 分段详情对话框 -->
    <el-dialog v-model="showSegmentsDialog" :title="`文档分段 - ${currentDoc?.name || ''}`" width="900px" top="5vh">
      <div class="segments-toolbar">
        <el-button v-if="canDelete" type="primary" size="small" @click="handleAddSegment">
          <el-icon><Plus /></el-icon>新增分段
        </el-button>
        <span class="segments-count">共 {{ segmentList.length }} 个分段</span>
      </div>
      <div class="segments-list" v-loading="segmentsLoading">
        <div v-for="(seg, index) in segmentList" :key="seg.id" class="segment-item">
          <div class="segment-item-header">
            <span class="seg-index">分段 {{ index + 1 }}</span>
            <div class="seg-meta">
              <el-tag size="small" type="info">{{ seg.word_count }} 词</el-tag>
              <el-tag size="small" type="success">命中 {{ seg.hit_count }} 次</el-tag>
              <el-tag size="small" :type="seg.enabled === 'true' || seg.enabled === true ? 'success' : 'danger'">
                {{ seg.enabled === 'true' || seg.enabled === true ? '启用' : '禁用' }}
              </el-tag>
            </div>
            <div class="seg-actions">
              <el-button v-if="canDelete" type="primary" link size="small" @click="handleEditSegment(seg)">编辑</el-button>
              <el-button type="info" link size="small" @click="handleViewChildChunks(seg)">子分段</el-button>
              <el-button v-if="canDelete" type="danger" link size="small" @click="handleDeleteSegment(seg)">删除</el-button>
            </div>
          </div>
          <div class="segment-item-content">{{ seg.content }}</div>
        </div>
        <el-empty v-if="segmentList.length === 0 && !segmentsLoading" description="暂无分段" />
      </div>
    </el-dialog>

    <!-- 编辑分段对话框 -->
    <el-dialog v-model="showEditSegmentDialog" :title="editSegmentId ? '编辑分段' : '新增分段'" width="700px">
      <el-form label-width="80px">
        <el-form-item label="内容" required>
          <el-input v-model="editSegmentForm.content" type="textarea" :rows="8" placeholder="分段内容" />
        </el-form-item>
        <el-form-item label="回答">
          <el-input v-model="editSegmentForm.answer" type="textarea" :rows="3" placeholder="可选：问答式分段的回答" />
        </el-form-item>
        <el-form-item label="启用" v-if="editSegmentId">
          <el-switch v-model="editSegmentForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditSegmentDialog = false">取消</el-button>
        <el-button type="primary" :loading="editSegmentSaving" @click="saveSegment">保存</el-button>
      </template>
    </el-dialog>

    <!-- 子分段管理对话框 -->
    <el-dialog v-model="showChildChunksDialog" :title="`子分段管理 - 分段 ${currentSegmentId?.substring(0,8)}...`" width="800px" top="5vh">
      <div class="segments-toolbar">
        <el-button v-if="canDelete" type="primary" size="small" @click="handleAddChildChunk">
          <el-icon><Plus /></el-icon>新增子分段
        </el-button>
        <span class="segments-count">共 {{ childChunkList.length }} 个子分段</span>
      </div>
      <div class="segments-list" v-loading="childChunksLoading">
        <div v-for="(chunk, index) in childChunkList" :key="chunk.id" class="segment-item">
          <div class="segment-item-header">
            <span class="seg-index">子分段 {{ index + 1 }}</span>
            <div class="seg-meta">
              <el-tag size="small" type="info">{{ chunk.word_count }} 词</el-tag>
              <el-tag size="small" :type="chunk.status === 'completed' ? 'success' : 'warning'">
                {{ chunk.status === 'completed' ? '已完成' : chunk.status }}
              </el-tag>
            </div>
            <div class="seg-actions">
              <el-button v-if="canDelete" type="primary" link size="small" @click="handleEditChildChunk(chunk)">编辑</el-button>
              <el-button v-if="canDelete" type="danger" link size="small" @click="handleDeleteChildChunk(chunk)">删除</el-button>
            </div>
          </div>
          <div class="segment-item-content">{{ chunk.content }}</div>
        </div>
        <el-empty v-if="childChunkList.length === 0 && !childChunksLoading" description="暂无子分段" />
      </div>
    </el-dialog>

    <!-- 编辑子分段对话框 -->
    <el-dialog v-model="showEditChildChunkDialog" :title="editChildChunkId ? '编辑子分段' : '新增子分段'" width="700px">
      <el-form label-width="80px">
        <el-form-item label="内容" required>
          <el-input v-model="editChildChunkForm.content" type="textarea" :rows="8" placeholder="子分段内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditChildChunkDialog = false">取消</el-button>
        <el-button type="primary" :loading="editChildChunkSaving" @click="saveChildChunk">保存</el-button>
      </template>
    </el-dialog>

    <!-- 文档预览 -->
    <el-dialog v-model="showPreviewDialog" title="文档预览" width="900px">
      <div class="preview-header">
        <h3>{{ previewDocument?.name }}</h3>
        <div class="preview-meta">
          <el-tag>词数: {{ previewDocument?.word_count }}</el-tag>
          <el-tag>命中: {{ previewDocument?.hit_count }} 次</el-tag>
          <el-tag :type="getStatusType(previewDocument?.display_status)">
            {{ getStatusText(previewDocument?.display_status) }}
          </el-tag>
        </div>
      </div>
      <div class="preview-content" v-loading="previewLoading">
        <div v-if="previewSegments.length" class="preview-text">
          <div v-for="(seg, index) in previewSegments" :key="index" class="preview-segment">
            <div class="segment-header">
              <span class="segment-index">第 {{ index + 1 }} 段</span>
              <span class="segment-info">{{ seg.word_count }} 词 | 命中 {{ seg.hit_count }} 次</span>
            </div>
            <div class="segment-content" v-html="renderContent(seg.content)"></div>
          </div>
        </div>
        <el-empty v-else description="暂无内容" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'
import {
  getDocumentSegments, createSegment, updateSegment, deleteSegment,
  getChildChunks, createChildChunk, updateChildChunk, deleteChildChunk,
  retrieveDataset, updateDataset
} from '@/api'

const userStore = useUserStore()
const loading = ref(false)
const uploading = ref(false)
const segmentsLoading = ref(false)
const previewLoading = ref(false)
const showUploadDialog = ref(false)
const showSegmentsDialog = ref(false)
const showPreviewDialog = ref(false)
const showEditSegmentDialog = ref(false)
const editSegmentSaving = ref(false)
const showChildChunksDialog = ref(false)
const childChunksLoading = ref(false)
const showEditChildChunkDialog = ref(false)
const editChildChunkSaving = ref(false)

// 权限判断
const canDelete = computed(() => userStore.roleId === 1 || userStore.roleId === 2)

// 编辑知识库
const showEditDsDialog = ref(false)
const editDsSaving = ref(false)
const editDsForm = reactive({ name: '', description: '' })
const editDsId = ref('')

const datasetList = ref<any[]>([])
const documentList = ref<any[]>([])
const segmentList = ref<any[]>([])
const childChunkList = ref<any[]>([])
const previewSegments = ref<any[]>([])
const previewDocument = ref<any>(null)
const currentDoc = ref<any>(null)
const currentSegmentId = ref('')
const selectedDataset = ref('')
const uploadDatasetId = ref('')
const uploadFile = ref<File | null>(null)
const searchKeyword = ref('')
const activeTab = ref('documents')

// 编辑分段相关
const editSegmentId = ref('')
const editSegmentForm = reactive({ content: '', answer: '', enabled: true })

// 编辑子分段相关
const editChildChunkId = ref('')
const editChildChunkForm = reactive({ content: '' })

const pagination = reactive({ page: 1, limit: 20, total: 0 })

// ==================== 数据检索 ====================
const retrieveQuery = ref('')
const retrieveTopK = ref(5)
const retrieveLoading = ref(false)
const retrieveResults = ref<any[]>([])
const retrieveSearched = ref(false)

const handleRetrieve = async () => {
  if (!retrieveQuery.value.trim()) return ElMessage.warning('请输入检索关键词')
  if (!selectedDataset.value) return ElMessage.warning('请先选择知识库')
  retrieveLoading.value = true
  retrieveSearched.value = true
  try {
    const res: any = await retrieveDataset(selectedDataset.value, {
      query: retrieveQuery.value,
      top_k: retrieveTopK.value
    })
    retrieveResults.value = res.data?.records || []
  } catch (e: any) {
    ElMessage.error(e.message || '检索失败')
  } finally {
    retrieveLoading.value = false
  }
}

// ==================== 通用方法 ====================

const renderContent = (content: string) => {
  if (!content) return ''
  let rendered = content.replace(/!\[([^\]]*)\]\(([^)]+)\)/g, (match, alt, src) => {
    // 将 Dify 内部文件路径代理到后端
    let imgSrc = src
    if (src.startsWith('/files/') || src.startsWith('/api/files/')) {
      imgSrc = `/api/dify/knowledge/files/preview?path=${encodeURIComponent(src)}`
    } else if (src.startsWith('http') || src.startsWith('data:')) {
      // 绝对URL或base64，直接使用
      imgSrc = src
    }
    return `<div style="margin:8px 0;text-align:center;">
      <img src="${imgSrc}" alt="${alt || '图片'}" style="max-width:100%;border-radius:6px;cursor:pointer;" onclick="window.open('${imgSrc}','_blank')" onerror="this.style.display='none';this.parentElement.innerHTML='<div style=\\'background:#f0f0f0;padding:12px;border-radius:6px;color:#999;font-size:12px;\\'>[图片加载失败]</div>'" />
    </div>`
  })
  rendered = rendered.replace(/\n/g, '<br>')
  return rendered
}

const filteredDocuments = computed(() => {
  if (!searchKeyword.value) return documentList.value
  return documentList.value.filter(d =>
    d.name.toLowerCase().includes(searchKeyword.value.toLowerCase())
  )
})

const formatWordCount = (c: number) => c >= 10000 ? (c / 10000).toFixed(1) + '万' : c?.toString() || '0'
const getStatusType = (s: string) => ({ available: 'success', indexing: 'warning' }[s] || 'info')
const getStatusText = (s: string) => ({ available: '可用', indexing: '索引中' }[s] || s)

// ==================== 知识库 ====================

const fetchDatasets = async () => {
  try {
    // 知识库管理页面仅展示指定知识库（后端按 scope 白名单过滤）
    const res: any = await request.get('/dify/knowledge/datasets', {
      params: { scope: 'knowledge-management' }
    })
    datasetList.value = res.data || []
    if (datasetList.value.length) {
      selectDataset(datasetList.value[0])
    }
  } catch (e) { console.error(e) }
}

const selectDataset = (d: any) => {
  selectedDataset.value = d.id
  pagination.page = 1
  fetchDocuments()
  // 切换知识库时清空检索结果
  retrieveResults.value = []
  retrieveSearched.value = false
}

// 编辑知识库
const showEditDatasetDialog = (dataset: any) => {
  editDsId.value = dataset.id
  editDsForm.name = dataset.name || ''
  editDsForm.description = dataset.description || ''
  showEditDsDialog.value = true
}

const saveDataset = async () => {
  if (!editDsForm.name.trim()) return ElMessage.warning('请输入知识库名称')
  editDsSaving.value = true
  try {
    await updateDataset(editDsId.value, {
      name: editDsForm.name,
      description: editDsForm.description
    })
    ElMessage.success('知识库更新成功')
    showEditDsDialog.value = false
    // 刷新知识库列表
    await fetchDatasets()
  } catch (e: any) {
    ElMessage.error(e.message || '更新失败')
  } finally {
    editDsSaving.value = false
  }
}

// ==================== 文档管理 ====================

const fetchDocuments = async () => {
  if (!selectedDataset.value) return
  loading.value = true
  try {
    const res: any = await request.get(`/dify/knowledge/datasets/${selectedDataset.value}/documents`, {
      params: { page: pagination.page, limit: pagination.limit }
    })
    documentList.value = res.data?.data || []
    pagination.total = res.data?.total || 0
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

const handleFileChange = (f: any) => { uploadFile.value = f.raw }

const handleUpload = async () => {
  if (!uploadDatasetId.value) return ElMessage.warning('请选择知识库')
  if (!uploadFile.value) return ElMessage.warning('请选择文件')
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', uploadFile.value)
    await request.post(`/dify/knowledge/datasets/${uploadDatasetId.value}/documents/upload`, fd, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    ElMessage.success('上传成功')
    showUploadDialog.value = false
    fetchDocuments()
    fetchDatasets()
  } catch (e) { console.error(e) }
  finally { uploading.value = false }
}

const handleDelete = async (doc: any) => {
  await ElMessageBox.confirm(`删除文档 "${doc.name}"？`, '确认', { type: 'warning' })
  await request.delete(`/dify/knowledge/datasets/${selectedDataset.value}/documents/${doc.id}`)
  ElMessage.success('已删除')
  fetchDocuments()
}

// ==================== 分段管理 ====================

const handleViewSegments = async (doc: any) => {
  currentDoc.value = doc
  showSegmentsDialog.value = true
  await fetchSegments()
}

const fetchSegments = async () => {
  if (!currentDoc.value) return
  segmentsLoading.value = true
  try {
    const res: any = await getDocumentSegments(selectedDataset.value, currentDoc.value.id, { page: 1, limit: 50 })
    segmentList.value = res.data?.data || []
  } catch (e) { console.error(e) }
  finally { segmentsLoading.value = false }
}

const handleAddSegment = () => {
  editSegmentId.value = ''
  editSegmentForm.content = ''
  editSegmentForm.answer = ''
  editSegmentForm.enabled = true
  showEditSegmentDialog.value = true
}

const handleEditSegment = (seg: any) => {
  editSegmentId.value = seg.id
  editSegmentForm.content = seg.content || ''
  editSegmentForm.answer = seg.answer || ''
  editSegmentForm.enabled = seg.enabled === 'true' || seg.enabled === true
  showEditSegmentDialog.value = true
}

const saveSegment = async () => {
  if (!editSegmentForm.content.trim()) return ElMessage.warning('请输入分段内容')
  editSegmentSaving.value = true
  try {
    if (editSegmentId.value) {
      await updateSegment(selectedDataset.value, currentDoc.value.id, editSegmentId.value, {
        content: editSegmentForm.content,
        answer: editSegmentForm.answer || undefined,
        enabled: editSegmentForm.enabled
      })
      ElMessage.success('分段已更新')
    } else {
      await createSegment(selectedDataset.value, currentDoc.value.id, {
        content: editSegmentForm.content,
        answer: editSegmentForm.answer || undefined
      })
      ElMessage.success('分段已创建')
    }
    showEditSegmentDialog.value = false
    await fetchSegments()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    editSegmentSaving.value = false
  }
}

const handleDeleteSegment = async (seg: any) => {
  await ElMessageBox.confirm('确认删除该分段？删除后不可恢复。', '删除分段', { type: 'warning' })
  try {
    await deleteSegment(selectedDataset.value, currentDoc.value.id, seg.id)
    ElMessage.success('分段已删除')
    await fetchSegments()
  } catch (e: any) {
    ElMessage.error(e.message || '删除失败')
  }
}

// ==================== 子分段管理 ====================

const handleViewChildChunks = async (seg: any) => {
  currentSegmentId.value = seg.id
  showChildChunksDialog.value = true
  await fetchChildChunks()
}

const fetchChildChunks = async () => {
  if (!currentSegmentId.value) return
  childChunksLoading.value = true
  try {
    const res: any = await getChildChunks(selectedDataset.value, currentDoc.value.id, currentSegmentId.value, { page: 1, limit: 50 })
    childChunkList.value = res.data?.data || []
  } catch (e) { console.error(e) }
  finally { childChunksLoading.value = false }
}

const handleAddChildChunk = () => {
  editChildChunkId.value = ''
  editChildChunkForm.content = ''
  showEditChildChunkDialog.value = true
}

const handleEditChildChunk = (chunk: any) => {
  editChildChunkId.value = chunk.id
  editChildChunkForm.content = chunk.content || ''
  showEditChildChunkDialog.value = true
}

const saveChildChunk = async () => {
  if (!editChildChunkForm.content.trim()) return ElMessage.warning('请输入子分段内容')
  editChildChunkSaving.value = true
  try {
    if (editChildChunkId.value) {
      await updateChildChunk(selectedDataset.value, currentDoc.value.id, currentSegmentId.value, editChildChunkId.value, {
        content: editChildChunkForm.content
      })
      ElMessage.success('子分段已更新')
    } else {
      await createChildChunk(selectedDataset.value, currentDoc.value.id, currentSegmentId.value, {
        content: editChildChunkForm.content
      })
      ElMessage.success('子分段已创建')
    }
    showEditChildChunkDialog.value = false
    await fetchChildChunks()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    editChildChunkSaving.value = false
  }
}

const handleDeleteChildChunk = async (chunk: any) => {
  await ElMessageBox.confirm('确认删除该子分段？删除后不可恢复。', '删除子分段', { type: 'warning' })
  try {
    await deleteChildChunk(selectedDataset.value, currentDoc.value.id, currentSegmentId.value, chunk.id)
    ElMessage.success('子分段已删除')
    await fetchChildChunks()
  } catch (e: any) {
    ElMessage.error(e.message || '删除失败')
  }
}

// ==================== 预览和下载 ====================

const handlePreview = async (doc: any) => {
  previewDocument.value = doc
  showPreviewDialog.value = true
  previewLoading.value = true
  previewSegments.value = []
  try {
    const res: any = await getDocumentSegments(selectedDataset.value, doc.id, { page: 1, limit: 100 })
    previewSegments.value = res.data?.data || []
  } catch (e) {
    console.error(e)
    ElMessage.error('获取文档内容失败')
  } finally {
    previewLoading.value = false
  }
}

const handleDownload = async (doc: any) => {
  try {
    const res: any = await request.get(
      `/dify/knowledge/datasets/${selectedDataset.value}/documents/${doc.id}/download`
    )
    if (res.data) {
      window.open(res.data, '_blank')
      ElMessage.success('开始下载')
    } else {
      ElMessage.error('获取下载链接失败')
    }
  } catch (e: any) {
    console.error(e)
    ElMessage.error(e.message || '下载失败')
  }
}

onMounted(fetchDatasets)
</script>

<style scoped>
.knowledge-page { padding: 0; }

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 28px;
  background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
  border-radius: 12px;
  color: white;
  margin-bottom: 20px;
}
.page-header h1 { font-size: 22px; margin: 0; font-weight: 600; }
.page-header p { font-size: 13px; opacity: 0.7; margin: 4px 0 0; }
.header-btn { background: rgba(255,255,255,0.15) !important; border-color: rgba(255,255,255,0.3) !important; }
.header-btn:hover { background: rgba(255,255,255,0.25) !important; }

/* 知识库选择 - 横向滚动 */
.dataset-section { margin-bottom: 20px; }
.dataset-scroll {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  padding: 4px 0;
}
.dataset-scroll::-webkit-scrollbar { height: 4px; }
.dataset-scroll::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 2px; }

.dataset-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  min-width: 240px;
  background: white;
  border: 2px solid #e5e7eb;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}
.dataset-card:hover { border-color: #93c5fd; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
.dataset-card.active { border-color: #3b82f6; background: #eff6ff; }
.dataset-icon {
  width: 40px; height: 40px;
  background: #eff6ff;
  border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  color: #3b82f6;
}
.dataset-card.active .dataset-icon { background: #3b82f6; color: white; }
.dataset-info { flex: 1; min-width: 0; }
.dataset-info h3 { font-size: 13px; margin: 0 0 4px; font-weight: 600; }
.dataset-meta { display: flex; gap: 10px; font-size: 11px; color: #94a3b8; }
.dataset-meta .el-icon { margin-right: 2px; }
.dataset-check { color: #3b82f6; font-size: 18px; }

.dataset-edit-btn {
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.2s;
  margin-left: auto;
}
.dataset-card:hover .dataset-edit-btn {
  opacity: 1;
}

/* 主内容区 */
.main-content {
  background: white;
  border-radius: 12px;
  padding: 4px 20px 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

/* Tab 样式 */
.knowledge-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}
.knowledge-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
}
.knowledge-tabs :deep(.el-tabs__item) {
  font-size: 14px;
  height: 48px;
  line-height: 48px;
}
.tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
}
.tab-content {
  padding-top: 20px;
}

/* 文档工具栏 */
.doc-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

/* 文档列表 */
.document-grid { display: flex; flex-direction: column; gap: 8px; }
.document-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  transition: all 0.2s;
}
.document-card:hover { background: #f8fafc; border-color: #cbd5e1; }
.doc-icon {
  width: 36px; height: 36px;
  background: #eff6ff; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  color: #3b82f6;
}
.doc-info { flex: 1; min-width: 0; }
.doc-info h4 { font-size: 13px; margin: 0 0 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.doc-meta { display: flex; gap: 12px; font-size: 11px; color: #94a3b8; }
.doc-actions { display: flex; gap: 4px; }

.empty-state { text-align: center; padding: 40px; color: #94a3b8; }
.select-prompt {
  text-align: center; padding: 80px; background: white; border-radius: 12px; color: #94a3b8;
}
.select-prompt h3 { margin: 16px 0 4px; }
.select-prompt p { font-size: 13px; margin: 0; }

.pagination-wrapper {
  display: flex; justify-content: flex-end;
  margin-top: 16px; padding-top: 16px;
  border-top: 1px solid #f1f5f9;
}

/* ==================== 数据检索 ==================== */
.retrieve-section {
  max-width: 900px;
  margin: 0 auto;
}

.retrieve-input-area {
  margin-bottom: 28px;
}

/* 检索输入栏 */
.search-bar {
  display: flex;
  gap: 12px;
  align-items: stretch;
}
.search-input-wrapper {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 10px;
  padding: 0 14px;
  transition: all 0.25s;
}
.search-input-wrapper:focus-within {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59,130,246,0.1);
}
.search-input-icon {
  color: #94a3b8;
  font-size: 18px;
  flex-shrink: 0;
  margin-right: 10px;
  transition: color 0.25s;
}
.search-input-wrapper:focus-within .search-input-icon {
  color: #3b82f6;
}
.search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 14px;
  line-height: 44px;
  color: #1e293b;
  background: transparent;
}
.search-input::placeholder {
  color: #94a3b8;
}
.search-clear-icon {
  color: #cbd5e1;
  font-size: 16px;
  cursor: pointer;
  flex-shrink: 0;
  margin-left: 8px;
  transition: color 0.2s;
}
.search-clear-icon:hover {
  color: #64748b;
}
.search-btn {
  flex-shrink: 0;
  min-width: 120px;
  height: 48px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 0.5px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.retrieve-options {
  margin-top: 16px;
}
.option-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: #f8fafc;
  border: 1px solid #e8ecf1;
  border-radius: 8px;
}
.option-icon {
  color: #64748b;
  font-size: 16px;
}
.retrieve-label {
  font-size: 13px;
  color: #475569;
  white-space: nowrap;
  font-weight: 500;
}
.option-slider {
  flex: 1;
  max-width: 240px;
}
.option-value {
  font-size: 13px;
  font-weight: 600;
  color: #3b82f6;
  min-width: 36px;
  text-align: right;
}

.retrieve-results {
  min-height: 200px;
}

.results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
}
.results-count {
  font-size: 14px;
  font-weight: 500;
  color: #334155;
}

.result-card {
  margin-bottom: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
  transition: all 0.2s;
}
.result-card:hover {
  border-color: #93c5fd;
  box-shadow: 0 2px 12px rgba(59,130,246,0.1);
}

.result-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #f8fafc;
  border-bottom: 1px solid #e5e7eb;
}

.result-rank {
  width: 28px; height: 28px;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  border-radius: 6px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.rank-num {
  color: white;
  font-size: 13px;
  font-weight: 700;
}

.result-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}
.result-doc-name {
  font-size: 12px;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 4px;
}

.result-content {
  padding: 14px 16px;
  font-size: 13px;
  line-height: 1.8;
  color: #334155;
  white-space: pre-wrap;
}

.result-keywords {
  padding: 8px 16px 12px;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.kw-tag { font-size: 11px; }

.retrieve-placeholder {
  text-align: center;
  padding: 60px 20px;
  color: #94a3b8;
}
.retrieve-placeholder h3 {
  font-size: 18px;
  color: #64748b;
  margin: 20px 0 8px;
}
.retrieve-placeholder p {
  font-size: 13px;
  margin: 0 0 24px;
}
.retrieve-tips {
  display: flex;
  justify-content: center;
  gap: 24px;
}
.tip-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #94a3b8;
}
.empty-hint { font-size: 12px; color: #cbd5e1; margin-top: 8px; }

/* ==================== 分段管理 ==================== */
.segments-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
}
.segments-count { font-size: 13px; color: #94a3b8; }

.segments-list {
  max-height: 500px;
  overflow-y: auto;
}

.segment-item {
  margin-bottom: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.2s;
}
.segment-item:hover { border-color: #93c5fd; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }

.segment-item-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: #f8fafc;
  border-bottom: 1px solid #e5e7eb;
}

.seg-index {
  font-weight: 600;
  color: #3b82f6;
  font-size: 13px;
  min-width: 60px;
}

.seg-meta {
  display: flex;
  gap: 6px;
  flex: 1;
}

.seg-actions {
  display: flex;
  gap: 4px;
}

.segment-item-content {
  padding: 12px 16px;
  font-size: 13px;
  line-height: 1.7;
  color: #334155;
  white-space: pre-wrap;
  max-height: 120px;
  overflow-y: auto;
}

/* ==================== 预览样式 ==================== */
.preview-header {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e5e7eb;
}
.preview-header h3 { margin: 0 0 12px; font-size: 18px; }
.preview-meta { display: flex; gap: 8px; }

.preview-content {
  max-height: 600px;
  overflow-y: auto;
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
}

.preview-segment {
  margin-bottom: 20px;
  padding: 16px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.segment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f1f5f9;
}

.segment-index {
  font-weight: 600;
  color: #3b82f6;
  font-size: 13px;
}

.segment-info {
  font-size: 12px;
  color: #94a3b8;
}

.segment-content {
  font-size: 14px;
  line-height: 1.8;
  color: #334155;
  white-space: pre-wrap;
}

.segment-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 4px;
  margin: 8px 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
</style>
