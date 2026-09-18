<template>
  <div class="sop-page">
    <!-- ============================================================
         流程选择界面
         ============================================================ -->
    <div v-if="!testState.flow" class="sop-select">
      <div class="select-header">
        <div class="select-icon">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2"/>
            <rect x="9" y="3" width="6" height="4" rx="1"/>
            <path d="M9 14l2 2 4-4"/>
          </svg>
        </div>
        <h2>SOP 标准作业流程</h2>
        <p>选择设备检修流程，AI 将逐步引导完成标准化操作</p>
      </div>

      <div class="flow-list" v-if="flowList.length">
        <button
          v-for="flow in flowList"
          :key="flow.id"
          class="flow-card"
          @click="startTest(flow.id)"
        >
          <div class="flow-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
          </div>
          <div class="flow-info">
            <h3>{{ flow.name }}</h3>
            <span class="flow-steps">{{ flow.totalSteps || '—' }} 个步骤</span>
          </div>
          <svg class="flow-arrow" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m9 18 6-6-6-6"/></svg>
        </button>
      </div>

      <div v-else-if="flowList.length === 0" class="flow-empty">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" opacity="0.3"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
        <p>暂无可用的 SOP 流程</p>
      </div>
    </div>

    <!-- ============================================================
         流程执行界面
         ============================================================ -->
    <template v-else>
      <!-- 顶部导航条 -->
      <div class="sop-topbar">
        <button class="back-btn" @click="resetTest">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="m15 18-6-6 6-6"/>
          </svg>
          返回
        </button>
        <div class="sop-title-area">
          <h3 class="sop-title">{{ testState.flow.name }}</h3>
        </div>
        <button class="preview-btn" @click="showPreview = true">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z"/><circle cx="12" cy="12" r="3"/></svg>
          流程预览
        </button>
      </div>

      <!-- 步骤进度条 -->
      <div class="step-progress">
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
        </div>
        <div class="progress-steps">
          <div
            v-for="(step, si) in testState.steps"
            :key="si"
            class="progress-dot"
            :class="{
              completed: si < testState.currentStep,
              active: si === testState.currentStep,
              pending: si > testState.currentStep
            }"
            :title="step.title || step.name || `步骤 ${si + 1}`"
          >
            <div class="dot-inner">
              <svg v-if="si < testState.currentStep" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
              <span v-else>{{ si + 1 }}</span>
            </div>
          </div>
        </div>
        <span class="progress-label">{{ Math.min(testState.currentStep + 1, testState.steps.length) }} / {{ testState.steps.length }}</span>
      </div>

      <!-- 当前步骤内容展示 -->
      <div v-if="testState.currentStep < testState.steps.length" class="current-step-area">
        <div class="step-card">
          <div class="step-title-row">
            <span class="step-number">{{ testState.currentStep + 1 }}</span>
            <h4>{{ testState.steps[testState.currentStep]?.title || testState.steps[testState.currentStep]?.name }}</h4>
          </div>
          <div class="step-detail">
            <div class="step-field" v-if="testState.steps[testState.currentStep]?.description">
              <label>操作描述</label>
              <p>{{ testState.steps[testState.currentStep]?.description }}</p>
            </div>
            <div class="step-field" v-if="testState.steps[testState.currentStep]?.acceptance_criteria">
              <label>验收标准</label>
              <p class="criteria">{{ testState.steps[testState.currentStep]?.acceptance_criteria }}</p>
            </div>
            <div class="step-field" v-if="testState.steps[testState.currentStep]?.tools?.length">
              <label>所需工具</label>
              <div class="tool-tags">
                <span v-for="(tool, ti) in testState.steps[testState.currentStep]?.tools" :key="ti" class="tool-tag">{{ tool }}</span>
              </div>
            </div>
            <div class="step-field safety" v-if="testState.steps[testState.currentStep]?.safety_notes">
              <label>安全注意</label>
              <p class="safety-text">{{ testState.steps[testState.currentStep]?.safety_notes }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 对话区 -->
      <div class="sop-messages" ref="sopMsgArea">
        <div
          v-for="(msg, idx) in testState.messages"
          :key="idx"
          class="sop-message"
          :class="msg.role"
        >
          <div v-if="msg.role === 'ai'" class="msg-avatar ai-avatar-small">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="2" y="2" width="20" height="8" rx="2" ry="2"/><rect x="2" y="14" width="20" height="8" rx="2" ry="2"/><line x1="6" y1="6" x2="6.01" y2="6"/><line x1="6" y1="18" x2="6.01" y2="18"/>
            </svg>
          </div>
          <div class="msg-bubble sop-bubble" :class="msg.role">
            <div class="bubble-text" v-html="renderMarkdown(msg.content)"></div>
            <div v-if="msg.imageUrl" class="bubble-attachment">
              <img :src="msg.imageUrl" alt="附件图片" @click="previewImage(msg.imageUrl)" />
            </div>
            <!-- 判定结果 -->
            <div v-if="msg.passed !== undefined" class="verdict-badge" :class="{ pass: msg.passed, fail: !msg.passed }">
              <svg v-if="msg.passed" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
              <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
              {{ msg.passed ? '步骤通过' : '步骤未通过' }}
            </div>
          </div>
        </div>

        <!-- AI 加载中 -->
        <div v-if="testState.loading" class="sop-message ai">
          <div class="msg-avatar ai-avatar-small">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="2" y="2" width="20" height="8" rx="2" ry="2"/><rect x="2" y="14" width="20" height="8" rx="2" ry="2"/>
            </svg>
          </div>
          <div class="msg-bubble sop-bubble ai">
            <div class="typing-indicator">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>
      </div>

      <!-- 完成状态 -->
      <div v-if="testState.currentStep >= testState.steps.length" class="sop-complete">
        <div class="complete-card">
          <div class="complete-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>
            </svg>
          </div>
          <h3>所有步骤已完成</h3>
          <p>该 SOP 流程已全部执行完成</p>
          <button class="complete-btn" @click="resetTest">选择其他流程</button>
        </div>
      </div>

      <!-- 下一步按钮 -->
      <div v-if="testState.stepPassed && testState.currentStep < testState.steps.length" class="sop-next-area">
        <button class="next-btn" @click="nextStep">
          进入下一步
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m9 18 6-6-6-6"/></svg>
        </button>
      </div>

      <!-- 输入区域（UserSearch 风格） -->
      <div v-if="testState.currentStep < testState.steps.length && !testState.stepPassed" class="input-area">
        <div class="search-box">
          <!-- 图片预览行 -->
          <div v-if="sopUploadedImg" class="preview-row">
            <div class="preview-tag">
              <div class="preview-popup">
                <img :src="sopUploadedImg" alt="预览" />
              </div>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/>
              </svg>
              <span class="preview-name">图片</span>
              <button class="preview-remove" @click="sopUploadedImg = ''" aria-label="移除">×</button>
            </div>
          </div>

          <textarea
            v-model="sopInput"
            placeholder="描述您的操作情况..."
            :disabled="testState.loading"
            rows="1"
            class="search-input"
            ref="sopInputRef"
            @keydown.enter.exact.prevent="sendInput"
            @input="autoGrow"
          ></textarea>

          <!-- 底栏：操作按钮 -->
          <div class="search-bottom">
            <div class="search-actions">
              <label class="upload-btn" title="上传操作图片" v-if="!sopUploadedImg">
                <input type="file" accept="image/jpeg,image/png" hidden :disabled="testState.loading" @change="handleImgUpload" />
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/>
                </svg>
              </label>
              <button
                class="send-btn"
                :disabled="(!sopInput.trim() && !sopUploadedImg) || testState.loading"
                @click="sendInput"
                aria-label="发送"
              >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
        <p class="input-hint">Enter 发送 · Shift+Enter 换行</p>
      </div>
    </template>

    <!-- 流程预览弹窗 -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="showPreview" class="modal-overlay" @click.self="showPreview = false">
          <div class="modal-content preview-modal">
            <div class="modal-header">
              <h3>{{ testState.flow?.name || 'SOP' }} — 全部步骤</h3>
              <button class="modal-close" @click="showPreview = false">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
            <div class="modal-body preview-body">
              <div class="preview-timeline">
                <div
                  v-for="(step, si) in testState.steps"
                  :key="si"
                  class="preview-step"
                  :class="{ active: si === testState.currentStep }"
                >
                  <div class="preview-step-marker">
                    <div class="marker-dot">{{ si + 1 }}</div>
                    <div v-if="si < testState.steps.length - 1" class="marker-line"></div>
                  </div>
                  <div class="preview-step-content">
                    <h4>{{ step.title || step.name || `步骤 ${si + 1}` }}</h4>
                    <p v-if="step.description" class="preview-desc">{{ step.description }}</p>
                    <div v-if="step.acceptance_criteria" class="preview-criteria">
                      <span class="preview-tag-label">验收标准</span>
                      <span>{{ step.acceptance_criteria }}</span>
                    </div>
                    <div v-if="step.tools?.length" class="preview-tools">
                      <span v-for="tool in step.tools" :key="tool" class="preview-tool-tag">{{ tool }}</span>
                    </div>
                    <div v-if="step.safety_notes" class="preview-safety">
                      <span class="preview-tag-label">安全注意</span>
                      <span>{{ step.safety_notes }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { getSopFlowList, uploadFile } from '@/api'
import { renderMarkdown } from '@/utils/markdown'

const flowList = ref<any[]>([])
const sopMsgArea = ref<HTMLElement | null>(null)
const sopInputRef = ref<HTMLTextAreaElement | null>(null)

const sopInput = ref('')
const sopUploadedImg = ref('')
const showPreview = ref(false)

const testState = reactive({
  flow: null as any,
  steps: [] as any[],
  currentStep: 0,
  stepPassed: false,
  loading: false,
  messages: [] as { role: string; content: string; passed?: boolean; imageUrl?: string }[]
})

const progressPercent = computed(() => {
  if (!testState.steps.length) return 0
  return Math.round((testState.currentStep / testState.steps.length) * 100)
})

onMounted(async () => {
  try {
    const res: any = await getSopFlowList()
    flowList.value = res.data || []
  } catch { /* ignore */ }
})

const startTest = (id: number) => {
  const item = flowList.value.find(f => f.id === id)
  if (!item) return
  try {
    const parsed = typeof item.stepJson === 'string' ? JSON.parse(item.stepJson) : item.stepJson
    testState.flow = item
    testState.steps = Array.isArray(parsed) ? parsed : []
    testState.currentStep = 0
    testState.stepPassed = false
    testState.loading = false
    testState.messages = []
    sopInput.value = ''
    sopUploadedImg.value = ''
    ElMessage.success(`已加载：${item.name}`)
  } catch {
    ElMessage.error('流程步骤解析失败')
  }
}

const resetTest = () => {
  testState.flow = null
  testState.steps = []
  testState.messages = []
  testState.currentStep = 0
  testState.stepPassed = false
  sopInput.value = ''
  sopUploadedImg.value = ''
}

const autoGrow = () => {
  const el = sopInputRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 120) + 'px'
}

const scrollBottom = () => {
  nextTick(() => {
    if (sopMsgArea.value) sopMsgArea.value.scrollTop = sopMsgArea.value.scrollHeight
  })
}

const sendInput = async () => {
  if (!sopInput.value.trim() && !sopUploadedImg.value) return
  if (testState.loading) return

  const img = sopUploadedImg.value
  testState.messages.push({ role: 'user', content: sopInput.value, imageUrl: img || undefined })
  testState.loading = true
  const userMsg = sopInput.value
  sopInput.value = ''
  sopUploadedImg.value = ''
  nextTick(() => autoGrow())
  scrollBottom()

  try {
    const resp: any = await request({
      method: 'POST',
      url: '/workflow/sop/' + testState.flow.id + '/test',
      data: {
        stepIndex: testState.currentStep,
        userInput: userMsg,
        imageUrl: img || undefined
      }
    })

    if (resp.code === 200 && resp.data) {
      const r = resp.data
      const parts: string[] = []
      if (r.summary) parts.push(r.summary)
      if (r.reason) parts.push(r.reason)
      if (r.suggestion) parts.push(r.suggestion)
      const content = parts.length ? parts.join('\n') : (r.passed ? '✅ 操作正确，步骤通过' : '❌ 操作不正确，请根据提示重新执行')
      testState.messages.push({ role: 'ai', content, passed: r.passed })
      testState.stepPassed = r.passed
    }
  } catch (e: any) {
    testState.messages.push({ role: 'ai', content: '评估请求失败: ' + (e.message || '未知错误') })
  } finally {
    testState.loading = false
    scrollBottom()
  }
}

const nextStep = () => {
  testState.currentStep++
  sopInput.value = ''
  sopUploadedImg.value = ''
  testState.stepPassed = false
  testState.messages = []
  nextTick(() => autoGrow())
}

const handleImgUpload = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 10MB')
    input.value = ''
    return
  }
  try {
    const res: any = await uploadFile(file)
    sopUploadedImg.value = res.data?.url || ''
    ElMessage.success('上传成功')
  } catch {
    ElMessage.error('上传失败')
  }
  input.value = ''
}

