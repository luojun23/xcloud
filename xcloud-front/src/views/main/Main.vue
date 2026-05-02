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
    <!-- 图片相册视图 -->
    <template v-if="category=='image'">
      <div class="image-album-wrapper" v-if="imageGroups.length>0">
        <div class="image-album" @scroll="onAlbumScroll">
          <div v-for="group in imageGroups" :key="group.date" :id="'year-' + group.date.split('-')[0]" :data-date="group.date" class="date-group">
            <div class="date-header">
              <el-checkbox v-model="group.checked" @change="toggleGroupSelect(group)"></el-checkbox>
              <span class="date-title">{{ formatDateTitle(group.date) }}</span>
            </div>
            <div class="image-grid">
              <div v-for="item in group.items" :key="item.fileId" class="image-item"
                   @mouseenter="item.showOp=true" @mouseleave="item.showOp=false">
                <el-checkbox v-model="item.selected" class="item-check" @change="updateSelectFileList"></el-checkbox>
                <div class="image-thumb" @click="preview(item)">
                  <img :src="item.fileCover ? proxy.globalInfo.imageUrl + item.fileCover : defaultImageIcon" />
                </div>
                <div class="image-op" v-show="item.showOp">
                  <span class="iconfont icon-download" @click.stop="share(item)" title="分享"></span>
                  <span class="iconfont icon-download" @click.stop="download(item)" title="下载"></span>
                  <span class="iconfont icon-del" @click.stop="delFile(item)" title="删除"></span>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="timeline" v-if="timelineYears.length>0">
          <div v-for="year in timelineYears" :key="year"
               @click="scrollToYear(year)"
               :class="['year-item', currentYear==year?'active':'']">
            <span class="year-text">{{ currentYear==year ? currentMonthLabel : year }}</span>
          </div>
        </div>
      </div>
      <div class="no-data" v-else>
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
          </div>
        </div>
      </div>
    </template>

    <!-- 普通文件列表视图 -->
    <template v-else>
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
    </template>
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
const defaultImageIcon = new URL('@/assets/icon-image/image.png', import.meta.url).href
//将子组件方法传给父组件
const emit = defineEmits(["addFile", "refreshSpace"]);
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

// 图片相册分组
const imageGroups = computed(() => {
  if (!tableData.value.list || category.value != 'image') return []
  const groups = {}
  tableData.value.list.forEach(item => {
    const date = item.lastUpdateTime ? item.lastUpdateTime.split(' ')[0] : '未知日期'
    if (!groups[date]) {
      groups[date] = { date, items: [], checked: false }
    }
    groups[date].items.push(item)
  })
  return Object.values(groups).sort((a, b) => new Date(b.date) - new Date(a.date))
})

const timelineYears = computed(() => {
  const years = new Set()
  if (tableData.value.list) {
    tableData.value.list.forEach(item => {
      const year = item.lastUpdateTime ? item.lastUpdateTime.split('-')[0] : null
      if (year) years.add(year)
    })
  }
  return Array.from(years).sort((a, b) => b - a)
})

const currentYear = ref('')
const currentMonthLabel = ref('')
const scrollToYear = (year) => {
  const el = document.getElementById('year-' + year)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    currentYear.value = year
  }
}

const formatDateTitle = (dateStr) => {
  if (!dateStr || dateStr === '未知日期') return dateStr
  const d = new Date(dateStr)
  const year = d.getFullYear()
  const month = (d.getMonth() + 1).toString().padStart(2, '0')
  const day = d.getDate().toString().padStart(2, '0')
  const weekDays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  const week = weekDays[d.getDay()]
  return `${year}年${month}月${day}日 ${week}`
}

const toggleGroupSelect = (group) => {
  group.items.forEach(item => {
    item.selected = group.checked
  })
  updateSelectFileList()
}

const updateSelectFileList = () => {
  selectFileList.value = []
  if (tableData.value.list) {
    tableData.value.list.forEach(item => {
      if (item.selected) {
        selectFileList.value.push(item.fileId)
      }
    })
  }
}

const loadDataList = async () => {
  let params = {
    pageNo: tableData.value.pageNo,
    pageSize: category.value == 'image' ? 1000 : tableData.value.pageSize,
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
  if (result.data.list) {
    result.data.list.forEach(item => {
      item.selected = false
      item.showOp = false
    })
  }
  tableData.value = result.data;
  if (category.value == 'image' && timelineYears.value.length > 0) {
    currentYear.value = timelineYears.value[0]
  }
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

// 监听滚动更新当前年份
let scrollTimer = null
const onAlbumScroll = (e) => {
  if (scrollTimer) return
  scrollTimer = setTimeout(() => {
    scrollTimer = null
    const groups = document.querySelectorAll('.date-group')
    for (let i = 0; i < groups.length; i++) {
      const rect = groups[i].getBoundingClientRect()
      if (rect.top >= 0 && rect.top < 200) {
        const date = groups[i].getAttribute('data-date')
        if (date) {
          const [year, month] = date.split('-')
          currentYear.value = year
          currentMonthLabel.value = `${year}年${month}月`
        }
        break
      }
    }
  }, 100)
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
        emit("refreshSpace");
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
        emit("refreshSpace");
        
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

.image-album-wrapper {
  display: flex;
  height: calc(100vh - 180px);
  overflow: hidden;
}

.image-album {
  flex: 1;
  overflow-y: auto;
  padding: 10px 20px;
}

.date-group {
  margin-bottom: 20px;
}

.date-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  padding: 5px 0;

  .date-title {
    font-size: 14px;
    color: #333;
    font-weight: 500;
  }
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 10px;
}

.image-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
  background: #f5f5f5;

  .item-check {
    position: absolute;
    top: 6px;
    left: 6px;
    z-index: 5;
    opacity: 0;
    transition: opacity 0.2s;
  }

  &:hover .item-check {
    opacity: 1;
  }

  .image-thumb {
    width: 100%;
    height: 100%;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }
  }

  .image-op {
    position: absolute;
    top: 6px;
    right: 6px;
    display: flex;
    gap: 6px;
    z-index: 5;

    .iconfont {
      width: 28px;
      height: 28px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(0, 0, 0, 0.5);
      color: #fff;
      border-radius: 50%;
      font-size: 12px;
      cursor: pointer;
      transition: background 0.2s;

      &:hover {
        background: rgba(0, 0, 0, 0.7);
      }
    }
  }
}

.timeline {
  width: 100px;
  padding: 20px 0;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow-y: auto;

  &::before {
    content: '';
    position: absolute;
    right: 30px;
    top: 20px;
    bottom: 20px;
    width: 2px;
    background: #e8e8e8;
  }

  .year-item {
    position: relative;
    text-align: right;
    padding-right: 45px;
    font-size: 14px;
    color: #aaa;
    cursor: pointer;
    line-height: 36px;
    transition: all 0.2s;

    &::after {
      content: '';
      position: absolute;
      right: 26px;
      top: 50%;
      transform: translateY(-50%);
      width: 10px;
      height: 2px;
      background: #ddd;
      transition: all 0.2s;
    }

    &::before {
      content: '';
      position: absolute;
      right: 22px;
      top: 50%;
      transform: translateY(-50%);
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #ddd;
      border: 2px solid #fff;
      z-index: 2;
      transition: all 0.2s;
    }

    &:hover {
      color: #409eff;
    }

    &.active {
      color: #409eff;
      font-weight: 500;

      &::after {
        background: #409eff;
        width: 14px;
      }

      &::before {
        background: #409eff;
        border-color: #fff;
        box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.2);
      }
    }
  }
}
</style>