<template>
  <div class="ai-analysis">
    <!-- 顶部操作区 -->
    <div class="ai-header">
      <div class="ai-title">
        <el-icon :size="22" color="#409eff"><VideoCamera /></el-icon>
        <span>AI 视频智能分析</span>
      </div>
      <div class="ai-desc">上传视频文件或粘贴视频链接，AI 将自动提取字幕、总结内容、生成洞察报告</div>
    </div>

    <!-- 上传区域 -->
    <div class="upload-zone">
      <div class="upload-split">
        <!-- 本地文件上传 -->
        <div class="upload-pane local-pane" @click="triggerFileInput" @dragover.prevent="isDragOver=true" @dragleave.prevent="isDragOver=false" @drop.prevent="handleDrop">
          <input type="file" ref="fileInputRef" @change="handleFileChange" accept="video/*" hidden />
          <el-icon :size="40" color="#409eff"><UploadFilled /></el-icon>
          <div class="pane-title">本地上传</div>
          <div class="pane-desc">{{ isDragOver ? '松手上传' : '点击或拖拽视频文件' }}</div>
        </div>
        <!-- URL上传 -->
        <div class="upload-pane url-pane">
          <el-icon :size="40" color="#67c23a"><Link /></el-icon>
          <div class="pane-title">链接下载</div>
          <div class="pane-desc">B站 / YouTube / 抖音</div>
          <div class="url-input-box" @click.stop>
            <el-input v-model="videoUrl" placeholder="粘贴视频链接..." @keyup.enter="handleUrlUpload" clearable>
              <template #append>
                <el-button :icon="Right" @click="handleUrlUpload" :loading="uploading" />
              </template>
            </el-input>
          </div>
        </div>
      </div>
    </div>

    <!-- 消息提示 -->
    <el-alert v-if="message" :title="message" :type="messageType" show-icon closable @close="message=''" style="margin-bottom:16px;" />

    <!-- 视频列表 -->
    <div class="video-list-section" v-if="mediaList.length > 0">
      <div class="section-bar">
        <span class="section-title">工作台</span>
        <el-tag size="small" type="info">{{ mediaList.length }} 个任务</el-tag>
      </div>
      <div class="card-grid">
        <el-card v-for="item in mediaList" :key="item.id" class="video-card" shadow="hover">
          <div class="card-header-row">
            <div class="card-info">
              <el-icon :size="24" color="#409eff"><VideoPlay /></el-icon>
              <div class="card-meta">
                <div class="file-name" :title="item.filename">{{ item.filename }}</div>
                <div class="card-tags">
                  <span class="time-tag">{{ formatTime(item.uploadTime) }}</span>
                  <el-tag :type="item.status === 'COMPLETED' ? 'success' : 'warning'" size="small" effect="dark">
                    {{ item.status === 'COMPLETED' ? '就绪' : '处理中' }}
                  </el-tag>
                </div>
              </div>
            </div>
          </div>
          <div class="card-actions">
            <el-button size="small" @click="transcribe(item)" :disabled="item.status !== 'COMPLETED'" :icon="Document">提取文字</el-button>
            <el-button size="small" type="primary" @click="aiAnalyze(item)" :disabled="item.status !== 'COMPLETED'" :icon="MagicStick">AI 智能总结</el-button>
            <el-button size="small" @click="downloadAudio(item)" :disabled="item.status !== 'COMPLETED'" :icon="Download">下载音频</el-button>
            <el-button size="small" type="danger" :icon="Delete" @click="deleteItem(item)" circle />
          </div>
          <!-- AI 结果预览 -->
          <div v-if="item.aiSummary" class="ai-preview" @click="showAiResult(item, 'ai')">
            <el-icon color="#409eff"><MagicStick /></el-icon>
            <span class="preview-label">AI 总结已生成，点击查看</span>
          </div>
          <div v-if="item.transcriptText" class="ai-preview text-preview" @click="showAiResult(item, 'text')">
            <el-icon color="#67c23a"><Document /></el-icon>
            <span class="preview-label">文字提取完成，点击查看</span>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <el-empty description="暂无视频文件，请上传或粘贴链接开始 AI 分析">
        <template #image>
          <el-icon :size="80" color="#c0c4cc"><VideoCamera /></el-icon>
        </template>
      </el-empty>
    </div>

    <!-- AI 结果侧边抽屉 -->
    <el-drawer v-model="drawerVisible" :title="drawerTitle" size="550px" direction="rtl" :destroy-on-close="true">
      <div v-if="drawerLoading" class="drawer-loading">
        <el-icon class="is-loading" :size="32" color="#409eff"><Loading /></el-icon>
        <p>AI 正在分析中，请稍候...</p>
      </div>
      <div v-else class="drawer-content">
        <div v-if="drawerType === 'ai'" class="markdown-body" v-html="renderedMarkdown"></div>
        <div v-else class="text-body">
          <pre>{{ drawerContent }}</pre>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { getCurrentInstance } from 'vue'
