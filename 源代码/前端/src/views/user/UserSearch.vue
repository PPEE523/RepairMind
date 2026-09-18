<template>
  <div class="search-page">
    <!-- 会话标题栏 -->
    <div v-if="conversationId || messages.length > 0" class="conv-header">
      <span class="conv-title">{{ conversationTitle }}</span>
    </div>

    <!-- 消息区域 -->
    <div class="messages-area" ref="msgArea">
      <!-- 空状态 -->
      <div v-if="messages.length === 0" class="empty-state">
        <div class="empty-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8"/>
            <path d="m21 21-4.3-4.3"/>
            <path d="M8 11h6"/>
            <path d="M11 8v6"/>
          </svg>
        </div>
        <h3>智能故障诊断</h3>
        <p>描述设备故障现象 — 支持文字描述、图片、视频和音频上传，AI 将为您匹配最相关的维修方案</p>
        <div class="empty-hints">
          <span>可以试试：</span>
          <button @click="quickStart('发动机启动后有异响，伴随震动')">发动机异响排查</button>
          <button @click="quickStart('液压系统压力不足，油缸动作缓慢')">液压系统故障</button>
          <button @click="quickStart('设备运行中突然停机，控制面板无报警')">设备异常停机</button>
        </div>
      </div>

      <!-- 消息列表 -->
      <div v-for="(msg, idx) in messages" :key="idx" class="message-group">
        <!-- 用户消息 -->
        <div class="message user-message">
          <div class="msg-bubble user-bubble">
            <div class="bubble-content">
              <p class="bubble-text">{{ msg.question }}</p>
              <div v-if="msg.equipModel" class="bubble-model-tag">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
                {{ msg.equipModel }}
              </div>
            </div>
            <div v-if="msg.images && msg.images.length" class="bubble-images">
              <img v-for="(img, i) in msg.images" :key="i" :src="img" class="bubble-img" @click="previewImage(img)" />
            </div>
            <div v-if="msg.files && msg.files.length" class="bubble-files">
              <div v-for="(_f, i) in msg.files" :key="i" class="bubble-file-tag">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14 2 14 8 20 8"/>
                </svg>
                <span>{{ msg.fileNames?.[i] || '附件' }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- AI 消息 -->
        <div class="message ai-message">
          <div class="ai-avatar">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="2" y="2" width="20" height="8" rx="2" ry="2"/>
              <rect x="2" y="14" width="20" height="8" rx="2" ry="2"/>
              <line x1="6" y1="6" x2="6.01" y2="6"/>
              <line x1="6" y1="18" x2="6.01" y2="18"/>
            </svg>
          </div>
          <div class="msg-bubble ai-bubble">
            <!-- 加载状态 -->
            <div v-if="!msg.answer && !msg.error" class="typing-indicator">
              <span></span><span></span><span></span>
            </div>

            <!-- 错误状态 -->
            <div v-else-if="msg.error" class="error-message">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
              {{ msg.error }}
              <button class="retry-btn" @click="retryMessage(idx)">重试</button>
            </div>

            <!-- 正常回答 -->
            <div v-else class="bubble-content">
              <div
                class="markdown-body"
                v-html="msg.answer"
                @click="handleCitationClick"
                @mouseover="handleCitationHover"
                @mouseout="handleCitationLeave"
              ></div>
              <span v-if="msg.streaming" class="streaming-cursor">|</span>
            </div>

            <!-- 参考来源 -->
            <div v-if="msg.resources && msg.resources.length && !msg.streaming" class="sources-area">
              <div class="sources-header">
                <div class="sources-header-left">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>
                  参考来源
                </div>

              </div>
              <div v-for="(r, ri) in getDedupedResources(msg)" :key="ri" class="source-item">
                <div class="source-name">{{ r.documentName || r.datasetName || '未知文档' }}</div>
                <div class="source-score" :style="{ '--score': (r.score || 0) * 100 }">
                  <div class="score-bar"></div>
                  <span>{{ ((r.score || 0) * 100).toFixed(0) }}%</span>
                </div>
              </div>
              <!-- 反馈按钮 -->
              <div class="sources-feedback">
                <template v-if="msg.feedbackStatus === 'correct' || msg.feedbackStatus === 'incorrect' || msg.feedbackStatus === 'processed'">
                  <span :class="['feedback-status-tag', msg.feedbackStatus]">
                    <template v-if="msg.feedbackStatus === 'correct'">✓ 已标注正确</template>
                    <template v-else-if="msg.feedbackStatus === 'processed'">✓ 已处理</template>
                    <template v-else>✗ 已标注错误</template>
                  </span>
                </template>
                <template v-else>
                  <span class="feedback-label">此回答是否正确？</span>
                  <button class="fb-btn fb-correct" @click="handleFeedback(msg, 'correct')">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
                    正确
                  </button>
                  <button class="fb-btn fb-incorrect" @click="handleFeedback(msg, 'incorrect')">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                    错误
                  </button>
                </template>
                <!-- 总结为案例按钮（仅最后一条AI消息，且未总结过，非流式中，无错误）-->
                <button
                  v-if="idx === messages.length - 1 && !isSummarized && !msg.streaming && !msg.error"
                  class="summarize-btn"
                  @click="confirmSummarize"
                  :disabled="summarizing"
                  title="总结为案例"
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/>
                    <line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/>
                  </svg>
                  {{ summarizing ? '正在总结...' : '总结为案例' }}
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- 相关案例卡片（消息气泡下方） -->
        <div v-if="msg.caseRefs?.length && !msg.streaming" class="case-cards-outer">
          <div class="case-cards-outer-header">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="3" width="20" height="14" rx="2" ry="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>
            相关案例 ({{ msg.caseRefs.length }})
          </div>
          <div class="case-cards-outer-grid">
            <div
              v-for="(ref, ri) in msg.caseRefs"
              :key="ri"
              class="case-card-outer"
              @click="showCaseDetail(ref)"
            >
              <div class="case-card-outer-title">{{ ref.documentName }}</div>
              <div class="case-card-outer-snippet">
                {{ ref.content?.substring(0, 100) }}{{ ref.content?.length > 100 ? '...' : '' }}
              </div>
              <div class="case-card-outer-footer">
                <span class="case-card-outer-score">相关度 {{ (ref.score * 100).toFixed(0) }}%</span>
                <span class="case-card-outer-arrow">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m9 18 6-6-6-6"/></svg>
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 停止按钮 -->
      <div v-if="loading" class="stop-area">
        <button class="stop-btn" @click="stopGeneration">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="6" y="6" width="12" height="12" rx="1"/></svg>
          停止生成
        </button>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="input-area">
      <div class="search-box">
        <!-- 预览行：图片 + 文档（输入框顶部） -->
        <div v-if="previewImages.length || previewDocUrls.length" class="preview-row">
          <div v-for="(img, i) in previewImages" :key="'img-'+i" class="preview-tag">
            <div class="preview-popup">
              <img :src="img" alt="预览" />
            </div>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/>
            </svg>
            <span class="preview-name">{{ previewFileNames[i] || '图片' }}</span>
            <button class="preview-remove" @click="removePreview(i)" aria-label="移除">×</button>
          </div>
          <div v-for="(_doc, i) in previewDocUrls" :key="'doc-'+i" class="preview-tag preview-tag-doc">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14 2 14 8 20 8"/>
            </svg>
            <span class="preview-name">{{ previewDocNames[i] || '附件' }}</span>
            <button class="preview-remove" @click="removeDocPreview(i)" aria-label="移除">×</button>
          </div>
        </div>

        <textarea
          v-model="inputText"
          placeholder="描述故障现象，如：发动机启动后异响..."
          :disabled="loading"
          rows="1"
          class="search-input"
          ref="textInputRef"
          @keydown.enter.exact.prevent="handleSend"
          @input="autoResize"
        ></textarea>

        <!-- 底栏：操作按钮 -->
        <div class="search-bottom">
          <div class="search-actions">
            <label class="upload-btn" title="上传附件文件">
              <input type="file" accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.md,.csv,.rtf,.epub,.mp3,.wav,.aac,.ogg,.m4a,.flac" hidden :disabled="loading" @change="handleDocSelect" />
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/>
              </svg>
            </label>
            <label class="upload-btn" title="上传故障图片">
              <input type="file" accept="image/jpeg,image/png" hidden :disabled="loading" @change="handleFileSelect" />
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/>
              </svg>
            </label>
            <label class="upload-btn audio-btn" title="上传音频文件">
              <input type="file" accept="audio/*" hidden :disabled="loading" @change="handleAudioFileSelected" />
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/><path d="M19 10v2a7 7 0 0 1-14 0v-2"/><line x1="12" y1="19" x2="12" y2="23"/><line x1="8" y1="23" x2="16" y2="23"/>
              </svg>
            </label>
            <label class="upload-btn" title="上传故障视频">
              <input type="file" accept="video/mp4,video/x-msvideo,video/quicktime,video/x-matroska,video/webm" hidden :disabled="loading" @change="handleFileSelect" />
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polygon points="5 3 19 12 5 21 5 3"/>
              </svg>
            </label>
            <button
              class="send-btn"
              :disabled="(!inputText.trim() && !previewImages.length && !previewDocUrls.length) || loading"
              @click="handleSend"
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

    <!-- 引用详情浮层（全局定位） -->
    <Teleport to="body">
      <div
        v-show="citationPopover.visible"
        class="citation-popover"
        :style="{ top: citationPopover.y + 'px', left: citationPopover.x + 'px' }"
        @mouseenter="keepPopoverOpen"
        @mouseleave="hideCitationPopover"
      >
        <div class="citation-popover-header">
          <span :class="['citation-type', citationPopover.type]">
            {{ citationPopover.type === 'case' ? '📁' : '📚' }}
          </span>
          <span class="citation-name">{{ citationPopover.docName }}</span>
          <span v-if="citationPopover.score" class="citation-score-badge">
            {{ (citationPopover.score * 100).toFixed(0) }}%
          </span>
        </div>
        <div class="citation-popover-body">
          <div class="citation-content-text">
            {{ citationPopover.content || '暂无内容摘要' }}
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 图片预览弹窗 -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="imagePreviewVisible" class="modal-overlay image-preview-overlay" @click.self="imagePreviewVisible = false">
          <button class="image-preview-close" @click="imagePreviewVisible = false" aria-label="关闭预览">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
          <img :src="imagePreviewSrc" class="image-preview-img" @click.stop alt="预览图片" />
        </div>
      </Transition>
    </Teleport>

    <!-- 案例详情弹窗（自定义 Modal，参考 /user/case 风格） -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="caseDialogVisible" class="modal-overlay" @click.self="caseDialogVisible = false">
          <div class="modal-content">
            <div class="modal-header">
              <h3>案例详情</h3>
              <button class="modal-close" @click="caseDialogVisible = false">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
            <div v-if="selectedCase" class="modal-body">
              <div class="detail-grid">
                <div class="detail-field">
                  <span class="detail-label">案例标题</span>
                  <span class="detail-value">{{ selectedCase.title || '未命名' }}</span>
                </div>
                <div class="detail-field">
                  <span class="detail-label">设备型号</span>
                  <span class="detail-value">
                    <span class="detail-tag">{{ selectedCase.equipModel || '-' }}</span>
                  </span>
                </div>
                <div v-if="selectedCase.createTime" class="detail-field">
                  <span class="detail-label">创建时间</span>
                  <span class="detail-value">{{ formatDateTime(selectedCase.createTime) }}</span>
                </div>
              </div>
              <div class="detail-content-section">
                <h4>故障描述</h4>
                <div class="detail-content">{{ selectedCase.symptom || '暂无描述' }}</div>
              </div>
              <div class="detail-content-section">
                <h4>解决方案</h4>
                <div class="detail-content solution-section">{{ selectedCase.solution || '暂无方案' }}</div>
              </div>
              <div v-if="selectedCase.imgUrl" class="detail-image">
                <h4>附图</h4>
                <img :src="selectedCase.imgUrl" alt="案例图片" />
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 总结案例预览对话框 -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="summaryDialogVisible" class="modal-overlay" @click.self="cancelSummary">
          <div class="modal-content" style="max-width: 700px;">
            <div class="modal-header">
              <h3>案例总结预览</h3>
              <button class="modal-close" @click="cancelSummary">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
            <div class="modal-body">
              <div style="margin-bottom: var(--space-4); font-size: var(--text-xs); color: var(--text-tertiary);">
                以下是AI根据对话总结的案例，您可以编辑修改后再提交审核。
              </div>
              <div class="detail-grid">
                <div class="detail-field" style="grid-column: 1 / -1;">
                  <span class="detail-label">案例标题 <span style="color: var(--color-danger);">*</span></span>
                  <input
                    v-model="summarizedCase.title"
                    class="summary-input"
                    placeholder="请输入案例标题（不超过30字）"
                    maxlength="30"
                  />
                </div>
                <div class="detail-field">
                  <span class="detail-label">设备型号</span>
                  <input
                    v-model="summarizedCase.equipModel"
                    class="summary-input"
                    placeholder="未识别可留空"
                  />
                </div>
              </div>
              <div class="detail-content-section">
                <h4>故障描述 <span style="color: var(--color-danger); font-size: var(--text-xs);">*</span></h4>
                <textarea
                  v-model="summarizedCase.symptom"
                  class="summary-textarea"
                  placeholder="请输入故障描述"
                  rows="5"
                ></textarea>
              </div>
              <div class="detail-content-section">
                <h4>解决方案 <span style="color: var(--color-danger); font-size: var(--text-xs);">*</span></h4>
                <textarea
                  v-model="summarizedCase.solution"
                  class="summary-textarea"
                  placeholder="请输入解决方案"
                  rows="6"
                ></textarea>
              </div>
              <div v-if="summarizedCase.images.length" class="detail-content-section">
                <h4>附图 ({{ summarizedCase.images.length }}张)</h4>
                <div style="display: flex; gap: 8px; flex-wrap: wrap;">
                  <img
                    v-for="(img, i) in summarizedCase.images"
                    :key="i"
                    :src="img"
                    style="width: 80px; height: 80px; object-fit: cover; border-radius: 6px; border: 1px solid var(--border-light);"
                  />
                </div>
              </div>
            </div>
            <div style="display: flex; justify-content: flex-end; gap: 12px; padding: var(--space-4) var(--space-6); border-top: 1px solid var(--border-light);">
              <button class="summarize-cancel-btn" @click="cancelSummary">取消上传</button>
              <button class="summarize-submit-btn" :disabled="uploadCaseLoading" @click="submitSummary">
                {{ uploadCaseLoading ? '提交中...' : '上传案例' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted, watch, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { renderMarkdown, renderMarkdownStreaming } from '@/utils/markdown'
import { uploadFile, uploadChatFile, getChatConversationDetail, getCasesByDifyDocIds, submitChatFeedback, summarizeToCase, uploadKnowledge, audioToText } from '@/api'
import { fetchCaseDatasetIds, getCaseDatasetIds } from '@/utils/caseDatasetIds'

const router = useRouter()
const route = useRoute()

// 案例库 dataset ID 白名单（从后端 dify.dataset.visible-ids.case-audit 动态获取）
const CASE_DATASET_IDS = ref<string[]>([])

// 加载案例库 ID
const loadCaseDatasetIds = async () => {
  CASE_DATASET_IDS.value = await fetchCaseDatasetIds()
}

// ============ 引用解析类型 ============
interface ParsedRef {
  type: 'knowledge' | 'case'
  docId: string
  segId: string
  score: number
  index: number
  fullMatch: string
}

interface RefDetail extends ParsedRef {
  documentName: string
  datasetId: string
  content: string
}

interface ChatMsg {
  question: string
  equipModel?: string
  images: string[]
  files?: string[]       // 附件文档 URL
  fileNames?: string[]    // 附件文档名
  fileTypes?: string[]    // 附件文档类型(image/audio/document/video)
  answer: string
  streaming: boolean
  error?: string
  resources: any[]
  knowledgeRefs?: RefDetail[]
  caseRefs?: RefDetail[]
  _rawAnswer?: string  // 原始回答（含引用标记），用于 done 时重新解析
  chatHistoryId?: number     // 用于反馈提交
  feedbackStatus?: string    // 反馈状态: '' | 'correct' | 'incorrect'
}

const messages = ref<ChatMsg[]>([])
const inputText = ref('')
const loading = ref(false)
const previewImages = ref<string[]>([])
const previewFileNames = ref<string[]>([])
const previewDocUrls = ref<string[]>([])     // 附件文档URL
const previewDocNames = ref<string[]>([])     // 附件文档名
const previewDocTypes = ref<string[]>([])     // 附件文档类型(image/audio/document/video)
const msgArea = ref<HTMLElement | null>(null)
const textInputRef = ref<HTMLTextAreaElement | null>(null)

// 对话ID管理
const conversationId = ref<string>('')
const conversationTitle = computed(() => {
  if (!conversationId.value && messages.value.length === 0) return '新会话'
  const firstUser = messages.value.find(m => m.question && !m.question.startsWith('['))
  return firstUser?.question?.substring(0, 30) || '智能故障诊断'
})

let abortController: AbortController | null = null

// ============ 语音转文字 ============
const transcribing = ref(false)

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
    inputText.value = (inputText.value ? inputText.value + '\n' : '') + text
    nextTick(() => autoResize())
    ElMessage.success('语音识别完成，已填入输入框')
  } catch (error: any) {
    console.error('语音转文字失败', error)
    ElMessage.error('语音转文字失败：' + (error?.message || '请重试'))
  } finally {
    loadingMsg?.close()
    transcribing.value = false
  }
}

// 从 route query 读取 conversationId
const initConversationId = () => {
  const cid = route.query.conversationId as string
  if (cid && cid !== conversationId.value) {
    conversationId.value = cid
    loadConversationHistory(cid)
  } else if (!cid) {
    conversationId.value = ''
    messages.value = []
  }
}

// 加载已有对话的历史记录
const loadConversationHistory = async (cid: string) => {
  try {
    const res: any = await getChatConversationDetail(cid)
    const data = res.data
    if (data && Array.isArray(data)) {
      // 每条 ChatMessageVO 是一轮问答，合并到一个 msg 对象
      const msgs: ChatMsg[] = data.map((item: any) => {
        const answer = item.answerContent || ''
        const resources = item.resources || []

        // 方案1：从文本解析引用标记做行内图标替换
        const refs = parseReferences(answer, resources)
        const { processedMarkdown, knowledgeRefs: textKnowledge, caseRefs: textCase } = processReferences(answer, refs, resources)

        // 方案2：从 resources 直接构建引用（兜底）
        const directRefs = buildRefsFromResources(resources)

        const finalKnowledge = textKnowledge.length > 0 ? textKnowledge : directRefs.knowledgeRefs
        const finalCase = textCase.length > 0 ? textCase : directRefs.caseRefs

        // 从后端返回的 files 恢复附件信息
        const apiFiles: any[] = item.files || []
        const imageUrls: string[] = []
        const fileUrls: string[] = []
        const fileNames: string[] = []
        const fileTypes: string[] = []
        for (const f of apiFiles) {
          const fp = f.filePath || ''
          const ft = f.fileType || 'document'
          const fn = f.fileName || '附件'
          if (ft === 'image') {
            imageUrls.push(fp)
          } else {
            fileUrls.push(fp)
            fileNames.push(fn)
            fileTypes.push(ft)
          }
        }

        console.log(`[History] round ${item.roundNum} textKnowledge:`, textKnowledge.length, 'directKnowledge:', directRefs.knowledgeRefs.length, 'textCase:', textCase.length, 'directCase:', directRefs.caseRefs.length)

        return {
          question: item.userQuestion || '',
          equipModel: item.deviceModel || '',
          images: imageUrls,
          files: fileUrls,
          fileNames: fileNames,
          fileTypes: fileTypes,
          answer: insertCitationIcons(renderMd(processedMarkdown), [...textKnowledge, ...textCase]),
          streaming: false,
          resources,
          knowledgeRefs: finalKnowledge,
          caseRefs: finalCase,
          chatHistoryId: item.id || undefined,
          feedbackStatus: item.feedbackStatus || 'pending'
        }
      })
      messages.value = msgs
      // 为历史消息批量查询数据库中的案例标题
      for (const msg of messages.value) {
        if (msg.caseRefs?.length) {
          fetchCaseTitles(msg.caseRefs, msg)
        }
      }
      nextTick(() => scrollToBottom())
    }
  } catch {
    ElMessage.warning('加载对话历史失败')
  }
}

// ============ 参考来源去重 ============
// 同一文档多条分段只保留匹配度最高的一条
const getDedupedResources = (msg: ChatMsg) => {
  const resources = msg.resources
  if (!resources?.length) return []

  const deduped = new Map<string, any>()
  for (const r of resources) {
    const key = r.documentId || r.documentName || r.datasetId || ''
    const existing = deduped.get(key)
    if (!existing || (r.score || 0) > (existing.score || 0)) {
      deduped.set(key, r)
    }
  }
  return [...deduped.values()]
}

// 更新 URL 中的 conversationId
const updateConversationUrl = (cid: string) => {
  if (cid && route.query.conversationId !== cid) {
    router.replace({ query: { conversationId: cid } })
  }
}

const renderMd = renderMarkdown
const renderMdStreaming = renderMarkdownStreaming

/**
 * 从 Dify metadata resources 直接构建引用详情
 * 不依赖文本解析，始终可靠
 */
const buildRefsFromResources = (
  resources: any[]
): { knowledgeRefs: RefDetail[]; caseRefs: RefDetail[] } => {
  const knowledgeRefs: RefDetail[] = []
  const caseRefs: RefDetail[] = []
  const seenDocs = new Set<string>()

  for (const r of resources) {
    const docId = r.documentId || ''
    if (!docId || seenDocs.has(docId)) continue
    seenDocs.add(docId)

    const isCase = CASE_DATASET_IDS.value.includes(r.datasetId || '')
    const detail: RefDetail = {
      type: isCase ? 'case' : 'knowledge',
      docId, segId: r.segmentId || '', score: r.score || 0,
      index: 0, fullMatch: '',
      documentName: r.documentName || '未知文档',
      datasetId: r.datasetId || '', content: r.content || ''
    }
    if (isCase) caseRefs.push(detail)
    else knowledgeRefs.push(detail)
  }
  knowledgeRefs.forEach((r, i) => r.index = i + 1)
  caseRefs.forEach((r, i) => r.index = i + 1)
  return { knowledgeRefs, caseRefs }
}

// ============ 流式过程中过滤引用标记 ============
const stripCitations = (text: string): string => {
  let cleaned = text
  // 1. 移除完整的 [ref:knowledge:doc_id:xxx,seg_id:yyy] 标记
  cleaned = cleaned.replace(
    /\[ref:(knowledge|case)\s*:\s*doc_id\s*:\s*([a-f0-9-]+)\s*(?:,\s*seg_id\s*:\s*([a-f0-9-]+))?\s*(?:,\s*score\s*:\s*([\d.]+))?\s*\]/g,
    ''
  )
  // 2. 移除 [N] 格式（排除图片语法 ![N]）
  cleaned = cleaned.replace(/(?<!!)\[(\d+)\]/g, '')
  // 3. 流式截断保护：移除末尾不完整的 [ref:... （未闭合 ]），
  //    防止流式输出时 Dify 分片导致半截标记显示在页面上。
  //    完整标记已在上方被移除，此处只处理末尾未闭合的残留。
  cleaned = cleaned.replace(/\[ref:(knowledge|case)[^\]]*$/, '')
  return cleaned
}

// ============ 引用解析 ============
const parseReferences = (text: string, resources: any[] = []): ParsedRef[] => {
  // 格式1: [ref:knowledge:doc_id:xxx,seg_id:yyy] (新格式，可能含空格)
  // 格式1b: [ref:knowledge:doc_id:xxx,seg_id:yyy,score:0.85]
  // 格式2: [1],[2]... Dify 标准引用（resources 顺序对应）
  const refs: ParsedRef[] = []
  const matchedPositions = new Set<number>()  // 记录已匹配位置，避免重复

  // 1. 先解析 [ref:...] 自定义格式（seg_id 可选，兼容空格）
  const customRegex = /\[ref:(knowledge|case)\s*:\s*doc_id\s*:\s*([a-f0-9-]+)\s*(?:,\s*seg_id\s*:\s*([a-f0-9-]+))?\s*(?:,\s*score\s*:\s*([\d.]+))?\s*\]/g
  let match
  let index = 0
  while ((match = customRegex.exec(text)) !== null) {
    // 记录该引用标记在文本中的起止位置
    for (let i = match.index; i < match.index + match[0].length; i++) {
      matchedPositions.add(i)
    }
    refs.push({
      type: match[1] as 'knowledge' | 'case',
      docId: match[2],
      segId: match[3] || '',         // seg_id 可能为空（新格式）
      score: match[4] ? parseFloat(match[4]) : 0,
      index: ++index,
      fullMatch: match[0]
    })
  }

  // 2. 再解析 [N] 标准格式（只取 1~resources.length 范围内的数字，且不在 [ref:...] 内部）
  if (resources.length > 0) {
    const numberRegex = /(?<!!)\[(\d+)\]/g
    while ((match = numberRegex.exec(text)) !== null) {
      const n = parseInt(match[1])
      if (n < 1 || n > resources.length) continue
      // 检查位置是否已被 [ref:...] 覆盖
      if (matchedPositions.has(match.index)) continue
      const res = resources[n - 1]
      const docId = res.documentId || ''
      const isCase = CASE_DATASET_IDS.value.includes(res.datasetId || '')
      refs.push({
        type: isCase ? 'case' : 'knowledge',
        docId: docId,
        segId: res.segmentId || '',
        score: res.score || 0,
        index: ++index,
        fullMatch: match[0]
      })
    }
  }

  return refs
}

const processReferences = (
  answer: string,
  refs: ParsedRef[],
  resources: any[]
): { processedMarkdown: string; knowledgeRefs: RefDetail[]; caseRefs: RefDetail[] } => {

  const findResource = (docId: string, segId: string) => {
    return resources.find((r: any) => r.documentId === docId && r.segmentId === segId) ||
           resources.find((r: any) => r.documentId === docId) ||
           // 兼容 snake_case 字段名（Dify metadata 可能使用不同格式）
           resources.find((r: any) => r.document_id === docId && r.segment_id === segId) ||
           resources.find((r: any) => r.document_id === docId) ||
           {}
  }

  const knowledgeRefs: RefDetail[] = []
  const caseRefs: RefDetail[] = []

  for (const ref of refs) {
    const res = findResource(ref.docId, ref.segId) as any
    const detail: RefDetail = {
      ...ref,
      documentName: res.documentName || '未知文档',
      datasetId: res.datasetId || '',
      content: res.content || '',
      score: res.score || ref.score || 0
    }
    // 调试：记录匹配失败的引用
    if (!res.documentName && ref.type === 'knowledge') {
      console.warn(`[processReferences] 知识库引用匹配失败: docId=${ref.docId}, segId=${ref.segId}, resources数组长度=${resources.length}`)
    }

    const isCase = CASE_DATASET_IDS.value.includes(res.datasetId) || ref.type === 'case'
    if (isCase) caseRefs.push(detail)
    else knowledgeRefs.push(detail)
  }

  // 去重（同 docId + segId 只保留一个）
  const unique = (arr: RefDetail[]) => arr.filter((ref, idx, self) =>
    self.findIndex(r => r.docId === ref.docId && r.segId === ref.segId) === idx
  )
  const uniqueKnowledge = unique(knowledgeRefs)
  const uniqueCase = unique(caseRefs)
  uniqueKnowledge.forEach((r, i) => r.index = i + 1)
  uniqueCase.forEach((r, i) => r.index = i + 1)

  // 替换引用标记为唯一占位符（不含 HTML，待 markdown 渲染后再插入 <sup> 图标）
  // 使用 compound key 避免 knowledge/case 的 index 冲突
  let result = answer
  const allRefs = [...uniqueKnowledge, ...uniqueCase]

  for (const ref of allRefs) {
    const ph = `\u00ACCITE_${ref.type}_${ref.index}\u00AC`
    result = result.split(ref.fullMatch).join(ph)
  }

  // 清理残留的裸 [N] 标记（LLM 自行生成的引用编号，非 Dify 资源）
  // 排除图片语法 ![N]，此时尚无 <sup> 标签，无需保护
  result = result.replace(/(?<!!)\[(\d+)\]/g, '')

  // 清理未被替换的 [ref:case:...] / [ref:knowledge:...] 原始标记
  result = result.replace(
    /\[ref:(knowledge|case)\s*:\s*doc_id\s*:\s*[a-f0-9-]+\s*(?:,\s*seg_id\s*:\s*[a-f0-9-]+)?\s*(?:,\s*score\s*:\s*[\d.]+)?\s*\]/g,
    ''
  )

  return { processedMarkdown: result, knowledgeRefs: uniqueKnowledge, caseRefs: uniqueCase }
}

// ============ 在渲染后的 HTML 中插入引用图标 ============
const insertCitationIcons = (html: string, allRefs: RefDetail[]): string => {
  let result = html
  for (const ref of allRefs) {
    const ph = `\u00ACCITE_${ref.type}_${ref.index}\u00AC`
    if (ref.type === 'case') {
      result = result.split(ph).join('')
    } else {
      const iconHtml = `<sup class="citation-icon knowledge-ref" data-index="${ref.index}" data-type="${ref.type}" data-doc-id="${ref.docId}" data-seg-id="${ref.segId}">&#x24D8;</sup>`
      result = result.split(ph).join(iconHtml)
    }
  }
  return result
}

// ============ 通过数据库查询案例标题 ============
const fetchCaseTitles = async (caseRefs: RefDetail[], reactiveMsg: ChatMsg) => {
  try {
    const docIds = [...new Set(caseRefs.map(r => r.docId).filter(Boolean))]
    if (!docIds.length) return

    const res: any = await getCasesByDifyDocIds(docIds)
    const cases: any[] = res?.data || res || []

    console.log('[fetchCaseTitles] 请求 docIds:', docIds)
    console.log('[fetchCaseTitles] 返回数据:', cases)

    // 构建 docId → title 映射
    const titleMap: Record<string, string> = {}
    for (const c of cases) {
      const docId = c.documentId || ''
      if (docId && c.title) {
        titleMap[docId] = c.title
      }
    }
    console.log('[fetchCaseTitles] 标题映射:', titleMap)

    // 更新 caseRefs 中的 documentName 为数据库中的案例标题
    for (const ref of caseRefs) {
      if (titleMap[ref.docId]) {
        ref.documentName = titleMap[ref.docId]
      }
    }

    // 同步更新 resources 中的 documentName
    if (reactiveMsg.resources?.length) {
      for (const resItem of reactiveMsg.resources) {
        if (titleMap[resItem.documentId]) {
          resItem.documentName = titleMap[resItem.documentId]
        }
      }
    }

    // 强制触发 Vue 响应式更新（重新赋值数组）
    reactiveMsg.caseRefs = [...caseRefs]
    if (reactiveMsg.resources?.length) {
      reactiveMsg.resources = [...reactiveMsg.resources]
    }
  } catch (e) {
    console.warn('[fetchCaseTitles] 查询失败:', e)
  }
}

// ============ 引用浮层 ============
const citationPopover = reactive({
  visible: false,
  x: 0,
  y: 0,
  docName: '',
  content: '',
  type: 'knowledge' as string,
  score: 0,
  timer: null as any
})

const showCitationPopover = (e: MouseEvent, refData: RefDetail) => {
  // 使用实际被 hover 的元素位置（事件委托下 e.currentTarget 是容器，需用 e.target）
  const el = (e.target as HTMLElement).closest('.citation-icon') as HTMLElement || e.target as HTMLElement
  const rect = el.getBoundingClientRect()
  citationPopover.x = rect.left + rect.width / 2
  citationPopover.y = rect.bottom + 6
  citationPopover.docName = refData.documentName
  citationPopover.content = refData.content
  citationPopover.type = refData.type
  citationPopover.score = refData.score
  citationPopover.visible = true
}

const keepPopoverOpen = () => {
  if (citationPopover.timer) clearTimeout(citationPopover.timer)
}

const hideCitationPopover = () => {
  citationPopover.timer = setTimeout(() => {
    citationPopover.visible = false
  }, 150)
}

// ============ 总结为案例功能 ============
const isSummarized = ref(false)    // 当前会话是否已总结
const summarizing = ref(false)    // 总结进行中
const summaryDialogVisible = ref(false)
const uploadCaseLoading = ref(false)
const summarizedCase = reactive({
  title: '',
  equipModel: '',
  symptom: '',
  solution: '',
  images: [] as string[]
})

const stripHtml = (html: string): string => {
  if (!html) return ''
  const tmp = document.createElement('div')
  tmp.innerHTML = html
  return tmp.textContent || tmp.innerText || ''
}

const confirmSummarize = async () => {
  if (isSummarized.value) return

  try {
    await ElMessageBox.confirm(
      '本次会话仅能进行一次总结，是否开始总结？',
      '总结为案例',
      {
        confirmButtonText: '是',
        cancelButtonText: '否',
        type: 'warning',
        closeOnClickModal: false
      }
    )
  } catch {
    // 用户点"否"或关闭弹窗 → 直接返回
    return
  }

  // 用户点"是"，开始总结
  summarizing.value = true
  isSummarized.value = true
  try {
    // 拼接 Q/A 对话内容
    const content = messages.value
      .map(m => `问：${m.question}\n答：${stripHtml(m.answer).substring(0, 800)}`)
      .join('\n\n')

    const res: any = await summarizeToCase({ conversationContent: content })
    const data = res.data || {}

    // 收集所有历史图片
    const allImages: string[] = []
    for (const m of messages.value) {
      if (m.images && m.images.length) {
        allImages.push(...m.images)
      }
    }

    // 填充预览表单
    summarizedCase.title = data.title || ''
    summarizedCase.equipModel = data.equipModel || ''
    summarizedCase.symptom = data.symptom || ''
    summarizedCase.solution = data.solution || ''
    summarizedCase.images = allImages

    summaryDialogVisible.value = true
  } catch (e: any) {
    ElMessage.error('总结失败: ' + (e?.message || '请重试'))
  } finally {
    summarizing.value = false
  }
}

const submitSummary = async () => {
  if (!summarizedCase.title?.trim() || !summarizedCase.symptom?.trim() || !summarizedCase.solution?.trim()) {
    ElMessage.warning('请完善案例标题、故障描述和解决方案')
    return
  }
  uploadCaseLoading.value = true
  try {
    await uploadKnowledge({
      title: summarizedCase.title,
      symptom: summarizedCase.symptom,
      solution: summarizedCase.solution,
      equipModel: summarizedCase.equipModel,
      images: summarizedCase.images.join(',')
    })
    ElMessage.success('案例已提交，等待审核')
    summaryDialogVisible.value = false
  } catch {
    ElMessage.error('提交失败')
  } finally {
    uploadCaseLoading.value = false
  }
}

const cancelSummary = () => {
  summaryDialogVisible.value = false
  // isSummarized 保持 true，不再允许再次总结
}

// ============ 案例卡片与详情弹窗 ============
const caseDialogVisible = ref(false)
const caseDialogLoading = ref(false)
const selectedCase = ref<any>(null)

// 图片预览
const imagePreviewVisible = ref(false)
const imagePreviewSrc = ref('')

const showCaseDetail = async (refData: RefDetail) => {
  caseDialogLoading.value = true
  try {
    const res: any = await getCasesByDifyDocIds([refData.docId])
    const cases = res.data || []
    if (cases.length > 0) {
      selectedCase.value = cases[0]
      caseDialogVisible.value = true
    } else {
      ElMessage.info('该案例暂无完整详情，仅显示摘要信息')
    }
  } catch {
    ElMessage.error('获取案例详情失败')
  } finally {
    caseDialogLoading.value = false
  }
}

const findRefFromElement = (target: HTMLElement): RefDetail | undefined => {
  const icon = target.closest('.citation-icon') as HTMLElement
  if (!icon) return undefined
  const type = icon.dataset.type
  const docId = icon.dataset.docId
  const segId = icon.dataset.segId
  // 从所有消息中查找匹配的引用（修复之前只查最后一条的Bug）
  for (let i = messages.value.length - 1; i >= 0; i--) {
    const msg = messages.value[i]
    const refs = type === 'case' ? msg.caseRefs : msg.knowledgeRefs
    const found = refs?.find((r: RefDetail) => r.docId === docId && r.segId === segId)
    if (found) return found
  }
  return undefined
}

const handleCitationClick = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  // 点击图片使用弹窗预览（避免新标签页打开代理路径失败）
  if (target.tagName === 'IMG') {
    const src = (target as HTMLImageElement).src
    if (src) {
      imagePreviewSrc.value = src
      imagePreviewVisible.value = true
    }
    return
  }
  const matched = findRefFromElement(target)
  if (matched) showCitationPopover(e, matched)
}

