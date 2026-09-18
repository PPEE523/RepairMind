<template>
  <div class="home-page">
    <!-- ============================================================
         中部：问候语 + 搜索 + 统计（可滚动）
         ============================================================ -->
    <div class="home-content">
      <section class="greeting-section">
        <h1 class="greeting-text">
          {{ greeting }}<span v-if="userStore.nickname" class="greeting-name">{{ userStore.nickname }}</span>
        </h1>
        <p class="greeting-sub">智能诊断，精准检修 — AI 辅助设备故障排查</p>
      </section>

      <section class="search-section">
        <div class="search-box">
          <!-- 图片预览行（输入框顶部） -->
          <div v-if="previewImages.length" class="preview-row">
            <div v-for="(img, i) in previewImages" :key="i" class="preview-tag">
              <div class="preview-popup">
                <img :src="img" alt="预览" />
              </div>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/>
              </svg>
              <span class="preview-name">{{ previewFileNames[i] || '图片' }}</span>
              <button class="preview-remove" @click="removePreview(i)" aria-label="移除">×</button>
            </div>
          </div>

          <textarea
            v-model="searchText"
            class="search-input"
            :placeholder="isLoggedIn ? '描述设备故障现象，如：发动机异响怎么排查？' : '登录后可进行智能故障检索'"
            :disabled="!isLoggedIn"
            rows="1"
            ref="searchTextarea"
            @keydown.enter.exact.prevent="handleSearch"
            @focus="checkLogin"
            @input="autoResize"
          ></textarea>

          <div class="search-bottom">
            <div class="quick-tags">
              <button
                v-for="tag in quickTags"
                :key="tag"
                class="tag-pill"
                @click="quickFill(tag)"
              >{{ tag }}</button>
            </div>

            <div class="search-actions">
              <label class="upload-btn" v-if="isLoggedIn" title="上传故障图片">
                <input type="file" accept="image/jpeg,image/png" hidden @change="handleImageSelect" />
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/>
                </svg>
              </label>
              <button
                class="send-btn"
                :disabled="(!searchText.trim() && !previewImages.length) || !isLoggedIn"
                @click="handleSearch"
                aria-label="发送检索"
              >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </section>

      <section v-if="isLoggedIn && hasStats" class="stats-row">
        <div class="stat-item" v-if="stats.knowledgeCount > 0">
          <span class="stat-num">{{ formatNum(stats.knowledgeCount) }}</span>
          <span class="stat-desc">知识案例</span>
        </div>
        <span v-if="stats.knowledgeCount > 0 && stats.sopCount > 0" class="stat-sep">·</span>
        <div class="stat-item" v-if="stats.sopCount > 0">
          <span class="stat-num">{{ stats.sopCount }}</span>
          <span class="stat-desc">SOP 流程</span>
        </div>
      </section>

    </div>

    <!-- ============================================================
         底部：案例卡片（固定在首屏底部）
         ============================================================ -->
    <div class="home-bottom" v-if="isLoggedIn">
      <section v-if="recentCases.length > 0" class="cases-section">
        <h2 class="section-title">最新案例</h2>
        <div class="cases-grid">
          <div
            v-for="item in recentCases"
            :key="item.knowledgeId"
            class="case-card"
            @click="viewCase(item)"
          >
            <div class="case-card-body">
              <h4 class="case-card-title">{{ item.title || '未命名案例' }}</h4>
              <p class="case-card-desc">{{ truncate(item.content, 60) }}</p>
            </div>
            <div class="case-card-footer">
              <span class="case-card-tag">{{ item.equipModel || '通用' }}</span>
              <span class="case-card-time">{{ formatTime(item.createTime) }}</span>
            </div>
          </div>
        </div>
      </section>

      <footer class="home-footer">
        <span>设备检修知识检索与作业辅助系统 v1.0</span>
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { getKnowledgeList, getSopFlowList, uploadFile } from '@/api'

const router = useRouter()
const userStore = useUserStore()
const searchText = ref('')
const searchTextarea = ref<HTMLTextAreaElement | null>(null)
const previewImages = ref<string[]>([])
const previewFileNames = ref<string[]>([])
const isLoggedIn = computed(() => !!userStore.token)

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了，'
  if (hour < 12) return '早上好，'
  if (hour < 14) return '中午好，'
  if (hour < 18) return '下午好，'
  return '晚上好，'
})

const quickTags = [
  '发动机异响怎么排查？',
  '液压系统压力不足',
  '设备启动后自动停机',
  '轴承温度过高怎么办？'
]

const stats = reactive({
  knowledgeCount: 0,
  sopCount: 0
})

const hasStats = computed(() => stats.knowledgeCount > 0 || stats.sopCount > 0)

const recentCases = ref<any[]>([])

const checkLogin = () => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录使用检索功能')
    router.push('/user/login')
  }
}

const quickFill = (text: string) => {
  if (!isLoggedIn.value) { checkLogin(); return }
  searchText.value = text
}

const autoResize = () => {
  const el = searchTextarea.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 160) + 'px'
}

const handleSearch = () => {
  if (!isLoggedIn.value) { checkLogin(); return }
  if (!searchText.value.trim() && !previewImages.value.length) return
  localStorage.setItem('user_search_text', searchText.value.trim() || '[图片故障诊断]')
  if (previewImages.value.length) {
    localStorage.setItem('user_search_img', JSON.stringify(previewImages.value))
  }
  previewImages.value = []
  previewFileNames.value = []
  router.push('/user/search')
}

