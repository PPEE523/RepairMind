<template>
  <div class="page-container">


<el-row :gutter="20" class="sop-container">
  <!-- 左侧：流程列表 -->
  <el-col :span="9">
    <el-card class="sop-list-card">
      <template #header>
        <div class="card-title">
          <el-icon><Document /></el-icon>
          <span>SOP流程列表</span>
          <el-button type="primary" size="small" class="btn-generate" @click="openGenerateDialog">
            <el-icon><Plus /></el-icon>生成SOP流程
          </el-button>
        </div>
      </template>
      <div v-if="sopFlowList.length" class="sop-list">
        <div
          v-for="item in sopFlowList"
          :key="item.id"
          class="sop-item"
          :class="{ active: selectedSopFlow?.id === item.id }"
          @click="selectSopFlow(item)"
        >
          <div class="sop-item-header">
            <span class="sop-name">{{ item.name }}</span>
            <el-tag size="small">{{ item.totalSteps }} 步</el-tag>
          </div>
          <div class="sop-item-time">{{ formatTime(item.createTime) }}</div>
          <div class="sop-item-actions">
            <el-button link type="primary" size="small" @click.stop="startTest(item)">
              <el-icon><VideoPlay /></el-icon>测试
            </el-button>
            <el-button link type="warning" size="small" @click.stop="editSopFlow(item)">
              <el-icon><Edit /></el-icon>修改
            </el-button>
            <el-button link type="danger" size="small" @click.stop="deleteSopFlowItem(item)">
              <el-icon><Delete /></el-icon>删除
            </el-button>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无SOP流程，请点击上方按钮生成" :image-size="80" />
    </el-card>
  </el-col>

  <!-- 右侧：流程测试窗口 -->
  <el-col :span="15">
    <el-card class="sop-test-card">
      <template #header>
        <div class="card-title">
          <el-icon><Cpu /></el-icon>
          <span>流程测试</span>
        </div>
      </template>

      <div v-if="testState.flow" class="test-container">
        <!-- 上方可滚动区域 -->
        <div class="test-scroll-area" ref="scrollAreaRef">
          <!-- 顶部信息栏 -->
          <div class="test-header">
            <div class="test-flow-info">
              <h3>{{ testState.flow.name }}</h3>
              <el-tag type="info" size="large">共 {{ testState.flow.totalSteps }} 步</el-tag>
              <el-tag v-if="testState.currentStep < testState.steps.length" type="warning" style="margin-left:8px">
                当前第 {{ testState.currentStep + 1 }} 步
              </el-tag>
              <el-tag v-else type="success" style="margin-left:8px">全部完成</el-tag>
            </div>
          </div>

          <!-- 当前步骤内容展示 -->
          <div v-if="testState.currentStep < testState.steps.length" class="current-step-display">
            <div class="step-card">
              <div class="step-title-row">
                <span class="step-number">{{ testState.currentStep + 1 }}</span>
                <h4>{{ testState.steps[testState.currentStep]?.title }}</h4>
              </div>
              <div class="step-detail">
                <div class="step-field">
                  <label>操作描述：</label>
                  <p>{{ testState.steps[testState.currentStep]?.description }}</p>
                </div>
                <div class="step-field" v-if="testState.steps[testState.currentStep]?.acceptance_criteria">
                  <label>验收标准：</label>
                  <p class="criteria">{{ testState.steps[testState.currentStep]?.acceptance_criteria }}</p>
                </div>
                <div class="step-field" v-if="testState.steps[testState.currentStep]?.tools?.length">
                  <label>所需工具：</label>
                  <el-tag v-for="tool in testState.steps[testState.currentStep]?.tools" :key="tool" size="small" class="tool-tag">
                    {{ tool }}
                  </el-tag>
                </div>
                <div class="step-field safety" v-if="testState.steps[testState.currentStep]?.safety_notes">
                  <label>安全注意：</label>
                  <p class="safety-text">{{ testState.steps[testState.currentStep]?.safety_notes }}</p>
                </div>
              </div>
            </div>
          </div>

          <!-- 测试完成提示 -->
          <div v-else class="test-complete">
            <el-result icon="success" title="所有步骤测试完成" sub-title="该SOP流程已全部测试通过">
              <template #extra>
                <el-button type="primary" @click="resetTest">重新测试</el-button>
              </template>
            </el-result>
          </div>

          <!-- 对话消息区域 -->
          <div v-if="testState.messages.length" class="chat-messages">
            <div
              v-for="(msg, index) in testState.messages"
              :key="index"
              class="chat-msg"
              :class="msg.role"
            >
              <div class="msg-role">{{ msg.role === 'user' ? '你' : 'AI' }}</div>
              <div class="msg-content" v-html="formatFeedback(msg.content)"></div>
              <div v-if="msg.imageUrl" class="msg-image">
                <img :src="msg.imageUrl" class="msg-image-content" @click="msg.previewVisible = true" />
                <div v-if="msg.previewVisible" class="msg-image-overlay" @click="msg.previewVisible = false">
                  <img :src="msg.imageUrl" class="msg-image-large" />
                </div>
              </div>
              <div v-if="msg.passed !== undefined" class="msg-verdict" :class="{ pass: msg.passed, fail: !msg.passed }">
                {{ msg.passed ? '✓ 通过' : '✗ 未通过' }}
              </div>
            </div>
          </div>
        </div>

        <!-- 底部固定区域：输入 + 进度条 -->
        <div class="test-bottom-area">
          <!-- 用户输入区域 -->
          <div class="test-input-area">
            <div class="input-row">
              <el-input
                v-model="testState.userInput"
                type="textarea"
                :rows="3"
                placeholder="请描述您的操作情况..."
                :disabled="testState.loading"
              />
            </div>
            <div class="input-actions">
              <div class="left-actions">
                <div v-if="testState.uploadedImage" class="image-preview-wrap">
                  <img :src="testState.uploadedImage" class="preview-thumb" />
                  <el-icon class="preview-remove" @click="testState.uploadedImage = ''"><CircleClose /></el-icon>
                </div>
                <el-upload
                  v-else
                  :http-request="handleTestFileUpload"
                  :show-file-list="false"
                  accept="image/*"
                  :disabled="testState.loading"
                >
                  <el-button :disabled="testState.loading">
                    <el-icon><Picture /></el-icon>上传图片
                  </el-button>
                </el-upload>
              </div>
              <div class="right-actions">
                <el-button
                  type="primary"
                  :disabled="testState.loading || !testState.stepPassed"
                  @click="nextStep"
                >
                  下一步<el-icon><ArrowRight /></el-icon>
                </el-button>
                <el-button
                  type="success"
                  :disabled="testState.loading || (!testState.userInput.trim() && !testState.uploadedImage)"
                  :loading="testState.loading"
                  @click="sendTestInput"
                >
                  <el-icon><Promotion /></el-icon>发送
                </el-button>
              </div>
            </div>
          </div>

          <!-- 流程步骤预览条 -->
          <div class="step-progress-bar">
            <div
              v-for="(step, index) in testState.steps"
              :key="index"
              class="step-dot"
              :class="{
                completed: index < testState.currentStep,
                current: index === testState.currentStep,
                pending: index > testState.currentStep
              }"
              :title="step.title"
            >
              <span class="dot-index">{{ index + 1 }}</span>
            </div>
          </div>
        </div>
      </div>

      <el-empty v-else description="请从左侧选择一个SOP流程开始测试" :image-size="120" />
    </el-card>
  </el-col>
