<template>
  <div class="chat-container">
    <!-- 左侧：对话历史列表 -->
    <div class="chat-sidebar">
      <div class="sidebar-header">
        <h3>多模态检索</h3>
        <el-button type="primary" size="small" @click="handleNewConversation" style="width: 100%">
          <el-icon><Plus /></el-icon> 新建对话
        </el-button>
      </div>
      <div class="sidebar-list">
        <div v-if="conversationList.length === 0" class="sidebar-empty">
          <el-icon :size="40" color="#c0c4cc"><ChatDotRound /></el-icon>
          <p>暂无对话记录</p>
        </div>
        <div
          v-for="item in conversationList"
          :key="item.conversationId"
          class="conversation-item"
          :class="{ active: currentConversationId === item.conversationId }"
          @click="handleSelectConversation(item)"
        >
          <div class="conversation-title">{{ item.userQuestion || '新对话' }}</div>
          <div class="conversation-meta">
            <span v-if="item.deviceModel" class="meta-tag">{{ item.deviceModel }}</span>
            <span class="meta-time">{{ formatTime(item.createTime) }}</span>
          </div>
          <div class="conversation-actions">
            <el-button type="danger" link size="small" @click.stop="handleDeleteConversation(item)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧：对话区域 -->
    <div class="chat-main">
      <!-- 上部：对话内容展示区 -->
      <div class="chat-messages" ref="messagesContainer" @scroll="handleScroll">
        <div v-if="messageList.length === 0" class="messages-empty">
          <el-icon :size="60" color="#dcdfe6"><Monitor /></el-icon>
          <h3>机械诊断检测助手</h3>
          <p>请输入故障描述，AI将基于知识库为您诊断分析</p>
        </div>
        <div v-for="(msg, index) in messageList" :key="index" class="message-group">
          <!-- 用户消息 -->
          <div class="message user-message">
            <div class="message-avatar user-avatar">
              <el-icon><User /></el-icon>
            </div>
            <div class="message-content">
              <div class="message-text">{{ msg.userQuestion }}</div>
              <div v-if="msg.deviceModel" class="message-device">
                <el-tag size="small" type="info">{{ msg.deviceModel }}</el-tag>
              </div>
              <!-- 附件文件回显 -->
              <div v-if="msg.files && msg.files.length > 0" class="message-files">
                <template v-for="file in msg.files" :key="file.id || file.filePath">
                  <!-- 图片回显 -->
                  <el-image
                    v-if="file.fileType === 'image'"
                    :src="file.filePath"
                    :preview-src-list="getImageFiles(msg.files)"
                    :initial-index="getImageIndex(msg.files, file)"
                    fit="cover"
                    class="file-preview file-image"
                  />
                  <!-- 视频回显 -->
                  <video
                    v-else-if="file.fileType === 'video'"
                    :src="file.filePath"
                    controls
                    class="file-preview file-video"
                  />
                  <!-- 音频回显 -->
                  <div v-else-if="file.fileType === 'audio'" class="file-preview file-audio">
                    <el-icon :size="20" color="#409eff"><Headset /></el-icon>
                    <audio :src="file.filePath" controls class="audio-player" />
                  </div>
                  <!-- 文件回显 -->
                  <div v-else-if="file.fileType === 'file'" class="file-preview file-document">
                    <el-icon :size="20" color="#409eff"><Document /></el-icon>
                    <a :href="file.filePath" target="_blank" class="file-download-link">{{ file.fileName }}</a>
                  </div>
                </template>
              </div>
            </div>
          </div>
          <!-- AI消息 -->
          <div class="message ai-message">
            <div class="message-avatar ai-avatar">
              <el-icon><Cpu /></el-icon>
            </div>
            <div class="message-content">
              <div v-if="msg.answerContent || msg.streamingAnswer" class="message-text markdown-body"
                   v-html="renderMarkdown(msg.answerContent || msg.streamingAnswer || '')"></div>
              <div v-if="msg.loading" class="typing-indicator">
                <span></span><span></span><span></span>
              </div>
              <!-- 引用文档 -->
              <div v-if="msg.resources && msg.resources.length > 0" class="message-resources">
                <div class="resources-title">参考来源：</div>
                <el-table :data="msg.resources" border size="small">
                  <el-table-column prop="documentName" label="文档名称" min-width="120" />
                  <el-table-column prop="datasetName" label="知识库" width="100" />
                  <el-table-column prop="score" label="匹配度" width="90" align="center">
                    <template #default="{ row }">
                      <el-tag type="success" effect="plain" size="small">{{ (row.score * 100).toFixed(1) }}%</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="命中内容" min-width="200">
                    <template #default="{ row }">
                      <el-popover
                        placement="top-start"
                        :width="480"
                        trigger="hover"
                      >
                        <template #reference>
                          <div class="content-ellipsis">{{ row.content || '-' }}</div>
                        </template>
                        <div class="content-popover markdown-body" v-html="renderMarkdown(row.content || '')"></div>
                      </el-popover>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
              <!-- 反馈按钮（仅引用了文档时显示） -->
              <div v-if="msg.resources && msg.resources.length > 0 && !msg.loading && msg.answerContent"
                   class="message-feedback">
                <template v-if="msg.feedbackStatus === 'correct' || msg.feedbackStatus === 'incorrect' || msg.feedbackStatus === 'processed'">
                  <el-tag :type="msg.feedbackStatus === 'correct' ? 'success' : msg.feedbackStatus === 'processed' ? 'primary' : 'danger'" size="small" effect="plain">
                    {{ msg.feedbackStatus === 'correct' ? '已标注正确' : msg.feedbackStatus === 'processed' ? '已处理' : '已标注不正确' }}
                  </el-tag>
                </template>
                <template v-else>
                  <span class="feedback-label">回答是否正确？</span>
                  <el-button size="small" type="success" plain @click="handleFeedback(msg, 'correct')">
                    <el-icon><Check /></el-icon> 正确
                  </el-button>
                  <el-button size="small" type="danger" plain @click="handleFeedback(msg, 'incorrect')">
                    <el-icon><Close /></el-icon> 不正确
                  </el-button>
                </template>
              </div>
              <!-- 未引用文档提示 -->
              <div v-if="msg.answerContent && (!msg.resources || msg.resources.length === 0) && !msg.loading"
                   class="message-no-ref">
                <el-tag size="small" type="info" effect="plain">未引用知识库文档</el-tag>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 下部：提问输入区 -->
      <div class="chat-input-area">
        <div class="input-meta-row">
          <div class="input-device">
            <span class="input-label">设备型号：</span>
            <el-input
              v-model="inputForm.equipModel"
              placeholder="选填"
              size="small"
              clearable
              style="width: 200px"
            />
          </div>

          <!-- 音频转文字 -->
          <div class="voice-input-row">
            <el-tooltip content="上传音频文件" placement="top">
              <el-button
                circle
                size="small"
                @click="triggerAudioFileInput"
                :disabled="transcribing"
              >
                <el-icon><FolderOpened /></el-icon>
              </el-button>
            </el-tooltip>
            <input
              ref="audioFileInputRef"
              type="file"
              accept="audio/*"
              style="display: none"
              @change="handleAudioFileSelected"
            />
            <span v-if="transcribing" class="transcribing-text">识别中...</span>
          </div>

        </div>
        <div class="input-files-row">
          <el-upload
            action="#"
            :http-request="handleFileUpload"
            :limit="9"
            :accept="acceptTypes"
            v-model:file-list="fileList"
            list-type="picture-card"
            :before-upload="beforeFileUpload"
            :on-exceed="() => ElMessage.warning('最多上传9个文件')"
          >
            <el-icon><Plus /></el-icon>
            <template #file="{ file }">
              <div class="upload-file-card">
                <img v-if="getFileType(file) === 'image'" :src="file.url" class="upload-thumbnail" />
                <div v-else-if="getFileType(file) === 'video'" class="upload-media-thumb">
                  <el-icon :size="24" style="color: #fff"><VideoPlay /></el-icon>
                  <span class="media-label">视频</span>
                </div>
                <div v-else-if="getFileType(file) === 'audio'" class="upload-media-thumb audio-thumb">
                  <el-icon :size="24" style="color: #fff"><Headset /></el-icon>
                  <span class="media-label">音频</span>
                </div>
                <div v-else-if="getFileType(file) === 'file'" class="upload-media-thumb file-thumb">
                  <el-icon :size="24" style="color: #fff"><Document /></el-icon>
                  <span class="media-label file-name-label">{{ file.name }}</span>
                </div>
                <div v-else class="upload-media-thumb file-thumb">
                  <el-icon :size="24" style="color: #fff"><Document /></el-icon>
                  <span class="media-label">文件</span>
                </div>
                <span class="upload-file-actions">
                  <el-icon @click.stop="handleFileRemove(file)"><Delete /></el-icon>
                </span>
              </div>
            </template>
          </el-upload>
        </div>
        <div class="input-query-row">
          <el-input
            v-model="inputForm.query"
            type="textarea"
            :rows="1"
            :autosize="{ minRows: 1, maxRows: 6 }"
            placeholder="请输入故障描述或问题..."
            :disabled="sending"
            @keydown.enter.exact.prevent="handleSend"
          />
          <div class="input-actions">
            <el-button type="primary" :loading="sending" @click="handleSend" :disabled="!inputForm.query.trim()">
              <el-icon><Promotion /></el-icon> 发送
            </el-button>
            <el-button @click="handleStop" :disabled="!sending">
              <el-icon><VideoPause /></el-icon> 停止
            </el-button>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick, onMounted, onUnmounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { renderMarkdown } from '@/utils/markdown'
