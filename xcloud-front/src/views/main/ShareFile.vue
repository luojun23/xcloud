<template>
  <Dialog
      :show="dialogConfig.show"
      :title="dialogConfig.title"
      :buttons="dialogConfig.buttons"
      width="500px"
      :showCancel="showCancle"
      @close="dialogConfig.show=false"
  >
    <el-form
        :model="formData"
        :rules="rules"
        ref="formDataRef"
        label-width="100px"
        @submit.prevent
    >
      <template v-if="showType==0">
        <el-form-item label="文件">
          {{ formData.fileName }}
        </el-form-item>
        <el-form-item label="有效期" prop="validType">
          <el-radio-group v-model="formData.validType">
            <el-radio :label="0">1天</el-radio>
            <el-radio :label="1">7天</el-radio>
            <el-radio :label="2">10天</el-radio>
            <el-radio :label="3">永久有效</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="提取码" prop="codeType">
          <el-radio-group v-model="formData.codeType">
            <el-radio :label="0">自定义</el-radio>
            <el-radio :label="1">系统生成</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="formData.codeType==0" prop="code">
          <el-input clearable
                    placeholder="请输入5位提取码"
                    v-model="formData.code"
                    maxlength="5"
                    :style="{width:'130px'}"
          ></el-input>
        </el-form-item>
      </template>
      <template v-else>
        <el-form-item label="分享链接">{{shareUrl}}{{resultInfo.shareId}}</el-form-item>
        <el-form-item label="提取码">{{resultInfo.code}}</el-form-item>
        <el-form-item>
          <el-button type="primary" @click="copy">复制链接及提取码</el-button>
        </el-form-item>
      </template>
    </el-form>
  </Dialog>
</template>

<script setup>
import useClipboard from "vue-clipboard3";
const { toClipboard } = useClipboard()
import Dialog from "@/components/Dialog.vue";
import {getCurrentInstance, nextTick, ref} from "vue";

const {proxy} = getCurrentInstance();

const api = {
  shareFile: "/share/shareFile"
}
const shareUrl = ref(document.location.origin + "/share/");

const formData = ref({});
const formDataRef = ref();
const rules = {
  validType: [
    {required: true, message: "请选择有效期"},
  ],
  codeType: [
    {required: true, message: "请选择提取码类型"},
  ],
  code: [
    {required: true, message: "请输入提取码"},
    {validator: proxy.Verify.shareCode, message: "请输入提取码"},
    {min: 5, message: "提取码至少5位"}
  ]
}
//关闭按钮
const showCancle = ref(true);
const showType = ref(0)
const dialogConfig = ref({
  show: false,
  title: "分享",
  buttons: [
    {
      type: "primary",
      text: "确定",
      click: (e) => {
        share();
      },
    },
  ],
});

const resultInfo = ref({})
const share = async () => {
  if (Object.keys(resultInfo.value).length > 0) {
    dialogConfig.value.show = false;
    return;
  }
  formDataRef.value.validate(async (valid)=>{
    if (!valid){
      return;
    }
    let params = {}
    Object.assign(params,formData.value)
    let result =await proxy.Request({
      url:api.shareFile,
      params:params
    })
    if (!result){
      return
    }
    showType.value = 1;
    resultInfo.value = result.data
    dialogConfig.value.buttons[0].text="关闭"
    showCancle.value = false;
  })
}

const shareShow = (data) => {
  showType.value = 0;
  dialogConfig.value.show = true;
  showCancle.value = true;
  resultInfo.value= {}
  nextTick(() => {
    formDataRef.value.resetFields();
    formData.value = Object.assign({}, data)
  })
}
defineExpose({shareShow})

const copy =async ()=>{
  await toClipboard(`链接：${shareUrl.value}${resultInfo.value.shareId} 提取码：${resultInfo.value.code}`);
  proxy.Message.success("复制成功")
}
</script>

<style>

</style>