</el-row>

    <!-- 生成SOP对话框 -->
    <el-dialog v-model="showGenerateDialog" title="生成SOP流程" width="650px" :close-on-click-modal="false">
      <div class="generate-steps">
<!-- 选择文档 -->
<div class="gen-section">
  <h4><el-tag type="primary">1</el-tag> 选择SOP文档</h4>
  <div class="doc-list" v-loading="genState.loadingDocs">
    <div v-if="genState.documents.length === 0 && !genState.loadingDocs" style="color:#999; text-align:center; padding:20px;">
      该知识库暂无文档
    </div>
    <div
      v-for="doc in genState.documents"
      :key="doc.id"
      class="doc-item"
      :class="{ selected: genState.selectedDoc?.id === doc.id }"
      @click="genState.selectedDoc = doc"
    >
      <div class="doc-name">{{ doc.name || doc.id }}</div>
      <div class="doc-meta" v-if="doc.indexing_status">{{ doc.indexing_status }}</div>
    </div>
  </div>
</div>

<!-- 生成按钮 -->
<div class="gen-actions" v-if="genState.selectedDoc">
  <el-button type="primary" :loading="genState.generating" @click="doGenerateSop" size="large" style="width:100%">
    <el-icon><MagicStick /></el-icon>
    {{ genState.generating ? 'AI正在生成SOP流程...' : '开始生成SOP流程' }}
  </el-button>
  <p v-if="genState.generating" class="gen-hint">正在调用AI Agent梳理文档并生成标准化操作流程，请稍候...</p>