const previewImage = (url: string) => {
  window.open(url, '_blank')
}

</script>

<style scoped>
.sop-page {
  width: 100%;
  max-width: var(--content-max-width);
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 0 var(--space-4);
}

/* ============================================================
   选择界面
   ============================================================ */
.sop-select {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: var(--space-4) 0;
  overflow-y: auto;
}

.select-header {
  text-align: center;
  padding: var(--space-8) 0 var(--space-6);
}

.select-icon {
  display: inline-flex;
  width: 72px;
  height: 72px;
  align-items: center;
  justify-content: center;
  background: var(--primary-50);
  color: var(--primary-600);
  border-radius: var(--radius-lg);
  margin-bottom: var(--space-4);
}

.select-header h2 {
  font-size: var(--text-xl);
  font-weight: var(--weight-bold);
  color: var(--text-primary);
  margin: 0 0 var(--space-2);
}

.select-header p {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  margin: 0;
  max-width: 320px;
  margin-inline: auto;
}

.flow-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.flow-card {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-4) var(--space-5);
  background: var(--surface-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-base);
  text-align: left;
}

.flow-card:hover {
  border-color: var(--primary-300);
  box-shadow: var(--shadow-md);
  background: linear-gradient(90deg, var(--primary-50), var(--surface-card));
}

