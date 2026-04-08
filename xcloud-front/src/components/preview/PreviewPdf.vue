<template>
  <div class="pdf">
    <vue-pdf-embed ref="pdfRef"
                  :source="state.url"
                  class="vue-pdf-embed"
                  width="850"
                  :page="state.pageNum"
    >
    </vue-pdf-embed>
  </div>
</template>

<script setup>
import {getCurrentInstance, ref, onMounted} from "vue";
import VuePdfEmbed from "vue-pdf-embed"
const {proxy} = getCurrentInstance();

const props = defineProps({
  url: {
    type: String
  }
})

const pdfRef = ref()

const state = ref({
  url: "",
  pageNum: 0,
  pageSize: 0,
})

const initPdf = async () => {
  state.value.url = "/api" + props.url
}

onMounted(() => {
  initPdf();
})
</script>

<style lang="scss" scoped>
.pdf {
  width: 100%;
}
</style>