const handleCitationHover = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  const matched = findRefFromElement(target)
  if (matched) showCitationPopover(e, matched)
}

const handleCitationLeave = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  if (!target.closest('.citation-icon')) return
  hideCitationPopover()
}

const scrollToBottom = () => {
  nextTick(() => {
    if (msgArea.value) msgArea.value.scrollTop = msgArea.value.scrollHeight
  })
}

const autoResize = () => {
  const el = textInputRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 120) + 'px'
}

const handleFileSelect = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  const isVideo = file.type.startsWith('video/')
  const maxSize = isVideo ? 50 * 1024 * 1024 : 10 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.warning(isVideo ? '视频大小不能超过 50MB' : '图片大小不能超过 10MB')
    input.value = ''
    return
  }
  try {
    const res: any = await uploadFile(file)
    const data = res?.data || {}
    if (Array.isArray(data.urls) && data.urls.length) {
      // 视频关键帧提取结果：后端返回4帧，取后3帧使用（跳过第1帧）
      const frameUrls: string[] = data.urls
      const frameNames: string[] = data.names || []
      const useUrls = frameUrls.slice(1)  // 跳过第1帧，取后3帧
      const useNames = frameNames.slice(1)

      useUrls.forEach((url: string, idx: number) => {
        previewImages.value.push(url)
        previewFileNames.value.push(useNames[idx] || `frame_${idx + 2}.jpg`)
      })

      // 从帧URL反推视频URL，将视频文件添加到对话附件中
      // 帧URL: /uploads/videos/frames/yyyy/MM/dd/uuid_frame_1.jpg
      // 视频URL: /uploads/yyyy/MM/dd/uuid.ext
      const firstFrame: string = frameUrls[0]
      const ext = file.name.substring(file.name.lastIndexOf('.'))
      const dateMatch = firstFrame.match(/\/uploads\/videos\/frames\/(\d{4}\/\d{2}\/\d{2})\//)
      const nameMatch = firstFrame.match(/\/([^/]+)_frame_\d+\.jpg$/)
      if (dateMatch && nameMatch) {
        const videoUrl = `/uploads/${dateMatch[1]}/${nameMatch[1]}${ext}`
        previewDocUrls.value.push(videoUrl)
        previewDocNames.value.push(file.name)
        previewDocTypes.value.push('video')
      }

      ElMessage.success(`视频截帧成功，已提取 ${useUrls.length} 张关键帧`)
    } else {
      // 普通图片
      const url = data.url || ''
      if (url) {
        previewImages.value.push(url)
        previewFileNames.value.push(file.name)
        ElMessage.success('图片上传成功')
      }
    }
  } catch {
    ElMessage.error(isVideo ? '视频上传失败' : '图片上传失败')
  }
  input.value = ''
}