.flow-icon {
  width: 42px;
  height: 42px;
  border-radius: var(--radius-md);
  background: var(--primary-50);
  color: var(--primary-600);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.flow-info {
  flex: 1;
  min-width: 0;
}

.flow-info h3 {
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  color: var(--text-primary);
  margin: 0 0 2px;
}

.flow-steps {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}

.flow-arrow {
  color: var(--text-tertiary);
  flex-shrink: 0;
  transition: all var(--transition-base);
}

.flow-card:hover .flow-arrow {
  transform: translateX(3px);
  color: var(--primary-600);
}

.flow-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  color: var(--text-tertiary);
  font-size: var(--text-sm);
}

/* ============================================================
   执行界面 — 顶栏
   ============================================================ */
.sop-topbar {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) 0;
  flex-shrink: 0;
  border-bottom: 1px solid var(--border-light);
}

.back-btn {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: var(--text-sm);
  cursor: pointer;
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);
  transition: all var(--transition-base);
  flex-shrink: 0;
}

.back-btn:hover {
  background: var(--color-neutral-100);
  color: var(--text-primary);
}

.sop-title-area {
  flex: 1;
  min-width: 0;
}

.sop-title {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--text-primary);
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 预览按钮 */
.preview-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: var(--space-1) var(--space-3);
  border: 1.5px solid var(--border-base);
  background: var(--surface-card);
  color: var(--text-secondary);
  border-radius: var(--radius-md);
  font-size: var(--text-xs);
  cursor: pointer;
  transition: all var(--transition-base);
  flex-shrink: 0;
  white-space: nowrap;
}