import {
  getChatConversations,
  getChatConversationDetail,
  deleteChatConversation,
  submitChatFeedback,
  uploadChatFile,
  getChatUploadConfig,
  audioToText
} from '@/api'

// ==================== 类型定义 ====================

interface ChatFileItem {
  id?: number
  fileName: string
  filePath: string
  fileSize?: number
  fileType: string  // image/video/audio/file
  createTime?: string
}

interface ChatMessage {
  userQuestion: string
  deviceModel?: string
  answerContent: string
  streamingAnswer: string
  messageId?: string
  conversationId?: string
  chatHistoryId?: number
  resources: any[]
  loading: boolean
  feedbackStatus: string
  files: ChatFileItem[]
}

interface ConversationItem {
  conversationId: string
  userQuestion: string
  deviceModel: string
  createTime: string
  status: string
  roundCount: number
}

// ==================== 响应式数据 ====================

const conversationList = ref<ConversationItem[]>([])
const currentConversationId = ref('')
const currentRound = ref(0)
const messageList = ref<ChatMessage[]>([])
const sending = ref(false)
const messagesContainer = ref<HTMLElement | null>(null)
const isUserAtBottom = ref(true)
const fileList = ref<any[]>([])
let abortController: AbortController | null = null

// ============ 语音转文字 ============
const transcribing = ref(false)

