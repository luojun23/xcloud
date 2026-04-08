<template>
  <div class="sys-setting">
    <el-form
        :model="formData"
        :rules="rule"
        ref="formDataRef"
        label-width="120px"
        @submit.prevent
    >
      <!--input输入 -->
      <el-form-item label="注册邮件标题" prop="registerEmailTitle">
        <el-input
            clearable
            placeholder="请输入注册邮件标题"
            v-model.trim="formData.registerEmailTitle"
        >
        </el-input>
      </el-form-item>
      <el-form-item label="注册邮件内容" prop="registerEmailContent">
        <el-input
            clearable
            placeholder="请输入注册邮件内容"
            v-model.trim="formData.registerEmailContent"
        >
        </el-input>
      </el-form-item>
      <el-form-item label="初始空间大小" prop="userInitUseSpace">
        <el-input
            clearable
            placeholder="请输入初始空间大小"
            v-model.trim="formData.userInitUseSpace"
        >
          <template #suffix>MB</template>
        </el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="saveSysSettings">保存</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import {getCurrentInstance, nextTick, ref} from "vue";
const {proxy} = getCurrentInstance();
const formData = ref({});
const formDataRef = ref();
const api = {
  getSysSettings: "/admin/getSysSettings",
  saveSysSettings: "/admin/saveSysSettings",
}

const rule = {
  registerEmailTitle:[
    {required:true,message:"请输入注册邮件标题"},
  ],
  registerEmailContent:[
    {required:true,message:"请输入注册邮件内容"},
  ],
  userInitUseSpace:[
    {required:true,message:"请输入初始空间大小"},
    {validator:proxy.Verify.number,message: "空间大小只能是数值"}
  ],
}

const getSysSettings = async () => {
  let result = await proxy.Request({
    url: api.getSysSettings,
  })
  if (!result) {
    return;
  }
  formData.value = result.data;
}
getSysSettings()

const saveSysSettings = ()=>{
  formDataRef.value.validate(async (valid)=>{
    if (!valid){
      return;
    }
    let parmas = {};
    Object.assign(parmas,formData.value)
    let result = proxy.Request({
      url:api.saveSysSettings,
      params:parmas,
    })
    if (!result){
      return;
    }
    proxy.Message.success("操作成功")
  })
}
</script>


<style lang="scss" scoped>
.sys-setting{
  margin-top: 20px;
  width: 600px;
}
</style>