.preview-btn:hover {
  border-color: var(--primary-300);
  color: var(--primary-600);
  background: var(--primary-50);
}

/* ============================================================
   步骤进度条
   ============================================================ */
.step-progress {
  padding: var(--space-3) 0;
  flex-shrink: 0;
}

.progress-bar {
  height: 4px;
  background: var(--color-neutral-200);
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: var(--space-2);
}

.progress-fill {
  height: 100%;
  background: var(--primary-500);
  border-radius: 2px;
  transition: width 0.5s ease;
}

.progress-steps {
  display: flex;
  justify-content: space-between;
  margin-bottom: var(--space-1);
}

.progress-dot {
  display: flex;
  align-items: center;
  justify-content: center;
}

.dot-inner {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: var(--weight-semibold);
  transition: all var(--transition-base);
}

.progress-dot.completed .dot-inner {
  background: var(--color-success-500);
  color: #fff;
}

.progress-dot.active .dot-inner {
  background: var(--primary-600);
  color: #fff;
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.12);
}

.progress-dot.pending .dot-inner {
  background: var(--color-neutral-200);
  color: var(--text-tertiary);
}

.progress-label {
  display: block;
  text-align: center;
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}

/* ============================================================
   当前步骤内容展示
   ============================================================ */
.current-step-area {
  flex-shrink: 0;
  margin-bottom: var(--space-3);
}

