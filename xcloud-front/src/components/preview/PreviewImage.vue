<template>
  <div class="image-viewer">
    <el-image-viewer
        :initial-index="previewImgIndex"
        hide-on-click-modal
        :url-list="imageList"
        @close="closeImgViewer"
        v-if="previewImgIndex!=null"
    >
    </el-image-viewer>
  </div>
</template>

<script setup>
import {getCurrentInstance, nextTick, ref,defineEmits} from "vue";
const {proxy} = getCurrentInstance();

const props = defineProps({
  imageList:{
    type:Array,
  }
})
const previewImgIndex = ref(null)
const show = (index)=>{
  stopScroll()
  previewImgIndex.value = index;
}
defineExpose({
  show
})
const closeImgViewer = ()=>{
  startScroll()
  previewImgIndex.value = null;
}

const stopScroll=()=>{
  document.body.style.overflow = "hidden";
}

const startScroll = ()=>{
  document.body.style.overflow = "auto";
}
</script>

<style>
.image-viewer{
  .el-image-viewer_msk{
    opacity: 0.7;
  }
}
</style>