</div>
      </div>
    </el-dialog>

    <!-- 编辑SOP对话框 -->
    <el-dialog v-model="showEditDialog" title="编辑SOP流程" width="850px" :close-on-click-modal="false" class="sop-edit-dialog">
      <div class="edit-dialog-body" v-if="editState.flow">
        <!-- 固定顶部：流程名称 -->
        <div class="edit-top-fixed">
          <el-form label-width="80px" size="default">
            <el-form-item label="流程名称">
              <el-input v-model="editState.flow.name" placeholder="请输入流程名称" />
            </el-form-item>
          </el-form>
        </div>
        <!-- 固定工具栏 -->
        <div class="edit-steps-toolbar">
          <h4>步骤列表（{{ editState.editSteps.length }} 步）</h4>
          <span class="toolbar-hint">拖拽手柄排序</span>
          <el-button type="primary" size="small" @click="addEditStep">
            <el-icon><Plus /></el-icon>新增步骤
          </el-button>
        </div>
        <!-- 可滚动内容区 -->
        <div class="edit-steps" ref="editStepsRef">
          <div
            v-for="(step, index) in editState.editSteps"
            :key="step._key"
            class="edit-step-item"
            :class="{ 'drag-over': dragOverIndex === index, 'is-empty': hasStepEmpty(step) }"
            draggable="true"
            @dragstart="onDragStart($event, index)"
            @dragover.prevent="onDragOver($event, index)"
            @dragleave="onDragLeave"
            @drop.prevent="onDrop($event, index)"
            @dragend="onDragEnd"
          >
            <div class="edit-step-header">
              <span class="drag-handle" title="拖拽排序">
                <el-icon><Rank /></el-icon>
              </span>
              <span class="step-index">步骤 {{ index + 1 }}</span>
              <el-button
                link
                type="danger"
                size="small"
                class="step-delete-btn"
                @click="removeEditStep(index)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <el-form label-width="80px" size="small">
              <el-form-item label="标题" :class="{ 'is-error': !step.title?.trim() }">
                <el-input v-model="step.title" placeholder="请输入步骤标题" />
              </el-form-item>
              <el-form-item label="操作描述" :class="{ 'is-error': !step.description?.trim() }">
                <el-input v-model="step.description" type="textarea" :rows="2" placeholder="请输入操作描述" />
              </el-form-item>
              <el-form-item label="验收标准">
                <el-input v-model="step.acceptance_criteria" placeholder="可选" />
              </el-form-item>
              <el-form-item label="安全注意">
                <el-input v-model="step.safety_notes" placeholder="可选" />
              </el-form-item>
            </el-form>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="edit-dialog-footer">
          <el-button @click="showEditDialog = false">取消</el-button>
          <el-button type="primary" @click="saveEditSop">保存修改</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { sanitizeHtml } from '@/utils/sanitize'
import {
  getDifyDatasets,
  getDifyDocuments,
  getSopFlowList,
  generateSopFlow,
  updateSopFlow,
  deleteSopFlow as apiDeleteSopFlow,
  uploadFile
} from '@/api'


// ============ 公共 ============

// ============ SOP流程管理 ============
const sopFlowList = ref<any[]>([])
const selectedSopFlow = ref<any>(null)

// 生成SOP对话框 - 通过后端白名单获取知识库ID
const showGenerateDialog = ref(false)
const genState = reactive({
  datasetId: '',
  documents: [] as any[],
  selectedDoc: null as any,
  loadingDocs: false,
  generating: false
})

// 编辑SOP对话框
const showEditDialog = ref(false)
const scrollAreaRef = ref<HTMLElement | null>(null)
const editStepsRef = ref<HTMLElement | null>(null)
const editState = reactive({
  flow: null as any,
  editSteps: [] as any[]
})
const dragIndex = ref(-1)
const dragOverIndex = ref(-1)

// 测试状态
const testState = reactive({
  flow: null as any,
  steps: [] as any[],
  currentStep: 0,
  userInput: '',
  uploadedImage: '',
  aiFeedback: '',
  stepPassed: false,
  loading: false,
  messages: [] as { role: string; content: string; passed?: boolean; imageUrl?: string; previewVisible?: boolean }[]
})

// 初始化加载
onMounted(() => {
  fetchSopFlowList()
})

async function fetchSopFlowList() {
  try {
    const res: any = await getSopFlowList()
    sopFlowList.value = res.data || []
  } catch (error) {
    console.error('获取SOP流程列表失败', error)
  }
}

function formatTime(time: string) {
  if (!time) return ''
  return time.substring(0, 10)
}

