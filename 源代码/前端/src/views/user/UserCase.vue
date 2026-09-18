<template>
  <div class="case-page">
    <!-- 页面头部 -->
    <div class="case-header">
      <div class="header-left">
        <h2>案例知识库</h2>
        <span class="case-count">{{ pagination.total }} 条记录</span>
      </div>
      <div class="header-search" v-if="caseList.length > 0 || searchKeyword">
        <div class="header-search-row">
          <div class="search-input-wrap">
            <svg class="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>
            <input
              v-model="searchKeyword"
              type="text"
              class="search-input"
              placeholder="搜索案例标题或内容..."
              @input="onSearchInput"
            />
            <button v-if="searchKeyword" class="clear-btn" @click="clearSearch">×</button>
          </div>
          <button class="upload-case-btn" @click="showUploadDialog = true">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
            上传案例
          </button>
        </div>
      </div>
    </div>

    <!-- 案例列表 -->
    <div class="case-list" v-loading="loading">
      <!-- 空状态 -->
      <div v-if="caseList.length === 0 && !loading" class="empty-state">
        <div class="empty-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>
        </div>
        <h3 v-if="searchKeyword">未找到匹配案例</h3>
        <h3 v-else>暂无已审核案例</h3>
        <p v-if="searchKeyword">尝试更换搜索关键词</p>
        <p v-else>知识库中的案例将在审核通过后展示</p>
        <button v-if="searchKeyword" class="empty-btn" @click="clearSearch">清除搜索</button>
      </div>

      <!-- 案例卡片 -->
      <div v-for="item in caseList" :key="item.knowledgeId" class="case-card" @click="showDetail(item)">
        <div class="card-top">
          <h3 class="card-title">{{ highlightMatch(item.title || '未命名案例') }}</h3>
          <div class="card-tags">
            <span class="card-tag equip-tag">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
              {{ item.equipModel || '未分类' }}
            </span>
            <span class="card-tag time-tag">{{ formatDate(item.createTime) }}</span>
          </div>
        </div>
        <p class="card-desc" v-html="highlightMatch(truncate(item.symptom || item.content, 200))"></p>
        <div class="card-footer">
          <span class="card-action">
            查看详情
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m9 18 6-6-6-6"/></svg>
          </span>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="case-pagination" v-if="pagination.total > pagination.pageSize">
      <button
        class="page-btn"
        :disabled="pagination.pageNum <= 1"
        @click="changePage(pagination.pageNum - 1)"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m15 18-6-6 6-6"/></svg>
      </button>
      <span class="page-info">{{ pagination.pageNum }} / {{ totalPages }}</span>
      <button
        class="page-btn"
        :disabled="pagination.pageNum >= totalPages"
        @click="changePage(pagination.pageNum + 1)"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m9 18 6-6-6-6"/></svg>
      </button>
    </div>

    <!-- 上传案例弹窗 -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="showUploadDialog" class="modal-overlay" @click.self="cancelUpload">
          <div class="modal-content upload-modal">
            <div class="modal-header">
              <h3>上传检修案例</h3>
              <button class="modal-close" @click="cancelUpload">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
            <div class="modal-body">
              <div class="upload-form-item">
                <label class="upload-form-label">案例标题 <span class="required">*</span></label>
                <input v-model="uploadForm.title" class="upload-form-input" placeholder="请输入案例标题" />
                <p v-if="uploadErrors.title" class="upload-form-error">{{ uploadErrors.title }}</p>
              </div>
              <div class="upload-form-item">
                <label class="upload-form-label">设备型号 <span class="required">*</span></label>
                <input v-model="uploadForm.equipModel" class="upload-form-input" placeholder="请输入设备型号" />
                <p v-if="uploadErrors.equipModel" class="upload-form-error">{{ uploadErrors.equipModel }}</p>
              </div>
              <div class="upload-form-item">
                <label class="upload-form-label">故障描述 <span class="required">*</span></label>
                <textarea v-model="uploadForm.symptom" class="upload-form-textarea" rows="5" placeholder="请详细描述设备故障现象"></textarea>
                <div class="voice-input-actions">
                  <button type="button" class="voice-btn" :disabled="transcribing" @click="startRecord('symptom')">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3z"/><path d="M19 10v2a7 7 0 0 1-14 0v-2"/><line x1="12" y1="19" x2="12" y2="22"/></svg>
                    {{ recordingField === 'symptom' ? '停止录音' : '语音输入' }}
                  </button>
                  <button type="button" class="voice-btn" :disabled="transcribing" @click="triggerAudioUpload('symptom')">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
                    上传音频
                  </button>
                </div>
                <p v-if="uploadErrors.symptom" class="upload-form-error">{{ uploadErrors.symptom }}</p>
              </div>
              <div class="upload-form-item">
                <label class="upload-form-label">解决方案 <span class="required">*</span></label>
                <textarea v-model="uploadForm.solution" class="upload-form-textarea" rows="5" placeholder="请详细描述故障解决方案和操作步骤"></textarea>
                <div class="voice-input-actions">
                  <button type="button" class="voice-btn" :disabled="transcribing" @click="startRecord('solution')">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3z"/><path d="M19 10v2a7 7 0 0 1-14 0v-2"/><line x1="12" y1="19" x2="12" y2="22"/></svg>
                    {{ recordingField === 'solution' ? '停止录音' : '语音输入' }}
                  </button>
                  <button type="button" class="voice-btn" :disabled="transcribing" @click="triggerAudioUpload('solution')">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
                    上传音频
                  </button>
                </div>
                <p v-if="uploadErrors.solution" class="upload-form-error">{{ uploadErrors.solution }}</p>
              </div>
              <div class="upload-form-item">
                <label class="upload-form-label">案例图片</label>
                <div class="image-upload-area">
                  <div v-for="(img, index) in imageList" :key="index" class="image-thumb">
                    <img :src="img.url" alt="案例图片" @click="previewImage(img.url)" />
                    <button type="button" class="image-thumb-remove" :disabled="uploading" @click="removeImage(index)">×</button>
                    <span v-if="img.uploading" class="image-thumb-loading">上传中...</span>
                  </div>
                  <button
                    v-if="imageList.length < 5"
                    type="button"
                    class="image-add-btn"
                    :disabled="uploading"
                    @click="imageFileInput?.click()"
                  >
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                    <span>添加图片</span>
                  </button>
                </div>
                <input ref="imageFileInput" type="file" accept="image/*" multiple style="display:none" @change="handleImageSelected" />
                <input ref="symptomFileInput" type="file" accept="audio/*" style="display:none" @change="handleAudioFileSelected($event, 'symptom')" />
                <input ref="solutionFileInput" type="file" accept="audio/*" style="display:none" @change="handleAudioFileSelected($event, 'solution')" />
                <p class="upload-form-tip">最多上传5张图片，单张不超过10MB</p>
              </div>
              <div v-if="recordingField" class="status-bar recording">
                <span class="status-dot"></span>
                <span>正在录音...点击“停止录音”结束</span>
                <span class="status-time">{{ formatRecordDuration(recordDuration) }}</span>
              </div>
              <div v-if="transcribing" class="status-bar transcribing">
                <span class="status-dot"></span>
                <span>正在转写语音为文字，请稍候...</span>
              </div>
            </div>
            <div class="modal-footer">
              <button class="upload-cancel-btn" @click="cancelUpload">取消</button>
              <button class="upload-submit-btn" :disabled="uploading" @click="handleUpload">
                {{ uploading ? '提交中...' : '提交审核' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 语音转写结果弹窗 -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="showTranscribeResult" class="modal-overlay" @click.self="showTranscribeResult = false">
          <div class="modal-content transcribe-modal">
            <div class="modal-header">
              <h3>语音转写结果</h3>
              <button class="modal-close" @click="showTranscribeResult = false">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
            <div class="modal-body">
              <div class="transcribe-field-label">转写字段：{{ transcribeFieldLabel }}</div>
              <div class="transcribe-text">{{ transcribePreviewText }}</div>
              <p v-if="hasExistingText" class="transcribe-warn">原输入框已有内容，将追加到末尾</p>
            </div>
            <div class="modal-footer">
              <button class="upload-cancel-btn" @click="showTranscribeResult = false">取消</button>
              <button class="upload-cancel-btn" @click="applyTranscribeText(false)">追加到输入框</button>
              <button class="upload-submit-btn" @click="applyTranscribeText(true)">覆盖输入框</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 详情弹窗 -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="detailVisible" class="modal-overlay" @click.self="detailVisible = false">
          <div class="modal-content">
            <div class="modal-header">
              <h3>案例详情</h3>
              <button class="modal-close" @click="detailVisible = false">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
            <div v-if="detailItem" class="modal-body">
              <div class="detail-grid">
                <div class="detail-field">
                  <span class="detail-label">案例标题</span>
                  <span class="detail-value">{{ detailItem.title || '未命名' }}</span>
                </div>
                <div class="detail-field">
                  <span class="detail-label">设备型号</span>
                  <span class="detail-value">
                    <span class="detail-tag">{{ detailItem.equipModel || '-' }}</span>
                  </span>
                </div>
                <div class="detail-field">
                  <span class="detail-label">提交时间</span>
                  <span class="detail-value">{{ formatDateTime(detailItem.createTime) }}</span>
                </div>
                <div class="detail-field">
                  <span class="detail-label">提交人</span>
                  <span class="detail-value">{{ detailItem.uploadUserName || '-' }}</span>
                </div>
              </div>
              <div class="detail-content-section">
                <h4>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/></svg>
                  故障描述
                </h4>
                <div class="detail-content symptom-block">{{ detailItem.symptom || '暂无描述' }}</div>
              </div>
              <div class="detail-content-section">
                <h4>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m12 3 2 3.09A6 6 0 1 1 9.05 10"/><path d="M6 16l6-6 6 6"/><path d="M8 22h8"/></svg>
                  解决方案
                </h4>
                <div class="detail-content solution-block">{{ detailItem.solution || '暂无方案' }}</div>
              </div>
              <div v-if="detailItem.imgUrl" class="detail-image">
                <h4>附图</h4>
                <img :src="detailItem.imgUrl" alt="案例图片" @click="previewImage(detailItem.imgUrl)" />
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { getKnowledgeList, uploadKnowledge, audioToText, uploadCaseImage } from '@/api'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const caseList = ref<any[]>([])
const detailVisible = ref(false)
const detailItem = ref<any>(null)
const searchKeyword = ref('')
let searchTimer: ReturnType<typeof setTimeout> | null = null

// 上传案例
const showUploadDialog = ref(false)
const uploading = ref(false)
const uploadForm = reactive({
  title: '',
  equipModel: '',
  symptom: '',
  solution: '',
  images: '' as string
})
const uploadErrors = reactive({
  title: '',
  equipModel: '',
  symptom: '',
  solution: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const totalPages = computed(() => Math.max(1, Math.ceil(pagination.total / pagination.pageSize)))

onMounted(() => fetchCases())

onUnmounted(() => {
  if (recordTimer) clearInterval(recordTimer)
  if (recordingField.value) {
    stopRecord()
  }
})

const fetchCases = async () => {
  loading.value = true
  try {
    const res: any = await getKnowledgeList({
      auditStatus: 1,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    const data = res.data
    let allCases = data?.records || data || []

    // 前端搜索过滤
    if (searchKeyword.value.trim()) {
      const kw = searchKeyword.value.trim().toLowerCase()
      allCases = allCases.filter((item: any) =>
        (item.title || '').toLowerCase().includes(kw) ||
        (item.symptom || '').toLowerCase().includes(kw) ||
        (item.solution || '').toLowerCase().includes(kw) ||
        (item.equipModel || '').toLowerCase().includes(kw)
      )
      pagination.total = allCases.length
      caseList.value = allCases
    } else {
      caseList.value = allCases
      pagination.total = data?.total || allCases.length
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
}

const onSearchInput = () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    pagination.pageNum = 1
    fetchCases()
  }, 300)
}

const clearSearch = () => {
  searchKeyword.value = ''
  pagination.pageNum = 1
  fetchCases()
}

const changePage = (page: number) => {
  pagination.pageNum = page
  fetchCases()
}

const showDetail = (item: any) => {
  detailItem.value = item
  detailVisible.value = true
}

const highlightMatch = (text: string) => {
  if (!text || !searchKeyword.value.trim()) return text
  const kw = searchKeyword.value.trim()
  const escaped = kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return text.replace(new RegExp(`(${escaped})`, 'gi'), '<mark>$1</mark>')
}

const truncate = (text: string, max: number) => {
  if (!text) return '暂无描述'
  return text.length > max ? text.substring(0, max) + '...' : text
}

const formatDate = (time: string) => {
  if (!time) return ''
  return time.substring(0, 10)
}

const formatDateTime = (time: string) => {
  if (!time) return '-'
  return time.substring(0, 16).replace('T', ' ')
}

const previewImage = (url: string) => {
  window.open(url, '_blank')
}

// ========== 案例图片上传 ==========
interface ImageItem {
  url: string
  uploading: boolean
}
const imageList = ref<ImageItem[]>([])
const imageFileInput = ref<HTMLInputElement | null>(null)

const handleImageSelected = async (e: Event) => {
  const input = e.target as HTMLInputElement
  if (!input.files || input.files.length === 0) return
  const files = Array.from(input.files)
  input.value = ''

  const remain = 5 - imageList.value.length
  if (files.length > remain) {
    ElMessage.warning(`最多上传5张图片，仅添加前 ${remain} 张`)
  }
  const toUpload = files.slice(0, remain)

  for (const file of toUpload) {
    if (!file.type.startsWith('image/')) {
      ElMessage.error(`${file.name} 不是图片格式`)
      continue
    }
    if (file.size > 10 * 1024 * 1024) {
      ElMessage.error(`${file.name} 超过10MB`)
      continue
    }
    const localUrl = URL.createObjectURL(file)
    const item: ImageItem = reactive({ url: localUrl, uploading: true })
    imageList.value.push(item)
    try {
      const res: any = await uploadCaseImage(file)
      console.log('[用户案例图片上传] 响应:', res)
      const remoteUrl = res.data?.url
      if (remoteUrl) {
        URL.revokeObjectURL(localUrl)
        item.url = remoteUrl
      } else {
        throw new Error('上传返回的URL为空')
      }
    } catch (err) {
      console.error('[用户案例图片上传] 失败:', err)
      ElMessage.error('图片上传失败')
      const idx = imageList.value.indexOf(item)
      if (idx >= 0) imageList.value.splice(idx, 1)
      URL.revokeObjectURL(localUrl)
    } finally {
      item.uploading = false
    }
  }
}

const removeImage = (index: number) => {
  imageList.value.splice(index, 1)
}

// ========== 语音转文字 ==========
const symptomFileInput = ref<HTMLInputElement | null>(null)
const solutionFileInput = ref<HTMLInputElement | null>(null)
const recordingField = ref<'symptom' | 'solution' | ''>('')
const transcribing = ref(false)
const recordDuration = ref(0)
let recordTimer: ReturnType<typeof setInterval> | null = null

const showTranscribeResult = ref(false)
const transcribeTargetField = ref<'symptom' | 'solution' | ''>('')
const transcribePreviewText = ref('')
const transcribeFieldLabel = computed(() =>
  transcribeTargetField.value === 'symptom' ? '故障描述' : '解决方案'
)
const hasExistingText = computed(() => {
  const f = transcribeTargetField.value
  if (!f) return false
  return !!(f === 'symptom' ? uploadForm.symptom : uploadForm.solution).trim()
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

    audioProcessor.value.onaudioprocess = (ev) => {
      const channelData = ev.inputBuffer.getChannelData(0)
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

  const writeString = (off: number, str: string) => {
    for (let i = 0; i < str.length; i++) {
      view.setUint8(off + i, str.charCodeAt(i))
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

  let off32 = 44
  for (let i = 0; i < result.length; i++) {
    const s = Math.max(-1, Math.min(1, result[i]))
    view.setInt16(off32, s < 0 ? s * 0x8000 : s * 0x7FFF, true)
    off32 += 2
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
  try {
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
    transcribing.value = false
  }
}

const applyTranscribeText = (overwrite = false) => {
  const field = transcribeTargetField.value
  if (!field) return
  if (field === 'symptom') {
    uploadForm.symptom = overwrite
      ? transcribePreviewText.value
      : (uploadForm.symptom ? uploadForm.symptom + '\n' + transcribePreviewText.value : transcribePreviewText.value)
  } else {
    uploadForm.solution = overwrite
      ? transcribePreviewText.value
      : (uploadForm.solution ? uploadForm.solution + '\n' + transcribePreviewText.value : transcribePreviewText.value)
  }
  showTranscribeResult.value = false
  ElMessage.success('已填入输入框')
}

const clearUploadErrors = () => {
  uploadErrors.title = ''
  uploadErrors.equipModel = ''
  uploadErrors.symptom = ''
  uploadErrors.solution = ''
}

const validateUpload = (): boolean => {
  clearUploadErrors()
  let valid = true
  if (!uploadForm.title.trim()) { uploadErrors.title = '请输入案例标题'; valid = false }
  if (!uploadForm.equipModel.trim()) { uploadErrors.equipModel = '请输入设备型号'; valid = false }
  if (!uploadForm.symptom.trim()) { uploadErrors.symptom = '请输入故障描述'; valid = false }
  if (!uploadForm.solution.trim()) { uploadErrors.solution = '请输入解决方案'; valid = false }
  return valid
}

const handleUpload = async () => {
  if (!validateUpload()) return
  // 存在尚未上传完成的图片时阻止提交
  if (imageList.value.some(i => i.uploading)) {
    ElMessage.warning('图片正在上传，请稍候')
    return
  }
  uploading.value = true
  try {
    const imageUrls = imageList.value.map(i => i.url).filter(Boolean)
    uploadForm.images = imageUrls.length > 0 ? JSON.stringify(imageUrls) : ''

    console.log('[用户案例提交] imageList:', imageList.value.map(i => ({ url: i.url, uploading: i.uploading })))
    console.log('[用户案例提交] images 字段:', uploadForm.images)

    await uploadKnowledge({
      title: uploadForm.title.trim(),
      equipModel: uploadForm.equipModel.trim(),
      symptom: uploadForm.symptom.trim(),
      solution: uploadForm.solution.trim(),
      images: uploadForm.images
    })
    ElMessage.success('案例已提交，等待审核')
    closeUploadDialog()
  } catch {
    ElMessage.error('上传案例失败')
  } finally {
    uploading.value = false
  }
}

const closeUploadDialog = () => {
  showUploadDialog.value = false
  uploadForm.title = ''
  uploadForm.equipModel = ''
  uploadForm.symptom = ''
  uploadForm.solution = ''
  uploadForm.images = ''
  imageList.value = []
  clearUploadErrors()
  if (recordingField.value) {
    stopRecord()
  }
  transcribing.value = false
}

const cancelUpload = () => {
  closeUploadDialog()
}
</script>

<style scoped>
.case-page {
  width: 100%;
  max-width: var(--content-max-width);
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: var(--space-4);
  overflow-y: auto;
}

/* ============================================================
   页面头部
   ============================================================ */
.case-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-4);
  flex-wrap: wrap;
  position: sticky;
  top: 0;
  z-index: 10;
  background: var(--surface-page);
  padding-top: var(--space-4);
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: var(--space-3);
}

.header-left h2 {
  font-size: var(--text-xl);
  font-weight: var(--weight-bold);
  color: var(--text-primary);
  margin: 0;
}

.case-count {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}

.header-search {
  flex-shrink: 0;
}

.header-search-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
}

.header-search-row .search-input-wrap {
  height: 42px;
  padding: var(--space-1) var(--space-4);
}

.search-input-wrap {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-1) var(--space-3);
  border: 1.5px solid var(--border-base);
  border-radius: var(--radius-full);
  background: var(--surface-card);
  transition: all var(--transition-base);
  width: 200px;
}

.search-input-wrap:focus-within {
  border-color: var(--primary-400);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.06);
}

.search-icon {
  color: var(--text-tertiary);
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: var(--text-sm);
  color: var(--text-primary);
  background: transparent;
  min-width: 0;
}

.search-input::placeholder {
  color: var(--text-tertiary);
}

.clear-btn {
  border: none;
  background: transparent;
  color: var(--text-tertiary);
  cursor: pointer;
  font-size: 16px;
  padding: 0;
  line-height: 1;
}

.upload-case-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: var(--space-2) var(--space-4);
  background: linear-gradient(135deg, var(--primary-600), var(--primary-700));
  color: #fff;
  border: none;
  border-radius: var(--radius-full);
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  cursor: pointer;
  transition: all var(--transition-base);
  white-space: nowrap;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.15);
}

.upload-case-btn:hover {
  background: linear-gradient(135deg, var(--primary-700), var(--primary-800));
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.25);
  transform: translateY(-1px);
}

.upload-case-btn:active {
  transform: translateY(0);
}

/* ============================================================
   案例列表
   ============================================================ */
.case-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

/* 空状态 */
.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: var(--space-12) var(--space-4);
}

.empty-icon {
  color: var(--color-neutral-300);
  margin-bottom: var(--space-4);
}

.empty-state h3 {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--text-secondary);
  margin: 0 0 var(--space-1);
}

