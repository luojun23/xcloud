<template>
  <div class="ppt-preview">
    <div class="ppt-page-info" v-if="slides.length">
      {{ currentIndex + 1 }} / {{ slides.length }}
    </div>
    <el-carousel
        height="650px"
        :autoplay="false"
        arrow="always"
        indicator-position="outside"
        @change="currentIndex = $event"
    >
      <el-carousel-item v-for="(slide, i) in slides" :key="i">
        <div class="slide-box">
          <div v-for="(text, j) in slide.texts" :key="j" class="slide-text">
            {{ text }}
          </div>
          <img v-for="(img, k) in slide.images" :key="'img'+k" :src="img" class="slide-img"/>
        </div>
      </el-carousel-item>
    </el-carousel>
  </div>
</template>

<script setup>
import {getCurrentInstance, ref, onMounted, onUnmounted} from "vue";
import JSZip from "jszip";

const {proxy} = getCurrentInstance();

const props = defineProps({
  url: {
    type: String
  }
})

const slides = ref([])
const currentIndex = ref(0)
const objectUrls = ref([])

const initPpt = async () => {
  let result = await proxy.Request({
    url: props.url,
    responseType: "blob",
  })
  if (!result) {
    return;
  }

  try {
    const zip = await JSZip.loadAsync(result)

    // 收集所有 slide 文件
    const slideEntries = []
    zip.forEach((path, file) => {
      if (/^ppt\/slides\/slide\d+\.xml$/.test(path)) {
        slideEntries.push({path, file})
      }
    })

    // 按 slide 编号排序
    slideEntries.sort((a, b) => {
      const numA = parseInt(a.path.match(/slide(\d+)\.xml/)[1])
      const numB = parseInt(b.path.match(/slide(\d+)\.xml/)[1])
      return numA - numB
    })

    for (const {path, file} of slideEntries) {
      const xmlStr = await file.async('text')
      const parser = new DOMParser()
      const xmlDoc = parser.parseFromString(xmlStr, 'application/xml')

      // 提取文本 <a:t>
      const textNodes = xmlDoc.getElementsByTagName('a:t')
      const texts = []
      for (let i = 0; i < textNodes.length; i++) {
        const text = textNodes[i].textContent.trim()
        if (text) texts.push(text)
      }

      // 提取图片
      const images = []
      const slideName = path.split('/').pop()
      const relsPath = 'ppt/slides/_rels/' + slideName + '.rels'
      const relsFile = zip.file(relsPath)

      if (relsFile) {
        const relsStr = await relsFile.async('text')
        const relsDoc = parser.parseFromString(relsStr, 'application/xml')
        const relationships = relsDoc.getElementsByTagName('Relationship')

        for (let i = 0; i < relationships.length; i++) {
          const type = relationships[i].getAttribute('Type')
          if (type && type.includes('image')) {
            let target = relationships[i].getAttribute('Target')
            // 处理相对路径 ../media/image1.png -> ppt/media/image1.png
            let imgPath
            if (target.startsWith('../')) {
              imgPath = target.replace('../', 'ppt/')
            } else if (target.startsWith('/')) {
              imgPath = target.substring(1)
            } else {
              imgPath = 'ppt/slides/' + target
            }

            const imgFile = zip.file(imgPath)
            if (imgFile) {
              const imgBlob = await imgFile.async('blob')
              const url = URL.createObjectURL(imgBlob)
              objectUrls.value.push(url)
              images.push(url)
            }
          }
        }
      }

      slides.value.push({texts, images})
    }
  } catch (e) {
    console.error('PPT 解析失败', e)
    proxy.Message.error('PPT 预览失败')
  }
}

onMounted(() => {
  initPpt()
})

onUnmounted(() => {
  objectUrls.value.forEach(url => URL.revokeObjectURL(url))
})
</script>

<style scoped>
.ppt-preview {
  padding: 20px;
}

.slide-box {
  background: #fff;
  border-radius: 8px;
  padding: 40px;
  height: 100%;
  overflow-y: auto;
  box-sizing: border-box;
}

.slide-text {
  margin-bottom: 12px;
  font-size: 16px;
  line-height: 1.6;
  color: #333;
}

.slide-img {
  max-width: 100%;
  max-height: 320px;
  object-fit: contain;
  margin-top: 10px;
  display: block;
}

.ppt-page-info {
  text-align: center;
  margin-bottom: 10px;
  color: #666;
  font-size: 14px;
}
</style>