const removePreview = (i: number) => {
  previewImages.value.splice(i, 1)
  previewFileNames.value.splice(i, 1)
}

const handleDocSelect = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (file.size > 50 * 1024 * 1024) {
    ElMessage.warning('文件大小不能超过 50MB')
    input.value = ''
    return
  }
  try {
    const res: any = await uploadChatFile(file)
    const url = res.data?.url || ''
    if (url) {
      previewDocUrls.value.push(url)
      previewDocNames.value.push(file.name)
      previewDocTypes.value.push(res.data?.fileType || 'document')
      ElMessage.success('附件上传成功')
    }
  } catch {
    ElMessage.error('附件上传失败')
  }
  input.value = ''
}

const removeDocPreview = (i: number) => {
  previewDocUrls.value.splice(i, 1)
  previewDocNames.value.splice(i, 1)
  previewDocTypes.value.splice(i, 1)
}

const quickStart = (text: string) => {
  inputText.value = text
  handleSend()
}

const retryMessage = (idx: number) => {
  const msg = messages.value[idx]
  if (!msg) return
  msg.error = undefined
  msg.answer = ''
  msg.streaming = true
  doSend(msg.question, msg.images, msg.images.map(() => ''), msg.files || [], msg.fileNames || [], msg.fileTypes || [])
}

