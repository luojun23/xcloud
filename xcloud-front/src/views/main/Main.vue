<template>
  <div>
    <div class="top">
      <div class="top-op">
        <div class="btn">
          <el-upload
              :show-file-list="false"
              :with-credentials="true"
              :multiple="true"
              :http-request="addFile"
              :accept="fileAccept"
          >
            <el-button type="primary">
              <span class="iconfont icon-upload"></span>
              上传
            </el-button>
          </el-upload>
        </div>
        <el-button type="success" @click="newFolder" v-if="category=='all'">
          <span class="iconfont icon-folder-add" style="font-size: 14px">
            新建文件夹
          </span>
        </el-button>
        <el-button type="danger" :disabled="selectFileList.length==0" @click="delFileBatch">
          <span class="iconfont icon-del" style="font-size: 14px">
            批量删除
          </span>
        </el-button>
        <el-button type="warning" :disabled="selectFileList.length==0" @click="moveFileBatch">
          <span class="iconfont icon-move" style="font-size: 14px">
            批量移动
          </span>
        </el-button>
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
        <div class="iconfont icon-refresh" @click="refresh">
        </div>
      </div>
      <!-- 导航     -->
      <Navigation ref="navigationRef" @navChange="navChange"></Navigation>
    </div>
    <div class="file-list" v-show="tableData.list&&tableData.list.length>0">
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
            <template v-if="(row.fileType==1||row.fileType==3)&&row.status==2">
              <Icon :cover="row.fileCover" :width="34" @click="preview(row)"></Icon>
            </template>
            <template v-else>
              <!-- 文件 -->
              <Icon v-if="row.folderType == 0" :file-type="row.fileType" @click="preview(row)"></Icon>
              <!-- 文件夹 -->
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
                <span class="iconfont icon-share1" @click="share(row)">分享</span>
                <span class="iconfont icon-download" v-if="row.folderType==0" @click="download(row)">下载</span>
                <span class="iconfont icon-del" @click="delFile(row)">删除</span>
                <span class="iconfont icon-edit" @click="editFileName(index)">重命名</span>
                <span class="iconfont icon-move" @click="moveFolder(row)">移动</span>
              </template>
            </span>
          </div>
        </template>
        <template #fileSize="{ index,row }">
          <span v-if="row.fileSize">{{ proxy.Utils.sizeToStr(row.fileSize) }}</span>
        </template>
      </Table>
    </div>
    <div class="no-data" v-show="!(tableData.list&&tableData.list.length>0)">
      <div class="no-data-inner">
        <Icon iconName="no_data" :width="120" fit="fill"></Icon>
        <div class="tip">当前目录为空,上传你的第一个文件吧</div>
        <div class="op-list">
          <el-upload
              :show-file-list="false"
              :with-credentials="true"
              :multiple="true"
              :http-request="addFile"
              :accept="fileAccept"
          >
            <div class="op-item">
              <Icon iconName="file" :width="60"></Icon>
              <div>上传文件</div>
            </div>
          </el-upload>
          <div class="op-item"
               v-if="category=='all'"
               @click="newFolder"
          >
            <Icon iconName="folder" :width="60"></Icon>
            <div>新建目录</div>
          </div>
        </div>
      </div>
    </div>
    <FolderSelect ref="folderSelectRef" @folderSelect="folderSelectDone"></FolderSelect>
    <!--  预览  -->
    <Preview ref="previewRef"></Preview>
    <ShareFile ref="shareFileRef"></ShareFile>
  </div>
</template>

<script setup>
import CategoryInfo from "@/js/CategoryInfo";
import {computed, defineEmits, getCurrentInstance, nextTick, ref} from "vue";
import Navigation from "@/components/Navigation.vue";
import Icon from "@/components/Icon.vue";
import ShareFile from "@/views/main/ShareFile.vue";

