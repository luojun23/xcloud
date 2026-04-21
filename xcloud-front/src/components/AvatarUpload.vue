<template>
  <div class="avatar-upload">
    <div class="avatar-show">
      <template v-if="localFile">
        <img :src="localFile" style="width: 150px ;height: 150px"/>
      </template>
      <template v-else>
        <img
            :src="'${modelValue.qqAvatar}'"
            v-if="modelValue && modelValue.qqAvatar"
            style="width: 150px ;height: 150px"
        />
        <img :src="`/api/getAvatar/${modelValue.userId}?${timestamp}`" style="width: 150px ;height: 150px" v-else/>
      </template>
    </div>
    <div class="select-btn">
      <el-upload
      :name="file"
      :show-file-list="false"
      accept=".png,.PNG,.jpg,.JPG,.jpeg,.JPEG,.gif,.GIF,.BMP,.bmp"
      :multiple="false"
      :http-request="uploadImage"
      >
        <el-button type="primary">选择</el-button>
      </el-upload>
    </div>
  </div>
</template>

<script setup>
import {getCurrentInstance, reactive, ref, watch} from "vue";
import {useRouter,useRoute} from "vue-router";

const { proxy } = getCurrentInstance();
const router = useRouter();
const route = useRoute();

const timestamp = ref(new Date().getTime())


const props = defineProps({
  modelValue:{
    type:Object,
    default:()=>({})
  }
});

const localFile = ref(null);

const emit = defineEmits();
const uploadImage = async (file) =>{
  file = file.file;
  let img = new FileReader();
  img.readAsDataURL(file);
  img.onload = ({ target }) =>{
    localFile.value = target.result;
  };
  emit("update:modelValue",file)
}
</script>

<style scoped>
.avatar-upload{
  display: flex;
  justify-content: center;
  align-items: end;
  .avatar-show{
    background: rgb(245,245,245);
    width: 150px;
    height: 150px;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    position: relative;
    .iconfont{
      font-size: 50px;
      color: #ddd;
    }
    img{
      width: 150px;
      height: 150px;
    }
    .op{
      position: absolute;
      color:#0e8aef;
      top:80px;
    }
  }
  .select-btn {
    margin-left: 10px;
    vertical-align: bottom;
  }
}
</style>