const handleSend = async () => {
  const text = inputText.value.trim()
  if (!text && !previewImages.value.length && !previewDocUrls.value.length) return
  if (loading.value) return
  // 默认问题文本
  let q = text
  if (!q && previewDocUrls.value.length && previewDocNames.value.length) {
    q = '[附件文件: ' + previewDocNames.value.join(', ') + ']'
  } else if (!q) {
    q = '[图片故障诊断]'
  }
  const imgs = [...previewImages.value]
  const imgNames = [...previewFileNames.value]
  const docs = [...previewDocUrls.value]
  const docNames = [...previewDocNames.value]
  const docTypes = [...previewDocTypes.value]
  inputText.value = ''
  previewImages.value = []
  previewFileNames.value = []
  previewDocUrls.value = []
  previewDocNames.value = []
  previewDocTypes.value = []
  nextTick(() => autoResize())
  doSend(q, imgs, imgNames, docs, docNames, docTypes)
}

const startNewChat = () => {
  conversationId.value = ''
  messages.value = []
  router.replace({ query: {} })
  inputText.value = ''
  previewImages.value = []
  previewFileNames.value = []
  previewDocUrls.value = []
  previewDocNames.value = []
  previewDocTypes.value = []
  isSummarized.value = false
}

const doSend = async (question: string, images: string[] = [], imgNames: string[] = [], docs: string[] = [], docNames: string[] = [], docTypes: string[] = []) => {
  const msg: ChatMsg = {
    question,
    images: [...images],
    files: [...docs],
    fileNames: [...docNames],
    fileTypes: [...docTypes],
    answer: '',
    streaming: true,
    resources: []
  }
  messages.value.push(msg)
  // 从响应式数组中取出 Proxy 代理对象，后续通过它修改才能触发 Vue 响应式更新
  const reactiveMsg = messages.value[messages.value.length - 1]

  loading.value = true
  scrollToBottom()

  const token = localStorage.getItem('token')
  abortController = new AbortController()

  try {
    const body: any = {
      query: question
    }
    // 携带 conversationId 实现多轮对话
    if (conversationId.value) {
      body.conversationId = conversationId.value
    }
    // 合并图片和文档URL（过滤掉视频类型，视频只在前端显示，不发送给AI）
    const allFileUrls: string[] = [...images]
    const allFileNames: string[] = [...imgNames]
    const allFileTypes: string[] = [...images.map(() => 'image')]
    for (let i = 0; i < docs.length; i++) {
      if (docTypes[i] !== 'video') {
        allFileUrls.push(docs[i])
        allFileNames.push(docNames[i])
        allFileTypes.push(docTypes[i])
      }
    }
    if (allFileUrls.length) {
      body.fileUrls = allFileUrls
      body.fileTypes = allFileTypes
      body.fileNames = allFileNames
    }

    const resp = await fetch('/api/chat/send', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(body),
      signal: abortController.signal
    })

    if (!resp.ok) throw new Error(`请求失败 (${resp.status})`)

    const reader = resp.body?.getReader()
    if (!reader) return
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const blocks = buffer.split('\n\n')
      buffer = blocks.pop() || ''

      for (const block of blocks) {
        if (!block.trim()) continue
        const fields = block.split('\n')
        let evt = '', data = ''
        for (const f of fields) {
          if (f.startsWith('event:')) evt = f.substring(6).trim()
          else if (f.startsWith('data:')) {
            if (data) data += '\n'
            data += f.substring(5)
          }
        }
        if (evt === 'message') {
          reactiveMsg._rawAnswer = (reactiveMsg._rawAnswer || '') + data
          // 流式过程中过滤引用标记，避免显示 [ref:...] 和 [1][2] 等原始标记
          reactiveMsg.answer = renderMdStreaming(stripCitations(reactiveMsg._rawAnswer))
          scrollToBottom()
        } else if (evt === 'done') {
          reactiveMsg.streaming = false
          try {
            const parsed = JSON.parse(data)
            // 优先用流式累积的 _rawAnswer（含完整 [ref:...] 标记）进行引用解析
            // parsed.answer 可能已被 Dify 清理掉引用标记
            const rawAnswer = reactiveMsg._rawAnswer || parsed.answer || ''
            const resources = parsed.resources || []

            // 调试日志：查看从 SSE done 事件中获取的 resources 数据
            console.log('[SSE done] resources count:', resources.length)
            if (resources.length > 0) {
              resources.forEach((r: any, i: number) => {
                console.log(`[SSE done] resource[${i}]:`, { documentId: r.documentId, segmentId: r.segmentId, documentName: r.documentName, datasetId: r.datasetId })
              })
            }

            // 方案1：从文本解析引用标记做行内图标替换（正则匹配 [ref:...]）
            const refs = parseReferences(rawAnswer, resources)
            console.log('[SSE done] parsed refs from text:', refs.length, refs.map((r: any) => ({ type: r.type, docId: r.docId, segId: r.segId })))
            const { processedMarkdown, knowledgeRefs: textKnowledge, caseRefs: textCase } = processReferences(
              rawAnswer, refs, resources
            )

            // 方案2：从 resources 直接构建引用（可靠兜底，在方案1为空时使用）
            const directRefs = buildRefsFromResources(resources)

            // 最终使用：方案1优先（有行内图标），方案2兜底
            const finalKnowledge = textKnowledge.length > 0 ? textKnowledge : directRefs.knowledgeRefs
            const finalCase = textCase.length > 0 ? textCase : directRefs.caseRefs

            reactiveMsg.answer = insertCitationIcons(renderMd(processedMarkdown), [...textKnowledge, ...textCase])
            reactiveMsg.resources = parsed.resources || []
            reactiveMsg.knowledgeRefs = finalKnowledge
            reactiveMsg.caseRefs = finalCase

            // 通过数据库查询案例标题（替换 Dify metadata 中的文档名）
            if (finalCase.length) {
              fetchCaseTitles(finalCase, reactiveMsg)
            }

            // 保存 chatHistoryId 用于反馈
            reactiveMsg.chatHistoryId = parsed.chatHistoryId
            reactiveMsg.feedbackStatus = 'pending'

            // 保存 Dify 返回的 conversationId
            if (parsed.conversationId && !conversationId.value) {
              conversationId.value = parsed.conversationId
              updateConversationUrl(parsed.conversationId)
            }
          } catch { reactiveMsg.streaming = false }
        } else if (evt === 'error') {
          reactiveMsg.error = data || '服务异常'
          reactiveMsg.streaming = false
        }
      }
    }
    if (reactiveMsg.streaming) reactiveMsg.streaming = false
  } catch (e: any) {
    if (e.name !== 'AbortError') {
      reactiveMsg.error = '请求失败，请检查网络后重试'
      reactiveMsg.streaming = false
    }
  } finally {
    loading.value = false
    abortController = null
    scrollToBottom()
  }
}