.empty-state p {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
  margin: 0 0 var(--space-4);
}

.empty-btn {
  padding: var(--space-2) var(--space-5);
  border: 1px solid var(--border-base);
  background: var(--surface-card);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-base);
  font-weight: var(--weight-medium);
}

.empty-btn:hover {
  border-color: var(--primary-400);
  color: var(--primary-600);
  background: var(--primary-50);
}

/* 案例卡片 */
.case-card {
  padding: var(--space-4) var(--space-5);
  background: var(--surface-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-base);
}

.case-card:hover {
  border-color: var(--primary-200);
  box-shadow: var(--shadow-md);
  transform: translateX(4px);
  background: linear-gradient(90deg, var(--primary-50), var(--surface-card));
}

.case-card:active {
  transform: translateX(2px);
}

.card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-2);
  margin-bottom: var(--space-2);
}

.card-title {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--text-primary);
  margin: 0;
  flex: 1;
  min-width: 0;
}

.card-tags {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-shrink: 0;
}

.card-tag {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  padding: 3px 10px;
  border-radius: var(--radius-full);
  white-space: nowrap;
  font-weight: var(--weight-medium);
}

.equip-tag {
  background: var(--primary-50);
  color: var(--primary-600);
}

.time-tag {
  background: var(--gray-100);
  color: var(--text-tertiary);
}