// ============ 生成SOP对话框逻辑 ============
async function openGenerateDialog() {
  showGenerateDialog.value = true
  genState.documents = []
  genState.selectedDoc = null
  genState.datasetId = ''
  genState.loadingDocs = true
  try {
    // 1. 通过后端白名单获取知识库ID
    const dsRes: any = await getDifyDatasets('sop-workflow')
    const dsList = dsRes.data || []
    if (dsList.length === 0) {
      ElMessage.warning('未找到可用的 SOP 知识库，请检查配置')
      genState.loadingDocs = false
      return
    }
    // 2. 取第一个知识库ID并加载文档
    const datasetId = dsList[0].id
    genState.datasetId = datasetId
    const res: any = await getDifyDocuments(datasetId, 1, 50)
    genState.documents = Array.isArray(res.data) ? res.data : (res.data?.data || [])
  } catch (error) {
    ElMessage.error('获取文档列表失败')
  } finally {
    genState.loadingDocs = false
  }
}

async function doGenerateSop() {
  if (!genState.selectedDoc) {
    ElMessage.warning('请选择文档')
    return
  }
  genState.generating = true
  try {
    const doc = genState.selectedDoc
    await generateSopFlow({
      documentId: doc.id,
      documentName: doc.name || doc.id,
      datasetId: genState.datasetId
    })
    ElMessage.success('SOP流程生成成功！')
    showGenerateDialog.value = false
    await fetchSopFlowList()
  } catch (error: any) {
    ElMessage.error(error?.msg || '生成失败')
  } finally {
    genState.generating = false
  }
}

// ============ 流程操作 ============
function selectSopFlow(item: any) {
  selectedSopFlow.value = item
}

function startTest(item: any) {
  selectedSopFlow.value = item
  // 解析步骤
  try {
    const parsed = typeof item.stepJson === 'string' ? JSON.parse(item.stepJson) : item.stepJson
    testState.flow = item
    testState.steps = parsed || []
    testState.currentStep = 0
    testState.userInput = ''
    testState.uploadedImage = ''
    testState.aiFeedback = ''
    testState.stepPassed = false
    testState.loading = false
    testState.messages = []
    ElMessage.success(`已加载流程: ${item.name}`)
  } catch (e) {
    ElMessage.error('流程步骤解析失败')
  }
}

function handleComplete(parsed: any, fullContent: string, aiMsgIndex: number) {
  testState.messages[aiMsgIndex].passed = parsed.passed || false
  testState.stepPassed = parsed.passed || false
  testState.loading = false
  
  // 解析 AI 返回的 JSON，提取可读内容
  let displayText = fullContent
  try {
    const answer = JSON.parse(fullContent.trim())
    const parts = []
    if (answer.summary) parts.push(answer.summary)
    if (answer.reason) parts.push(answer.reason)
    if (answer.suggestion) parts.push(answer.suggestion)
    if (parts.length) displayText = parts.join('\n')
  } catch {
    // JSON 解析失败，尝试从 fullContent 提取纯文本
    const start = fullContent.indexOf('"summary":"')
    if (start > 0) {
      displayText = fullContent.substring(start + 11).split('"')[0]
    }
  }
  testState.messages[aiMsgIndex].content = displayText
  testState.aiFeedback = displayText
  
  if (testState.stepPassed) {
    ElMessage.success('当前步骤通过！')
  } else {
    ElMessage.warning('当前步骤未通过，请重做')
  }
}

async function sendTestInput() {
  if (!testState.userInput.trim() && !testState.uploadedImage) {
    ElMessage.warning('请输入操作描述或上传图片')
    return
  }

  // 添加用户消息
  const imageUrl = testState.uploadedImage
  testState.messages.push({ role: 'user', content: testState.userInput, imageUrl: imageUrl || undefined })
  testState.loading = true
  testState.aiFeedback = ''

  const userMsg = testState.userInput
  testState.userInput = ''
  testState.uploadedImage = ''

  // 添加初始AI消息占位
  testState.messages.push({ role: 'ai', content: '正在评估中...' })
  const aiMsgIndex = testState.messages.length - 1
  scrollToBottom()

  try {
    const response: any = await request({
      method: 'POST',
      url: '/workflow/sop/' + testState.flow.id + '/test',
      data: {
        stepIndex: testState.currentStep,
        userInput: userMsg,
        imageUrl: imageUrl
      }
    })
    console.log('[SOP测试] 收到结果', response)

    if (response.code === 200 && response.data) {
      const result = response.data
      const parts: string[] = []
      if (result.summary) parts.push(result.summary)
      if (result.reason) parts.push(result.reason)
      if (result.suggestion) parts.push(result.suggestion)
      const displayText = parts.length ? parts.join('\n') : (result.passed ? '✅ 当前步骤已通过' : '❌ 当前步骤未通过')

      testState.messages[aiMsgIndex].content = displayText
      testState.messages[aiMsgIndex].passed = result.passed
      testState.stepPassed = result.passed
      testState.aiFeedback = displayText
      testState.loading = false

      if (result.passed) {
        ElMessage.success('当前步骤通过！')
      } else {
        ElMessage.warning('当前步骤未通过，请重做')
      }
      scrollToBottom()
    } else {
      throw new Error(response.msg || '返回数据异常')
    }
  } catch (error: any) {
    testState.messages[aiMsgIndex].content = '请求失败: ' + error.message
    testState.aiFeedback = '请求失败: ' + error.message
    testState.loading = false
    ElMessage.error('测试请求失败')
    scrollToBottom()
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (scrollAreaRef.value) {
      scrollAreaRef.value.scrollTop = scrollAreaRef.value.scrollHeight
    }
  })
}