const triggerAudioFileInput = () => { audioFileInputRef.value?.click() }

const handleAudioFileSelected = (e: Event) => {
  const input = e.target as HTMLInputElement
  if (input.files?.[0]) { transcribeAudio(input.files[0]); input.value = '' }
}

const transcribeAudio = async (file: File) => {
  transcribing.value = true
  let loadingMsg: ReturnType<typeof ElMessage> | null = null
  try {
    loadingMsg = ElMessage({ message: '正在识别语音内容，请稍候...', type: 'info', duration: 0, showClose: false })
    const res: any = await audioToText(file)
    const text = res.data?.text || ''
    if (!text) { ElMessage.warning('未识别到语音内容'); return }
    inputForm.query = (inputForm.query ? inputForm.query + '\n' : '') + text
    ElMessage.success('语音识别完成，已填入输入框')
  } catch (error: any) {
    console.error('语音转文字失败', error)
    ElMessage.error('语音转文字失败：' + (error?.message || '请重试'))
  } finally {
    loadingMsg?.close()
    transcribing.value = false
  }
}

const audioFileInputRef = ref<HTMLInputElement | null>(null)

// 上传配置
const uploadConfig = reactive({
  enableImageUpload: true,
  enableVideoUpload: true,
  enableAudioUpload: true,
  enableFileUpload: true,
  allowedImageTypes: 'jpg,jpeg,png,gif,webp,svg',
  allowedVideoTypes: 'mp4,mov,mpeg,webm',
  allowedAudioTypes: 'mp3,m4a,wav,webm,mpga',
  allowedFileTypes: 'txt,md,pdf,xls,xlsx,doc,docx',
  maxFileSizeMb: 50
})