.card-desc {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  margin: 0 0 var(--space-3);
  line-height: var(--leading-relaxed);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 搜索高亮 */
.card-desc :deep(mark),
.card-title :deep(mark) {
  background: var(--accent-100);
  color: var(--accent-600);
  padding: 0 2px;
  border-radius: 2px;
  font-weight: var(--weight-medium);
}

.card-footer {
  display: flex;
  justify-content: flex-end;
}

.card-action {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-xs);
  color: var(--primary-600);
  font-weight: var(--weight-medium);
  transition: gap var(--transition-base);
}

.case-card:hover .card-action {
  gap: var(--space-2);
}

/* ============================================================
   分页
   ============================================================ */
.case-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
  padding: var(--space-4) 0 var(--space-8);
  flex-shrink: 0;
}

.page-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border-base);
  border-radius: var(--radius-md);
  background: var(--surface-card);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-base);
}

.page-btn:hover:not(:disabled) {
  border-color: var(--primary-300);
  color: var(--primary-600);
  background: var(--primary-50);
  transform: scale(1.05);
}

.page-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.page-info {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  min-width: 60px;
  text-align: center;
}

/* ============================================================
   详情弹窗
   ============================================================ */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: var(--surface-overlay);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-4);
}

.modal-content {
  width: 100%;
  max-width: 640px;
  max-height: 85vh;
  background: var(--surface-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xl);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4) var(--space-6);
  border-bottom: 1px solid var(--border-light);
}

