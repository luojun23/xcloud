<template>
  <el-drawer v-model="visible" :title="panelTitle" size="550px" direction="rtl" :destroy-on-close="true" @close="handleClose">
    <div v-if="loading" class="panel-loading">
      <el-icon class="is-loading" :size="32" color="#409eff"><Loading /></el-icon>
      <p>{{ loadingText }}</p>
    </div>
    <div v-else class="panel-content">
      <div v-if="panelType === 'ai'" class="markdown-body" v-html="renderedMarkdown"></div>
      <div v-else class="text-body">
        <pre>{{ content }}</pre>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, computed } from 'vue'
import { marked } from 'marked'
import { Loading } from '@element-plus/icons-vue'

const visible = ref(false)
const panelTitle = ref('')
const panelType = ref('ai')
const content = ref('')
const loading = ref(false)
const loadingText = ref('AI 正在分析中...')

const renderedMarkdown = computed(() => {
  if (!content.value) return ''
  let cleanText = content.value.replace(/<think>[\s\S]*?<\/think>/gi, '')
  if (cleanText.includes('```')) cleanText = cleanText.split('```').pop()
  if (!cleanText.trim()) cleanText = content.value
  return marked.parse(cleanText)
})

const show = (type, title, data, isLoading = false, loadingMsg = 'AI 正在分析中...') => {
  panelType.value = type
  panelTitle.value = title
  loading.value = isLoading
  loadingText.value = loadingMsg
  content.value = data || ''
  visible.value = true
}

const updateContent = (data) => {
  content.value = data
  loading.value = false
}

const setLoading = (isLoading, msg) => {
  loading.value = isLoading
  if (msg) loadingText.value = msg
}

const handleClose = () => {
  visible.value = false
}

defineExpose({ show, updateContent, setLoading })

const emit = defineEmits(['close'])
</script>

<style scoped>
.panel-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  gap: 16px;
  color: #909399;
}

.panel-content {
  line-height: 1.8;
  font-size: 14px;
}

.text-body pre {
  white-space: pre-wrap;
  font-family: monospace;
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 16px;
  border-radius: 8px;
  font-size: 13px;
  max-height: 70vh;
  overflow-y: auto;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  color: #409eff;
  margin-top: 1.2em;
  margin-bottom: 0.5em;
}

.markdown-body :deep(h1) {
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 8px;
}

.markdown-body :deep(h2) {
  border-bottom: 1px solid #f2f2f2;
  padding-bottom: 6px;
}

.markdown-body :deep(strong) {
  color: #409eff;
}

.markdown-body :deep(ul) {
  padding-left: 20px;
}

.markdown-body :deep(li) {
  margin-bottom: 6px;
}

.markdown-body :deep(p) {
  margin-bottom: 1em;
}

.markdown-body :deep(blockquote) {
  border-left: 4px solid #409eff;
  padding: 8px 16px;
  background: #ecf5ff;
  margin: 12px 0;
  color: #606266;
}

html.dark .text-body pre {
  background: #1a1a1a;
}

html.dark .markdown-body :deep(blockquote) {
  background: #1a2b4c;
}
</style>