/** 动态构建 accept 属性 */
const acceptTypes = computed(() => {
  const parts: string[] = []
  if (uploadConfig.enableImageUpload) {
    parts.push('image/jpeg,image/png,image/jpg,image/gif,image/webp,image/svg+xml')
  }
  if (uploadConfig.enableVideoUpload) {
    parts.push('video/mp4,video/quicktime,video/mpeg,video/webm')
  }
  if (uploadConfig.enableAudioUpload) {
    parts.push('audio/mpeg,audio/mp4,audio/wav,audio/webm,audio/x-mpeg')
  }
  if (uploadConfig.enableFileUpload) {
    // 文件类型通过扩展名指定
    const fileExts = uploadConfig.allowedFileTypes.split(',').map(e => `.${e.trim()}`).join(',')
    parts.push(fileExts)
  }
  return parts.join(',')
})

const inputForm = reactive({
  query: '',
  equipModel: ''
})

// ==================== 方法 ====================

/** 格式化时间 */
const formatTime = (time: string) => {
  if (!time) return ''
  return time.substring(0, 16).replace('T', ' ')
}

/** 获取上传文件类型 */
const getFileType = (file: any): string => {
  if (file.response?.data?.fileType) return file.response.data.fileType
  if (file.raw?.type?.startsWith('video/')) return 'video'
  if (file.raw?.type?.startsWith('audio/')) return 'audio'
  if (file.raw?.type?.startsWith('image/')) return 'image'
  // 通过扩展名判断文件类型
  const name = file.name || file.raw?.name || ''
  const ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase()
  const fileExts = uploadConfig.allowedFileTypes.split(',').map(e => e.trim().toLowerCase())
  if (fileExts.includes(ext)) return 'file'
  return 'image'
}

/** 获取图片文件列表（用于预览） */
const getImageFiles = (files: ChatFileItem[]): string[] => {
  return files.filter(f => f.fileType === 'image').map(f => f.filePath)
}

/** 获取图片在图片列表中的索引 */
const getImageIndex = (files: ChatFileItem[], file: ChatFileItem): number => {
  const imageFiles = files.filter(f => f.fileType === 'image')
  return imageFiles.findIndex(f => f.filePath === file.filePath)
}

/** 文件上传前校验 */
const beforeFileUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  const isVideo = file.type.startsWith('video/')
  const isAudio = file.type.startsWith('audio/')

  // 通过扩展名判断是否为文档文件
  const ext = file.name.substring(file.name.lastIndexOf('.') + 1).toLowerCase()
  const fileExts = uploadConfig.allowedFileTypes.split(',').map(e => e.trim().toLowerCase())
  const isFile = !isImage && !isVideo && !isAudio && fileExts.includes(ext)

  if (!isImage && !isVideo && !isAudio && !isFile) {
    ElMessage.error('只能上传图片、视频、音频或文档文件')
    return false
  }

  // 检查对应类型的上传开关
  if (isImage && !uploadConfig.enableImageUpload) {
    ElMessage.error('图片上传功能已关闭')
    return false
  }
  if (isVideo && !uploadConfig.enableVideoUpload) {
    ElMessage.error('视频上传功能已关闭')
    return false
  }
  if (isAudio && !uploadConfig.enableAudioUpload) {
    ElMessage.error('音频上传功能已关闭')
    return false
  }
  if (isFile && !uploadConfig.enableFileUpload) {
    ElMessage.error('文件上传功能已关闭')
    return false
  }

  // 校验文件大小
  const maxSize = uploadConfig.maxFileSizeMb * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error(`文件大小不能超过${uploadConfig.maxFileSizeMb}MB`)
    return false
  }
  return true
}

/** 文件上传处理 */
const handleFileUpload = async (options: any) => {
  try {
    const res: any = await uploadChatFile(options.file)
    const data = res?.data || {}
    if (data.urls && Array.isArray(data.urls)) {
      // 视频关键帧提取结果：多个图片URL
      const uploadedResults = data.urls.map((url: string, index: number) => ({
        data: {
          url,
          fileType: 'image',
          name: data.names?.[index] || `frame_${index + 1}.jpg`
        }
      }))
      // 第一个帧作为主文件
      options.onSuccess(uploadedResults[0])
      // 后续帧手动添加到 fileList
      for (let i = 1; i < uploadedResults.length; i++) {
        fileList.value.push({
          name: uploadedResults[i].data.name,
          url: uploadedResults[i].data.url,
          status: 'success',
          uid: Date.now() + i,
          response: uploadedResults[i]
        })
      }
    } else {
      // 普通文件
      options.onSuccess(res)
    }
  } catch (error) {
    ElMessage.error('文件上传失败')
    throw error
  }
}