.modal-header h3 {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--text-primary);
  margin: 0;
}

.modal-close {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  border-radius: var(--radius-sm);
  color: var(--text-tertiary);
  cursor: pointer;
  transition: all var(--transition-base);
}

.modal-close:hover {
  background: var(--color-neutral-100);
  color: var(--text-primary);
}

.modal-body {
  padding: var(--space-6);
  overflow-y: auto;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-3);
  margin-bottom: var(--space-6);
}

.detail-field {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.detail-label {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}

.detail-value {
  font-size: var(--text-sm);
  color: var(--text-primary);
  font-weight: var(--weight-medium);
}

.detail-tag {
  display: inline-block;
  font-size: var(--text-xs);
  padding: 1px 8px;
  background: var(--color-brand-50);
  color: var(--color-brand-600);
  border-radius: var(--radius-full);
}

.detail-content-section h4,
.detail-image h4 {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  color: var(--text-primary);
  margin: 0 0 var(--space-2);
}

.detail-content {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  line-height: var(--leading-relaxed);
  border-radius: var(--radius-md);
  padding: var(--space-4);
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 300px;
  overflow-y: auto;
}

.symptom-block {
  background: var(--surface-card);
  border: 1px solid var(--border-light);
}

.solution-block {
  background: var(--surface-card);
  border: 1px solid var(--border-light);
  margin-top: var(--space-3);
}

.detail-content-section {
  margin-bottom: var(--space-4);
}

.detail-image {
  margin-top: var(--space-6);
}

.detail-image img {
  max-width: 100%;
  max-height: 300px;
  border-radius: var(--radius-md);
  cursor: pointer;
  border: 1px solid var(--border-light);
}

/* 弹窗动画 */
.modal-fade-enter-active {
  transition: opacity 0.2s ease;
}
.modal-fade-enter-active .modal-content {
  transition: transform 0.25s var(--ease-out-expo), opacity 0.2s ease;
}
.modal-fade-leave-active {
  transition: opacity 0.15s ease;
}
.modal-fade-leave-active .modal-content {
  transition: transform 0.15s ease, opacity 0.15s ease;
}
.modal-fade-enter-from {
  opacity: 0;
}
.modal-fade-enter-from .modal-content {
  transform: scale(0.95) translateY(8px);
  opacity: 0;
}
.modal-fade-leave-to {
  opacity: 0;
}
.modal-fade-leave-to .modal-content {
  transform: scale(0.97);
  opacity: 0;
}

/* ============================================================
   上传表单
   ============================================================ */
.upload-modal {
  max-width: 560px;
}

.upload-form-item {
  margin-bottom: var(--space-4);
}

.upload-form-label {
  display: block;
  font-size: var(--text-xs);
  color: var(--text-secondary);
  font-weight: var(--weight-medium);
  margin-bottom: 6px;
}

.upload-form-label .required {
  color: var(--color-danger);
}

.upload-form-input {
  width: 100%;
  padding: 10px var(--space-3);
  border: 1.5px solid var(--border-base);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  color: var(--text-primary);
  background: var(--surface-card);
  outline: none;
  transition: border-color var(--transition-fast);
  box-sizing: border-box;
  font-family: inherit;
}

.upload-form-input:focus {
  border-color: var(--primary-400);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.06);
}

