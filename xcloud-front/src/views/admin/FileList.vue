<template>
  <div>
    <div class="top">
      <div class="top-op">
        <div class="btn">
          <el-button type="danger" :disabled="selectFileList.length==0" @click="delFileBatch">
          <span class="iconfont icon-del" style="font-size: 14px">
            批量删除
          </span>
          </el-button>
        </div>
        <div class="search-panel">
          <el-input clearable placeholder="请输入文件名搜索"
                    v-model="fileNameFuzzy"
                    @keyup.enter="search"
          >
            <template #suffix>
              <i class="iconfont icon-search" @click="search"></i>
            </template>
          </el-input>
        </div>
        <div class="iconfont icon-refresh" @click="refresh"></div>
      </div>
      <!-- 导航     -->
      <Navigation ref="navigationRef" @navChange="navChange" :adminShow="true" :watchPath="false"></Navigation>
    </div>
    <div class="file-list" >
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
        <template #fileName="{ index,row }">
          <div class="file-item" @mouseenter="showOp(row)" @mouseleave="cancelShowOp(row)">
            <template v-if="(row.fileType==3||row.fileType==1)&&row.status==2">
              <Icon :cover="row.fileCover" :width="34"></Icon>
            </template>
            <template v-else>
              <Icon v-if="row.folderType == 0" :file-type="row.fileType"></Icon>
              <Icon v-if="row.folderType == 1" :file-type="0" @click="preview(row)"></Icon>
            </template>
            <span class="file-name" :title="row.fileName" v-if="!row.showEdit">
              <span @click="preview(row)"> {{ row.fileName }}</span>
              <span v-if="row.status==0" class="transfer-status">转码中</span>
              <span v-if="row.status==1" class="transfer-status transfer-fail">转码失败</span>
            </span>
            <div class="edit-panel" v-if="row.showEdit">
              <el-input v-model.trim="row.fileNameReal"
                        ref="editNameRef"
                        :maxlength="190"
                        @keyup.enter="saveNameEdit(index)">
                <template #suffix>{{ row.fileNameSuffix }}</template>
              </el-input>
              <span :class="['iconfont icon-right1',row.fileNameReal?'':'not-allow']"
                    @click="saveNameEdit(index)"></span>
              <span class="iconfont icon-error" @click="cancelNameEdit(index)"></span>
            </div>
            <span class="op">
              <template v-if="row.showOp&&row.fileId&&row.status==2">
                <span class="iconfont icon-download" v-if="row.folderType==0" @click="download(row)">下载</span>
                <span class="iconfont icon-del" @click="delFile(row)">删除</span>
              </template>
            </span>
          </div>
        </template>
        <template #fileSize="{ index,row }">
          <span v-if="row.fileSize">{{ proxy.Utils.sizeToStr(row.fileSize) }}</span>
        </template>
      </Table>
    </div>
    <Preview ref="previewRef"></Preview>
  </div>
</template>

<script setup>
import {getCurrentInstance, ref} from "vue";

import Navigation from "@/components/Navigation.vue";
import Icon from "@/components/Icon.vue";

const {proxy} = getCurrentInstance();

const api = {
  loadDataList:"/admin/loadFileList",
  delFile:"/admin/delFile",
  createDownloadUrl: "/admin/createDownloadUrl",
  downloadUrl: "/api/admin/download"
}


const columns = [
  {
    label: "文件名",
    prop: "fileName",
    scopedSlots: "fileName"
  },
  {
    label: "发布人",
    prop: "nickName",
    width: "200"
  },
  {
    label: "修改时间",
    prop: "lastUpdateTime",
    width: "250"
  },
  {
    label: "大小",
    prop: "fileSize",
    scopedSlots: "fileSize",
    width: "200"
  },
];
//当前目录
const currentFolder = ref({fileId: "0"});
const fileNameFuzzy = ref();
//分类
const category = ref();

const tableData = ref({});
const tableOptions = ref({
  extHeight: 50,
  selectType: "checkbox",
})
const showLoading = ref(false)
const loadDataList = async () => {
  let params = {
    pageNo: tableData.value.pageNo,
    pageSize: tableData.value.pageSize,
    fileNameFuzzy: fileNameFuzzy.value,
    filePid: currentFolder.value.fileId,
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
    selectFileList.value.push(item.userId+"_"+item.fileId);
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

const navChange = (data) => {
  const {categoryId, curFolder} = data;
  if (typeof curFolder === 'string') {
    currentFolder.value = {fileId: curFolder};
  } else {
    currentFolder.value = curFolder;
  }
  category.value = categoryId;
  loadDataList();
}

const navigationRef = ref();
//预览
const previewRef = ref()
const preview = (data) => {
  //目录
  if (data.folderType == 1) {
    navigationRef.value.openFolder(data)
    return;
  }
  //文件
  if (data.status != 2) {
    proxy.Message.warning("文件转码中,无法预览")
    return;
  }
  previewRef.value.shows(data, 1)
}

//下载文件
const download = async (row) => {
  let result = await proxy.Request({
    url: api.createDownloadUrl + "/" +  row.userId+"/"+row.fileId
  })
  if (!result) {
    return
  }
  window.location.href = api.downloadUrl +"/"+ result.data
}

const delFile = (row) => {
  proxy.Confirm(`你确定要删除【${row.fileName}】吗?删除的文件不可恢复`,
      async () => {
        let result = await proxy.Request(
            {
              url: api.delFile,
              params: {
                fileId: row.userId+"_"+row.fileId,
              }
            })
        if (!result) {
          return;
        }
        proxy.Message.success("删除成功")
        loadDataList();
      }
  )
}
//批量删除
const delFileBatch = () => {
  if (selectFileList.value.length == 0) {
    return;
  }
  proxy.Confirm(`你确定要删除这些文件吗?删除的文件不可恢复`,
      async () => {
        let result = await proxy.Request(
            {
              url: api.delFile,
              params: {
                fileId: selectFileList.value.join(","),
              }
            })
        if (!result) {
          return;
        }
        proxy.Message.success("删除成功")
        loadDataList();
      }
  )
}
</script>


<style lang="scss" scoped>
@import "@/assets/file.list.scss";
.search-panel{
  margin-left: 0px !important;
}
.file-list{
  margin-top: 10px;
  .file-item{
    .op{
      width: 120px;
    }
  }
}
</style>