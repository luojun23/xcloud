<template>
  <div class="share">
    <div class="header">
      <div class="header-content">
        <div class="logo" @click="jump">
          <span class="iconfont icon-pan"></span>
          <span class="name">XCloud云盘</span>
        </div>
      </div>
    </div>
  </div>
  <div class="share-body">
    <template v-if="Object.keys(shareInfo).length ==0">
      <div class="loading" v-loading="Object.keys(shareInfo).length==0"></div>
    </template>
    <template v-else>
      <div class="share-panel">
        <div class="share-user-info">
          <div class="avatar">
            <Avatar :user-id="shareInfo.userId" :avatar="shareInfo.avatar" :width="40"></Avatar>
          </div>
          <div class="share-info">
            <div class="user-info">
              <div class="nick-name">{{shareInfo.nickName}}</div>
              <div class="share-time">分享于{{shareInfo.shareTime}}</div>
            </div>
            <div class="file-name">分享文件：{{shareInfo.fileName}}</div>
          </div>
        </div>
        <div class="share-op-btn">
          <el-button type="primary" @click="cancelShare" v-if="shareInfo.currentUser">取消分享</el-button>
          <el-button type="primary" @click="save2MyPanDone" v-else :disabled="selectFileList.length==0" >保存到我的网盘</el-button>
        </div>
      </div>
      <Navigation ref="navigationRef" @navChange="navChange" :share-id="shareId"></Navigation>
    </template>
    <div class="file-list">
      <Table
          ref="dataTableRef"
          :columns="columns"
          :show-pagination="true"
          :dataSource="tableData"
          :fetch="loadFileList"
          :initFetch="false"
          :options="tableOptions"
      >
        <template #fileName="{ index,row }">
          <div class="file-item" @mouseenter="showOp(row)" @mouseleave="cancelShowOp(row)">
            <template v-if="(row.fileType==3||row.fileType==1)&&row.status==2">
              <Icon :cover="row.fileCover" :width="34"></Icon>
            </template>
            <template v-else>
              <Icon v-if="row.folderType == 0" :file-type="row.fileType"></Icon>
              <Icon v-if="row.folderType == 1" :file-type="0"></Icon>
            </template>
            <span class="file-name" :title="row.fileName" v-if="!row.showEdit">
               <span @click="preview(row)"> {{ row.fileName }}</span>
          </span>
            <span class="op">
              <template v-if="row.showOp&&row.fileId&&row.status==2">
                <span class="iconfont icon-download" v-if="row.folderType==0" @click="download(row)">下载</span>
                <span class="iconfont icon-import" @click="save2MyPanDone(row)" v-if="showOp&&shareInfo.currentUser">保存到我的网盘</span>
              </template>
            </span>
          </div>
        </template>
        <template #fileSize="{ index,row }">
          <span v-if="row.fileSize">{{ proxy.Utils.sizeToStr(row.fileSize) }}</span>
        </template>
        <template #lastUpdateTime="{ index,row }">
          {{shareInfo.lastUpdateTime}}
        </template>
      </Table>
    </div>
    <!--  目录选择  -->
    <FolderSelect ref="folderSelectRef" @folderSelect="save2MyPanDone"></FolderSelect>
    <!--  预览  -->
    <Preview ref="previewRef"></Preview>
  </div>
</template>

<script setup>
import {getCurrentInstance, ref} from "vue";
import Avatar from "@/components/Avatar.vue";
import {useRoute, useRouter} from "vue-router";
import Navigation from "@/components/Navigation.vue";
import Icon from "@/components/Icon.vue";
import FolderSelect from "@/components/FolderSelect.vue";

const {proxy} = getCurrentInstance();

const router = useRouter();
const route = useRoute();
const shareId = route.params.shareId;
const shareInfo = ref({})

const columns = [
  {
    label: "文件名",
    prop: "fileName",
    scopedSlots: "fileName"
  },
  {
    label: "更新时间",
    prop: "lastUpdateTime",
  },
  {
    label: "大小",
    prop: "fileSize",
    scopedSlots: "fileSize",
    width:200,
  },
];

