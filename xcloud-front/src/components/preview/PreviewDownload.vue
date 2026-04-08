<template>
  <div class="others">
    <div class="body-content">
      <Icon
          :icon-name="fileInfo.fileType==0?'zip':'others'"
          width="80"
      ></Icon>
      <div class="file-name">{{fileInfo.fileName}}</div>
      <div class="txt">该类型的文件暂不支持预览 请下载后查看</div>
      <div class="download-btn">
        <el-button type="primary" @click="download">点击下载{{proxy.Utils.sizeToStr(fileInfo.fileSize)}}</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import {getCurrentInstance, ref, onMounted, onUnmounted} from "vue";
import {read} from "xlsx";
import Icon from "@/components/Icon.vue";
import Utils from "@/utils/Utils";
const {proxy} = getCurrentInstance();

const props = defineProps({
  downloadUrl: {
    type: String
  },
  createDownloadUrl:{
    type:String
  },
  fileInfo:{
    type:Object
  }
})

const download =async()=>{
  let result = await proxy.Request({
    url:props.createDownloadUrl
  })
  if (!result){
    return;
  }
  window.location.href = props.downloadUrl+"/"+result.data
}

onMounted(() => {

})

onUnmounted(()=>{
})
</script>

<style scoped>
.others{
  display: flex;
  align-items: center;
  justify-content: center;
  width:100%;
  .body-content{
    text-align:center;
    .file-name{
      font-weight: bold;
    }
    .txt{
      color: #999898;
      margin-top:5px;
      font-size:13px;
    }
    .download-btn{
      margin-top: 20px;
    }
  }
}
</style>