const stopGeneration = () => {
  if (abortController) {
    abortController.abort()
    abortController = null
  }
  loading.value = false
  const last = messages.value[messages.value.length - 1]
  if (last && last.streaming) {
    last.streaming = false
  }
}

const handleFeedback = async (msg: ChatMsg, status: string) => {
  if (!msg.chatHistoryId) {
    ElMessage.warning('无法提交反馈：缺少消息记录')
    return
  }
  try {
    await submitChatFeedback({
      chatHistoryId: msg.chatHistoryId,
      feedbackStatus: status
    })
    msg.feedbackStatus = status
    ElMessage.success('反馈提交成功')
  } catch {
    ElMessage.error('反馈提交失败')
  }
}

const previewImage = (url: string) => {
  window.open(url, '_blank')
}

const formatDateTime = (time: string) => {
  if (!time) return '-'
  return time.substring(0, 16).replace('T', ' ')
}

// 监听路由变化（侧边栏点击其他会话时）
watch(() => route.query.conversationId, () => {
  initConversationId()
})

onMounted(() => {
  // 动态加载案例库 dataset ID
  loadCaseDatasetIds()

  // 从首页带来的搜索文本
  const saved = localStorage.getItem('user_search_text')
  if (saved) {
    inputText.value = saved
    localStorage.removeItem('user_search_text')

    const savedImg = localStorage.getItem('user_search_img')
    if (savedImg) {
      try {
        const urls = JSON.parse(savedImg)
        if (Array.isArray(urls)) {
          urls.forEach((url: string, i: number) => {
            previewImages.value.push(url)
            previewFileNames.value.push('图片 ' + (i + 1))
          })
        } else {
          previewImages.value.push(savedImg)
          previewFileNames.value.push('图片')
        }
      } catch {
        // 兼容旧版单个 URL 字符串
        previewImages.value.push(savedImg)
        previewFileNames.value.push('图片')
      }
      localStorage.removeItem('user_search_img')
    }

    nextTick(() => {
      autoResize()
      handleSend()
    })
  } else {
    initConversationId()
  }
})
</script>