.upload-form-textarea {
  width: 100%;
  padding: 10px var(--space-3);
  border: 1.5px solid var(--border-base);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  color: var(--text-primary);
  background: var(--surface-card);
  outline: none;
  resize: vertical;
  font-family: inherit;
  line-height: var(--leading-relaxed);
  transition: border-color var(--transition-fast);
}

.upload-form-textarea:focus {
  border-color: var(--primary-400);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.06);
}

.upload-form-error {
  font-size: 12px;
  color: var(--color-danger);
  margin: 4px 0 0;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
  padding: var(--space-4) var(--space-6);
  border-top: 1px solid var(--border-light);
}

.upload-cancel-btn {
  padding: var(--space-2) var(--space-5);
  border: 1px solid var(--border-base);
  background: var(--surface-card);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-base);
  font-weight: var(--weight-medium);
}

.upload-cancel-btn:hover {
  border-color: var(--primary-300);
  color: var(--primary-600);
}

.upload-submit-btn {
  padding: var(--space-2) var(--space-5);
  border: none;
  background: linear-gradient(135deg, var(--primary-600), var(--primary-700));
  color: #fff;
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  cursor: pointer;
  transition: all var(--transition-base);
}

.upload-submit-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, var(--primary-700), var(--primary-800));
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.2);
}

