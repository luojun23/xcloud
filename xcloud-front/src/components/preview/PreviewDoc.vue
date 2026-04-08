<template>
  <div ref="docRef" class="doc-content"></div>
</template>

<script setup>
import {getCurrentInstance, ref, onMounted} from "vue";
import * as doc from "docx-preview"

const {proxy} = getCurrentInstance();

const props = defineProps({
  url: {
    type: String
  }
})

const docRef = ref()

const initDoc = async () => {
  let result = await proxy.Request({
    url: props.url,
    responseType: "blob",
  })
  if (!result) {
    return;
  }
  doc.renderAsync(result, docRef.value)
}

onMounted(() => {
  initDoc();
})
</script>

<style lang="scss" scoped>
.doc-content {
  margin: 0px auto;

  :deep .docx-wrapper {
    background: #fff;
    padding: 10px 0px;
  }

  :deep .docx-wrapper > section.docx {
    margin-bottom: 0px;
  }
}
</style>