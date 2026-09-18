<template>
  <div class="page-container">
    <el-card class="main-card">
      <template #header>
        <div class="card-header">
          <div class="card-title">
            <el-icon><Collection /></el-icon>
            <span>知识案例</span>
          </div>
          <div class="header-actions">
            <el-button type="primary" @click="showUploadDialog = true">
              <el-icon><Upload /></el-icon>上传案例
            </el-button>
            <el-button type="success" plain @click="refreshList">
              <el-icon><Refresh /></el-icon>刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- 知识库信息栏 -->
      <div class="dataset-info-bar">
        <el-icon color="#409eff"><FolderOpened /></el-icon>
        <span class="dataset-label">当前知识库：</span>
        <span class="dataset-name">案例库</span>
        <el-tag type="info" size="small" class="dataset-id-tag">ID: {{ DATASET_ID }}</el-tag>
        <el-tag type="primary" effect="plain" size="small">{{ pagination.total }} 篇文档</el-tag>
      </div>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-form :inline="true">
          <el-form-item label="文档名称">
            <el-input v-model="searchKeyword" placeholder="搜索文档名称..." clearable style="width: 220px" @clear="fetchDocuments" @keyup.enter="fetchDocuments" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="statusFilter" placeholder="全部" clearable style="width: 120px" @change="fetchDocuments">
              <el-option label="全部" value="" />
              <el-option label="可用" value="available" />
              <el-option label="归档" value="archived" />
              <el-option label="启用" value="enabled" />
              <el-option label="禁用" value="disabled" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="fetchDocuments">查询</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 文档列表 -->
      <el-table :data="documentList" border stripe v-loading="loading" class="data-table">
        <el-table-column label="序号" width="60" align="center" type="index" />
        <el-table-column prop="name" label="文档名称" min-width="250" show-overflow-tooltip />
        <el-table-column prop="word_count" label="字数" width="100" align="center">
          <template #default="{ row }">
            <span>{{ formatNumber(row.word_count) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="tokens" label="Token数" width="100" align="center">
          <template #default="{ row }">
            <span>{{ formatNumber(row.tokens) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="hit_count" label="命中次数" width="100" align="center" />
        <el-table-column prop="indexing_status" label="索引状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getIndexStatusType(row.indexing_status)" size="small">
              {{ getIndexStatusText(row.indexing_status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="display_status" label="显示状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.display_status === 'available' ? 'success' : 'info'" size="small">
              {{ row.display_status === 'available' ? '可用' : '归档' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="enabled" label="启用" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 'true' || row.enabled === true ? 'success' : 'danger'" size="small">
              {{ row.enabled === 'true' || row.enabled === true ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170" align="center">
          <template #default="{ row }">
            {{ row.created_at ? formatTime(row.created_at) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleViewSegments(row)">
              <el-icon><View /></el-icon>详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.limit"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchDocuments"
          @current-change="fetchDocuments"
        />
      </div>
    </el-card>

    <!-- 上传案例对话框 -->
    <el-dialog v-model="showUploadDialog" title="上传检修案例" width="600px">
      <el-form ref="uploadFormRef" :model="uploadForm" :rules="uploadRules" label-width="100px">
        <el-form-item label="案例标题" prop="title">
          <el-input v-model="uploadForm.title" placeholder="请输入案例标题" />
        </el-form-item>
        <el-form-item label="设备型号" prop="equipModel">
          <el-input v-model="uploadForm.equipModel" placeholder="请输入设备型号" />
        </el-form-item>
        <el-form-item label="故障描述" prop="symptom">
          <el-input
            v-model="uploadForm.symptom"
            type="textarea"
            :rows="5"
            placeholder="请详细描述设备故障现象"
          />
          <div class="voice-input-actions">
            <el-button type="primary" link :icon="Microphone" @click="startRecord('symptom')" :disabled="transcribing">
              {{ recordingField === 'symptom' ? '停止录音' : '语音输入' }}
            </el-button>
            <el-button type="primary" link :icon="UploadFilled" @click="triggerAudioUpload('symptom')" :disabled="transcribing">
              上传音频
            </el-button>
            <input
              ref="symptomFileInput"
              type="file"
              accept="audio/*"
              style="display: none"
              @change="handleAudioFileSelected($event, 'symptom')"
            />
          </div>
        </el-form-item>
        <el-form-item label="解决方案" prop="solution">
          <el-input
            v-model="uploadForm.solution"
            type="textarea"
            :rows="5"
            placeholder="请详细描述故障解决方案和操作步骤"
          />
          <div class="voice-input-actions">
            <el-button type="primary" link :icon="Microphone" @click="startRecord('solution')" :disabled="transcribing">
              {{ recordingField === 'solution' ? '停止录音' : '语音输入' }}
            </el-button>
            <el-button type="primary" link :icon="UploadFilled" @click="triggerAudioUpload('solution')" :disabled="transcribing">
              上传音频
            </el-button>
            <input
              ref="solutionFileInput"
              type="file"
              accept="audio/*"
              style="display: none"
              @change="handleAudioFileSelected($event, 'solution')"
            />
          </div>
        </el-form-item>
        <el-form-item label="案例图片">
          <el-upload
            v-model:file-list="imageFileList"
            list-type="picture-card"
            :auto-upload="true"
            :http-request="handleImageUpload"
            :before-upload="beforeImageUpload"
            :on-remove="handleImageRemove"
            :limit="5"
            :on-exceed="handleImageExceed"
            accept="image/*"
          >
            <el-icon><Plus /></el-icon>
            <template #tip>
              <div class="el-upload__tip">最多上传5张图片，单张不超过10MB</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <div v-if="recordingField" class="recording-bar">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>正在录音...点击“停止录音”结束</span>
        <span class="recording-time">{{ formatRecordDuration(recordDuration) }}</span>
      </div>
      <div v-if="transcribing" class="recording-bar">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>正在转写语音为文字，请稍候...</span>
      </div>
      <template #footer>
        <el-button @click="closeUploadDialog">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">提交审核</el-button>
      </template>
    </el-dialog>

    <!-- 语音转写提示对话框 -->
    <el-dialog v-model="showTranscribeResult" title="语音转写结果" width="500px">
      <el-alert :title="`转写字段：${transcribeFieldLabel}`" type="info" :closable="false" show-icon />
      <div class="transcribe-text">{{ transcribePreviewText }}</div>
      <el-alert
        v-if="transcribeTargetField"
        title="原输入框已有内容，将追加到末尾"
        type="warning"
        :closable="false"
        show-icon
        style="margin-top: 12px"
      />
      <template #footer>
        <el-button @click="showTranscribeResult = false">取消</el-button>
        <el-button type="primary" @click="applyTranscribeText(false)">追加到输入框</el-button>
        <el-button type="success" @click="applyTranscribeText(true)">覆盖输入框</el-button>
      </template>
    </el-dialog>

    <!-- 文档分段详情对话框 -->
    <el-dialog v-model="showSegmentsDialog" :title="`文档分段 - ${currentDoc?.name || ''}`" width="900px" top="5vh">
      <span class="segments-count">共 {{ segmentList.length }} 个分段</span>
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
          </div>
          <div class="segment-item-content">{{ seg.content }}</div>
        </div>
        <el-empty v-if="segmentList.length === 0 && !segmentsLoading" description="暂无分段" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Microphone, UploadFilled, Loading, Plus } from '@element-plus/icons-vue'
import { getDifyDocuments, uploadKnowledge, getDocumentSegments, audioToText, uploadCaseImage } from '@/api'
import type { UploadProps, UploadUserFile } from 'element-plus'
import { fetchCaseDatasetIds } from '@/utils/caseDatasetIds'

// 案例库 dataset ID（从后端动态获取）
const DATASET_ID = ref('')

// 加载案例库 ID
const loadCaseDatasetId = async () => {
  const ids = await fetchCaseDatasetIds()
  DATASET_ID.value = ids[0] || ''
}

const loading = ref(false)
const uploading = ref(false)
const showUploadDialog = ref(false)
const documentList = ref<any[]>([])
const total = ref(0)
const uploadFormRef = ref<FormInstance>()
const searchKeyword = ref('')
const statusFilter = ref('')

const pagination = reactive({
  page: 1,
  limit: 10,
  total: 0
})

const uploadForm = reactive({
  title: '',
  symptom: '',
  solution: '',
  equipModel: '',
  images: '' as string
})

// 案例图片上传
const imageFileList = ref<UploadUserFile[]>([])
// 独立维护已上传的图片URL，不依赖 el-upload 内部状态
const uploadedImageUrls = ref<string[]>([])

const beforeImageUpload: UploadProps['beforeUpload'] = (rawFile) => {
  if (!rawFile.type.startsWith('image/')) {
    ElMessage.error('仅支持图片格式')
    return false
  }
  if (rawFile.size > 10 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过10MB')
    return false
  }
  return true
}

const handleImageUpload = async (options: { file: File; onSuccess: Function; onError: Function }) => {
  try {
    const res = await uploadCaseImage(options.file)
    console.log('[案例图片上传] 响应:', res)
    const url = (res as any)?.data?.url
    if (url) {
      uploadedImageUrls.value.push(url)
      console.log('[案例图片上传] 已收集URL, 当前列表:', uploadedImageUrls.value)
      options.onSuccess({ data: { url } })
    } else {
      options.onError(new Error('上传返回的URL为空'))
      ElMessage.error('图片上传返回的URL为空')
    }
  } catch (error: any) {
    options.onError(error as Error)
    const errMsg = error?.msg || error?.message || '未知错误'
    ElMessage.error('图片上传失败: ' + errMsg)
    console.error('[案例图片上传] 失败:', error)
  }
}

const handleImageRemove = (uploadFile: any) => {
  const url = (uploadFile?.response as any)?.data?.url
  if (url) {
    const idx = uploadedImageUrls.value.indexOf(url)
    if (idx >= 0) uploadedImageUrls.value.splice(idx, 1)
    console.log('[案例图片移除] 移除URL:', url, '剩余:', uploadedImageUrls.value)
  }
}

const handleImageExceed = () => {
  ElMessage.warning('最多上传5张图片')
}

// 分段详情
const showSegmentsDialog = ref(false)
const segmentsLoading = ref(false)
const segmentList = ref<any[]>([])
const currentDoc = ref<any>(null)

const uploadRules: FormRules = {
  title: [{ required: true, message: '请输入案例标题', trigger: 'blur' }],
  symptom: [{ required: true, message: '请输入故障描述', trigger: 'blur' }],
  solution: [{ required: true, message: '请输入解决方案', trigger: 'blur' }],
  equipModel: [{ required: true, message: '请输入设备型号', trigger: 'blur' }]
}

const getIndexStatusType = (status: string) => {
  const map: Record<string, string> = {
    'waiting': 'info',
    'indexing': 'warning',
    'paused': 'warning',
    'completed': 'success',
    'error': 'danger'
  }
  return map[status] || 'info'
}

const getIndexStatusText = (status: string) => {
  const map: Record<string, string> = {
    'waiting': '等待中',
    'indexing': '索引中',
    'paused': '已暂停',
    'completed': '已完成',
    'error': '失败'
  }
  return map[status] || status || '未知'
}

const formatNumber = (num: number) => {
  if (!num && num !== 0) return '-'
  return num.toLocaleString()
}

const formatTime = (time: string) => {
  if (!time) return '-'
  // Dify 返回的时间戳可能是秒级
  const ts = typeof time === 'number' ? time * 1000 : time
  const d = new Date(ts)
  if (isNaN(d.getTime())) return time
  return d.toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit'
  })
}

const fetchDocuments = async () => {
  loading.value = true
  try {
    const res: any = await getDifyDocuments(DATASET_ID.value, pagination.page, pagination.limit)
    // Dify API 返回格式：{ data: [...], total: number }
    const data = res.data || res
    documentList.value = Array.isArray(data) ? data : (data.data || [])
    pagination.total = data.total || documentList.value.length
  } catch (error) {
    console.error('获取文档列表失败', error)
    ElMessage.error('获取文档列表失败')
  } finally {
    loading.value = false
  }
}

const refreshList = () => {
  pagination.page = 1
  fetchDocuments()
}

const handleUpload = async () => {
  const valid = await uploadFormRef.value?.validate().catch(() => false)
  if (!valid) return

  // 存在尚未上传完成的图片时阻止提交
  if (imageFileList.value.some(f => f.status === 'uploading')) {
    ElMessage.warning('图片正在上传，请稍候')
    return
  }

  uploading.value = true
  try {
    // 直接使用独立维护的URL列表，不依赖 el-upload 内部状态
    const imageUrls = uploadedImageUrls.value.filter(Boolean)
    uploadForm.images = imageUrls.length > 0 ? JSON.stringify(imageUrls) : ''

    console.log('[案例提交] uploadedImageUrls:', uploadedImageUrls.value)
    console.log('[案例提交] images 字段:', uploadForm.images)

    await uploadKnowledge({
      title: uploadForm.title.trim(),
      symptom: uploadForm.symptom.trim(),
      solution: uploadForm.solution.trim(),
      equipModel: uploadForm.equipModel.trim(),
      images: uploadForm.images
    })
    ElMessage.success('案例已提交，等待审核')
    closeUploadDialog()
  } catch (error) {
    console.error('上传失败', error)
  } finally {
    uploading.value = false
  }
}

const closeUploadDialog = () => {
  showUploadDialog.value = false
  uploadForm.title = ''
  uploadForm.symptom = ''
  uploadForm.solution = ''
  uploadForm.equipModel = ''
  uploadForm.images = ''
  imageFileList.value = []
  uploadedImageUrls.value = []
  // 关闭弹窗时如果有录音未停止，强制停止
  if (recordingField.value) {
    stopRecord()
  }
  transcribing.value = false
}

const handleViewSegments = async (doc: any) => {
  currentDoc.value = doc
  showSegmentsDialog.value = true
  segmentsLoading.value = true
  segmentList.value = []
  try {
    const res: any = await getDocumentSegments(DATASET_ID.value, doc.id, { page: 1, limit: 50 })
    segmentList.value = res.data?.data || []
  } catch (error) {
    console.error('获取分段失败', error)
    ElMessage.error('获取分段失败')
  } finally {
    segmentsLoading.value = false
  }
}

// ========== 语音转文字逻辑 ==========
const symptomFileInput = ref<HTMLInputElement | null>(null)
const solutionFileInput = ref<HTMLInputElement | null>(null)
const recordingField = ref<'symptom' | 'solution' | ''>('')
const transcribing = ref(false)
const recordDuration = ref(0)
let recordTimer: ReturnType<typeof setInterval> | null = null

// 转写结果对话框
const showTranscribeResult = ref(false)
const transcribeTargetField = ref<'symptom' | 'solution' | ''>('')
const transcribePreviewText = ref('')
const transcribeFieldLabel = computed(() => {
  return transcribeTargetField.value === 'symptom' ? '故障描述' : '解决方案'
})

const formatRecordDuration = (s: number) => {
  const m = Math.floor(s / 60).toString().padStart(2, '0')
  const sec = (s % 60).toString().padStart(2, '0')
  return `${m}:${sec}`
}

const audioContext = ref<AudioContext | null>(null)
const audioChunksFloat32 = ref<Float32Array[]>([])
const audioSource = ref<MediaStreamAudioSourceNode | null>(null)
const audioProcessor = ref<ScriptProcessorNode | null>(null)

const startRecord = async (field: 'symptom' | 'solution') => {
  if (recordingField.value) {
    stopRecord()
    return
  }
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    audioContext.value = new AudioContext()
    audioSource.value = audioContext.value.createMediaStreamSource(stream)
    audioProcessor.value = audioContext.value.createScriptProcessor(4096, 1, 1)
    audioChunksFloat32.value = []

    audioProcessor.value.onaudioprocess = (e) => {
      const channelData = e.inputBuffer.getChannelData(0)
      audioChunksFloat32.value.push(new Float32Array(channelData))
    }

    audioSource.value.connect(audioProcessor.value)
    audioProcessor.value.connect(audioContext.value.destination)

    recordingField.value = field
    recordDuration.value = 0
    recordTimer = setInterval(() => { recordDuration.value++ }, 1000)
    ElMessage.success('开始录音')
  } catch (error) {
    console.error('无法访问麦克风', error)
    ElMessage.error('无法访问麦克风，请检查浏览器权限')
  }
}

const stopRecord = () => {
  if (audioProcessor.value) {
    audioProcessor.value.disconnect()
    audioProcessor.value = null
  }
  if (audioSource.value) {
    audioSource.value.disconnect()
    audioSource.value = null
  }
  if (audioContext.value) {
    audioContext.value.close()
    audioContext.value = null
  }
  if (recordTimer) {
    clearInterval(recordTimer)
    recordTimer = null
  }
  
  if (recordingField.value && audioChunksFloat32.value.length > 0) {
    const wavBlob = float32ToWav(audioChunksFloat32.value, 16000)
    const file = new File([wavBlob], `录音-${Date.now()}.wav`, { type: 'audio/wav' })
    transcribeAudio(file, recordingField.value)
  }
  
  recordingField.value = ''
  audioChunksFloat32.value = []
}

const float32ToWav = (audioChunks: Float32Array[], sampleRate: number): Blob => {
  const totalLength = audioChunks.reduce((acc, chunk) => acc + chunk.length, 0)
  const result = new Float32Array(totalLength)
  let offset = 0
  for (const chunk of audioChunks) {
    result.set(chunk, offset)
    offset += chunk.length
  }

  const buffer = new ArrayBuffer(44 + result.length * 2)
  const view = new DataView(buffer)

  const writeString = (offset: number, str: string) => {
    for (let i = 0; i < str.length; i++) {
      view.setUint8(offset + i, str.charCodeAt(i))
    }
  }

  writeString(0, 'RIFF')
  view.setUint32(4, 36 + result.length * 2, true)
  writeString(8, 'WAVE')
  writeString(12, 'fmt ')
  view.setUint32(16, 16, true)
  view.setUint16(20, 1, true)
  view.setUint16(22, 1, true)
  view.setUint32(24, sampleRate, true)
  view.setUint32(28, sampleRate * 2, true)
  view.setUint16(32, 2, true)
  view.setUint16(34, 16, true)
  writeString(36, 'data')
  view.setUint32(40, result.length * 2, true)

  let offset32 = 44
  for (let i = 0; i < result.length; i++) {
    const s = Math.max(-1, Math.min(1, result[i]))
    view.setInt16(offset32, s < 0 ? s * 0x8000 : s * 0x7FFF, true)
    offset32 += 2
  }

  return new Blob([buffer], { type: 'audio/wav' })
}

const triggerAudioUpload = (field: 'symptom' | 'solution') => {
  transcribeTargetField.value = field
  if (field === 'symptom') symptomFileInput.value?.click()
  else solutionFileInput.value?.click()
}

const handleAudioFileSelected = (e: Event, field: 'symptom' | 'solution') => {
  const input = e.target as HTMLInputElement
  if (input.files && input.files.length > 0) {
    transcribeAudio(input.files[0], field)
    input.value = ''
  }
}

const transcribeAudio = async (file: File, field: 'symptom' | 'solution') => {
  transcribing.value = true
  transcribeTargetField.value = field
  let loadingMsg: ReturnType<typeof ElMessage> | null = null
  try {
    loadingMsg = ElMessage({ message: '正在识别语音内容，请稍候...', type: 'info', duration: 0, showClose: false })
    const res: any = await audioToText(file)
    const text = res.data?.text || ''
    if (!text) {
      ElMessage.warning('未识别到语音内容')
      return
    }
    transcribePreviewText.value = text
    showTranscribeResult.value = true
  } catch (error: any) {
    console.error('语音转文字失败', error)
    ElMessage.error('语音转文字失败：' + (error?.message || '请重试'))
  } finally {
    loadingMsg?.close()
    transcribing.value = false
  }
}

const applyTranscribeText = (overwrite = false) => {
  const field = transcribeTargetField.value
  if (!field) return
  if (field === 'symptom') {
    uploadForm.symptom = overwrite ? transcribePreviewText.value : (uploadForm.symptom ? uploadForm.symptom + '\n' + transcribePreviewText.value : transcribePreviewText.value)
  } else {
    uploadForm.solution = overwrite ? transcribePreviewText.value : (uploadForm.solution ? uploadForm.solution + '\n' + transcribePreviewText.value : transcribePreviewText.value)
  }
  showTranscribeResult.value = false
  ElMessage.success('已填入输入框')
}

onMounted(async () => {
  await loadCaseDatasetId()
  fetchDocuments()
})

onUnmounted(() => {
  if (recordTimer) clearInterval(recordTimer)
  if (recordingField.value) {
    stopRecord()
  }
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

.header-actions {
  display: flex;
  gap: 8px;
}

.dataset-info-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  margin-bottom: 16px;
  background: var(--el-color-primary-light-9);
  border-radius: 8px;
  font-size: 14px;
}

.dataset-label {
  color: var(--text-secondary);
}

.dataset-name {
  font-weight: 600;
  color: var(--el-color-primary);
}

.dataset-id-tag {
  margin-left: 8px;
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

/* 分段详情 */
.segments-count {
  font-size: 13px;
  color: #94a3b8;
  display: block;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
}

.segments-list {
  max-height: 500px;
  overflow-y: auto;
}

.segment-item {
  margin-bottom: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}

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

.segment-item-content {
  padding: 12px 16px;
  font-size: 13px;
  line-height: 1.7;
  color: #334155;
  white-space: pre-wrap;
  max-height: 120px;
  overflow-y: auto;
}

/* 语音输入按钮区 */
.voice-input-actions {
  display: flex;
  gap: 8px;
  margin-top: 6px;
}

/* 录音状态条 */
.recording-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  margin-top: 12px;
  background: rgba(245, 108, 108, 0.08);
  border: 1px solid rgba(245, 108, 108, 0.3);
  border-radius: 6px;
  color: #f56c6c;
  font-size: 13px;
}

.recording-bar .el-icon {
  animation: pulse 1s infinite;
}

.recording-time {
  margin-left: auto;
  font-family: var(--font-mono, monospace);
  font-weight: 600;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

/* 转写结果文本 */
.transcribe-text {
  margin-top: 12px;
  padding: 16px;
  background: var(--el-fill-color-light, #f5f7fa);
  border-radius: 6px;
  font-size: 14px;
  line-height: 1.7;
  color: #334155;
  white-space: pre-wrap;
  max-height: 300px;
  overflow-y: auto;
}
</style>