import { marked } from 'marked'
import {
  UploadFilled, Link, Right, VideoCamera, VideoPlay,
  Document, MagicStick, Download, Delete, Loading
} from '@element-plus/icons-vue'

const { proxy } = getCurrentInstance()

// 状态变量
const videoUrl = ref('')
const message = ref('')
const messageType = ref('success')
const uploading = ref(false)
const isDragOver = ref(false)
const mediaList = ref([])
const fileInputRef = ref(null)

// 侧边抽屉
const drawerVisible = ref(false)
const drawerTitle = ref('')
const drawerContent = ref('')
const drawerType = ref('ai')
const drawerLoading = ref(false)

// 轮询定时器
const pollingTimers = ref({})

// Markdown 渲染
const renderedMarkdown = computed(() => {
  if (!drawerContent.value) return ''
  let cleanText = drawerContent.value.replace(/<think>[\s\S]*?<\/think>/gi, '')
  if (cleanText.includes('```')) cleanText = cleanText.split('```').pop()
  if (!cleanText.trim()) cleanText = drawerContent.value
  return marked.parse(cleanText)
})

// API 地址
const API_BASE = '/api/ai'

// 消息提示
const showMsg = (msg, type = 'success') => {
  message.value = msg
  messageType.value = type
  setTimeout(() => { if (message.value === msg) message.value = '' }, 4000)
}

// 触发文件选择
const triggerFileInput = () => {
  fileInputRef.value?.click()
}

// 文件选择处理
const handleFileChange = async (e) => {
  const selectedFile = e.target.files[0]
  if (!selectedFile) return
  file.value = selectedFile
  videoUrl.value = ''
  await uploadFile()
  e.target.value = ''
}

// 拖拽处理
const handleDrop = async (e) => {
  isDragOver.value = false
  const droppedFiles = e.dataTransfer.files
  if (!droppedFiles || droppedFiles.length === 0) return
  const selectedFile = droppedFiles[0]
  if (!selectedFile.type.startsWith('video/')) {
    showMsg('仅支持上传视频文件', 'warning')
    return
  }
  file.value = selectedFile
  videoUrl.value = ''
  await uploadFile()
}

// 本地文件上传
const file = ref(null)
const uploadFile = async () => {
  if (!file.value) return
  uploading.value = true
  const formData = new FormData()
  formData.append('file', file.value)

  try {
    const res = await fetch(API_BASE + '/upload', {
      method: 'POST',
      body: formData,
      credentials: 'include'
    })
    const text = await res.text()
    if (!res.ok) throw new Error(text || '上传失败')
    showMsg('本地上传完成')
    fetchList()
  } catch (error) {
    showMsg('上传失败: ' + error.message, 'error')
  } finally {
    uploading.value = false
  }
}

// URL上传
const handleUrlUpload = async () => {
  if (!videoUrl.value) return
  if (!videoUrl.value.startsWith('http')) {
    showMsg('请输入合法的 http/https 链接', 'warning')
    return
  }
  uploading.value = true
  const formData = new FormData()
  formData.append('url', videoUrl.value)

  try {
    const res = await fetch(API_BASE + '/upload-url', {
      method: 'POST',
      body: formData,
      credentials: 'include'
    })
    const text = await res.text()
    if (!res.ok) throw new Error(text)
    showMsg('链接资源已入库')
    videoUrl.value = ''
    fetchList()
  } catch (error) {
    let errMsg = error.message
    if (errMsg.includes('Unsupported URL')) errMsg = '不支持该平台链接'
    showMsg('解析失败: ' + errMsg, 'error')
  } finally {
    uploading.value = false
  }
}

