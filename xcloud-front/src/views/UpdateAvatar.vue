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
        @submit.prevent
    >
      <!--input输入 -->
      <el-form-item label="昵称">
        {{formData.nickname}}
      </el-form-item>
      <el-form-item label="头像">
        <AvatarUpload v-model="formData.avatar"></AvatarUpload>
      </el-form-item>
    </el-form>
  </Dialog>
</template>

<script setup>
import {getCurrentInstance, reactive, ref} from "vue";
import Dialog from "@/components/Dialog.vue";
import {useRoute, useRouter} from "vue-router";
import AvatarUpload from "@/components/AvatarUpload.vue";
import Request from "@/utils/Request";

const router = useRouter();
const route = useRoute();
const { proxy } = getCurrentInstance();

const userInfo = ref(
    proxy.Cookies.get("userInfo")
)
const formData = ref({});
const formDataRef = ref();
const rules = {

}
const api = {
  updateUserAvatar:"/updateUserAvatar",
}

const show =(data)=>{
  formData.value = Object.assign({},data)
  formData.avatar = {userId:data.userId,qqAvatar:data.avatar};
  dialogConfig.value.show = true;
}
defineExpose({show})
const dialogConfig = ref({
  show: false,
  title: "修改头像",
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
const emit = defineEmits();
const submitForm=async()=>{
  if (!(formData.value.avatar instanceof File)){
    dialogConfig.value.show = false;
    return;
  }
    let result =await proxy.Request({
      url:api.updateUserAvatar,
      params:{
         avatar:formData.value.avatar,
      }
    })
    if (!result){
      return;
    }
    dialogConfig.value.show = false
    const CookieUserInfo = proxy.Cookies.get("userInfo")
    delete CookieUserInfo.avatar;
    proxy.Cookies.set("userInfo",CookieUserInfo,0)
    emit("updateAvatar")
}
</script>

<style scoped>

</style>