.step-card {
  padding: var(--space-5);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  background: var(--surface-card);
  box-shadow: var(--shadow-xs);
}

.step-title-row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--border-light);
}

.step-number {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: var(--radius-md);
  background: var(--primary-600);
  color: #fff;
  font-weight: var(--weight-bold);
  font-size: 13px;
  flex-shrink: 0;
}

.step-title-row h4 {
  margin: 0;
  color: var(--text-primary);
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
}

.step-detail {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.step-field {
  font-size: var(--text-sm);
}

.step-field label {
  display: block;
  font-weight: var(--weight-semibold);
  color: var(--text-secondary);
  font-size: var(--text-xs);
  margin-bottom: var(--space-1);
  text-transform: none;
  letter-spacing: normal;
}

.step-field p {
  margin: 0;
  color: var(--text-primary);
  line-height: var(--leading-relaxed);
  white-space: pre-wrap;
  word-break: break-word;
  font-size: var(--text-sm);
}

.step-field .criteria {
  padding: var(--space-3);
  background: #f9fafb;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  color: var(--text-primary);
  font-size: var(--text-sm);
}

.step-field.safety .safety-text {
  padding: var(--space-3);
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: var(--radius-md);
  color: #92400e;
  font-size: var(--text-sm);
}

.tool-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
}

.tool-tag {
  display: inline-block;
  font-size: var(--text-xs);
  padding: 1px 8px;
  background: var(--primary-100);
  color: var(--primary-700);
  border-radius: var(--radius-full);
}

/* ============================================================
   对话区
   ============================================================ */
.sop-messages {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-3) 0;
  min-height: 0;
}

.sop-message {
  display: flex;
  gap: var(--space-2);
  margin-bottom: var(--space-3);
}

.sop-message.user {
  flex-direction: row-reverse;
}

.sop-message.ai {
  align-items: flex-start;
}

.sop-bubble {
  max-width: 82%;
  border-radius: var(--radius-md);
  padding: var(--space-3) var(--space-4);
  font-size: var(--text-sm);
  line-height: var(--leading-relaxed);
}

.sop-bubble.user {
  background: linear-gradient(135deg, var(--primary-600), var(--primary-700));
  color: #fff;
  border-bottom-right-radius: var(--radius-xs);
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.15);
}

.sop-bubble.ai {
  background: var(--surface-card);
  border: 1px solid var(--border-light);
  border-top-left-radius: var(--radius-xs);
  box-shadow: var(--shadow-xs);
}

.msg-avatar.ai-avatar-small {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-sm);
  background: linear-gradient(135deg, var(--primary-100), var(--primary-200));
  color: var(--primary-600);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.bubble-attachment img {
  max-width: 200px;
  max-height: 150px;
  border-radius: var(--radius-sm);
  margin-top: var(--space-2);
  cursor: pointer;
  border: 1px solid var(--border-light);
  transition: transform var(--transition-base);
}