function nextStep() {
  if (testState.currentStep < testState.steps.length) {
    testState.currentStep++
    testState.userInput = ''
    testState.uploadedImage = ''
    testState.aiFeedback = ''
    testState.stepPassed = false
    testState.messages = []
    ElMessage.info(`进入第 ${testState.currentStep + 1} 步`)
  }
}

function resetTest() {
  testState.currentStep = 0
  testState.userInput = ''
  testState.uploadedImage = ''
  testState.aiFeedback = ''
  testState.stepPassed = false
  testState.loading = false
  testState.messages = []
}

async function handleTestFileUpload(options: any) {
  try {
    const res: any = await uploadFile(options.file)
    testState.uploadedImage = res.data?.url || ''
    options.onSuccess(res)
    ElMessage.success('图片上传成功')
  } catch (error: any) {
    options.onError(error)
    ElMessage.error('图片上传失败')
  }
}

function formatFeedback(text: string) {
  if (!text) return ''
  const html = text.replace(/\n/g, '<br>')
  return sanitizeHtml(html)
}

// ============ 编辑SOP ============
function editSopFlow(item: any) {
  try {
    editState.flow = { ...item }
    const raw = typeof item.stepJson === 'string'
      ? JSON.parse(item.stepJson)
      : JSON.parse(JSON.stringify(item.stepJson))
    // 为每个步骤添加唯一 key 用于拖拽排序的稳定标识
    editState.editSteps = (raw || []).map((step: any, i: number) => ({
      ...step,
      _key: `step_${Date.now()}_${i}_${Math.random().toString(36).slice(2, 8)}`
    }))
    showEditDialog.value = true
  } catch (e) {
    ElMessage.error('解析流程数据失败')
  }
}

function addEditStep() {
  editState.editSteps.push({
    _key: `step_${Date.now()}_${editState.editSteps.length}_${Math.random().toString(36).slice(2, 8)}`,
    title: '',
    description: '',
    acceptance_criteria: '',
    safety_notes: ''
  })
  // 新增后滚动到底部
  nextTick(() => {
    if (editStepsRef.value) {
      editStepsRef.value.scrollTo({ top: editStepsRef.value.scrollHeight, behavior: 'smooth' })
    }
  })
}

function removeEditStep(index: number) {
  editState.editSteps.splice(index, 1)
}

/** 判断步骤是否有必填项为空 */
function hasStepEmpty(step: any): boolean {
  return !step.title?.trim() || !step.description?.trim()
}

/** 收集所有有空必填项的步骤编号 */
function getEmptyStepLabels(): number[] {
  const empty: number[] = []
  editState.editSteps.forEach((step: any, i: number) => {
    if (hasStepEmpty(step)) empty.push(i + 1)
  })
  return empty
}

// ============ 拖拽排序 ============
function onDragStart(e: DragEvent, index: number) {
  dragIndex.value = index
  if (e.dataTransfer) {
    e.dataTransfer.effectAllowed = 'move'
    e.dataTransfer.setData('text/plain', String(index))
  }
}

function onDragOver(_e: DragEvent, index: number) {
  if (dragIndex.value === -1 || dragIndex.value === index) return
  dragOverIndex.value = index
}

function onDragLeave(e: DragEvent) {
  // 仅当离开整个步骤卡片时才清除（而非移动到子元素）
  const target = e.currentTarget as HTMLElement
  if (e.relatedTarget && target.contains(e.relatedTarget as Node)) return
  dragOverIndex.value = -1
}

function onDrop(e: DragEvent, index: number) {
  e.stopPropagation()
  if (dragIndex.value === -1 || dragIndex.value === index) {
    dragOverIndex.value = -1
    return
  }
  const steps = editState.editSteps
  const draggedItem = steps[dragIndex.value]
  steps.splice(dragIndex.value, 1)
  steps.splice(index, 0, draggedItem)
  dragOverIndex.value = -1
}

