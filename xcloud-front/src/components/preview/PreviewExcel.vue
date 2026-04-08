<template>
  <div v-html="excelContent" class="tableInfo"></div>
</template>

<script setup>
import {getCurrentInstance, ref, onMounted} from "vue";
import * as XLSX from "xlsx"

const {proxy} = getCurrentInstance();

const props = defineProps({
  url: {
    type: String
  }
})

const excelContent = ref()

const initExcel = async () => {
  let result = await proxy.Request({
    url: props.url,
    responseType: "arraybuffer",
  })
  if (!result) {
    return;
  }
  let workbook = XLSX.read(new Uint8Array(result),{type:"array"})
  var worksheet = workbook.Sheets[workbook.SheetNames[0]];
  excelContent.value = XLSX.utils.sheet_to_html(worksheet);
}

onMounted(() => {
  initExcel();
})
</script>

<style lang="scss" scoped>
.tableInfo{
  width: 100%;
  padding: 10px;
  :deep table{
    width: 100%;
    border-collapse: collapse;
    td{
      border: 1px solid #ddd;
      border-collapse: collapse;
      padding: 5px;
      height: 30px;
      min-height: 50px  ;
    }
  }
}
</style>