.upload-submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ============================================================
   语音输入 / 图片上传
   ============================================================ */
.voice-input-actions {
  display: flex;
  gap: var(--space-2);
  margin-top: 6px;
}

.voice-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border: 1px solid var(--border-base);
  background: var(--surface-card);
  color: var(--primary-600);
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: var(--weight-medium);
  cursor: pointer;
  transition: all var(--transition-base);
}

.voice-btn:hover:not(:disabled) {
  border-color: var(--primary-300);
  background: var(--primary-50);
}

.voice-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.upload-form-tip {
  font-size: 12px;
  color: var(--text-tertiary);
  margin: 4px 0 0;
}

.image-upload-area {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.image-thumb {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid var(--border-light);
  background: var(--color-neutral-100);
}

.image-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: pointer;
  display: block;
}

.image-thumb-remove {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
  padding: 0;
}

.image-thumb-remove:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.image-thumb-loading {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.7);
  font-size: 11px;
  color: var(--text-secondary);
}

.image-add-btn {
  width: 80px;
  height: 80px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  border: 1.5px dashed var(--border-base);
  background: var(--surface-card);
  color: var(--text-tertiary);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-base);
  font-size: 11px;
}

.image-add-btn:hover:not(:disabled) {
  border-color: var(--primary-400);
  color: var(--primary-600);
  background: var(--primary-50);
}

