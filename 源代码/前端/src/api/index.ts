import request from '@/utils/request'

// 登录
export function login(data: { username: string; password: string }) {
  return request.post('/auth/login', data)
}

// 注册
export function register(data: { username: string; password: string }) {
  return request.post('/auth/register', data)
}

// 退出登录
export function logout() {
  return request.post('/auth/logout')
}

// 获取用户菜单
export function getMenu() {
  return request.get('/auth/menu')
}

// 多模态检索
export function multiSearch(data: {
  query?: string
  equipModel?: string
  fileUrls?: string[]
  fileTypes?: string[]
  topK?: number
  agentType?: string
}) {
  return request.post('/search/multi', data)
}

// 获取检索历史
export function getSearchHistory(params: {
  pageNum: number
  pageSize: number
}) {
  return request.get('/search/history', { params })
}

// 获取检索历史列表（带筛选）
export function getSearchHistoryList(params: {
  pageNum: number
  pageSize: number
  equipModel?: string
  searchSuccess?: boolean
  startTime?: string
  endTime?: string
  keyword?: string
}) {
  return request.get('/search/history', { params })
}

// 获取检索历史详情
export function getSearchHistoryDetail(id: number) {
  return request.get(`/search/history/${id}`)
}

// 删除检索历史
export function deleteSearchHistory(id: number) {
  return request.delete(`/search/history/${id}`)
}

// 批量删除检索历史
export function batchDeleteSearchHistory(ids: number[]) {
  return request.delete('/search/history/batch', { data: ids })
}

// 上传文件
export function uploadFile(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 上传知识案例
export function uploadKnowledge(data: {
  title: string
  symptom: string
  solution: string
  imgUrl?: string
  images?: string
  equipModel: string
}) {
  return request.post('/knowledge/upload', data)
}

/**
 * 案例图片上传
 * @param file 图片文件
 * @returns { url: string, fileName: string }
 */
export function uploadCaseImage(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/knowledge/upload-image', formData)
}

/**
 * 语音转文字 - 上传音频文件调用 Dify 语音转文字接口
 * @param file 音频文件（mp3/wav/m4a 等）
 * @returns { text: string }
 */
export function audioToText(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/knowledge/audio-to-text', formData, {
    timeout: 60000
  })
}

// 获取案例列表（自己上传的+他人已审核通过的）
export function getKnowledgeList(params: {
  auditStatus?: number
  pageNum: number
  pageSize: number
}) {
  return request.get('/knowledge/list', { params })
}

// 获取待审核列表（管理员/审核人使用）
export function getAuditList(params: {
  auditStatus?: number
  pageNum: number
  pageSize: number
}) {
  return request.get('/knowledge/audit/list', { params })
}

// 审核案例
export function auditKnowledge(data: {
  knowledgeId: number
  auditStatus: number
  remark?: string
}) {
  return request.post('/knowledge/audit', data)
}

// 批量检查知识库文档是否存在
export function checkDocuments(knowledgeIds: number[]) {
  return request.post('/knowledge/check-documents', knowledgeIds)
}

// 获取作业流程
export function getWorkFlow(params: {
  equipModel: string
  repairLevel: string
}) {
  return request.get('/workflow/get', { params })
}

// 保存作业流程
export function saveWorkFlow(data: {
  equipModel: string
  repairLevel: string
  stepJson: string
}) {
  return request.post('/workflow/save', data)
}

// ========== SOP流程管理 ==========

// 获取SOP流程列表
export function getSopFlowList() {
  return request.get('/workflow/sop/list')
}

// 获取SOP流程详情
export function getSopFlowDetail(id: number) {
  return request.get(`/workflow/sop/${id}`)
}

// 生成SOP流程
export function generateSopFlow(data: {
  documentId: string
  documentName: string
  datasetId: string
}) {
  return request.post('/workflow/sop/generate', data)
}

// 修改SOP流程
export function updateSopFlow(id: number, data: { name?: string; stepJson?: string; totalSteps?: number }) {
  return request.put(`/workflow/sop/${id}`, data)
}

// 删除SOP流程
export function deleteSopFlow(id: number) {
  return request.delete(`/workflow/sop/${id}`)
}

// 获取Dify知识库列表
export function getDifyDatasets(scope?: string) {
  return request.get('/dify/knowledge/datasets', {
    params: scope ? { scope } : {}
  })
}

// 更新知识库信息
export function updateDataset(datasetId: string, data: { name?: string; description?: string }) {
  return request.patch(`/dify/knowledge/datasets/${datasetId}`, data)
}

// 获取知识库文档列表
export function getDifyDocuments(datasetId: string, page?: number, limit?: number) {
  return request.get(`/dify/knowledge/datasets/${datasetId}/documents`, {
    params: { page: page || 1, limit: limit || 20 }
  })
}

// 获取操作日志
export function getOperLogs(params: {
  userId?: number
  pageNum: number
  pageSize: number
}) {
  return request.get('/system/log', { params })
}

// 删除单条操作日志
export function deleteOperLog(id: number) {
  return request.delete(`/system/log/${id}`)
}

// 批量删除操作日志
export function batchDeleteOperLogs(ids: number[]) {
  return request.delete('/system/log/batch', { data: ids })
}

// 清空所有操作日志
export function clearOperLogs() {
  return request.delete('/system/log/clear')
}

