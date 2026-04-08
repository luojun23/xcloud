<template>
  <div class="no-data">
    <div>
      <Icon
        icon-name="no_data"
        :width="120"
      ></Icon>
    </div>
    <div class="msg">{{msg}}</div>
  </div>
</template>

<script setup>
const props = defineProps({
  msg:{
    type:String,
  },
});
</script>

<style>
.no-data{
  text-align: center;
  padding: 10px 0px;
  .icon-empty{
    font-size: 50px;
    color: #bbb;
  }
  .msg{
    margin-top:10px;
    color: #909399;
    font-size: 14px;
  }
}
</style>