.image-add-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.status-bar {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: 8px 12px;
  margin-top: var(--space-3);
  border-radius: var(--radius-md);
  font-size: 13px;
}

.status-bar.recording {
  background: rgba(245, 108, 108, 0.08);
  border: 1px solid rgba(245, 108, 108, 0.3);
  color: #f56c6c;
}

.status-bar.transcribing {
  background: var(--primary-50);
  border: 1px solid var(--primary-200);
  color: var(--primary-600);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
  animation: status-pulse 1s infinite;
}

.status-time {
  margin-left: auto;
  font-family: var(--font-mono, monospace);
  font-weight: var(--weight-semibold);
}

@keyframes status-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.35; }
}

.transcribe-modal {
  max-width: 520px;
}

.transcribe-field-label {
  font-size: var(--text-sm);
  color: var(--primary-600);
  font-weight: var(--weight-medium);
  margin-bottom: var(--space-3);
}

.transcribe-text {
  padding: var(--space-4);
  background: var(--color-neutral-100);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  line-height: var(--leading-relaxed);
  color: var(--text-primary);
  white-space: pre-wrap;
  max-height: 300px;
  overflow-y: auto;
}

.transcribe-warn {
  font-size: 12px;
  color: #e6a23c;
  margin: var(--space-2) 0 0;
}

/* ============================================================
   响应式
   ============================================================ */
@media (max-width: 480px) {
  .case-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .search-input-wrap {
    width: 100%;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }

  .card-top {
    flex-direction: column;
  }
}
</style>