function onDragEnd(_e: DragEvent) {
  dragIndex.value = -1
  dragOverIndex.value = -1
}

async function saveEditSop() {
  // 空内容校验
  const emptyLabels = getEmptyStepLabels()
  if (emptyLabels.length > 0) {
    ElMessage.warning(`步骤 ${emptyLabels.join('、')} 的「标题」或「操作描述」不能为空，请填写完整后再保存`)
    return
  }

  try {
    // 保存前移除 _key 等前端临时字段
    const cleanSteps = editState.editSteps.map(({ _key, ...step }: any) => step)
    await updateSopFlow(editState.flow.id, {
      name: editState.flow.name,
      stepJson: JSON.stringify(cleanSteps),
      totalSteps: cleanSteps.length
    })
    ElMessage.success('保存成功')
    showEditDialog.value = false
    await fetchSopFlowList()
    // 如果正在测试此流程，刷新测试数据
    if (testState.flow?.id === editState.flow.id) {
      testState.steps = cleanSteps
      testState.flow.name = editState.flow.name
    }
  } catch (error: any) {
    ElMessage.error(error?.msg || '保存失败')
  }
}

// ============ 删除SOP ============
async function deleteSopFlowItem(item: any) {
  try {
    await ElMessageBox.confirm(`确定要删除流程"${item.name}"吗？`, '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await apiDeleteSopFlow(item.id)
    ElMessage.success('已删除')
    if (selectedSopFlow.value?.id === item.id) {
      selectedSopFlow.value = null
    }
    if (testState.flow?.id === item.id) {
      testState.flow = null
      testState.steps = []
    }
    await fetchSopFlowList()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.msg || '删除失败')
    }
  }
}
</script>

<style scoped>
.page-container { padding: 0; }
.main-tabs { background: var(--bg-color); }