// 获取系统配置
export function getSysConfigs() {
  return request.get('/system/config')
}

// 保存系统配置
export function saveSysConfig(data: {
  configKey: string
  configValue: string
  remark?: string
}) {
  return request.post('/system/config', data)
}

// ==================== 知识库分段管理 ====================

// 获取文档分段列表
export function getDocumentSegments(datasetId: string, documentId: string, params: {
  page: number
  limit: number
}) {
  return request.get(`/dify/knowledge/datasets/${datasetId}/documents/${documentId}/segments`, { params })
}

// 创建文档分段
export function createSegment(datasetId: string, documentId: string, data: {
  content: string
  answer?: string
}) {
  return request.post(`/dify/knowledge/datasets/${datasetId}/documents/${documentId}/segments`, data)
}

// 更新文档分段
export function updateSegment(datasetId: string, documentId: string, segmentId: string, data: {
  content?: string
  answer?: string
  keywords?: string[]
  enabled?: boolean
}) {
  return request.put(`/dify/knowledge/datasets/${datasetId}/documents/${documentId}/segments/${segmentId}`, data)
}

// 删除文档分段
export function deleteSegment(datasetId: string, documentId: string, segmentId: string) {
  return request.delete(`/dify/knowledge/datasets/${datasetId}/documents/${documentId}/segments/${segmentId}`)
}

// 获取子分段列表
export function getChildChunks(datasetId: string, documentId: string, segmentId: string, params: {
  page: number
  limit: number
}) {
  return request.get(`/dify/knowledge/datasets/${datasetId}/documents/${documentId}/segments/${segmentId}/child-chunks`, { params })
}

// 创建子分段
export function createChildChunk(datasetId: string, documentId: string, segmentId: string, data: {
  content: string
}) {
  return request.post(`/dify/knowledge/datasets/${datasetId}/documents/${documentId}/segments/${segmentId}/child-chunks`, data)
}

// 更新子分段
export function updateChildChunk(datasetId: string, documentId: string, segmentId: string, childChunkId: string, data: {
  content: string
}) {
  return request.put(`/dify/knowledge/datasets/${datasetId}/documents/${documentId}/segments/${segmentId}/child-chunks/${childChunkId}`, data)
}

// 删除子分段
export function deleteChildChunk(datasetId: string, documentId: string, segmentId: string, childChunkId: string) {
  return request.delete(`/dify/knowledge/datasets/${datasetId}/documents/${documentId}/segments/${segmentId}/child-chunks/${childChunkId}`)
}

// 知识库数据检索
export function retrieveDataset(datasetId: string, data: { query: string; top_k?: number }) {
  return request.post(`/dify/knowledge/datasets/${datasetId}/retrieve`, data)
}

// ==================== AI智能问答 ====================

// 获取对话历史列表
export function getChatConversations(params: {
  pageNum: number
  pageSize: number
}) {
  return request.get('/chat/conversations', { params })
}

// 获取对话多轮详情
export function getChatConversationDetail(conversationId: string) {
  return request.get(`/chat/conversations/${conversationId}`)
}

// 删除对话
export function deleteChatConversation(conversationId: string) {
  return request.delete(`/chat/conversations/${conversationId}`)
}

// 提交回答标注
export function submitChatFeedback(data: {
  chatHistoryId: number
  feedbackStatus: string
}) {
  return request.post('/chat/feedback', data)
}

// AI修正统计 - 基于chat_feedback
export function getChatFeedbackStats() {
  return request.get('/chat-feedback/stats')
}

export function getChatFeedbackList(params: {
  feedbackStatus?: string
  pageNum: number
  pageSize: number
}) {
  return request.get('/chat-feedback/list', { params })
}

export function getChatFeedbackDetail(id: number) {
  return request.get(`/chat-feedback/detail/${id}`)
}

export function processChatFeedback(data: {
  chatFeedbackId: number
  segmentId: string
  datasetId: string
  documentId: string
  content: string
}) {
  return request.put('/chat-feedback/process', data)
}

// 对话附件上传（含格式+大小校验）
export function uploadChatFile(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/chat/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 获取对话上传配置（开关状态和允许格式）
export function getChatUploadConfig() {
  return request.get('/chat/upload-config')
}

// 获取系统上传开关配置（管理员）
export function getSystemUploadConfig() {
  return request.get('/system/upload-config')
}

// 保存系统上传开关配置（管理员）
export function saveSystemUploadConfig(data: Record<string, any>) {
  return request.post('/system/upload-config', data)
}

// 检测案例相似度
export function checkSimilarity(data: {
  symptom: string
  solution: string
  equipModel?: string
}) {
  return request.post('/knowledge/check-similarity', data)
}

/**
 * 总结对话为案例 - 调用 Dify Workflow 进行总结
 * @param data.conversationContent 拼接后的 Q/A 对话内容
 * @returns { title, equipModel, symptom, solution }
 */
export function summarizeToCase(data: {
  conversationContent: string
}) {
  return request.post('/knowledge/summarize-to-case', data, {
    timeout: 90000
  })
}

// 根据 Dify 文档 ID 批量查询案例详情（用户端卡片使用）
export function getCasesByDifyDocIds(documentIds: string[]) {
  return request.post('/knowledge/by-dify-doc', { documentIds })
}
