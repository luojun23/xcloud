<template>
  <div class="top">
    <el-button type="success" :disabled="selectFileList.length==0" @click="revertBatch">
      <span class="iconfont icon-revert"></span>还原</el-button>
    <el-button type="danger" @click="cancelFileBatch" :disabled="selectFileList.length==0">
      <span class="iconfont icon-del"></span>批量删除</el-button>
  </div>
  <div class="file-list">
    <Table
        ref="dataTableRef"
        :columns="columns"
        :show-pagination="true"
        :dataSource="tableData"
        :fetch="loadDataList"
        :initFetch="true"
        :options="tableOptions"
        @rowSelected="rowSelected"
    >
      <template #fileName="{index,row}">
        <div class="file-item"
             @mouseenter="showOp(row)"
             @mouseleave="cancelShowOp(row)">
          <template v-if="(row.fileType==3||row.fileType==1)&&row.status!=0">
            <Icon :cover="row.fileCover" :width="32"></Icon>
          </template>
          <template v-else>
            <Icon v-if="row.folderType == 0" :file-type="row.fileType"></Icon>
            <Icon v-if="row.folderType == 1" :file-type="0"></Icon>
          </template>
          <span class="file-name" :title="row.fileName">
            <span> {{ row.fileName }}</span>
          </span>
          <span class="op" v-if="row.showOp">
            <span class="iconfont icon-revert" @click="revert(row)">还原</span>
            <span class="iconfont icon-del" @click="cancel(row)">删除</span>
          </span>
        </div>
      </template>
      <template #fileSize="{ index,row }">
        <span v-if="row.fileSize">{{ proxy.Utils.sizeToStr(row.fileSize) }}</span>
      </template>
    </Table>
  </div>
</template>

<script setup>
import {getCurrentInstance, nextTick, ref, defineEmits} from "vue";
import Icon from "@/components/Icon.vue";
const {proxy} = getCurrentInstance();
const api = {
  loadDataList:"/recycle/loadRecycleList",
  recoverFile:"/recycle/recoverFile",
  delFile:"/recycle/delFile"
}

const columns = [
  {
    label: "文件名",
    prop: "fileName",
    scopedSlots: "fileName"
  },
  {
    label: "删除时间",
    prop: "recoveryTime",
    width: "250"
  },
  {
    label: "大小",
    prop: "fileSize",
    scopedSlots: "fileSize",
    width: "200"
  },
];

const tableData = ref({});
const tableOptions = ref({
  extHeight: 50,
  selectType: "checkbox",
})
const showLoading = ref(false)
const loadDataList = async () => {
  let params = {
    pageNo: tableData.value.pageNo,
    pageSize: tableData.value.pageSize
  };
  let result = await proxy.Request({
    url: api.loadDataList,
    showLoading: showLoading.value,
    params: params,
  })
  if (!result) {
    return;
  }
  tableData.value = result.data;
}

//多选
const selectFileList = ref([]);
const rowSelected = (rows) => {
  selectFileList.value = [];
  rows.forEach((item) => {
    selectFileList.value.push(item.fileId);
  })
}

const showOp =(row)=>{
  tableData.value.list.forEach(item=>{
    item.showOp = false
  })
  row.showOp = true
}

const cancelShowOp=(row)=>{
  row.showOp = false
}
//恢复
const revert=(row)=>{
  proxy.Confirm(`你确定要还原【${row.fileName}】吗?`, async () => {
    let result = await proxy.Request({
      url: api.recoverFile,
      params: {
        fileIds: row.fileId
      }
    })
    if (!result) {
      return
    }
    proxy.Message.success("还原成功")
    loadDataList()
  })
}

//批量恢复
const revertBatch=()=>{
  proxy.Confirm(`你确定要还原这些文件吗?`, async () => {
    let result = await proxy.Request({
      url: api.recoverFile,
      params: {
        fileIds: selectFileList.value.join(",")
      }
    })
    if (!result) {
      return
    }
    proxy.Message.success("还原成功")
    loadDataList()
  })
}

const cancel = (row) => {
  cancelShareIdList.value = [row.fileId]
  cancelShareDone();
}

//批量删除
const cancelShareIdList = ref([])
const cancelFileBatch = () => {
  if (selectFileList.value.length == 0) {
    return;
  }
  cancelShareIdList.value = selectFileList.value
  cancelShareDone();
}

const cancelShareDone = () => {
  proxy.Confirm("你确定要删除这些文件吗?删除后不可恢复哦！", async () => {
    let result = await proxy.Request({
      url: api.delFile,
      params: {
        fileIds: cancelShareIdList.value.join(",")
      }
    })
    if (!result) {
      return
    }
    proxy.Message.success("删除成功")
    loadDataList()
  })
}
</script>

<style lang="scss" scoped>
@import "@/assets/file.list.scss";
.file-list{
  margin-top: 10px;
  .op{
    width: 120px;
  }
}
</style>