const {proxy} = getCurrentInstance();
//将子组件方法传给父组件
const emit = defineEmits("addFile");
//添加文件
const addFile = (fileData) => {
  emit("addFile", {file: fileData.file, filePid:currentFolder.value.fileId})
}
//当前目录
const currentFolder = ref({fileId: "0"});
const fileNameFuzzy = ref();
//是否展示加载
const showLoading = ref(true)
//分类
const category = ref();
const columns = [
  {
    label: "文件名",
    prop: "fileName",
    scopedSlots: "fileName"
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
const api = {
  loadDataList: "/file/loadDataList",
  newFolder: "/file/newFolder",
  rename: "/file/rename",
  delFile: "/file/delFile",
  changeFileFolder: "/file/changeFileFolder",
  createDownloadUrl: "/file/createDownloadUrl",
  downloadUrl: "/api/file/download"
}

//添加文件回调
const reload = () => {
  showLoading.value = false
  loadDataList();
}
defineExpose({reload})

const tableData = ref({});
const tableOptions = ref({
  extHeight: 50,
  selectType: "checkbox",
})
const loadDataList = async () => {
  let params = {
    pageNo: tableData.value.pageNo,
    pageSize: tableData.value.pageSize,
    fileNameFuzzy: fileNameFuzzy.value,
    filePid: currentFolder.value.fileId,
    category: category.value
  };
  if (params.category != "all") {
    delete params.filePid;
  }
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
const editNameRef = ref();
//编辑行
const editting = ref(false);
//新建文件夹
const newFolder = () => {
  if (editting.value) {
    return;
  }
  // 确保 list 是数组（空目录时后端可能返回 null）
  if (!tableData.value.list) {
    tableData.value.list = [];
  }
  tableData.value.list.forEach(element => {
    element.showEdit = false;
  })
  editting.value = true;
  tableData.value.list.unshift({
    showEdit: true,
    fileType: 0,
    fileId: "",
    filePid: currentFolder.value.fileId,
    fileNameReal: "",
  });
  console.log(tableData.value)
  nextTick(() => {
    // Vue3 v-for 中 ref 绑定不稳定，用 querySelector 兜底
    if (editNameRef.value && editNameRef.value.focus) {
      editNameRef.value.focus();
    } else {
      const inputEl = document.querySelector('.edit-panel .el-input__inner');
      if (inputEl) inputEl.focus();
    }
  })
}
//取消新建文件夹
const cancelNameEdit = (index) => {
  const fileData = tableData.value.list[index];
  if (fileData.fileId) {
    fileData.showEdit = false;
  } else {
    tableData.value.list.splice(index, 1);
    editting.value = false;
  }
}
//保存
const saveNameEdit = async (index) => {
  const {fileId, fileNameReal, fileNameSuffix} = tableData.value.list[index];
  if (!fileNameReal || fileNameReal == "" || fileNameReal.indexOf("/") != -1) {
    proxy.Message.warning("文件名不能为空且不含斜杠")
    if (fileId == "") {
      tableData.value.list.splice(index, 1);
    }
    editting.value = false;
    return;
  }
  let url = api.rename
  if (fileId == "") {
    url = api.newFolder
  }
  let result = await proxy.Request({
    url: url,
    params: {
      fileId: fileId,
      filePid: currentFolder.value.fileId,
      fileName: fileNameReal + (fileNameSuffix || "")
    }
  })
  if (!result) {
    return;
  }
  tableData.value.list[index] = result.data;
  editting.value = false;
}
//展示操作按钮
const showOp = (row) => {
  tableData.value.list.forEach(element => {
    if (element) {
      element.showOp = false;
    }
  })
  row.showOp = true;
};
const cancelShowOp = (row) => {
  row.showOp = false;
};
//重命名
const editFileName = (index) => {
  if (tableData.value.list[0].fileId == "") {
    tableData.value.list.splice(0, 1)
    index = index - 1
  }
  tableData.value.list.forEach(element => {
    element.showEdit = false;
  })
  let currentData = tableData.value.list[index];
  currentData.showEdit = true;
  //编辑文件
  if (currentData.folderType == 0) {
    currentData.fileNameReal = currentData.fileName.substring(0, currentData.fileName.indexOf("."));
    currentData.fileNameSuffix = currentData.fileName.substring(currentData.fileName.indexOf("."))
  } else {
    currentData.fileNameReal = currentData.fileName
    currentData.fileNameSuffix = ""
  }
  editting.value = false;
  nextTick(() => {
    editNameRef.value.focus();
  })
}
//多选
const selectFileList = ref([]);
const rowSelected = (rows) => {
  selectFileList.value = [];
  rows.forEach((item) => {
    selectFileList.value.push(item.fileId);
  })
}
//批量删除
const delFileBatch = () => {
  if (selectFileList.value.length == 0) {
    return;
  }
  proxy.Confirm(`你确定要删除这些文件吗?删除的文件可在10天内通过回收站找回`,
      async () => {
        let result = await proxy.Request(
            {
              url: api.delFile,
              params: {
                fileIds: selectFileList.value.join(","),
              }
            })
        if (!result) {
          return;
        }
        loadDataList();
      }
  )
}
const delFile = (row) => {
  proxy.Confirm(`你确定要删除【${row.fileName}】吗?删除的文件可在10天内通过回收站找回`,
      async () => {
        let result = await proxy.Request(
            {
              url: api.delFile,
              params: {
                fileIds: row.fileId,
              }
            })
        if (!result) {
          return;
        }
        loadDataList();
      }
  )
}

const currentMoveFile = ref({})
//文件移动
const moveFolder = (data) => {
  currentMoveFile.value = data;
  folderSelectRef.value.showFolderDialog(data.fileId)
}
//批量移动
const folderSelectRef = ref()
const moveFileBatch = () => {
  currentMoveFile.value = {}
  folderSelectRef.value.showFolderDialog(selectFileList.value)
}
const folderSelectDone = async (folderId) => {
  if (currentFolder.value.fileId == folderId) {
    proxy.Message.warning("文件正在当前目录,无需移动")
    return
  }
  let fileIdsArray = [];
  if (currentMoveFile.value.fileId) {
    fileIdsArray.push(currentMoveFile.value.fileId)
  } else {
    //将两个数组合并
    fileIdsArray = fileIdsArray.concat(selectFileList.value)
  }
  let result = await proxy.Request({
    url: api.changeFileFolder,
    params: {
      fileIds: fileIdsArray.join(","),
      filePid: folderId
    }
  })
  if (!result) {
    return;
  }
  proxy.Message.success("移动成功")
  folderSelectRef.value.close()
  loadDataList()
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
  previewRef.value.shows(data, 0)
}

const navChange = (data) => {
  const {categoryId, curFolder} = data;
  currentFolder.value.fileId = curFolder;
  category.value = categoryId;
  loadDataList();
}

const refresh = () => {
  loadDataList()
}
const search = () => {
  showLoading.value = true;
  loadDataList()
}

//下载文件
const download = async (row) => {
  let result = await proxy.Request({
    url: api.createDownloadUrl + "/" + row.fileId
  })
  if (!result) {
    return
  }
  window.location.href = api.downloadUrl + "/" + result.data
}
const shareFileRef = ref()
const share = (row) => {
  shareFileRef.value.shareShow(row)
}

const fileAccept = computed(() => {
  const categoryItem = CategoryInfo[category.value];
  return categoryItem ? categoryItem.accept : "*"
})
</script>
<style scoped>
@import "@/assets/file.list.scss";
</style>