/** 文件移除处理：从 fileList 中移除文件 */
const handleFileRemove = (file: any) => {
  fileList.value = fileList.value.filter(f => f.uid !== file.uid)
}

/** 新建对话 */
const handleNewConversation = () => {
  currentConversationId.value = ''
  currentRound.value = 0
  messageList.value = []
  inputForm.query = ''
  inputForm.equipModel = ''
  fileList.value = []
}

/** 选择对话 */
const handleSelectConversation = async (item: ConversationItem) => {
  currentConversationId.value = item.conversationId
  currentRound.value = item.roundCount

  try {
    const res: any = await getChatConversationDetail(item.conversationId)
    const messages: any[] = res.data || []
    messageList.value = messages.map((msg: any) => ({
      userQuestion: msg.userQuestion || '',
      deviceModel: msg.deviceModel || '',
      answerContent: msg.answerContent || '',
      streamingAnswer: '',
      messageId: msg.messageId || '',
      conversationId: msg.conversationId || '',
      chatHistoryId: msg.id || undefined,
      resources: msg.resources || [],
      loading: false,
      feedbackStatus: msg.feedbackStatus || 'pending',
      files: (msg.files || []).map((f: any) => ({
        id: f.id,
        fileName: f.fileName || '',
        filePath: f.filePath || '',
        fileSize: f.fileSize,
        fileType: f.fileType || 'image',
        createTime: f.createTime
      }))
    }))
    scrollToBottom()
  } catch (error) {
    console.error('加载对话详情失败', error)
    ElMessage.error('加载对话详情失败')
  }
}

