<template>
  <div class="ai-feedback">
    <div class="feedback-actions">
      <el-button-group>
        <el-button
          :type="feedbackType === 'correct' ? 'success' : 'default'"
          size="small"
          @click="handleFeedback('correct')"
        >
          <el-icon><Check /></el-icon>
          正确
        </el-button>
        <el-button
          :type="feedbackType === 'wrong' ? 'danger' : 'default'"
          size="small"
          @click="handleFeedback('wrong')"
        >
          <el-icon><Close /></el-icon>
          错误
        </el-button>
      </el-button-group>
      <el-button
        v-if="feedbackType === 'wrong'"
        type="primary"
        size="small"
        link
        @click="showCorrection = !showCorrection"
      >
        {{ showCorrection ? '收起' : '修正内容' }}
      </el-button>
    </div>

    <el-collapse-transition>
      <div v-if="showCorrection" class="correction-form">
        <el-input
          v-model="correctedAnswer"
          type="textarea"
          :rows="3"
          placeholder="请输入正确的答案..."
        />
        <el-button
          type="primary"
          size="small"
          :loading="submitting"
          @click="submitCorrection"
          style="margin-top: 8px"
        >
          提交修正
        </el-button>
      </div>
    </el-collapse-transition>

    <div v-if="submitted" class="feedback-submitted">
      <el-icon color="#67c23a"><CircleCheck /></el-icon>
      <span>感谢您的反馈！</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const props = defineProps<{
  searchHistoryId: number
  originalAnswer: string
}>()

const feedbackType = ref<'correct' | 'wrong' | null>(null)
const showCorrection = ref(false)
const correctedAnswer = ref('')
const submitting = ref(false)
const submitted = ref(false)

const handleFeedback = async (type: 'correct' | 'wrong') => {
  feedbackType.value = type

  if (type === 'correct') {
    await submitFeedback(1, '')
  } else {
    showCorrection.value = true
  }
}

const submitCorrection = async () => {
  if (!correctedAnswer.value.trim()) {
    ElMessage.warning('请输入修正内容')
    return
  }
  await submitFeedback(0, correctedAnswer.value)
}

const submitFeedback = async (rating: number, corrected: string) => {
  submitting.value = true
  try {
    await request.post('/ai/feedback', {
      searchHistoryId: props.searchHistoryId,
      originalAnswer: props.originalAnswer,
      rating,
      correctedAnswer: corrected,
      feedbackType: rating === 1 ? 'correct' : 'wrong'
    })
    submitted.value = true
    showCorrection.value = false
    ElMessage.success('反馈提交成功')
  } catch (error) {
    console.error('提交反馈失败', error)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.ai-feedback {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #ebeef5;
}

.feedback-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.correction-form {
  margin-top: 12px;
}

.feedback-submitted {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  color: #67c23a;
  font-size: 14px;
}
</style>