// 获取视频列表
const fetchList = async () => {
  try {
    const timestamp = new Date().getTime()
    const res = await fetch(API_BASE + '/list?_t=' + timestamp, {
      credentials: 'include'
    })
    const data = await res.json()
    if (data.code === 200 && data.data) {
      mediaList.value = data.data.reverse()
    } else if (Array.isArray(data)) {
      mediaList.value = data.reverse()
    }
  } catch (error) {
    console.error('获取列表失败:', error)
  }
}

// 删除
const deleteItem = async (item) => {
  try {
    await proxy.Confirm(`确认要永久删除 "${item.filename}" 吗？`, async () => {
      const res = await fetch(API_BASE + '/delete?id=' + item.id, {
        method: 'DELETE',
        credentials: 'include'
      })
      const text = await res.text()
      if (text.includes('成功') || text.includes('ok')) {
        showMsg('文件已删除')
        mediaList.value = mediaList.value.filter(i => i.id !== item.id)
      } else {
        showMsg(text, 'error')
      }
    })
  } catch (e) {
    showMsg('删除请求失败', 'error')
  }
}

// 格式化时间
const formatTime = (timeStr) => {
  if (!timeStr) return '--'
  const date = new Date(timeStr)
  return `${date.getMonth() + 1}/${date.getDate()} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

// 下载音频
const downloadAudio = async (item) => {
  let fileName = item.filename || 'audio.mp3'
  fileName = fileName.replace(/\.[^/.]+$/, '') + '.mp3'
  try {
    showMsg('正在转码并下载...')
    const res = await fetch(API_BASE + '/download?id=' + item.id, {
      credentials: 'include'
    })
    if (!res.ok) throw new Error('下载失败')
    const blob = await res.blob()
    const downloadUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(downloadUrl)
    showMsg('下载完成')
  } catch (e) {
    showMsg('下载失败', 'error')
  }
}

// 提取文字
const transcribe = async (item) => {
  if (item.transcriptText) {
    showAiResult(item, 'text')
    return
  }
  if (pollingTimers.value[item.id] && pollingTimers.value[item.id].type === 'text') {
    drawerVisible.value = true
    drawerTitle.value = '全量文字提取'
    drawerType.value = 'text'
    drawerLoading.value = true
    drawerContent.value = '文字提取正在后台进行中...'
    return
  }
  drawerVisible.value = true
  drawerTitle.value = '全量文字提取'
  drawerType.value = 'text'
  drawerLoading.value = true
  drawerContent.value = '提取任务已提交，正在识别语音流...'
  try {
    await fetch(API_BASE + '/transcribe?id=' + item.id, { credentials: 'include' })
    startPolling(item.id, 'text')
  } catch (e) {
    drawerContent.value = 'Error: ' + e
    drawerLoading.value = false
  }
}

// AI 智能总结
const aiAnalyze = async (item) => {
  if (item.aiSummary && !item.aiSummary.includes('任务已') && !item.aiSummary.includes('正在') && !item.aiSummary.includes('[MQ]')) {
    showAiResult(item, 'ai')
    return
  }
  if (pollingTimers.value[item.id] && pollingTimers.value[item.id].type === 'ai') {
    drawerVisible.value = true
    drawerTitle.value = 'AI 智能总结'
    drawerType.value = 'ai'
    drawerLoading.value = true
    drawerContent.value = '系统正在后台计算中...'
    return
  }
  drawerVisible.value = true
  drawerTitle.value = 'AI 智能总结'
  drawerType.value = 'ai'
  drawerLoading.value = true
  drawerContent.value = '正在向 AI 集群请求计算资源...'
  try {
    const res = await fetch(API_BASE + '/analyze?id=' + item.id, { credentials: 'include' })
    const text = await res.text()
    if (text.includes('⚠') || text.includes('❌')) {
      showMsg(text, 'warning')
      drawerVisible.value = false
      drawerLoading.value = false
      return
    }
    startPolling(item.id, 'ai')
    drawerContent.value = text + '\n\n等待消费者接单处理...'
  } catch (e) {
    drawerContent.value = 'Error: ' + e
    drawerLoading.value = false
  }
}

// 显示已有结果
const showAiResult = (item, type) => {
  drawerVisible.value = true
  drawerTitle.value = type === 'ai' ? 'AI 智能总结' : '全量文字提取'
  drawerType.value = type
  drawerLoading.value = false
  drawerContent.value = type === 'ai' ? item.aiSummary : item.transcriptText
}

// 轮询
const startPolling = (id, type) => {
  if (pollingTimers.value[id]) clearInterval(pollingTimers.value[id].timer)

  const timer = setInterval(async () => {
    await fetchList()
    const item = mediaList.value.find(i => i.id === id)
    if (!item) return

    let isFinished = false
    let result = ''

    if (type === 'ai') {
      const text = item.aiSummary || ''
      const isSuccess = text.includes('##')
      const isError = text.includes('失败') || text.includes('Error') || text.includes('超时')
      if (isSuccess || isError) {
        isFinished = true
        result = text
      }
    } else if (type === 'text') {
      const text = item.transcriptText || ''
      if (text && (text.length > 10 || text.includes('失败'))) {
        isFinished = true
        result = text
      }
    }

    if (isFinished) {
      if (drawerVisible.value) {
        drawerContent.value = result
        drawerLoading.value = false
      }
      if (result.includes('失败') || result.includes('Error')) {
        showMsg('任务结束，但存在错误', 'warning')
      } else {
        showMsg('任务完成')
      }
      clearInterval(timer)
      delete pollingTimers.value[id]
    }
  }, 3000)

  pollingTimers.value[id] = { timer, type }

  setTimeout(() => {
    if (pollingTimers.value[id]) {
      clearInterval(pollingTimers.value[id].timer)
      delete pollingTimers.value[id]
    }
  }, 300000)
}

// 清理轮询
onBeforeUnmount(() => {
  Object.values(pollingTimers.value).forEach(item => {
    if (item.timer) clearInterval(item.timer)
  })
})

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.ai-analysis {
  padding: 10px 20px;
  height: calc(100vh - 56px);
  overflow-y: auto;
}

.ai-header {
  margin-bottom: 20px;
}

.ai-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 6px;
}

.ai-desc {
  color: #909399;
  font-size: 14px;
}

.upload-zone {
  margin-bottom: 24px;
}

.upload-split {
  display: flex;
  gap: 16px;
  height: 180px;
}

.upload-pane {
  flex: 1;
  border: 2px dashed #dcdfe6;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  background: #fafafa;
}

.upload-pane:hover {
  border-color: #409eff;
  background: #ecf5ff;
}

.local-pane {
  border-right-width: 2px;
}

.pane-title {
  font-size: 16px;
  font-weight: 600;
  margin: 8px 0 4px;
}

.pane-desc {
  color: #909399;
  font-size: 13px;
}

.url-input-box {
  margin-top: 12px;
  width: 80%;
  max-width: 280px;
}

.video-list-section {
  margin-top: 8px;
}

.section-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  padding-bottom: 10px;
  border-bottom: 2px solid #ebeef5;
}

.section-title {
  font-size: 17px;
  font-weight: 700;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
}

.video-card {
  transition: transform 0.2s;
}

.video-card:hover {
  transform: translateY(-2px);
}

.card-header-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.card-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.card-meta {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-tags {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 4px;
}

.time-tag {
  color: #909399;
  font-size: 12px;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 12px;
  flex-wrap: wrap;
}

.ai-preview {
  margin-top: 10px;
  padding: 8px 12px;
  background: #ecf5ff;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #409eff;
  transition: background 0.2s;
}

.ai-preview:hover {
  background: #d9ecff;
}

.ai-preview.text-preview {
  background: #f0f9eb;
  color: #67c23a;
}

.ai-preview.text-preview:hover {
  background: #e1f3d8;
}

.preview-label {
  font-weight: 500;
}

.empty-state {
  margin-top: 60px;
}

/* 抽屉样式 */
.drawer-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  gap: 16px;
  color: #909399;
}

.drawer-content {
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

/* 暗色模式 */
html.dark .upload-pane {
  background: #2c2d2f;
  border-color: #4c4d4f;
}

html.dark .upload-pane:hover {
  background: #1a2b4c;
  border-color: #409eff;
}

html.dark .section-bar {
  border-bottom-color: #4c4d4f;
}

html.dark .ai-preview {
  background: #1a2b4c;
}

html.dark .ai-preview.text-preview {
  background: #1a3320;
}

html.dark .text-body pre {
  background: #1a1a1a;
}
</style>