/* Tab 1: 检修作业执行 */
.main-card, .history-card { min-height: calc(100vh - 200px); }
.card-title {
  display: flex; align-items: center; gap: 8px;
  font-size: 16px; font-weight: 600; color: var(--text-primary);
  flex-wrap: wrap;
}
.search-bar {
  margin-bottom: 20px; padding: 16px;
  background: var(--bg-color); border-radius: 8px;
}
.workflow-content { padding: 16px; }
.workflow-header {
  display: flex; justify-content: space-between;
  align-items: center; margin-bottom: 24px;
}
.workflow-header h3 { margin: 0; color: var(--text-primary); }
.steps-container { margin: 24px 0; }
.step-actions, .start-actions {
  display: flex; justify-content: center; gap: 16px;
  margin-top: 24px; padding: 16px;
  background: var(--bg-color); border-radius: 8px;
}
.execution-list { max-height: 500px; overflow-y: auto; }
.execution-item {
  padding: 12px; border-bottom: 1px solid var(--border-color);
  cursor: pointer; transition: background-color 0.3s;
}
.execution-item:hover { background-color: var(--bg-color); }
.execution-info {
  display: flex; justify-content: space-between;
  align-items: center; margin-bottom: 8px;
}
.execution-status { padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.execution-status.in_progress { background: #e6f7ff; color: #1890ff; }
.execution-status.completed { background: #f6ffed; color: #52c41a; }
.execution-status.exception { background: #fff2e8; color: #fa8c16; }
.execution-time { font-size: 12px; color: var(--text-secondary); }

/* Tab 2: SOP流程管理 */
.sop-container { min-height: calc(100vh - 200px); }
.sop-list-card, .sop-test-card { height: calc(100vh - 180px); }

.btn-generate { margin-left: auto !important; }
.sop-list { max-height: 550px; overflow-y: auto; }
.sop-item {
  padding: 12px; margin-bottom: 8px;
  border: 1px solid var(--border-color); border-radius: 8px;
  cursor: pointer; transition: all 0.2s;
}
.sop-item:hover { border-color: var(--el-color-primary); background: var(--el-color-primary-light-9); }
.sop-item.active { border-color: var(--el-color-primary); background: var(--el-color-primary-light-8); }
.sop-item-header {
  display: flex; justify-content: space-between;
  align-items: center; margin-bottom: 4px;
}
.sop-name { font-weight: 600; font-size: 14px; }
.sop-item-time { font-size: 12px; color: var(--text-secondary); margin-bottom: 8px; }
.sop-item-actions { display: flex; gap: 8px; }

/* 测试窗口 */
.test-container { 
  display: flex; flex-direction: column; 
  height: calc(100vh - 280px); overflow: hidden;
}
.test-scroll-area {
  flex: 1; overflow-y: auto; padding-right: 4px;
  margin-bottom: 12px;
}
.test-bottom-area {
  flex-shrink: 0;
}
.test-header {
  padding: 12px 16px; background: var(--bg-color);
  border-radius: 8px;
}
.test-flow-info { display: flex; align-items: center; gap: 12px; }
.test-flow-info h3 { margin: 0; }

.current-step-display { }
.step-card {
  padding: 16px; border: 1px solid var(--el-color-primary-light-5);
  border-radius: 8px; background: var(--el-color-primary-light-9);
}
.step-title-row { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.step-number {
  display: inline-flex; align-items: center; justify-content: center;
  width: 32px; height: 32px; border-radius: 50%;
  background: var(--el-color-primary); color: #fff;
  font-weight: 700; font-size: 14px; flex-shrink: 0;
}
.step-title-row h4 { margin: 0; color: var(--text-primary); }
.step-detail { }
.step-field { margin-bottom: 10px; }
.step-field label { font-weight: 600; color: var(--text-secondary); font-size: 13px; display: block; margin-bottom: 4px; }
.step-field p { margin: 4px 0 0 0; line-height: 1.6; }
.step-field .criteria {
  padding: 8px 12px; background: #f0fdf4;
  border-left: 3px solid #22c55e; border-radius: 0 4px 4px 0;
}
.step-field.safety { }
.step-field .safety-text {
  padding: 8px 12px; background: #fff7ed;
  border-left: 3px solid #f97316; border-radius: 0 4px 4px 0;
}
.tool-tag { margin-right: 4px; margin-bottom: 4px; }

/* AI反馈 */
.ai-feedback {
  padding: 12px 16px; border-radius: 8px;
  border-left: 4px solid #ccc;
}
.ai-feedback.pass {
  background: #f0fdf4; border-color: #22c55e;
}
.ai-feedback.fail {
  background: #fef2f2; border-color: #ef4444;
}
.feedback-header {
  display: flex; align-items: center; gap: 8px;
  font-weight: 700; font-size: 15px; margin-bottom: 8px;
}
.feedback-header .el-icon { font-size: 20px; }
.pass .feedback-header { color: #16a34a; }
.fail .feedback-header { color: #dc2626; }
.feedback-content { font-size: 13px; line-height: 1.7; color: var(--text-primary); }

/* 输入区域 */
.test-input-area {
  padding: 16px; background: var(--bg-color);
  border-radius: 8px; border: 1px solid var(--border-color);
}
.input-row { margin-bottom: 12px; }
.input-actions { display: flex; justify-content: space-between; align-items: center; }
.left-actions { display: flex; align-items: center; gap: 8px; }
.right-actions { display: flex; gap: 8px; }

/* 步骤进度条 */
.step-progress-bar {
  display: flex; gap: 6px; flex-wrap: wrap;
  padding: 12px; background: var(--bg-color);
  border-radius: 8px; justify-content: center;
}
.step-dot {
  width: 28px; height: 28px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 700; cursor: pointer;
  transition: all 0.2s; border: 2px solid #ddd;
  background: #fff; color: #999;
}
.step-dot.completed { background: #22c55e; border-color: #22c55e; color: #fff; }
.step-dot.current { background: var(--el-color-primary); border-color: var(--el-color-primary); color: #fff; transform: scale(1.15); }
.step-dot.pending { background: #f5f5f5; border-color: #ddd; color: #bbb; }

/* 测试完成 */
.test-complete { padding: 40px 0; }

/* 对话消息 */
.chat-messages { margin-top: 12px; }
.chat-msg {
  margin-bottom: 12px; padding: 10px 14px; border-radius: 8px;
  line-height: 1.6;
}
.chat-msg.user { background: var(--el-color-primary-light-9); margin-left: 20px; }
.chat-msg.ai { background: #f5f7fa; margin-right: 20px; }
.msg-role { font-size: 11px; color: var(--text-secondary); margin-bottom: 4px; font-weight: 600; }
.msg-content { font-size: 13px; white-space: pre-wrap; word-break: break-word; }
.msg-verdict {
  margin-top: 6px; font-size: 12px; font-weight: 600;
  padding: 2px 8px; border-radius: 4px; display: inline-block;
}
.msg-verdict.pass { background: #dcfce7; color: #16a34a; }
.msg-verdict.fail { background: #fef2f2; color: #dc2626; }

/* 生成SOP对话框 */
.generate-steps { }
.gen-section { margin-bottom: 20px; }
.gen-section h4 { display: flex; align-items: center; gap: 8px; margin: 0 0 12px 0; font-size: 15px; }
.doc-list { max-height: 250px; overflow-y: auto; border: 1px solid var(--border-color); border-radius: 8px; }
.doc-item {
  padding: 12px 16px; border-bottom: 1px solid var(--border-color);
  cursor: pointer; transition: background 0.2s;
}
.doc-item:last-child { border-bottom: none; }
.doc-item:hover { background: var(--el-color-primary-light-9); }
.doc-item.selected { background: var(--el-color-primary-light-8); border-left: 3px solid var(--el-color-primary); }
.doc-name { font-weight: 500; }
.doc-meta { font-size: 12px; color: var(--text-secondary); margin-top: 4px; }
.gen-actions { margin-top: 24px; text-align: center; }
.gen-hint { font-size: 13px; color: var(--text-secondary); margin-top: 8px; }

/* 编辑对话框 */
.sop-edit-dialog :deep(.el-dialog__body) {
  padding: 0 20px 0 20px;
  max-height: 70vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.edit-dialog-body {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  flex: 1;
}
/* 固定顶部：流程名称区域 */
.edit-top-fixed {
  padding: 16px 0 0 0;
  background: #fff;
  position: sticky;
  top: 0;
  z-index: 11;
  flex-shrink: 0;
}
.edit-top-fixed :deep(.el-form-item) {
  margin-bottom: 12px;
}
.edit-dialog-footer {
  width: 100%;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
.edit-steps-toolbar {
  display: flex; align-items: center; gap: 12px;
  margin-bottom: 0; padding: 10px 12px;
  background: #fff;
  border-bottom: 1px solid var(--border-color);
  position: sticky; top: 0; z-index: 10;
  border-radius: 0;
  flex-shrink: 0;
}
.edit-steps-toolbar h4 { margin: 0; white-space: nowrap; }
.toolbar-hint {
  font-size: 12px; color: var(--text-secondary);
  margin-left: 8px;
}
.edit-steps-toolbar .el-button {
  margin-left: auto;
}
.edit-steps {
  flex: 1; overflow-y: auto; padding: 12px 4px 4px 4px;
  min-height: 200px;
  max-height: calc(70vh - 180px);
}
.edit-step-item {
  padding: 16px; margin-bottom: 12px;
  border: 2px solid var(--border-color); border-radius: 8px;
  background: var(--bg-color);
  cursor: grab;
  transition: border-color 0.2s, box-shadow 0.2s, transform 0.15s;
  user-select: none;
}
.edit-step-item:active { cursor: grabbing; }
.edit-step-item:hover {
  border-color: var(--el-color-primary-light-5);
  box-shadow: 0 2px 8px rgba(0,0,0,.06);
}
.edit-step-item.drag-over {
  border-color: var(--el-color-primary);
  border-style: dashed;
  box-shadow: 0 0 0 3px var(--el-color-primary-light-8);
  transform: translateY(-2px);
}
/* 有空必填项的步骤卡片轻提示 */
.edit-step-item.is-empty {
  border-left: 3px solid var(--el-color-warning);
}
.edit-step-header {
  display: flex; align-items: center; gap: 8px;
  margin-bottom: 12px;
}
.drag-handle {
  cursor: grab; color: var(--text-secondary);
  display: flex; align-items: center;
  font-size: 18px; padding: 2px 4px;
  border-radius: 4px;
  transition: background 0.2s, color 0.2s;
}
.drag-handle:hover { background: var(--el-color-primary-light-9); color: var(--el-color-primary); }
.drag-handle:active { cursor: grabbing; }
.step-index { font-weight: 700; color: var(--el-color-primary); flex: 1; }
.step-delete-btn {
  opacity: 0.5; transition: opacity 0.2s;
}
.edit-step-item:hover .step-delete-btn { opacity: 1; }

/* 图片预览 - 输入区 */
.image-preview-wrap {
  position: relative;
  width: 80px;
  height: 80px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  overflow: hidden;
  flex-shrink: 0;
}
.preview-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.preview-remove {
  position: absolute;
  top: -6px;
  right: -6px;
  font-size: 18px;
  color: #f56c6c;
  background: #fff;
  border-radius: 50%;
  cursor: pointer;
}

/* 图片展示 - 消息中 */
.msg-image {
  margin: 8px 0;
}
.msg-image-content {
  max-width: 200px;
  max-height: 150px;
  border-radius: 6px;
  border: 1px solid var(--border-color);
  cursor: zoom-in;
  transition: transform 0.2s;
}
.msg-image-content:hover {
  transform: scale(1.02);
}

/* 图片放大覆盖层 */
.msg-image-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  cursor: zoom-out;
}
.msg-image-large {
  max-width: 80vw;
  max-height: 80vh;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.3);
}
</style>
