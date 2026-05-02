<template>
  <div class="music-preview">
    <div class="record-box">
      <!-- 唱针 -->
      <div class="needle" :class="{play: isPlaying}">
        <div class="needle-head"></div>
        <div class="needle-body"></div>
        <div class="needle-tip"></div>
      </div>

      <!-- 唱片 -->
      <div class="disc" :class="{play: isPlaying}">
        <div class="disc-inner">
          <img :src="coverUrl" alt="cover"/>
        </div>
      </div>
    </div>

    <div class="song-name">{{ props.fileName }}</div>

    <div class="audio-controls">
      <audio ref="audioRef" :src="audioUrl" :loop="isLoop" @timeupdate="updateTime" @loadedmetadata="onLoaded" @ended="onEnded"></audio>

      <div class="control-row">
        <div class="btn-play" @click="togglePlay">
          <span v-if="!isPlaying">&#9654;</span>
          <span v-else>&#10073;&#10073;</span>
        </div>

        <div class="time-display">
          {{ formatTime(currentTime) }} / {{ formatTime(duration) }}
        </div>

        <div class="progress-container" @click="seek">
          <div class="progress-track">
            <div class="progress-fill" :style="{width: progressPercent + '%'}"></div>
            <div class="progress-dot" :style="{left: progressPercent + '%'}"></div>
          </div>
        </div>

        <div class="extra-btns">
          <div class="volume-box">
            <span class="btn-icon" @click="showVolume = !showVolume">{{ isMuted ? '&#128263;' : '&#128266;' }}</span>
            <div class="volume-popover" v-show="showVolume">
              <div class="volume-bar" @click="setVolume">
                <div class="volume-fill" :style="{height: Math.round(volume * 100) + '%'}"></div>
              </div>
            </div>
          </div>
          <span class="btn-icon loop-btn" @click="toggleLoop" :title="isLoop ? '单曲循环' : '单曲播放'">{{ isLoop ? '&#128257;' : '&#10148;' }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {ref, computed} from "vue";

const props = defineProps({
  url: {
    type: String
  },
  fileName:{
    type:String
  }
})

const audioRef = ref(null)
const isPlaying = ref(false)
const isMuted = ref(false)
const isLoop = ref(true)
const showVolume = ref(false)
const volume = ref(1)
const currentTime = ref(0)
const duration = ref(0)

const audioUrl = computed(() => `/api/${props.url}`)
const coverUrl = new URL('@/assets/player.jpg', import.meta.url).href
const progressPercent = computed(() => {
  if (duration.value === 0) return 0
  return (currentTime.value / duration.value) * 100
})

const togglePlay = () => {
  if (!audioRef.value) return
  if (isPlaying.value) {
    audioRef.value.pause()
    isPlaying.value = false
  } else {
    audioRef.value.play().then(() => {
      isPlaying.value = true
    }).catch(() => {})
  }
}

const updateTime = () => {
  if (audioRef.value) {
    currentTime.value = audioRef.value.currentTime
  }
}

const onLoaded = () => {
  if (audioRef.value) {
    duration.value = audioRef.value.duration
    audioRef.value.volume = volume.value
  }
}

const onEnded = () => {
  isPlaying.value = false
  currentTime.value = 0
}

const toggleMute = () => {
  if (!audioRef.value) return
  audioRef.value.muted = !audioRef.value.muted
  isMuted.value = audioRef.value.muted
}

const setVolume = (e) => {
  if (!audioRef.value) return
  const rect = e.currentTarget.getBoundingClientRect()
  const percent = 1 - Math.max(0, Math.min(1, (e.clientY - rect.top) / rect.height))
  volume.value = percent
  audioRef.value.volume = percent
  if (isMuted.value && percent > 0) {
    audioRef.value.muted = false
    isMuted.value = false
  }
}

const toggleLoop = () => {
  isLoop.value = !isLoop.value
  if (audioRef.value) {
    audioRef.value.loop = isLoop.value
  }
}

const seek = (e) => {
  if (!audioRef.value || duration.value === 0) return
  const rect = e.currentTarget.getBoundingClientRect()
  const percent = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width))
  audioRef.value.currentTime = percent * duration.value
}

