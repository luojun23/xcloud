<template>
  <div class="qq-callback">
    <el-result icon="info" title="QQ登录回调处理中..." sub-title="请稍候">
    </el-result>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

onMounted(() => {
  // QQ回调处理逻辑 - 后续对接QQ OAuth
  setTimeout(() => {
    router.push('/')
  }, 2000)
})
</script>