/** 删除对话 */
const handleDeleteConversation = async (item: ConversationItem) => {
  try {
    await ElMessageBox.confirm('确定删除该对话吗？删除后将同步删除远端记录。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteChatConversation(item.conversationId)
    ElMessage.success('删除成功')
    // 如果删除的是当前对话，清空右侧
    if (currentConversationId.value === item.conversationId) {
      handleNewConversation()
    }
    fetchConversationList()
  } catch {
    // 取消删除
  }
}

/** 发送消息 */
const handleSend = async () => {
  if (!inputForm.query.trim()) {
    ElMessage.warning('请输入问题内容')
    return
  }

  const query = inputForm.query.trim()
  const equipModel = inputForm.equipModel.trim()

  // 从 fileList 派生文件信息（仅包含上传成功的文件）
  const successFiles = fileList.value.filter(f => f.status === 'success')
  const fileUrls = successFiles.map(f => f.response?.data?.url).filter(Boolean)
  const fileTypes = successFiles.map(f => f.response?.data?.fileType || getFileType(f)).filter(Boolean)
  const fileNames = successFiles.map(f => f.response?.data?.name || f.name).filter(Boolean)

  // 构建当前消息的文件列表（用于回显）
  const currentFiles: ChatFileItem[] = successFiles.map(f => {
    const url = f.response?.data?.url || f.url
    return {
      filePath: url,
      fileType: f.response?.data?.fileType || getFileType(f),
      fileName: f.name || url.substring(url.lastIndexOf('/') + 1)
    }
  })

  // 添加用户消息到列表
  const userMsg: ChatMessage = {
    userQuestion: query,
    deviceModel: equipModel,
    answerContent: '',
    streamingAnswer: '',
    resources: [],
    loading: true,
    feedbackStatus: '',
    files: currentFiles
  }
  messageList.value.push(userMsg)
  // 获取响应式代理引用，后续修改通过代理触发 Vue 响应式更新
  const reactiveMsg = messageList.value[messageList.value.length - 1]

  // 清空输入和已上传文件
  inputForm.query = ''
  fileList.value = []

  sending.value = true
  isUserAtBottom.value = true
  scrollToBottom()

  // 构建SSE请求
  const token = localStorage.getItem('token')
  abortController = new AbortController()

  try {
    const response = await fetch('/api/chat/send', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        query,
        equipModel: equipModel || undefined,
        conversationId: currentConversationId.value || undefined,
        fileUrls: fileUrls.length > 0 ? fileUrls : undefined,
        fileTypes: fileTypes.length > 0 ? fileTypes : undefined,
        fileNames: fileNames.length > 0 ? fileNames : undefined
      }),
      signal: abortController.signal
    })

    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(`请求失败 (${response.status}): ${errorText}`)
    }

    const reader = response.body?.getReader()
    if (!reader) return

    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const eventBlocks = buffer.split('\n\n')
      buffer = eventBlocks.pop() || ''

      for (const block of eventBlocks) {
        if (!block.trim()) continue

        const fields = block.split('\n')
        let eventType = ''
        let eventData = ''

        for (const field of fields) {
          if (field.startsWith('event:')) {
            eventType = field.substring(6).trim()
          } else if (field.startsWith('data:')) {
            if (eventData) eventData += '\n'
            eventData += field.substring(5)
          }
        }

        if (eventType === 'message') {
          // 流式内容片段
          reactiveMsg.streamingAnswer += eventData
          scrollToBottom()
        } else if (eventType === 'done') {
          // 流式完成
          try {
            const doneData = JSON.parse(eventData)
            reactiveMsg.answerContent = doneData.answer || reactiveMsg.streamingAnswer
            reactiveMsg.messageId = doneData.messageId || ''
            reactiveMsg.conversationId = doneData.conversationId || ''
            reactiveMsg.chatHistoryId = doneData.chatHistoryId || undefined
            reactiveMsg.resources = doneData.resources || []
            reactiveMsg.feedbackStatus = 'pending'
            reactiveMsg.loading = false

            // 更新当前对话ID
            if (doneData.conversationId && !currentConversationId.value) {
              currentConversationId.value = doneData.conversationId
            }
            // 更新轮次
            currentRound.value += 1
          } catch {
            reactiveMsg.answerContent = reactiveMsg.streamingAnswer
            reactiveMsg.loading = false
          }
          scrollToBottom()
        } else if (eventType === 'error') {
          reactiveMsg.loading = false
          reactiveMsg.answerContent = '抱歉，发生了错误：' + eventData
          ElMessage.error(eventData || '对话请求失败')
        }
      }
    }

    // 如果流式结束但未收到done事件
    if (reactiveMsg.loading) {
      reactiveMsg.answerContent = reactiveMsg.streamingAnswer
      reactiveMsg.loading = false
    }

    fetchConversationList()
  } catch (error: any) {
    if (error.message !== 'The user aborted a request.') {
      console.error('对话请求失败', error)
      ElMessage.error('对话请求失败，请稍后重试')
      reactiveMsg.loading = false
      reactiveMsg.answerContent = '请求失败，请重试'
    }
  } finally {
    sending.value = false
  }
}

/** 停止生成 */
const handleStop = () => {
  if (abortController) {
    abortController.abort()
    abortController = null
  }
  sending.value = false
  // 将正在流式输出的消息标记为完成
  const lastMsg = messageList.value[messageList.value.length - 1]
  if (lastMsg && lastMsg.loading) {
    lastMsg.answerContent = lastMsg.streamingAnswer
    lastMsg.loading = false
  }
}

/** 提交反馈 */
const handleFeedback = async (msg: ChatMessage, status: string) => {
  try {
    await submitChatFeedback({
      chatHistoryId: msg.chatHistoryId!,
      feedbackStatus: status
    })

    msg.feedbackStatus = status
    ElMessage.success('反馈提交成功')
  } catch (error) {
    console.error('提交反馈失败', error)
    ElMessage.error('提交反馈失败')
  }
}

/** 获取对话历史列表 */
const fetchConversationList = async () => {
  try {
    const res: any = await getChatConversations({ pageNum: 1, pageSize: 50 })
    conversationList.value = res.data?.records || []
  } catch (error) {
    console.error('获取对话列表失败', error)
  }
}