const handleImageSelect = async (e: Event) => {
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
    const url = res.data?.url || ''
    if (url) {
      previewImages.value.push(url)
      previewFileNames.value.push(file.name)
      ElMessage.success('图片上传成功')
    }
  } catch {
    ElMessage.error('图片上传失败')
  }
  input.value = ''
}

const removePreview = (i: number) => {
  previewImages.value.splice(i, 1)
  previewFileNames.value.splice(i, 1)
}

const viewCase = (_item: any) => {
  router.push('/user/case')
}

const formatNum = (n: number) => n > 999 ? (n / 1000).toFixed(1) + 'k' : String(n)
const truncate = (text: string, max: number) => {
  if (!text) return '暂无描述'
  return text.length > max ? text.substring(0, max) + '...' : text
}
const formatTime = (time: string) => time ? time.substring(0, 10) : ''

onMounted(async () => {
  if (!isLoggedIn.value) return
  try {
    const r1: any = await getKnowledgeList({ auditStatus: 1, pageNum: 1, pageSize: 4 })
    const data = r1.data
    if (data) {
      recentCases.value = data.records?.slice(0, 4) || data.slice?.(0, 4) || []
      stats.knowledgeCount = data.total || recentCases.value.length
    }
  } catch { /* silent */ }
  try {
    const r2: any = await getSopFlowList()
    stats.sopCount = (r2.data || []).length
  } catch { /* silent */ }

  // 从 Search 页带过来的搜索文本
  const savedText = localStorage.getItem('user_search_text')
  if (savedText) {
    searchText.value = savedText
    localStorage.removeItem('user_search_text')
    nextTick(() => autoResize())
  }
})
</script>

<style scoped>
/* ============================================================
   Kimi 风格首页 — 中部案例 + 底部搜索
   ============================================================ */
.home-page {
  width: 100%;
  max-width: 780px;
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 0 var(--space-6);
  overflow: hidden;
}

/* ---- 中部可滚动区域 ---- */
.home-content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 72px 0 var(--space-8);
}

/* ---- 底部固定区域 ---- */
.home-bottom {
  flex-shrink: 0;
  padding: var(--space-4) 0;
  border-top: 1px solid var(--border-light);
}

/* ---- 问候语 ---- */
.greeting-section {
  margin-bottom: var(--space-4);
}

.greeting-text {
  font-size: 28px;
  font-weight: var(--weight-bold);
  color: var(--text-primary);
  margin: 0 0 var(--space-1);
  letter-spacing: -0.02em;
  line-height: 1.3;
}

.greeting-name {
  background: linear-gradient(135deg, var(--primary-600), var(--accent-500));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.greeting-sub {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
  margin: 0;
}

/* ---- 搜索区 ---- */
.search-section {
  margin-bottom: var(--space-4);
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
  justify-content: space-between;
  margin-top: var(--space-3);
  padding-top: var(--space-3);
  border-top: 1px solid var(--border-light);
  gap: var(--space-3);
}

/* 快捷标签 */
.quick-tags {
  display: flex;
  gap: var(--space-1);
  flex-wrap: wrap;
}

.tag-pill {
  padding: 3px 12px;
  font-size: var(--text-xs);
  color: var(--text-secondary);
  background: var(--gray-50);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: all var(--transition-base);
  white-space: nowrap;
}

.tag-pill:hover {
  color: var(--primary-600);
  border-color: var(--primary-200);
  background: var(--primary-50);
}

/* 操作区 */
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
  border-radius: var(--radius-sm);
  background: linear-gradient(135deg, var(--primary-600), var(--primary-700));
  color: #fff;
  cursor: pointer;
  transition: all var(--transition-base);
}

.send-btn:hover:not(:disabled) {
  transform: scale(1.06);
  box-shadow: 0 3px 12px rgba(37, 99, 235, 0.25);
}

.send-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

/* ---- 图片预览标签（输入框顶部） ---- */
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

/* ---- 统计数据行 ---- */
.stats-row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-2);
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  align-items: baseline;
  gap: var(--space-2);
}

.stat-num {
  font-size: 22px;
  font-weight: var(--weight-bold);
  color: var(--text-primary);
  font-family: var(--font-mono);
  letter-spacing: -0.02em;
}

.stat-desc {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
}

.stat-sep {
  color: var(--text-tertiary);
  font-size: var(--text-lg);
}

/* ---- 最新案例 ---- */
.cases-section {
  margin-bottom: 0;
}

.section-title {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--text-primary);
  margin: 0 0 var(--space-4);
}

.cases-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-3);
}

.case-card {
  background: var(--surface-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  cursor: pointer;
  transition: all var(--transition-base);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.case-card:hover {
  border-color: var(--primary-200);
  box-shadow: var(--shadow-sm);
  transform: translateY(-2px);
}

.case-card-body {
  margin-bottom: var(--space-3);
}

.case-card-title {
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  color: var(--text-primary);
  margin: 0 0 var(--space-1);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.case-card-desc {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  margin: 0;
  line-height: var(--leading-normal);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.case-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
}

.case-card-tag {
  font-size: 10px;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  background: var(--primary-50);
  color: var(--primary-600);
  font-weight: var(--weight-medium);
}

.case-card-time {
  font-size: 10px;
  color: var(--text-tertiary);
}

/* ---- 页脚 ---- */
.home-footer {
  padding: var(--space-2) 0;
  text-align: center;
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}

/* ---- 响应式 ---- */
@media (max-width: 640px) {
  .home-page {
    padding: var(--space-4) var(--space-4) var(--space-12);
  }

  .greeting-text {
    font-size: 22px;
  }

  .cases-grid {
    grid-template-columns: 1fr;
  }

  .search-bottom {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (min-width: 1024px) {
  .cases-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}
</style>
