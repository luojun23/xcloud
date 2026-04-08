<template>
<div class="center-box">
    <el-popover
    placement="left"
    title="xcloud"
    :width="200"
    trigger="click"
    content="this is content, this is content, this is content"
  >
    <template #reference>
      <span class="a-link">点点我</span>
    </template>
  </el-popover>
</div>
</template>


<script>
import { ref } from 'vue'


</script>

<style>
.center-box {
  width: 100%;
  text-align: center;
  margin-top: 100px;
.m-2 {
  width: 100px;
  margin: 2px;
  color: #0066cc;
  text-align: center;
  text-decoration: none;
}
}

</style>