<template>
  <div class="top-panel">
    <el-form
        :model="searchFormData"
        :rules="rules"
        ref="formDataRef"
        label-width="80px">
      <el-row>
        <el-col :span="4">
          <el-form-item label="用户昵称">
            <el-input clearable placeholder="支持模糊搜索"
                      v-model="searchFormData.nickNameFuzzy"
                      @keyup.enter="loadDataList"
            >
            </el-input>
          </el-form-item>
        </el-col>
        <el-col :span="5">
          <el-form-item label="状态">
            <el-select clearable placeholder="请选择状态" v-model="searchFormData.status">
              <el-option :value="1" label="启用"></el-option>
              <el-option :value="0" label="禁用"></el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="loadDataList" style="margin-left: 10px">查询</el-button>
        </el-col>
      </el-row>
    </el-form>
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
    >
      <template #avatar="{ index,row }">
        <div class="avatar">
          <Avatar :user-id="row.userId" :width="45" :avatar="row.qqAvatar"></Avatar>
        </div>
      </template>
      <template #space="{ index,row }">
        {{ proxy.Utils.sizeToStr(row.useSpace) }}/{{ proxy.Utils.sizeToStr(row.totalSpace) }}
      </template>
      <template #status="{ index,row }">
        <span v-if="row.status==1" :style="{'color':'#529b2e'}">启用</span>
        <span v-if="row.status==0" :style="{'color':'#f56c62'}">禁用</span>
      </template>
      <template #op="{ index,row }">
        <span class="a-link" @click="updateUserSpace(row)">分配空间</span>
        <el-divider direction="vertical"></el-divider>
        <span class="a-link" @click="updateUserStatus(row)">{{ row.status == 0 ? "启用" : "禁用" }}</span>
      </template>
    </Table>
  </div>
  <Dialog
      :show="dialogConfig.show"
      :title="dialogConfig.title"
      :buttons="dialogConfig.buttons"
      width="500px"
      :showCancel="false"
      @close="dialogConfig.show=false"
  >
    <el-form
        :model="formData"
        :rules="rule"
        ref="formRef"
        label-width="80px"
        @submit.prevent
    >
      <!--input输入 -->
      <el-form-item label="昵称">
        {{formData.nickName}}
      </el-form-item>
      <el-form-item label="空间大小" prop="changeSpace">
        <el-input
            clearable
            placeholder="请输入空间大小"
            v-model.trim="formData.changeSpace"
        >
          <template #suffix>MB</template>
        </el-input>
      </el-form-item>
    </el-form>
  </Dialog>
</template>

<script setup>
import {getCurrentInstance, nextTick, ref} from "vue";
import Avatar from "@/components/Avatar.vue";
import Dialog from "@/components/Dialog.vue";

const {proxy} = getCurrentInstance();
const formData = ref({});
const formRef = ref();

const rule = {
  changeSpace:[
    {required:true,message:"请输入空间大小"},
  ],
}

const api = {
  loadUserList: "/admin/loadUserList",
  updateUserSpace: "/admin/updateUserSpace",
  updateUserStatus: "/admin/updateUserStatus",
}

const columns = [
  {
    label: "头像",
    prop: "avatar",
    width: "80",
    scopedSlots: "avatar"
  },
  {
    label: "昵称",
    prop: "nickName",
    width: "150"
  },
  {
    label: "邮箱",
    prop: "email",
    width: "170",
  },
  {
    label: "空间使用",
    prop: "space",
    scopedSlots: "space",
    width: "150",
  },
  {
    label: "加入时间",
    prop: "joinTime",
    width: "200"
  },
  {
    label: "最后登录时间",
    prop: "lastLoginTime",
    width: "200"
  },
  {
    label: "状态",
    prop: "status",
    width: "80",
    scopedSlots: "status"
  },
  {
    label: "操作",
    prop: "op",
    width: "150",
    scopedSlots: "op"
  },
];

const searchFormData = ref({});
const formDataRef = ref();

const rules = {
  nickNameFuzzy: [
    {required: true, message: "请输入昵称"},
  ]
}

const tableData = ref({});
const tableOptions = ref({
  extHeight: 50,
})
const showLoading = ref(false)
const loadDataList = async () => {
  let params = {
    pageNo: tableData.value.pageNo,
    pageSize: tableData.value.pageSize,
  };
  Object.assign(params, searchFormData.value)
  let result = await proxy.Request({
    url: api.loadUserList,
    showLoading: showLoading.value,
    params: params,
  })
  if (!result) {
    return;
  }
  tableData.value = result.data;
}

//修改状态
const updateUserStatus = (row) => {
  proxy.Confirm(`你确定要【${row.status == 0 ? "启用" : "禁用"}】吗?`,
      async () => {
        let result = await proxy.Request(
            {
              url: api.updateUserStatus,
              params: {
                userId: row.userId,
                status: row.status == 0 ? 1 : 0
              }
            })
        if (!result) {
          return;
        }
        loadDataList();
      }
  )
}
const dialogConfig = ref({
  show: false,
  title: "更新空间大小",
  buttons: [
    {
      type:"primary",
      text:"确定",
      click:(e)=>{
        submitForm();
      },
    },
  ],
});
const submitForm = ()=>{
  formRef.value.validate(async (valid)=>{
    if (!valid){
      return;
    }
    let parmas = {};
    Object.assign(parmas,formData.value)
    let result = proxy.Request({
      url:api.updateUserSpace,
      params:parmas,
    })
    if (!result){
      return;
    }
    dialogConfig.value.show = false;
    proxy.Message.success("操作成功")
    loadDataList()
  })
}
//分配空间
const updateUserSpace = (data)=>{
    dialogConfig.value.show =true;
    nextTick(()=>{
      formRef.value.resetFields();
      formData.value = Object.assign({},data)
    })
}
</script>


<style scoped>
.top-panel {
  margin-top: 10px;
}

.avatar {
  width: 50px;
  height: 50px;
  border-radius: 25px;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
  }
}
</style>