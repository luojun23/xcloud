<template>
  <div class="video-player-wrapper">
    <div ref="player" id="player"></div>
    <div class="ai-actions" v-if="fileId">
      <el-button type="primary" size="small" :icon="MagicStick" @click="handleAiAnalyze" :loading="aiLoading">AI 智能总结</el-button>
      <el-button size="small" :icon="Document" @click="handleTranscribe" :loading="transcribeLoading">提取文字</el-button>
    </div>
    <AiPanel ref="aiPanelRef" />
  </div>
</template>

<script setup>
import DPlayer from "dplayer"
import {getCurrentInstance, nextTick, ref, defineEmits, onMounted, onBeforeUnmount} from "vue";
import Hls from "hls.js";
import {MagicStick, Document} from '@element-plus/icons-vue'
import AiPanel from '@/components/ai/AiPanel.vue'
const {proxy} = getCurrentInstance();

const props = defineProps({
  url:{
    type:String
  },
  fileId:{
    type:String,
    default:''
  }
})

const videoInfo = ref({
  video:null
})

const player = ref()
const aiPanelRef = ref()
const aiLoading = ref(false)
const transcribeLoading = ref(false)
const pollingTimer = ref(null)

const initPlayer = ()=>{
  const dp = new DPlayer({
    element:player.value,
    theme:"#b7daff",
    screenshot:true,
    video:{
      url:`/api/${props.url}`,
      type:"customHls",
      customType:{
        customHls:function (video,player){
          const hls = new Hls();
          hls.loadSource(video.src)
          hls.attachMedia(video);
        }
      }
    }
  });
}

const API_BASE = '/api/ai'

// AI 智能分析
const handleAiAnalyze = async () => {
  if (!props.fileId) return
  aiLoading.value = true
  aiPanelRef.value.show('ai', 'AI 智能总结', '', true, '正在向 AI 集群请求计算资源...')
  try {
    const res = await fetch(API_BASE + '/analyze?fileId=' + props.fileId, { credentials: 'include' })
    const text = await res.text()
    if (text.includes('⚠') || text.includes('❌')) {
      aiPanelRef.value.updateContent(text)
      aiLoading.value = false
      return
    }
    aiPanelRef.value.setLoading(true, text + '\n\n等待处理结果...')
    startPolling('ai')
  } catch (e) {
    aiPanelRef.value.updateContent('Error: ' + e.message)
    aiLoading.value = false
  }
}

// 提取文字
const handleTranscribe = async () => {
  if (!props.fileId) return
  transcribeLoading.value = true
  aiPanelRef.value.show('text', '全量文字提取', '', true, '提取任务已提交，正在识别语音流...')
  try {
    await fetch(API_BASE + '/transcribe?fileId=' + props.fileId, { credentials: 'include' })
    startPolling('text')
  } catch (e) {
    aiPanelRef.value.updateContent('Error: ' + e.message)
    transcribeLoading.value = false
  }
}

// 轮询结果
const startPolling = (type) => {
  if (pollingTimer.value) clearInterval(pollingTimer.value)
  pollingTimer.value = setInterval(async () => {
    try {
      const res = await fetch(API_BASE + '/fileInfo?fileId=' + props.fileId, { credentials: 'include' })
      const data = await res.json()
      const fileInfo = data.data || data
      
      let isFinished = false
      let result = ''
      
      if (type === 'ai') {
        const text = fileInfo.aiSummary || ''
        if (text.includes('##') || text.includes('失败') || text.includes('Error')) {
          isFinished = true
          result = text
        }
      } else {
        const text = fileInfo.transcriptText || ''
        if (text && (text.length > 10 || text.includes('失败'))) {
          isFinished = true
          result = text
        }
      }
      
      if (isFinished) {
        aiPanelRef.value.updateContent(result)
        aiLoading.value = false
        transcribeLoading.value = false
        clearInterval(pollingTimer.value)
        pollingTimer.value = null
      }
    } catch (e) {
      console.error('轮询失败:', e)
    }
  }, 3000)
  
  // 5分钟超时
  setTimeout(() => {
    if (pollingTimer.value) {
      clearInterval(pollingTimer.value)
      pollingTimer.value = null
      aiLoading.value = false
      transcribeLoading.value = false
    }
  }, 300000)
}

onMounted(()=>{
  initPlayer();
})

onBeforeUnmount(() => {
  if (pollingTimer.value) clearInterval(pollingTimer.value)
})
</script>

<style scoped>
.video-player-wrapper {
  position: relative;
}

#player{
  width:100%;
  :deep .dplayer-video-wrap{
    text-align: center;
    .dplayer-video{
      margin: 0px auto;
      max-height: calc(100vh - 41px);
    }
  }
}

.ai-actions {
  display: flex;
  gap: 8px;
  padding: 10px 0;
  justify-content: center;
  border-top: 1px solid #ebeef5;
  margin-top: 4px;
}
</style>