.bubble-attachment img:hover {
  transform: scale(1.03);
}

.verdict-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  margin-top: var(--space-2);
  font-size: var(--text-xs);
  font-weight: var(--weight-medium);
  padding: var(--space-1) var(--space-3);
  border-radius: var(--radius-full);
}

.verdict-badge.pass {
  background: #ecfdf5;
  color: #059669;
}

.verdict-badge.fail {
  background: #fef2f2;
  color: #dc2626;
}

/* 加载动画 */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: var(--space-1) 0;
}
.typing-indicator span {
  width: 6px; height: 6px; border-radius: 50%;
  background: var(--color-neutral-400);
  animation: typing-bounce 1.4s infinite both;
}
.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }
@keyframes typing-bounce {
  0%,80%,100% { opacity: 0.3; transform: translateY(0); }
  40% { opacity: 1; transform: translateY(-4px); }
}

/* ============================================================
   完成状态
   ============================================================ */
.sop-complete {
  padding: var(--space-8) 0;
  text-align: center;
}

.complete-card {
  padding: var(--space-8);
}

.complete-icon {
  color: var(--success-500);
  margin-bottom: var(--space-4);
  display: inline-flex;
}

.complete-card h3 {
  font-size: var(--text-lg);
  font-weight: var(--weight-bold);
  color: var(--text-primary);
  margin: 0 0 var(--space-2);
}

.complete-card p {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  margin: 0 0 var(--space-6);
}

.complete-btn {
  padding: var(--space-2) var(--space-6);
  border: 1.5px solid var(--primary-400);
  background: transparent;
  color: var(--primary-600);
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition-base);
}

.complete-btn:hover {
  background: var(--primary-50);
}

/* ============================================================
   下一步按钮
   ============================================================ */
.sop-next-area {
  padding: var(--space-3) 0;
  flex-shrink: 0;
  display: flex;
  justify-content: center;
}

.next-btn {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-8);
  background: linear-gradient(135deg, var(--success-500), #16a34a);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  cursor: pointer;
  transition: all var(--transition-base);
  box-shadow: 0 2px 10px rgba(34, 197, 94, 0.2);
}

.next-btn:hover {
  background: linear-gradient(135deg, #16a34a, #15803d);
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(34, 197, 94, 0.3);
}

.next-btn:active {
  transform: translateY(0);
}

/* ============================================================
   输入区域（UserSearch 风格）
   ============================================================ */
.input-area {
  padding: var(--space-3) 0;
  border-top: 1px solid var(--border-light);
  background: var(--surface-page);
  flex-shrink: 0;
}

.search-box {
  background: var(--surface-card);
  border: 1px solid var(--border-base);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  transition: all var(--transition-base);
  box-shadow: var(--shadow-xs);
}

.search-box:focus-within {
  border-color: var(--primary-300);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.06), var(--shadow-sm);
}

.preview-row {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  margin-bottom: var(--space-3);
  padding-bottom: var(--space-3);
  border-bottom: 1px solid var(--border-light);
}

.preview-tag {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 2px 8px 2px 4px;
  background: var(--color-neutral-100);
  border-radius: var(--radius-sm);
  font-size: 12px;
  color: var(--text-secondary);
  cursor: default;
  height: 28px;
  flex-shrink: 0;
}

.preview-popup {
  display: none;
  position: absolute;
  bottom: calc(100% + 8px);
  left: 50%;
  transform: translateX(-50%);
  z-index: 20;
}

.preview-tag:hover .preview-popup {
  display: block;
}

.preview-popup img {
  max-width: 220px;
  max-height: 180px;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--border-base);
}

.preview-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 80px;
}

.preview-remove {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: var(--text-tertiary);
  font-size: 14px;
  cursor: pointer;
  flex-shrink: 0;
  transition: all var(--transition-fast);
}

.preview-remove:hover {
  background: var(--color-danger);
  color: #fff;
}

