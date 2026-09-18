<template>
  <div class="page-container">
    <el-row :gutter="20">
      <el-col :span="16">
        <el-card class="search-card">
          <template #header>
            <div class="card-title">
              <el-icon><Search /></el-icon>
              <span>多模态故障检索</span>
            </div>
          </template>
          <el-form :model="searchForm" label-width="100px">
            <el-form-item label="故障描述" required>
              <el-input
                v-model="searchForm.query"
                type="textarea"
                :rows="4"
                placeholder="请输入故障现象描述，如：发动机启动后有异响，怠速时明显..."
              />
              <div class="voice-input-actions">
                <el-button type="primary" link @click="triggerAudioFileInput" :disabled="transcribing">
                  上传音频
                </el-button>
                <input
                  ref="audioFileInput"
                  type="file"
                  accept="audio/*"
                  style="display: none"
                  @change="handleAudioFileSelected"
                />
              </div>
            </el-form-item>
            <el-form-item label="设备型号">
              <el-input v-model="searchForm.equipModel" placeholder="请输入设备型号" clearable />
            </el-form-item>
            <el-form-item label="故障文件">
              <el-upload
                action="#"
                :http-request="handleUpload"
                :limit="9"
                accept="image/*,video/*"
                v-model:file-list="fileList"
                list-type="picture-card"
                :on-remove="handleRemove"
                :on-exceed="() => ElMessage.warning('最多上传9个文件')"
                :before-upload="beforeUpload"
              >
                <el-icon><Plus /></el-icon>
                <template #file="{ file }">
                  <div class="upload-file-card">
                    <img v-if="getUploadedFileType(file) === 'image'" :src="file.url" class="upload-thumbnail" />
                    <div v-else class="upload-video-thumb">
                      <el-icon :size="32" style="color: #fff"><VideoPlay /></el-icon>
                      <span class="video-label">视频</span>
                    </div>
                    <span class="upload-file-actions">
                      <el-icon @click.stop="previewUploadedFile(file)"><ZoomIn /></el-icon>
                      <el-icon @click.stop="handleRemove(file)"><Delete /></el-icon>
                    </span>
                  </div>
                </template>
              </el-upload>
              <div class="upload-tip">支持上传图片、视频和音频文件，最多9个</div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" @click="handleSearch">
                <el-icon><Search /></el-icon>开始检索
              </el-button>
              <el-button @click="handleStop" :disabled="!loading">
                <el-icon><VideoPause /></el-icon>停止生成
              </el-button>
              <el-button @click="handleReset">
                <el-icon><RefreshRight /></el-icon>重置
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card v-if="searchResult || streamingAnswer" class="result-card">
          <template #header>
            <div class="card-title">
              <el-icon><Document /></el-icon>
              <span>检索结果</span>
            </div>
          </template>

          <!-- 基本信息 -->
          <el-descriptions :column="2" border class="result-desc">
            <el-descriptions-item label="故障描述" :span="2">
              {{ searchForm.query || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="设备型号">
              {{ searchForm.equipModel || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="检索状态">
              <el-tag type="success" effect="plain">成功</el-tag>
            </el-descriptions-item>
          </el-descriptions>

          <!-- 检索结果 -->
          <div class="result-section">
            <h4>检索结果</h4>
            <div class="markdown-body result-content" v-html="displayAnswer"></div>
            <div v-if="loading" class="typing-indicator">
              <span></span><span></span><span></span>
            </div>
          </div>

          <!-- 参考来源 -->
          <div v-if="searchResult?.resources?.length" class="result-section">
            <div class="section-header">
              <h4>参考来源</h4>
              <div class="sort-controls">
                <span class="sort-label">排序：</span>
                <el-radio-group v-model="resourceSort" size="small">
                  <el-radio-button value="score">匹配度</el-radio-button>
                  <el-radio-button value="name">文档名</el-radio-button>
                  <el-radio-button value="dataset">知识库</el-radio-button>
                </el-radio-group>
                <el-button
                  :icon="sortOrder === 'asc' ? 'ArrowUp' : 'ArrowDown'"
                  size="small"
                  circle
                  @click="sortOrder = sortOrder === 'asc' ? 'desc' : 'asc'"
                  :title="sortOrder === 'asc' ? '升序' : '降序'"
                />
              </div>
            </div>
            <el-table :data="sortedResources" border size="small">
              <el-table-column prop="documentName" label="文档名称" min-width="150" />
              <el-table-column prop="datasetName" label="知识库" width="120" />
              <el-table-column prop="score" label="匹配度" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="getScoreTagType(row.score)" effect="plain">{{ (row.score * 100).toFixed(1) }}%</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="content" label="内容摘要" show-overflow-tooltip />
            </el-table>
          </div>

          <!-- AI反馈 -->
          <div v-if="currentHistoryId && !loading" class="result-section">
            <AiFeedback
              :search-history-id="currentHistoryId"
              :original-answer="searchResult?.answer || streamingAnswer"
            />
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="history-card">
          <template #header>
            <div class="card-header">
              <div class="card-title">
                <el-icon><Clock /></el-icon>
                <span>检索历史</span>
              </div>
              <el-button size="small" @click="fetchHistory">
                <el-icon><Refresh /></el-icon>
              </el-button>
            </div>
          </template>
          <div v-if="historyList.length" class="history-list">
            <div
              v-for="item in historyList"
              :key="item.id"
              class="history-item"
            >
              <div class="history-query" @click="handleReuse(item)">{{ item.queryText || '文件检索' }}</div>
              <div class="history-meta">
                <div class="meta-left">
                  <el-tag size="small" type="info">{{ item.equipModel || '无型号' }}</el-tag>
                  <el-tag :type="item.searchSuccess === false ? 'danger' : 'success'" size="small" effect="plain">
                    {{ item.searchSuccess === false ? '失败' : '成功' }}
                  </el-tag>
                </div>
                <span>{{ formatTime(item.createTime) }}</span>
              </div>
              <div class="history-actions">
                <el-button type="primary" link size="small" @click="handleDetail(item)">
                  <el-icon><View /></el-icon>详情
                </el-button>
                <el-button type="danger" link size="small" @click="handleDelete(item)">
                  <el-icon><Delete /></el-icon>删除
                </el-button>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无检索历史" />
          <div class="history-footer" v-if="historyList.length">
            <router-link to="/search-history" class="view-all-link">
              查看全部历史 <el-icon><ArrowRight /></el-icon>
            </router-link>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="检索详情" width="700px" destroy-on-close>
      <template v-if="detailData">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="故障描述" :span="2">
            {{ detailData.queryText || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="设备型号">
            {{ detailData.equipModel || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="检索状态">
            <el-tag :type="detailData.searchSuccess ? 'success' : 'danger'" effect="plain">
              {{ detailData.searchSuccess ? '成功' : '失败' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="检索时间" :span="2">
            {{ formatTime(detailData.createTime) }}
          </el-descriptions-item>
        </el-descriptions>
        <div v-if="detailAllFiles.length" class="detail-section">
          <h4>故障文件</h4>
          <div class="file-list">
            <template v-for="(file, index) in detailAllFiles" :key="index">
              <div class="file-item">
                <el-image
                  v-if="file.fileType === 'image'"
                  :src="file.url"
                  :preview-src-list="detailImageUrls"
                  :initial-index="detailImageUrls.indexOf(file.url)"
                  fit="cover"
                  referrerpolicy="no-referrer"
                  style="width: 120px; height: 120px; border-radius: 6px;"
                >
                  <template #error>
                    <div class="image-error">
                      <el-icon><Picture /></el-icon>
                      <span>加载失败</span>
                    </div>
                  </template>
                </el-image>
                <div v-else class="video-preview" @click="playVideo(file.url)">
                  <el-icon :size="32" style="color: #fff"><VideoPlay /></el-icon>
                  <span class="video-label">视频</span>
                </div>
                <div class="file-name" :title="file.name">{{ file.name }}</div>
              </div>
            </template>
          </div>
        </div>
        <div v-else-if="detailData.imageUrl" class="detail-section">
          <h4>故障图片</h4>
          <el-image
            :src="detailData.imageUrl"
            :preview-src-list="[detailData.imageUrl]"
            fit="cover"
            referrerpolicy="no-referrer"
            style="width: 120px; height: 120px; border-radius: 6px"
          >
            <template #error>
              <div class="image-error">
                <el-icon><Picture /></el-icon>
                <span>加载失败</span>
              </div>
            </template>
          </el-image>
        </div>
        <div v-if="detailData.resultAnswer" class="detail-section">
          <h4>检索结果</h4>
          <div class="markdown-body result-content" v-html="formatAnswer(detailData.resultAnswer)"></div>
        </div>
        <div v-if="!detailData.searchSuccess && detailData.errorMessage" class="detail-section">
          <h4>错误信息</h4>
          <el-alert :title="detailData.errorMessage" type="error" :closable="false" show-icon />
        </div>
      </template>
    </el-dialog>

    <!-- 图片预览弹窗 -->
    <el-dialog v-model="imagePreviewVisible" title="图片预览" width="800px" destroy-on-close>
      <img v-if="imagePreviewVisible" :src="imagePreviewUrl" style="width: 100%" />
    </el-dialog>

    <!-- 视频播放弹窗 -->
    <el-dialog v-model="videoDialogVisible" title="视频播放" width="700px" destroy-on-close>
      <video v-if="videoDialogVisible" :src="currentVideoUrl" controls style="width: 100%" />
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { marked } from 'marked'
import { getSearchHistory, getSearchHistoryDetail, deleteSearchHistory, uploadFile, audioToText } from '@/api'
import AiFeedback from '@/components/AiFeedback.vue'
import { useUserStore } from '@/store/user'
import { sanitizeHtml } from '@/utils/sanitize'


interface UploadedFile {
  url: string
  fileType: string // 'image' 或 'video'
  name: string // 原始文件名
}

const loading = ref(false)
const searchResult = ref<any>(null)
const streamingAnswer = ref('')
const historyList = ref<any[]>([])
const currentHistoryId = ref<number | null>(null)
const fileList = ref<any[]>([])
const uploadedFiles = ref<UploadedFile[]>([])
const detailVisible = ref(false)
const detailData = ref<any>(null)
const videoDialogVisible = ref(false)
const currentVideoUrl = ref('')
const imagePreviewVisible = ref(false)
const imagePreviewUrl = ref('')
let abortController: AbortController | null = null

// ============ 语音转文字 ============
const audioFileInput = ref<HTMLInputElement | null>(null)
const transcribing = ref(false)

const triggerAudioFileInput = () => { audioFileInput.value?.click() }

const handleAudioFileSelected = (e: Event) => {
  const input = e.target as HTMLInputElement
  if (input.files?.[0]) {
    transcribeAudio(input.files[0])
    input.value = ''
  }
}

const transcribeAudio = async (file: File) => {
  transcribing.value = true
  let loadingMsg: ReturnType<typeof ElMessage> | null = null
  try {
    loadingMsg = ElMessage({ message: '正在识别语音内容，请稍候...', type: 'info', duration: 0, showClose: false })
    const res: any = await audioToText(file)
    const text = res.data?.text || ''
    if (!text) {
      ElMessage.warning('未识别到语音内容')
      return
    }
    searchForm.query = (searchForm.query ? searchForm.query + '\n' : '') + text
    ElMessage.success('语音识别完成，已填入故障描述')
  } catch (error: any) {
    console.error('语音转文字失败', error)
    ElMessage.error('语音转文字失败：' + (error?.message || '请重试'))
  } finally {
    loadingMsg?.close()
    transcribing.value = false
  }
}

// ============ 检索结果排序 ============
const resourceSort = ref<'score' | 'name' | 'dataset'>('score')
const sortOrder = ref<'asc' | 'desc'>('desc')

const sortedResources = computed(() => {
  const resources = searchResult.value?.resources
  if (!resources?.length) return []
  const sorted = [...resources]
  const direction = sortOrder.value === 'asc' ? 1 : -1
  sorted.sort((a, b) => {
    switch (resourceSort.value) {
      case 'score':
        return direction * ((a.score || 0) - (b.score || 0))
      case 'name':
        return direction * (a.documentName || '').localeCompare(b.documentName || '')
      case 'dataset':
        return direction * (a.datasetName || '').localeCompare(b.datasetName || '')
      default:
        return 0
    }
  })
  return sorted
})

const getScoreTagType = (score: number) => {
  if (score >= 0.8) return 'success'
  if (score >= 0.5) return 'warning'
  return 'danger'
}

const searchForm = reactive({
  query: '',
  equipModel: '',
  imageUrl: ''
})

const displayAnswer = computed(() => {
  let text = streamingAnswer.value || searchResult.value?.answer || ''
  if (!text) return ''
  try {
    // 去除 Dify 返回的引用标签
    text = text.replace(/\[ref:[^\]]*\]/g, '').replace(/\[ref:[^\]]*$/g, '')
    const html = marked.parse(text, { breaks: true, gfm: true }) as string
    return sanitizeHtml(html)
  } catch {
    return sanitizeHtml(text)
  }
})

// 详情弹窗中的文件列表
const detailAllFiles = computed(() => {
  if (!detailData.value) return []
  const files: UploadedFile[] = []
  const urls: string[] = []
  const types: string[] = []
  const names: string[] = []

  // 优先使用 imageUrls + fileTypes
  if (detailData.value.imageUrls) {
    try {
      const parsed = JSON.parse(detailData.value.imageUrls)
      if (Array.isArray(parsed)) urls.push(...parsed)
    } catch { /* ignore */ }
  }
  if (detailData.value.fileTypes) {
    try {
      const parsed = JSON.parse(detailData.value.fileTypes)
      if (Array.isArray(parsed)) types.push(...parsed)
    } catch { /* ignore */ }
  }
  if (detailData.value.fileNames) {
    try {
      const parsed = JSON.parse(detailData.value.fileNames)
      if (Array.isArray(parsed)) names.push(...parsed)
    } catch { /* ignore */ }
  }

  if (urls.length > 0) {
    for (let i = 0; i < urls.length; i++) {
      files.push({
        url: urls[i],
        fileType: types[i] || 'image',
        name: names[i] || urls[i].split('/').pop() || ''
      })
    }
  } else if (detailData.value.imageUrl) {
    files.push({ url: detailData.value.imageUrl, fileType: 'image', name: '' })
  }
  return files
})

const detailImageUrls = computed(() => detailAllFiles.value.filter(f => f.fileType === 'image').map(f => f.url))

const beforeUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  const isVideo = file.type.startsWith('video/')
  if (!isImage && !isVideo) {
    ElMessage.error('只能上传图片或视频文件')
    return false
  }
  return true
}

const handleUpload = async (options: any) => {
  try {
    const res: any = await uploadFile(options.file)
    // 视频截帧返回 urls[] 数组，图片返回 url 单字符串
    const data = res?.data || {}
    if (data.urls && Array.isArray(data.urls)) {
      // 视频关键帧提取结果：多个图片URL
      const uploaded: UploadedFile[] = data.urls.map((url: string, index: number) => ({
        url,
        fileType: 'image',
        name: data.names?.[index] || `frame_${index + 1}.jpg`
      }))
      uploadedFiles.value.push(...uploaded)
      // 第一个帧作为 el-upload 的主文件
      options.onSuccess({
        data: { url: uploaded[0].url, fileType: 'image', name: uploaded[0].name }
      })
      // 后续帧手动添加到 fileList
      for (let i = 1; i < uploaded.length; i++) {
        fileList.value.push({
          name: uploaded[i].name,
          url: uploaded[i].url,
          status: 'success',
          uid: Date.now() + i,
          response: { data: { url: uploaded[i].url, fileType: 'image', name: uploaded[i].name } }
        })
      }
    } else {
      // 普通图片
      const url = data.url || ''
      const fileType = data.fileType || 'image'
      const name = data.name || options.file.name || ''
      uploadedFiles.value.push({ url, fileType, name })
      options.onSuccess(res)
    }
  } catch (error) {
    options.onError(error)
    ElMessage.error('文件上传失败')
  }
}

const handleRemove = (file: any) => {
  // 通过 el-upload file 的 response 找到对应的 url，从 uploadedFiles 中移除
  const url = file.response?.data?.url
  if (url) {
    const index = uploadedFiles.value.findIndex(f => f.url === url)
    if (index !== -1) {
      uploadedFiles.value.splice(index, 1)
    }
  } else {
    // 如果无法获取url，则按索引移除（兼容处理）
    const uid = file.uid
    const idx = fileList.value.findIndex(f => f.uid === uid)
    if (idx !== -1 && uploadedFiles.value.length > idx) {
      uploadedFiles.value.splice(idx, 1)
    }
  }
}

const playVideo = (url: string) => {
  currentVideoUrl.value = url
  videoDialogVisible.value = true
}

// 获取上传文件的类型（image/video）
const getUploadedFileType = (file: any): string => {
  // 优先从 uploadedFiles 中查找
  const url = file.response?.data?.url || file.url
  const found = uploadedFiles.value.find(f => f.url === url)
  if (found) return found.fileType
  // 回退：通过文件 MIME 类型判断
  if (file.raw?.type?.startsWith('video/')) return 'video'
  return 'image'
}

// 预览上传的文件（放大镜功能）
const previewUploadedFile = (file: any) => {
  const url = file.response?.data?.url || file.url
  const fileType = getUploadedFileType(file)
  if (fileType === 'video') {
    playVideo(url)
  } else {
    imagePreviewUrl.value = url
    imagePreviewVisible.value = true
  }
}

const handleSearch = async () => {
  if (!searchForm.query) {
    ElMessage.warning('请输入故障描述')
    return
  }

  loading.value = true
  searchResult.value = null
  streamingAnswer.value = ''
  currentHistoryId.value = null

  // 使用SSE流式输出（POST + JSON Body，避免URL编码破坏文件路径）
  const token = localStorage.getItem('token')

  const body: any = {
    query: searchForm.query || '',
    equipModel: searchForm.equipModel || '',
    agentType: 'zdjc'
  }

  // 添加文件URL和类型参数（以数组形式发送，避免逗号分隔导致的解析问题）
  if (uploadedFiles.value.length > 0) {
    body.fileUrls = uploadedFiles.value.map(f => f.url)
    body.fileTypes = uploadedFiles.value.map(f => f.fileType)
    body.fileNames = uploadedFiles.value.map(f => f.name)
  }

  abortController = new AbortController()

  try {
    const response = await fetch('/api/search/stream', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(body),
      signal: abortController.signal
    })

    if (!response.ok) {
      throw new Error('请求失败')
    }

    const reader = response.body?.getReader()
    if (!reader) return

    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      // 将二进制 chunk 解码并追加到缓冲区
      buffer += decoder.decode(value, { stream: true })

      // SSE 协议以两个换行符分隔事件，按 \n\n 拆分
      const eventBlocks = buffer.split('\n\n')
      // 最后一段可能不完整，保留到下次
      buffer = eventBlocks.pop() || ''

      for (const block of eventBlocks) {
        if (!block.trim()) continue

        // 解析单个事件块中的各行字段
        const fields = block.split('\n')
        let eventType = ''
        let eventData = ''

        for (const field of fields) {
          if (field.startsWith('event:')) {
            eventType = field.substring(6).trim()
          } else if (field.startsWith('data:')) {
            // data 可能有多行，用换行拼接
            if (eventData) eventData += '\n'
            eventData += field.substring(5)
          }
          // 忽略 id: / retry: 等其他 SSE 字段
        }

        // 根据事件类型处理
        if (eventType === 'message') {
          // 流式内容片段，拼接 markdown 文本
          streamingAnswer.value += eventData
        } else if (eventType === 'done') {
          // 流式完成，解析答案和historyId
          try {
            const doneData = JSON.parse(eventData)
            streamingAnswer.value = doneData.answer || eventData
            if (doneData.historyId) {
              currentHistoryId.value = doneData.historyId
            }
          } catch {
            streamingAnswer.value = eventData
          }
        } else if (eventType === 'error') {
          ElMessage.error(eventData || '检索失败')
        } else if (eventData) {
          // 兼容未声明 event 类型的 data 行
          streamingAnswer.value += eventData
        }
      }
    }

    // 流式完成，构建最终结果
    searchResult.value = {
      answer: streamingAnswer.value,
      resources: []
    }
    fetchHistory()

  } catch (error: any) {
    if (error.message !== 'The user aborted a request.') {
      console.error('检索失败', error)
      ElMessage.error('检索失败，请稍后重试')
    }
  } finally {
    loading.value = false
  }
}

const handleStop = () => {
  if (abortController) {
    abortController.abort()
    abortController = null
  }
  loading.value = false
  if (streamingAnswer.value) {
    searchResult.value = {
      answer: streamingAnswer.value,
      resources: []
    }
  }
}

const handleReset = () => {
  handleStop()
  searchForm.query = ''
  searchForm.equipModel = ''
  searchForm.imageUrl = ''
  searchResult.value = null
  streamingAnswer.value = ''
  fileList.value = []
  uploadedFiles.value = []
  currentHistoryId.value = null
}

const fetchHistory = async () => {
  try {
    const res: any = await getSearchHistory({ pageNum: 1, pageSize: 20 })
    historyList.value = res.data?.records || []
  } catch (error) {
    console.error('获取历史失败', error)
  }
}

const handleReuse = (item: any) => {
  searchForm.query = item.queryText || ''
  searchForm.equipModel = item.equipModel || ''
  currentHistoryId.value = item.id
}

const formatTime = (time: string) => {
  if (!time) return ''
  // 处理 ISO 格式 (2024-01-01T12:00:00) 或标准格式 (2024-01-01 12:00:00)
  return time.substring(0, 19).replace('T', ' ')
}

const handleDetail = async (item: any) => {
  try {
    const res: any = await getSearchHistoryDetail(item.id)
    detailData.value = res.data
    detailVisible.value = true
  } catch (error) {
    console.error('获取详情失败', error)
    ElMessage.error('获取详情失败')
  }
}

const handleDelete = async (item: any) => {
  try {
    await ElMessageBox.confirm('确定删除该检索记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteSearchHistory(item.id)
    ElMessage.success('删除成功')
    fetchHistory()
  } catch {
    // 取消删除
  }
}

const formatAnswer = (answer: string) => {
  if (!answer) return ''
  try {
    // 去除 Dify 返回的引用标签
    const text = answer.replace(/\[ref:[^\]]*\]/g, '').replace(/\[ref:[^\]]*$/g, '')
    return marked.parse(text, { breaks: true, gfm: true })
  } catch {
    return answer
  }
}

onMounted(() => {
  fetchHistory()
})
</script>

<style scoped>
.page-container {
  padding: 0;
}

.search-card, .result-card, .history-card {
  margin-bottom: 20px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.upload-tip {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.voice-input-actions {
  display: flex;
  gap: 8px;
  margin-top: 6px;
}

.upload-file-card {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  border-radius: 6px;
}

.upload-thumbnail {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-video-thumb {
  width: 100%;
  height: 100%;
  background: #1e1e1e;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.upload-file-actions {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: rgba(0, 0, 0, 0.5);
  opacity: 0;
  transition: opacity 0.3s;
  cursor: default;
}

.upload-file-card:hover .upload-file-actions {
  opacity: 1;
}

.upload-file-actions .el-icon {
  color: #fff;
  font-size: 20px;
  cursor: pointer;
  transition: transform 0.2s;
}

.upload-file-actions .el-icon:hover {
  transform: scale(1.2);
}

.result-desc {
  margin-bottom: 0;
}

.result-section {
  margin-top: 20px;
}

.result-section h4 {
  margin-bottom: 12px;
  color: var(--text-primary);
  font-size: 14px;
}

.file-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.file-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.file-name {
  font-size: 12px;
  color: var(--text-secondary);
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: center;
}

.video-preview {
  width: 120px;
  height: 120px;
  border-radius: 6px;
  background: #1e1e1e;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: opacity 0.3s;
}

.video-preview:hover {
  opacity: 0.8;
}

.video-label {
  color: #fff;
  font-size: 12px;
  margin-top: 4px;
}

.image-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #909399;
  font-size: 12px;
}

.image-error span {
  margin-top: 4px;
}

/* ============ Markdown 渲染样式 ============ */
.markdown-body {
  font-size: 14px;
  line-height: 1.8;
  word-wrap: break-word;
  overflow-wrap: break-word;
}

.markdown-body :deep(h1) {
  font-size: 22px;
  font-weight: 700;
  margin: 20px 0 12px;
  padding-bottom: 8px;
  border-bottom: 2px solid var(--border-color, #e4e7ed);
  color: var(--text-primary);
}

.markdown-body :deep(h2) {
  font-size: 19px;
  font-weight: 700;
  margin: 18px 0 10px;
  padding-bottom: 6px;
  border-bottom: 1px solid var(--border-color, #e4e7ed);
  color: var(--text-primary);
}

.markdown-body :deep(h3) {
  font-size: 16px;
  font-weight: 600;
  margin: 16px 0 8px;
  color: var(--text-primary);
}

.markdown-body :deep(h4) {
  font-size: 15px;
  font-weight: 600;
  margin: 14px 0 6px;
  color: var(--text-primary);
}

.markdown-body :deep(h5),
.markdown-body :deep(h6) {
  font-size: 14px;
  font-weight: 600;
  margin: 12px 0 6px;
  color: var(--text-primary);
}

.markdown-body :deep(p) {
  margin: 8px 0;
  line-height: 1.8;
  color: var(--text-regular);
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 24px;
  margin: 8px 0;
}

.markdown-body :deep(li) {
  margin: 4px 0;
  line-height: 1.8;
}

.markdown-body :deep(li > ul),
.markdown-body :deep(li > ol) {
  margin: 2px 0;
}

.markdown-body :deep(strong) {
  font-weight: 700;
  color: var(--text-primary);
}

.markdown-body :deep(em) {
  font-style: italic;
}

.markdown-body :deep(a) {
  color: var(--el-color-primary);
  text-decoration: none;
  border-bottom: 1px solid transparent;
  transition: border-color 0.2s;
}

.markdown-body :deep(a:hover) {
  border-bottom-color: var(--el-color-primary);
}

.markdown-body :deep(hr) {
  border: none;
  border-top: 1px solid var(--border-color, #e4e7ed);
  margin: 16px 0;
}

/* 行内代码 */
.markdown-body :deep(code) {
  background: #f0f2f5;
  color: #c7254e;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
}

/* 代码块 */
.markdown-body :deep(pre) {
  background: #1e1e1e;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 12px 0;
  position: relative;
}

.markdown-body :deep(pre code) {
  background: none;
  color: #d4d4d4;
  padding: 0;
  font-size: 13px;
  line-height: 1.6;
  border-radius: 0;
}

/* 引用块 */
.markdown-body :deep(blockquote) {
  border-left: 4px solid var(--el-color-primary);
  padding: 8px 16px;
  margin: 12px 0;
  background: rgba(64, 158, 255, 0.04);
  border-radius: 0 6px 6px 0;
  color: var(--text-secondary);
}

.markdown-body :deep(blockquote p) {
  margin: 4px 0;
}

/* 表格 */
.markdown-body :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 12px 0;
  font-size: 13px;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid var(--border-color, #dcdfe6);
  padding: 10px 14px;
  text-align: left;
}

.markdown-body :deep(th) {
  background: var(--bg-color, #f5f7fa);
  font-weight: 600;
  color: var(--text-primary);
}

.markdown-body :deep(tr:hover td) {
  background: rgba(64, 158, 255, 0.03);
}

/* 图片 */
.markdown-body :deep(img) {
  max-width: 100%;
  border-radius: 6px;
  margin: 8px 0;
}

/* 任务列表 */
.markdown-body :deep(input[type="checkbox"]) {
  margin-right: 6px;
}

/* 删除线 */
.markdown-body :deep(del) {
  text-decoration: line-through;
  color: var(--text-secondary);
}

/* ============ 结果内容容器 ============ */
.result-content {
  padding: 20px;
  background: #ffffff;
  border: 1px solid var(--border-color, #e4e7ed);
  border-radius: 8px;
  line-height: 1.8;
  color: var(--text-regular);
  max-height: 600px;
  overflow-y: auto;
}

/* ============ 打字动画 ============ */
.typing-indicator {
  display: flex;
  gap: 4px;
  margin-top: 12px;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: var(--primary-color);
  border-radius: 50%;
  animation: typing 1.4s infinite both;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.6;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

/* ============ 历史记录 ============ */
.history-list {
  max-height: 500px;
  overflow-y: auto;
}

.history-item {
  padding: 12px;
  border-bottom: 1px solid var(--border-color);
  cursor: pointer;
  transition: background-color 0.3s;
}

.history-item:hover {
  background-color: var(--bg-color);
}

.history-item:last-child {
  border-bottom: none;
}

.history-query {
  font-size: 14px;
  color: var(--text-primary);
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--text-secondary);
}

.meta-left {
  display: flex;
  align-items: center;
  gap: 4px;
}

.history-actions {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
  margin-top: 4px;
}

.detail-section {
  margin-top: 20px;
}

.detail-section h4 {
  margin-bottom: 12px;
  color: var(--text-primary);
  font-size: 14px;
}

.history-footer {
  text-align: center;
  padding: 12px 0 0;
  border-top: 1px solid var(--border-color);
  margin-top: 8px;
}

.view-all-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--el-color-primary);
  font-size: 13px;
  text-decoration: none;
  transition: opacity 0.3s;
}

.view-all-link:hover {
  opacity: 0.8;
}

/* ============ 排序控件 ============ */
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-header h4 {
  margin: 0;
}

.sort-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sort-label {
  font-size: 13px;
  color: var(--text-secondary);
}

</style>