<style scoped>
.search-page {
  width: 100%;
  max-width: var(--content-max-width);
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 0 var(--space-4);
}

/* ---- 会话标题栏 ---- */
.conv-header {
  padding: var(--space-3) 0;
  border-bottom: 1px solid var(--border-light);
  flex-shrink: 0;
}

.conv-title {
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: block;
}

/* ---- 消息区域 ---- */
.messages-area {
  flex: 1;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: var(--space-4) 0;
  scroll-behavior: smooth;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  padding: var(--space-8);
}

.empty-icon {
  color: var(--primary-300);
  margin-bottom: var(--space-4);
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

.empty-state h3 {
  font-size: var(--text-lg);
  font-weight: var(--weight-semibold);
  color: var(--text-primary);
  margin: 0 0 var(--space-2);
}

.empty-state p {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  margin: 0 0 var(--space-6);
  max-width: 380px;
  line-height: var(--leading-relaxed);
}

.empty-hints {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
}

.empty-hints > span {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  margin-bottom: var(--space-1);
}

.empty-hints button {
  border: 1px solid var(--border-light);
  background: var(--surface-card);
  border-radius: var(--radius-full);
  padding: var(--space-2) var(--space-4);
  font-size: var(--text-sm);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-base);
}

.empty-hints button:hover {
  border-color: var(--primary-300);
  color: var(--primary-600);
  background: var(--primary-50);
}