.search-input {
  width: 100%;
  border: none;
  outline: none;
  resize: none;
  font-size: var(--text-base);
  font-family: inherit;
  color: var(--text-primary);
  background: transparent;
  line-height: 1.6;
  min-height: 28px;
  max-height: 160px;
}

.search-input::placeholder {
  color: var(--text-tertiary);
}

.search-input:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.search-bottom {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-top: var(--space-3);
  padding-top: var(--space-3);
  border-top: 1px solid var(--border-light);
}

.search-actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-shrink: 0;
}

.upload-btn {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border-base);
  border-radius: var(--radius-sm);
  color: var(--text-tertiary);
  cursor: pointer;
  transition: all var(--transition-base);
  background: transparent;
}

.upload-btn:hover {
  border-color: var(--primary-300);
  color: var(--primary-600);
  background: var(--primary-50);
}

.send-btn {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--primary-600), var(--primary-700));
  color: #fff;
  cursor: pointer;
  transition: all var(--transition-base);
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.2);
}

.send-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, var(--primary-700), var(--primary-800));
  transform: scale(1.06);
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.3);
}

.send-btn:active:not(:disabled) {
  transform: scale(0.95);
}

.send-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
  box-shadow: none;
}

.input-hint {
  text-align: center;
  font-size: 11px;
  color: var(--text-tertiary);
  margin: var(--space-1) 0 0;
}

/* ============================================================
   流程预览弹窗
   ============================================================ */
.preview-modal {
  max-width: 620px;
}

.preview-body {
  max-height: 65vh;
  overflow-y: auto;
}

.preview-timeline {
  display: flex;
  flex-direction: column;
}

.preview-step {
  display: flex;
  gap: var(--space-4);
  position: relative;
}

.preview-step-marker {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
  width: 32px;
}

.marker-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--color-neutral-200);
  color: var(--text-tertiary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: var(--weight-bold);
  flex-shrink: 0;
}

.preview-step.active .marker-dot {
  background: var(--primary-600);
  color: #fff;
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.12);
}

.marker-line {
  width: 2px;
  flex: 1;
  min-height: 20px;
  background: var(--color-neutral-200);
  margin: 4px 0;
}

.preview-step-content {
  flex: 1;
  min-width: 0;
  padding-bottom: var(--space-4);
}

.preview-step-content h4 {
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  color: var(--text-primary);
  margin: 0 0 var(--space-2);
}

.preview-desc {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  margin: 0 0 var(--space-2);
  line-height: var(--leading-relaxed);
  white-space: pre-wrap;
  word-break: break-word;
}

.preview-criteria {
  display: flex;
  align-items: flex-start;
  gap: var(--space-2);
  font-size: var(--text-xs);
  color: var(--success-700);
  background: #f0fdf4;
  border-radius: var(--radius-sm);
  padding: var(--space-2);
  margin-bottom: var(--space-2);
}

.preview-tag-label {
  font-weight: var(--weight-semibold);
  flex-shrink: 0;
  white-space: nowrap;
}

.preview-tools {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
  margin-bottom: var(--space-2);
}

.preview-tool-tag {
  display: inline-block;
  font-size: 11px;
  padding: 1px 8px;
  background: var(--primary-100);
  color: var(--primary-700);
  border-radius: var(--radius-full);
}

.preview-safety {
  display: flex;
  align-items: flex-start;
  gap: var(--space-2);
  font-size: var(--text-xs);
  color: var(--warning-800);
  background: #fffbeb;
  border-radius: var(--radius-sm);
  padding: var(--space-2);
}

/* 弹窗通用 */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-4);
}

.modal-content {
  width: 100%;
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

.modal-fade-enter-active { transition: opacity 0.2s ease; }
.modal-fade-enter-active .modal-content { transition: transform 0.25s ease, opacity 0.2s ease; }
.modal-fade-leave-active { transition: opacity 0.15s ease; }
.modal-fade-enter-from { opacity: 0; }
.modal-fade-enter-from .modal-content { transform: scale(0.95) translateY(8px); opacity: 0; }
.modal-fade-leave-to { opacity: 0; }
</style>