// 2 分钟定时轮询刷新历史会话列表
let conversationPollTimer: ReturnType<typeof setInterval> | null = null
const startConversationPolling = () => {
  stopConversationPolling()
  conversationPollTimer = setInterval(() => {
    fetchConversationList()
  }, 120000) // 2 分钟
}
const stopConversationPolling = () => {
  if (conversationPollTimer) {
    clearInterval(conversationPollTimer)
    conversationPollTimer = null
  }
}

/** 监听滚动，判断用户是否在底部 */
const handleScroll = () => {
  if (!messagesContainer.value) return
  const { scrollTop, scrollHeight, clientHeight } = messagesContainer.value
  isUserAtBottom.value = scrollHeight - scrollTop - clientHeight <= 100
}

/** 滚动到底部 */
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value && isUserAtBottom.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

/** 获取上传配置 */
const fetchUploadConfig = async () => {
  try {
    const res: any = await getChatUploadConfig()
    if (res.data) {
      Object.assign(uploadConfig, res.data)
    }
  } catch (error) {
    console.error('获取上传配置失败', error)
  }
}

onMounted(() => {
  fetchConversationList()
  startConversationPolling()
  fetchUploadConfig()
})

onUnmounted(() => {
  stopConversationPolling()
})
</script>

<style scoped>
.chat-container {
  display: flex;
  height: calc(100vh - 120px);
  background: #f5f7fa;
  border-radius: 8px;
  overflow: hidden;
}

/* ========== 左侧对话历史列表 ========== */
.chat-sidebar {
  width: 280px;
  min-width: 280px;
  background: #fff;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.sidebar-header h3 {
  margin: 0 0 12px;
  font-size: 16px;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}

.sidebar-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.sidebar-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #c0c4cc;
}

.sidebar-empty p {
  margin-top: 12px;
  font-size: 14px;
}

.conversation-item {
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 4px;
  position: relative;
}

.conversation-item:hover {
  background: #f0f2f5;
}

.conversation-item.active {
  background: #ecf5ff;
  border: 1px solid #b3d8ff;
}

.conversation-title {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 6px;
}

.conversation-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #909399;
}

.meta-tag {
  background: #f0f2f5;
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 11px;
}

.conversation-actions {
  position: absolute;
  top: 8px;
  right: 8px;
  opacity: 0;
  transition: opacity 0.2s;
}

.conversation-item:hover .conversation-actions {
  opacity: 1;
}

/* ========== 右侧对话区域 ========== */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
}

/* 对话内容展示区 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}

.messages-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #c0c4cc;
}

.messages-empty h3 {
  margin: 16px 0 8px;
  color: #909399;
  font-size: 18px;
}

.messages-empty p {
  color: #c0c4cc;
  font-size: 14px;
}

.message-group {
  margin-bottom: 24px;
}

.message {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 18px;
}

.user-avatar {
  background: #409eff;
  color: #fff;
}

.ai-avatar {
  background: #67c23a;
  color: #fff;
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-text {
  font-size: 14px;
  line-height: 1.8;
  color: #303133;
  word-break: break-word;
}

.message-device {
  margin-top: 4px;
}

/* 用户消息右对齐 */
.user-message {
  flex-direction: row-reverse;
}

.user-message .message-text {
  background: #409eff;
  color: #fff;
  padding: 12px 16px;
  border-radius: 12px 0 12px 12px;
}