/* 消息组 */
.message-group {
  margin-bottom: var(--space-5);
}

.message {
  display: flex;
  gap: var(--space-2);
  margin-bottom: var(--space-3);
}

.user-message {
  justify-content: flex-end;
}

.ai-message {
  align-items: flex-start;
}

/* AI头像 */
.ai-avatar {
  width: 30px;
  height: 30px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--primary-100), var(--primary-200));
  color: var(--primary-600);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 2px;
  transition: transform var(--transition-base);
}

/* 消息气泡 */
.msg-bubble {
  max-width: 82%;
  border-radius: var(--radius-md);
  padding: var(--space-4) var(--space-5);
  font-size: 14px;
  line-height: 1.75;
  position: relative;
}

.user-bubble {
  background: linear-gradient(135deg, var(--primary-600), var(--primary-700));
  color: #fff;
  border-bottom-right-radius: var(--radius-xs);
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.15);
}

.ai-bubble {
  background: var(--surface-card);
  border: 1px solid var(--border-light);
  border-top-left-radius: var(--radius-xs);
  box-shadow: var(--shadow-xs);
  padding: var(--space-5);
}

.bubble-text {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.bubble-model-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: var(--space-2);
  font-size: var(--text-xs);
  background: rgba(255, 255, 255, 0.15);
  padding: 2px 8px;
  border-radius: var(--radius-full);
}

.bubble-images {
  display: flex;
  gap: var(--space-2);
  margin-top: var(--space-2);
  flex-wrap: wrap;
}

.bubble-img {
  width: 72px;
  height: 72px;
  border-radius: var(--radius-sm);
  object-fit: cover;
  cursor: pointer;
  transition: transform var(--transition-base);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.bubble-img:hover {
  transform: scale(1.05);
}

.bubble-files {
  display: flex;
  gap: var(--space-2);
  margin-top: var(--space-2);
  flex-wrap: wrap;
}

.bubble-file-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: rgba(255, 255, 255, 0.12);
  border-radius: var(--radius-sm);
  font-size: var(--text-xs);
  color: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.15);
}

/* Markdown */
.bubble-content .markdown-body {
  font-size: 14px;
  line-height: 1.75;
  color: var(--text-primary);
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4) {
  margin: var(--space-4) 0 var(--space-2);
  font-weight: var(--weight-semibold);
  line-height: 1.4;
}

.markdown-body :deep(h2) { font-size: 1.15em; }
.markdown-body :deep(h3) { font-size: 1.05em; }
.markdown-body :deep(h4) { font-size: 1em; }

.markdown-body :deep(p) {
  margin: 0 0 var(--space-3);
}

.markdown-body :deep(p:last-child) { margin-bottom: 0; }

.markdown-body :deep(ol),
.markdown-body :deep(ul) {
  padding-left: 1.5em;
  margin: var(--space-2) 0 var(--space-3);
}

.markdown-body :deep(li) {
  margin-bottom: var(--space-1);
}

.markdown-body :deep(li > ol),
.markdown-body :deep(li > ul) {
  margin-top: var(--space-1);
  margin-bottom: 0;
}

.markdown-body :deep(strong) {
  font-weight: var(--weight-semibold);
  color: var(--text-primary);
}

.markdown-body :deep(blockquote) {
  margin: var(--space-2) 0;
  padding: var(--space-2) var(--space-4);
  border-left: 3px solid var(--primary-300);
  background: var(--color-neutral-50);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  color: var(--text-secondary);
}

.markdown-body :deep(code) {
  background: var(--color-neutral-100);
  padding: 1px 5px;
  border-radius: 3px;
  font-size: 0.9em;
  font-family: var(--font-mono);
}

.markdown-body :deep(pre) {
  background: var(--color-neutral-100);
  border-radius: var(--radius-sm);
  padding: var(--space-3);
  overflow-x: auto;
  margin: var(--space-2) 0;
}

.markdown-body :deep(pre code) {
  background: none;
  padding: 0;
}

.markdown-body :deep(hr) {
  border: none;
  border-top: 1px solid var(--border-light);
  margin: var(--space-4) 0;
}

.markdown-body :deep(img) {
  max-width: 100%;
  max-height: 120px;
  border-radius: var(--radius-md);
  object-fit: contain;
  cursor: pointer;
  margin: var(--space-3) 0;
  display: block;
  border: 1px solid var(--border-light);
  transition: transform var(--transition-base);
}

.markdown-body :deep(img:hover) {
  transform: scale(1.02);
}

.markdown-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: var(--space-2) 0 var(--space-3);
  font-size: var(--text-xs);
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  padding: var(--space-1) var(--space-2);
  border: 1px solid var(--border-light);
  text-align: left;
}

.markdown-body :deep(th) {
  background: var(--color-neutral-50);
  font-weight: var(--weight-semibold);
}

/* 流式光标 */
.streaming-cursor {
  display: inline-block;
  animation: blink-cursor 1s step-end infinite;
  color: var(--primary-600);
  font-weight: var(--weight-bold);
  margin-left: 1px;
}

@keyframes blink-cursor {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

/* 加载动画 */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: var(--space-1) 0;
}

.typing-indicator span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-neutral-400);
  animation: typing-bounce 1.4s infinite both;
}

.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing-bounce {
  0%, 80%, 100% { opacity: 0.3; transform: translateY(0); }
  40% { opacity: 1; transform: translateY(-4px); }
}

/* 错误消息 */
.error-message {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--color-danger);
  font-size: var(--text-sm);
}

.retry-btn {
  border: 1px solid var(--color-danger-400);
  background: transparent;
  color: var(--color-danger);
  border-radius: var(--radius-xs);
  padding: 1px 8px;
  font-size: var(--text-xs);
  cursor: pointer;
  transition: all var(--transition-base);
}

.retry-btn:hover {
  background: var(--color-danger-light);
}

/* 参考来源 */
.sources-area {
  margin-top: var(--space-3);
  padding-top: var(--space-3);
  border-top: 1px solid var(--border-light);
}

.sources-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-1);
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  margin-bottom: var(--space-2);
}

.sources-header-left {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}



.source-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-1) 0;
  font-size: var(--text-xs);
}

.source-name {
  flex: 1;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.source-score {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  flex-shrink: 0;
  color: var(--color-success);
}

.score-bar {
  width: 40px;
  height: 4px;
  background: var(--color-neutral-200);
  border-radius: 2px;
  overflow: hidden;
}

.score-bar::after {
  content: '';
  display: block;
  height: 100%;
  width: calc(var(--score) * 1%);
  background: var(--color-success);
  border-radius: 2px;
}

/* 反馈按钮 */
.sources-feedback {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-top: var(--space-2);
  padding-top: var(--space-2);
  border-top: 1px dashed var(--border-light);
}

.feedback-label {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-right: var(--space-1);
}

.fb-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 10px;
  border-radius: var(--radius-xs);
  font-size: 12px;
  cursor: pointer;
  transition: all var(--transition-fast);
  border: 1px solid transparent;
  background: transparent;
}

