<template>
  <div class="top">
    <el-button
        type="primary"
        :disabled="selectFileList.length==0"
        @click="CancleShareBatch"
    >
      <span class="iconfont icon-cancel"></span>
      取消分享
    </el-button>
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
            <span class="iconfont icon-link" @click="copy(row)">复制链接</span>
            <span class="iconfont icon-link" @click="cancel(row)">取消分享</span>
          </span>
        </div>
      </template>
      <template #expireTime="{index,row}">
        {{ row.validType == 3 ? "永久" : row.expireTime }}
      </template>
    </Table>
  </div>
</template>

<script setup>
import useClipboard from "vue-clipboard3";

const {toClipboard} = useClipboard()
import {getCurrentInstance, nextTick, ref, defineEmits} from "vue";
import Icon from "@/components/Icon.vue";

const {proxy} = getCurrentInstance();
const api = {
  loadDataList: "/share/loadShareList",
  shareFile: "/share/shareFile",
  cancleShare: "/share/cancelShare"
}

const columns = [
  {
    label: "文件名",
    prop: "fileName",
    scopedSlots: "fileName"
  },
  {
    label: "分享时间",
    prop: "shareTime",
    width: "200"
  },
  {
    label: "失效时间",
    prop: "expireTime",
    scopedSlots: "expireTime",
    width: "200"
  },
  {
    label: "浏览次数",
    prop: "showCount",
    width: "200"
  }
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
    selectFileList.value.push(item.shareId);
  })
}

const showOp = (row) => {
  tableData.value.list.forEach(item => {
    item.showOp = false
  })
  row.showOp = true
}

const cancelShowOp = (row) => {
  row.showOp = false
}

const shareUrl = ref(document.location.origin + "/share/");
const copy = async (row) => {
  await toClipboard(`链接：${shareUrl.value}${row.shareId} 提取码：${row.code}`);
  proxy.Message.success("复制成功")
}

//取消分享
const cancel = (row) => {
  cancelShareIdList.value = [row.shareId]
  cancelShareDone();
}

//批量取消分享
const cancelShareIdList = ref([])
const CancleShareBatch = () => {
  if (selectFileList.value.length == 0) {
    return;
  }
  cancelShareIdList.value = selectFileList.value
  cancelShareDone();
}

const cancelShareDone = () => {
  proxy.Confirm("你确定要取消分享吗?", async () => {
    let result = await proxy.Request({
      url: api.cancleShare,
      params: {
        shareIds: cancelShareIdList.value.join(",")
      }
    })
    if (!result) {
      return
    }
    proxy.Message.success("取消分享成功")
    loadDataList()
  })
}
</script>

<style lang="scss" scoped>
@import "@/assets/file.list.scss";

.file-list {
  margin-top: 10px;

  .file-name {
    span {
      &:hover {
        color: #494944;
      }
    }
  }

  .op {
    width: 170px;
  }
}
</style>