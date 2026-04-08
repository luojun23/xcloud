<template>
  <Dialog
      :show="dialogConfig.show"
      :title="dialogConfig.title"
      :buttons="dialogConfig.buttons"
      width="500px"
      :showCancel="false"
      @close="dialogConfig.show=false"
  >
    <el-form
        :model="formData"
        :rules="rules"
        ref="formDataRef"
        label-width="80px"
        @submit.prevent
    >
      <!--input输入 -->
      <el-form-item label="新密码" prop="password">
        <el-input
            type="password"
            placeholder="请输入密码"
            size="large"
            v-model.trim="formData.password"
            show-password
        >
          <template #prefix>
            <span class="iconfont icon-password"></span>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item label="请确认" prop="ConfirmPassword">
        <el-input
            type="password"
            placeholder="请再次输入密码"
            size="large"
            v-model.trim="formData.ConfirmPassword"
            show-password
        >
          <template #prefix>
            <span class="iconfont icon-password"></span>
          </template>
        </el-input>
      </el-form-item>
    </el-form>
  </Dialog>
</template>

<script setup>
import {getCurrentInstance, nextTick, ref} from "vue";
import Dialog from "@/components/Dialog.vue";

const { proxy } = getCurrentInstance();
const formData = ref({});
const formDataRef = ref();

const api = {
  updatePassword:"/updatePassword",
}
const ConfirmPassword = (rule,value,callback)=>{
  if (value!==formData.value.password){
    callback(new Error(rule.message));
  }else {
    callback();
  }
}
const rules = {
  password:[
    {required:true,message:"请输入密码"},
    {validator:proxy.Verify.password,message: "密码必须包含字母、数字，且长度至少8位"}
  ],
  ConfirmPassword: [
    {required:true,message:"请再次输入密码"},
    {validator:ConfirmPassword,message: "两次输入密码不一致"}
  ],
}
const dialogConfig = ref({
  show: false,
  title: "修改密码",
  buttons: [
    {
      type:"primary",
      text:"确定",
      click:(e)=>{
        submitForm();
      },
    },
  ],
});
const shows1=()=>{
  dialogConfig.value.show = true;
  nextTick(()=>{
    formDataRef.value.resetFields();
    formData.value = {}
  })
}
defineExpose({shows1})
const submitForm = async ()=>{
  formDataRef.value.validate( (valid) => {
    if (!valid){
      return;
    }
    let result = proxy.Request({
      url:api.updatePassword,
      params:{
        password:formData.value.password
      },
      errorCallback:()=>{
        changeCheckCode(0)
      }
    })
    if (!result){
      return;
    }
    dialogConfig.value.show = false;
    proxy.Message.success("密码修改成功")
});
}
</script>

<style scoped>

</style>