.fb-correct {
  color: var(--color-success);
  border-color: var(--color-success-400);
}

.fb-correct:hover {
  background: var(--color-success-light);
}

.fb-incorrect {
  color: var(--color-danger);
  border-color: var(--color-danger-400);
}

.fb-incorrect:hover {
  background: var(--color-danger-light);
}

.feedback-status-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: var(--radius-xs);
}

.feedback-status-tag.correct {
  color: var(--color-success);
  background: var(--color-success-light);
  border: 1px solid var(--color-success-400);
}

.feedback-status-tag.processed {
  color: #409eff;
  background: #ecf5ff;
  border: 1px solid #b3d8ff;
}

.feedback-status-tag.incorrect {
  color: var(--color-danger);
  background: var(--color-danger-light);
  border: 1px solid var(--color-danger-400);
}

/* 停止按钮 */
.stop-area {
  display: flex;
  justify-content: center;
  padding: var(--space-2);
}

.stop-btn {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  border: 1px solid var(--border-base);
  background: var(--surface-card);
  border-radius: var(--radius-full);
  padding: var(--space-2) var(--space-4);
  font-size: var(--text-xs);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-base);
}

.stop-btn:hover {
  border-color: var(--color-danger);
  color: var(--color-danger);
  background: var(--color-danger-light);
}

/* ---- 输入区域 ---- */
.input-area {
  padding: var(--space-3) 0;
  border-top: 1px solid var(--border-light);
  background: var(--surface-page);
  flex-shrink: 0;
}

/* 卡片容器 */
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

/* 图片预览标签（输入框顶部） */
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

/* 输入框（无边框，边框在 search-box 上） */
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

/* 底栏 */
.search-bottom {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-top: var(--space-3);
  padding-top: var(--space-3);
  border-top: 1px solid var(--border-light);
}

/* 操作按钮组 */
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

.audio-btn {
  color: #8b5cf6;
}

.audio-btn:hover {
  border-color: #8b5cf6;
  color: #7c3aed;
  background: #f5f3ff;
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
  box-shadow: 0 1px 4px rgba(37, 99, 235, 0.15);
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

/* ============ 引用图标 ============ */
.citation-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  margin: 0 2px;
  padding: 2px;
  border-radius: 4px;
  cursor: pointer;
  vertical-align: text-bottom;
  transition: all 0.15s;
  user-select: none;
  text-decoration: none;
  line-height: 1;
}

.citation-icon.knowledge-ref {
  background: #eff6ff;
  color: #2563eb;
  border: 1px solid #bfdbfe;
}

.citation-icon.case-ref {
  background: #fffbeb;
  color: #d97706;
  border: 1px solid #fde68a;
}

.citation-icon:hover {
  transform: translateY(-1px);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
}

/* ============ 引用浮层 ============ */
.citation-popover {
  position: fixed;
  z-index: 2000;
  max-width: 420px;
  min-width: 200px;
  background: var(--surface-card);
  border: 1px solid var(--border-base);
  border-radius: var(--radius-md);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  padding: var(--space-3);
  font-size: var(--text-xs);
  line-height: 1.6;
  animation: popover-in 0.15s ease;
  transform: translateX(-50%);
}

@keyframes popover-in {
  from { opacity: 0; transform: translateX(-50%) translateY(-4px); }
  to { opacity: 1; transform: translateX(-50%) translateY(0); }
}

.citation-popover-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding-bottom: var(--space-2);
  margin-bottom: var(--space-2);
  border-bottom: 1px solid var(--border-light);
}

.citation-type {
  font-size: 13px;
  flex-shrink: 0;
}

.citation-name {
  flex: 1;
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: var(--text-xs);
}

.citation-score-badge {
  font-size: 11px;
  color: var(--color-success);
  background: #ecfdf5;
  padding: 1px 6px;
  border-radius: var(--radius-full);
  font-weight: 600;
  flex-shrink: 0;
}

.citation-popover-body {
  color: var(--text-secondary);
  max-height: 220px;
  overflow-y: auto;
}

.citation-content-text {
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--text-primary);
  border-top: 1px solid var(--border-light);
  padding-top: 10px;
}

/* ============ 案例卡片（消息下方） ============ */
.case-cards-outer {
  margin: 0 0 0 38px;
  padding: 0px 0;
  animation: fade-in 0.3s ease;
}

@keyframes fade-in {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}

.case-cards-outer-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-xs);
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: var(--space-3);
}

.case-cards-outer-grid {
  display: flex;
  flex-direction: row;
  gap: var(--space-2);
  overflow-x: auto;
}

.case-card-outer {
  background: var(--surface-card);
  border: 1px solid var(--primary-200);
  border-radius: var(--radius-sm);
  padding: var(--space-3);
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
  width: 200px;
}

.case-card-outer:hover {
  border-color: var(--primary-200);
  box-shadow: var(--shadow-md);
  background: linear-gradient(90deg, var(--primary-50), var(--surface-card));
}

.case-card-outer-title {
  font-size: var(--text-xs);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--space-2);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.case-card-outer-snippet {
  font-size: 11px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: var(--space-2);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.case-card-outer-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.case-card-outer-score {
  font-size: 11px;
  color: var(--color-success);
  font-weight: 500;
}

.case-card-outer-arrow {
  display: flex;
  color: var(--primary-500);
}

/* ============ 案例详情弹窗（参考 /user/case 风格） ============ */
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
  background: var(--primary-50);
  color: var(--primary-600);
  border-radius: var(--radius-full);
}

.detail-content-section h4,
.detail-image h4 {
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  color: var(--text-primary);
  margin: 0 0 var(--space-2);
}

.detail-content {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  line-height: var(--leading-relaxed);
  background: var(--color-neutral-50);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  padding: var(--space-4);
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 250px;
  overflow-y: auto;
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
  transition: transform 0.25s ease, opacity 0.2s ease;
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

/* ============ 图片预览弹窗 ============ */
.image-preview-overlay {
  background: rgba(0, 0, 0, 0.85);
  z-index: 2001;
  cursor: pointer;
}

.image-preview-close {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 50%;
  color: #fff;
  cursor: pointer;
  z-index: 10;
  transition: background 0.2s;
}

.image-preview-close:hover {
  background: rgba(255, 255, 255, 0.25);
}

.image-preview-img {
  max-width: 90vw;
  max-height: 90vh;
  object-fit: contain;
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  cursor: default;
}

/* ============ 总结为案例按钮 ============ */
.summarize-btn {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 12px;
  font-size: 12px;
  border: 1px solid var(--primary-300);
  color: var(--primary-600);
  background: var(--primary-50);
  border-radius: var(--radius-xs);
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}

.summarize-btn:hover:not(:disabled) {
  background: var(--primary-100);
  border-color: var(--primary-400);
}

.summarize-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 总结预览对话框输入框 */
.summary-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--border-base);
  border-radius: var(--radius-sm);
  font-size: var(--text-sm);
  font-family: inherit;
  color: var(--text-primary);
  background: var(--surface-page);
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.summary-input:focus {
  outline: none;
  border-color: var(--primary-400);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.06);
}

.summary-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--border-base);
  border-radius: var(--radius-sm);
  font-size: var(--text-sm);
  font-family: inherit;
  color: var(--text-primary);
  background: var(--surface-page);
  resize: vertical;
  min-height: 100px;
  line-height: 1.6;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.summary-textarea:focus {
  outline: none;
  border-color: var(--primary-400);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.06);
}

.summarize-cancel-btn {
  padding: 8px 20px;
  border: 1px solid var(--border-base);
  border-radius: var(--radius-sm);
  background: var(--surface-card);
  color: var(--text-secondary);
  font-size: var(--text-sm);
  cursor: pointer;
  transition: all 0.15s;
}

.summarize-cancel-btn:hover {
  border-color: var(--color-danger);
  color: var(--color-danger);
  background: var(--color-danger-light);
}

.summarize-submit-btn {
  padding: 8px 24px;
  border: none;
  border-radius: var(--radius-sm);
  background: linear-gradient(135deg, var(--primary-600), var(--primary-700));
  color: #fff;
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  cursor: pointer;
  transition: all 0.15s;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.2);
}

.summarize-submit-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, var(--primary-700), var(--primary-800));
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.3);
}

.summarize-submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  box-shadow: none;
}
</style>