const tableData = ref({});
const tableOptions = ref({
  extHeight: 80,
  selectType: "checkbox",
})
const showLoading = ref(false)
const loadFileList = async () => {
  let params = {
    pageNo: tableData.value.pageNo,
    pageSize: tableData.value.pageSize,
    shareId:shareId,
    filePid:currentFolder.value.fileId
  };
  let result = await proxy.Request({
    url: api.loadFileList,
    showLoading: showLoading.value,
    params: params,
  })
  if (!result) {
    return;
  }
  tableData.value = result.data;
}

//展示操作按钮
const showOp = (row) => {
  tableData.value.list.forEach(element => {
    element.showOp = false;
  })
  row.showOp = true;
};
const cancelShowOp = (row) => {
  row.showOp = false;
};

//多选
const selectFileList = ref([]);
const rowSelected = (rows) => {
  selectFileList.value = [];
  rows.forEach((item) => {
    selectFileList.value.push(item.fileId);
  })
}

const api = {
  loadFileList: "/showShare/loadFileList",
  getFolderInfo: "/showShare/getFolderInfo",
  fileUrl: "/showShare/getFile",
  videoUrl: "/showShare/ts/getVideo",
  createDownloadUrl: "/showShare/createDownloadUrl",
  downloadUrl: "/api/showShare/download",
  getShareLoginInfo: "/showShare/getShareLoginInfo",
}

const getShareInfo = async ()=>{
  let result = await proxy.Request({
    url:api.getShareLoginInfo,
    showLoading:true,
    params:{shareId}
  })
  if (!result){
    return;
  }
  if (result.data==null){
    router.push(`/shareCheck/${shareId}`);
    return
  }
  shareInfo.value = result.data
}
getShareInfo();

//当前目录
const currentFolder = ref({fileId: "0"});
const navigationRef = ref();
//导航
const navChange =(data)=> {
  const {curFolder} = data;
  currentFolder.value.fileId = curFolder;
  loadFileList();
}
//预览
const previewRef = ref()
const preview =(data)=>{
  //目录
  if (data.folderType == 1) {
    navigationRef.value.openFolder(data)
    return;
  }
  data.shareId = shareId;
  previewRef.value.shows(data,2)
}
//下载文件
const download = async (row) => {
  console.log(row)
  let result = await proxy.Request({
    url: api.createDownloadUrl + "/" + shareId + "/" + row.fileId
  })
  if (!result) {
    return
  }
  window.location.href = api.downloadUrl + "/" + result.data
}

//保存到我的网盘
const folderSelectRef = ref()
const save2MyPanFileIdArray = ref([])
/*const save2MyPanDone = ()=>{
  if (selectFileList.value.length==0){
    return;
  }
  if (!proxy.Cookies.get("userInfo")){
    router.push("/")
  }
}*/
</script>

<style lang="scss" scoped>
@import "@/assets/file.list.scss";
.header{
  width: 100%;
  position: fixed;
  background: #0c95f7;
  height: 50px;
  .header-content{
    width: 70%;
    margin: 0px auto;
    color: #fff;
    line-height: 50px;
    .logo{
      display: flex;
      align-items: center;
      cursor: pointer;
      .icon-pan{
        font-size: 40px;
      }
      .name{
        font-weight: bold;
        margin-left: 5px;
        font-size: 25px;
      }
    }
  }
}
.share-body{
  width: 70%;
  margin: 0px auto;
  padding-top: 50px;
  .loading{
    height: calc(100vh / 2);
    width: 100%;
  }
  .share-panel{
    margin-top: 20px;
    display: flex;
    justify-content: space-around;
    border-bottom: 1px solid #ddd;
    padding-bottom: 10px;
    .share-user-info{
      flex: 1;
      display: flex;
      align-items: center;
      .avatar{
        margin-right: 5px;
      }
      .share-info{
        .user-info{
          display: flex;
          align-items: center;
        .nick-name{
          font-size: 15px;
        }
        .share-time{
          margin-left: 20px;
          font-size: 12px;
          }
        }
      .file-name{
        margin-top: 10px;
        font-size: 12px;
        }
      }
    }
  }
}
.file-list{
  margin-top: 10px;
  .file-item{
    .op{
      width: 170px;
    }
  }
}
</style>