const formatTime = (seconds) => {
  if (!seconds || isNaN(seconds)) return '0:00'
  const m = Math.floor(seconds / 60)
  const s = Math.floor(seconds % 60)
  return `${m}:${s.toString().padStart(2, '0')}`
}
</script>

<style lang="scss" scoped>
.music-preview{
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: #f5f5f7;
  padding: 40px 20px;
  box-sizing: border-box;
}

.record-box{
  position: relative;
  width: 260px;
  height: 260px;
  margin-bottom: 30px;
}

.needle{
  position: absolute;
  top: -25px;
  right: 70px;
  width: 24px;
  z-index: 10;
  transform-origin: 12px 0;
  transform: rotate(-35deg);
  transition: transform 0.6s ease;

  &.play{
    transform: rotate(0deg);
  }

  .needle-head{
    width: 20px;
    height: 20px;
    background: #fff;
    border-radius: 50%;
    margin: 0 auto;
    box-shadow: 0 2px 6px rgba(0,0,0,0.15);
  }

  .needle-body{
    width: 3px;
    height: 70px;
    background: #e0e0e0;
    margin: 0 auto;
    border-radius: 2px;
  }

  .needle-tip{
    width: 14px;
    height: 22px;
    background: #fff;
    margin: 0 auto;
    border-radius: 3px;
    box-shadow: 0 1px 4px rgba(0,0,0,0.15);
  }
}

.disc{
  width: 260px;
  height: 260px;
  border-radius: 50%;
  background:
    radial-gradient(circle at 30% 30%, rgba(255,255,255,0.08) 0%, transparent 50%),
    repeating-radial-gradient(#181818 0, #181818 2px, #0f0f0f 3px, #0f0f0f 4px);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 12px 40px rgba(0,0,0,0.25);
  animation: rotate 25s linear infinite;
  animation-play-state: paused;

  &.play{
    animation-play-state: running;
  }

  &::after{
    content: '';
    position: absolute;
    width: 70px;
    height: 70px;
    border-radius: 50%;
    background: #111;
    z-index: 2;
  }

  .disc-inner{
    width: 160px;
    height: 160px;
    border-radius: 50%;
    overflow: hidden;
    z-index: 3;

    img{
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.song-name{
  font-size: 18px;
  color: #333;
  margin-bottom: 30px;
  text-align: center;
  max-width: 80%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audio-controls{
  width: 100%;
  max-width: 520px;
}

.control-row{
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 20px;
}

.btn-play{
  font-size: 18px;
  color: #333;
  cursor: pointer;
  user-select: none;
  width: 28px;
  text-align: center;
  flex-shrink: 0;
}

.time-display{
  font-size: 13px;
  color: #666;
  white-space: nowrap;
  font-family: monospace;
  flex-shrink: 0;
}

.progress-container{
  flex: 1;
  height: 20px;
  display: flex;
  align-items: center;
  cursor: pointer;
}

.progress-track{
  width: 100%;
  height: 4px;
  background: #ddd;
  border-radius: 2px;
  position: relative;
}

.progress-fill{
  height: 100%;
  background: #333;
  border-radius: 2px;
  transition: width 0.1s linear;
}

.progress-dot{
  position: absolute;
  top: 50%;
  width: 10px;
  height: 10px;
  background: #333;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  margin-top: 0;
}

.extra-btns{
  display: flex;
  gap: 12px;
  font-size: 16px;
  color: #666;
  flex-shrink: 0;

  .btn-icon{
    cursor: pointer;
    user-select: none;
  }
}

.volume-box{
  position: relative;
}

.volume-popover{
  position: absolute;
  bottom: 28px;
  left: 50%;
  transform: translateX(-50%);
  width: 32px;
  height: 100px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px 0;
  z-index: 20;
}

.volume-bar{
  width: 4px;
  height: 80px;
  background: #e0e0e0;
  border-radius: 2px;
  position: relative;
  cursor: pointer;
}

.volume-fill{
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  background: #333;
  border-radius: 2px;
}

.loop-btn{
  font-size: 14px;
}
</style>