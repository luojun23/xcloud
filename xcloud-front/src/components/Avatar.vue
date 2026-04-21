<template>
<span class="avatar" :style="{width: width + 'px',heigth: width + 'px'}">
  <img
    :src="getAvatarUrl()"
  >
</span>
</template>

<script setup>
import {getCurrentInstance} from "vue";
const { proxy } = getCurrentInstance();

const props = defineProps({
  userId: {
    type:String,
  },
  avatar:{
    type:String,
  },
  timestamp:{
    type:Number,
    default:0,
  },
  width:{
    type:Number,
    default:40,
  },
});

const getAvatarUrl = () => {
  // 如果 avatar 存在且不是 null/undefined/空字符串，则使用 avatar
  if (props.avatar && props.avatar !== 'null' && props.avatar !== '') {
    return props.avatar;
  }
  // 否则使用 getAvatar 接口
  return `${proxy.globalInfo.avatarUrl}${props.userId}?${props.timestamp}`;
};
</script>

<style scoped>
.avatar{
  display: flex;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  img{
    width:100%;
    object-fit: cover;
  }
}
</style>