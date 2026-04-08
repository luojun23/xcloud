<template>
  <div class="code" >
    <div class="tops">
      <div class="encode-select">
        <el-select placeholder="选择编码" clearable v-model="encode" @change="changeEncode">
          <el-option value="utf8" label="UTF8编码"></el-option>
          <el-option value="gbk" label="GBK编码"></el-option>
        </el-select>
        <div class="tips">乱码了?切换编码</div>
      </div>
      <div class="copy-btn">
        <el-button type="primary" @click="copy">复制</el-button>
      </div>
    </div>
    <highlightjs autodetect :code="txtContent"></highlightjs>
  </div>
</template>

<script setup>
import useClipboard from "vue-clipboard3";
const { toClipboard } = useClipboard()
import {read} from "xlsx";
import {getCurrentInstance, ref, onMounted} from "vue";
const {proxy} = getCurrentInstance();

const props = defineProps({
  url: {
    type: String
  }
})

const codeRef = ref()
const txtContent = ref("")
const blobResult = ref()
const encode = ref("utf8")
const  readTxt = async ()=>{
  let result = await proxy.Request({
    url:props.url,
    responseType:"blob"
  })
  if (!result){
    return
  }
  blobResult.value = result;
  showTxt();
}
//代码高亮
const showTxt=()=>{
    const reader = new FileReader();
    reader.onload=()=>{
        let txt= reader.result;
        txtContent.value = txt
    }
    reader.readAsText(blobResult.value,encode.value);
}

const changeEncode=(e)=>{
  encode.value = e;
  showTxt()
}
const copy =async ()=>{
  await toClipboard(txtContent.value);
  proxy.Message.success("复制成功")
}
onMounted(() => {
  readTxt();
})
</script>

<style lang="scss" scoped>
.code{
  width: 100%;
  .tops{
    display: flex;
    align-items: center;
    justify-content: space-around;
  }
  .encode-select{
    flex: 1;
    display: flex;
    align-items: center;
    margin: 5px 10px;
    height: 32px;
    .tips{
      margin-left: 10px;
      color: #828282;
    }
  }
  .copy-btn{
    margin-right: 10px;
  }
  pre{
    margin: 0px;
  }
}
</style>