.user-message .message-content {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.ai-message .message-text {
  background: #f5f7fa;
  padding: 12px 16px;
  border-radius: 0 12px 12px 12px;
}

/* ========== 附件文件回显 ========== */
.message-files {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.file-preview {
  border-radius: 8px;
  overflow: hidden;
}

.file-image {
  width: 120px;
  height: 120px;
  cursor: pointer;
  border: 1px solid #e4e7ed;
}

.file-video {
  max-width: 360px;
  max-height: 240px;
  border: 1px solid #e4e7ed;
}

.file-audio {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f0f7ff;
  border: 1px solid #d9ecff;
  border-radius: 8px;
  min-width: 200px;
}

.audio-player {
  height: 32px;
  max-width: 240px;
}

.file-document {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  min-width: 200px;
}

.file-download-link {
  color: #409eff;
  text-decoration: none;
  font-size: 13px;
  word-break: break-all;
}

.file-download-link:hover {
  text-decoration: underline;
}

.message-resources {
  margin-top: 12px;
  padding: 12px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.resources-title {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
  font-weight: 500;
}

.content-ellipsis {
  max-height: 60px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  word-break: break-all;
  line-height: 1.4;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
}

.content-popover {
  max-height: 400px;
  overflow-y: auto;
  font-size: 13px;
  line-height: 1.6;
  padding: 4px;
}

.content-popover :deep(pre) {
  background: #f5f7fa;
  padding: 8px 12px;
  border-radius: 4px;
  overflow-x: auto;
  font-size: 12px;
}

.content-popover :deep(code) {
  background: #f5f7fa;
  padding: 2px 4px;
  border-radius: 3px;
  font-size: 12px;
}

.message-feedback {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.feedback-label {
  font-size: 13px;
  color: #909399;
}

.message-no-ref {
  margin-top: 8px;
}

/* 打字动画 */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 8px 0;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: #67c23a;
  border-radius: 50%;
  animation: typing 1.4s infinite both;
}

.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.6; }
  40% { transform: scale(1); opacity: 1; }
}

/* ========== 提问输入区 ========== */
.chat-input-area {
  border-top: 1px solid #e4e7ed;
  padding: 8px 24px;
  background: #fff;
}

.input-meta-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 6px;
}

.input-device {
  display: flex;
  align-items: center;
  gap: 4px;
}

.input-label {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
}

.input-files-row {
  margin-bottom: -24px;
}

.input-files-row :deep(.el-upload--picture-card) {
  width: 50px;
  height: 50px;
}

.input-files-row :deep(.el-upload-list--picture-card .el-upload-list__item) {
  width: 50px;
  height: 50px;
}

.input-query-row {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.input-query-row :deep(.el-textarea__inner) {
  resize: none;
}

.input-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.input-actions :deep(.el-button) {
  width: 100%;
  margin-left: 0;
}

.voice-input-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.transcribing-text {
  color: #8b5cf6;
  font-size: 13px;
}

.rounds-warning {
  margin-top: 4px;
}

/* ========== 上传文件卡片 ========== */
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

.upload-media-thumb {
  width: 100%;
  height: 100%;
  background: #1e1e1e;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.upload-media-thumb.audio-thumb {
  background: #2b4c7e;
}

.upload-media-thumb.file-thumb {
  background: #5b6b7a;
}

.file-name-label {
  max-width: 44px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.media-label {
  color: #fff;
  font-size: 11px;
  margin-top: 2px;
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
}

/* ========== Markdown 渲染样式 ========== */
.markdown-body {
  font-size: 14px;
  line-height: 1.8;
  word-wrap: break-word;
  overflow-wrap: break-word;
}

.markdown-body :deep(h1), .markdown-body :deep(h2), .markdown-body :deep(h3), .markdown-body :deep(h4) {
  margin: 12px 0 8px;
  color: #303133;
}

.markdown-body :deep(p) {
  margin: 6px 0;
  line-height: 1.8;
}

.markdown-body :deep(ul), .markdown-body :deep(ol) {
  padding-left: 20px;
  margin: 6px 0;
}

.markdown-body :deep(li) {
  margin: 3px 0;
  line-height: 1.8;
}

.markdown-body :deep(strong) {
  font-weight: 700;
  color: #303133;
}

.markdown-body :deep(code) {
  background: #f0f2f5;
  color: #c7254e;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
}

.markdown-body :deep(pre) {
  background: #1e1e1e;
  padding: 14px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 10px 0;
}

.markdown-body :deep(pre code) {
  background: none;
  color: #d4d4d4;
  padding: 0;
  font-size: 13px;
  line-height: 1.6;
  border-radius: 0;
}

.markdown-body :deep(blockquote) {
  border-left: 4px solid #409eff;
  padding: 6px 14px;
  margin: 10px 0;
  background: rgba(64, 158, 255, 0.04);
  border-radius: 0 6px 6px 0;
  color: #606266;
}

.markdown-body :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 10px 0;
  font-size: 13px;
}

.markdown-body :deep(th), .markdown-body :deep(td) {
  border: 1px solid #dcdfe6;
  padding: 8px 12px;
  text-align: left;
}

.markdown-body :deep(th) {
  background: #f5f7fa;
  font-weight: 600;
}

.markdown-body :deep(img) {
  max-width: 100%;
  border-radius: 6px;
  margin: 6px 0;
}

</style>
