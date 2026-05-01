<template>
  <div class="home"></div>
  <div class="header">
    <div class="logo">
      <span class="iconfont icon-pan"></span>
      <div class="name">XCloud云盘</div>
    </div>
    <div class="right-panel">
      <el-popover
          :width="800"
          trigger="click"
          v-model:visible="showUploder"
          :offset="20"
          transition="none"
          :hide-after="0"
          :popper-style="{padding:'0px'}"
      >
        <template #reference>
          <span class="iconfont icon-transfer"></span>
        </template>
        <template #default>
          <Uploader ref="uploaderRef" @uploadCallback="uploadCallbackHandler"></Uploader>
        </template>
      </el-popover>
      <el-dropdown>
        <div class="user_info">
          <div class="avatar">
            <Avatar :user-id="userInfo.userId" :width="40" :avatar="userInfo.avatar" :timestamp="timestamp"></Avatar>
          </div>
          <span class="nick_name">{{ userInfo.nickName }}</span>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="updateAvatar">修改头像</el-dropdown-item>
            <el-dropdown-item @click="updatepassword">修改密码</el-dropdown-item>
            <el-dropdown-item @click="logout">退出</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
  <div class="body">
    <div class="left-sider">
      <div class="menu-list">
        <div v-for="item in menus">
          <div :class="['menu-item', item.menuCode==currentMenu.menuCode?'active':'']"
               @click="jump(item)" v-if="item.allshow=='true'">
            <div :class="['iconfont', 'icon-'+item.icon ]"></div>
            <div class="text">{{ item.name }}</div>
          </div>
        </div>
      </div>
      <div class="menu-sub-list">
        <div :class="['item-sub-menu', currentPath==sub.path?'active':'']" v-for="sub in currentMenu.children"
             @click="jump(sub)">
          <span
              :class="['iconfont','icon-'+sub.icon]"
              v-if="sub.icon"
          ></span>                                                                                                                                                                            
          <span class="text">{{ sub.name }}</span>
        </div>
        <div class="txt" v-if="currentMenu && currentMenu.tips">{{ currentMenu.tips }}</div>
        <div class="space-info">
          <div>空间使用</div>
          <div class="percent">
            <el-progress
                :percentage="Math.floor(
                (useSpaceInfo.useSpace/useSpaceInfo.totalSpace)*10000
             )/100"
                color="#409eff"
            ></el-progress>
          </div>
          <div class="space-use">
            <div class="use">
              {{ proxy.Utils.sizeToStr(useSpaceInfo.useSpace) }}/{{
                proxy.Utils.sizeToStr(useSpaceInfo.totalSpace)
              }}
            </div>
            <div class="iconfont icon-refresh" @click="getUseSpace"></div>
          </div>
        </div>
      </div>
    </div>
    <div class="body-content">
      <router-view v-slot="{ Component }">
        <component :is="Component"
                   @addFile="addFile"
                   @refreshSpace="getUseSpace"
                   ref="routerViewRef"
        ></component>
      </router-view>
    </div>
    <UpdateAvatar
        ref="updateAvatarRef"
        @updateAvatar="reloadAvatar"
    ></UpdateAvatar>
    <UpdatePassword ref="updatePasswordRef"></UpdatePassword>
  </div>
</template>
<script setup>
import {getCurrentInstance, nextTick, ref, watch} from "vue";
import {useRoute, useRouter} from "vue-router";
import Avatar from "@/components/Avatar.vue";
import UpdatePassword from "@/views/UpdatePassword.vue";
import UpdateAvatar from "@/views/UpdateAvatar.vue";
import Uploader from "@/components/Uploader.vue";
import Utils from "@/utils/Utils";

const {proxy} = getCurrentInstance();
const router = useRouter();
const route = useRoute();

const menus = [
  {
    icon: "cloude",
    name: "首页",
    menuCode: "main",
    path: "/main/all",
    allshow: "true",
    children: [
      {
        icon: "cloude",
        name: "全部",
        category: "all",
        path: "/main/all"
      },
      {
        icon: "video",
        name: "视频",
        category: "video",
        path: "/main/video"
      },
      {
        icon: "music",
        name: "音频",
        category: "music",
        path: "/main/music"
      },
      {
        icon: "image",
        name: "图片",
        category: "image",
        path: "/main/image"
      },
      {
        icon: "doc",
        name: "文档",
        category: "doc",
        path: "/main/doc"
      },
      {
        icon: "more",
        name: "其他",
        category: "others",
        path: "/main/others"
      },
    ]
  },
  {
    icon: "share",
    name: "分享",
    menuCode: "share",
    path: "/myshare",
    allshow: "true",
    children: [
      {
        name: "分享记录",
        path: "/myshare"
      }
    ]
  },
  {
    icon: "del",
    name: "回收站",
    menuCode: "recycle",
    path: "/recycle",
    tips: "回收站为你保存10天内删除的文件",
    allshow: "true",
    children: [
      {
        name: "删除的文件",
        path: "/recycle"
      },
    ]
  },
  {
    icon: "settings",
    name: "设置",
    menuCode: "settings",
    path: "/settings/fileList",
    allshow: "false",
    children: [
      {
        name: "用户文件",
        path: "/settings/fileList"
      },
      {
        name: "用户管理",
        path: "/settings/userList"
      },
      {
        name: "系统设置",
        path: "/setting/sysSetting"
      }
    ]
  },
];

const currentMenu = ref({})
const currentPath = ref()

const timestamp = ref(new Date().getTime())
const userInfo = ref(
    proxy.Cookies.get("userInfo")
);

const init = () => {
  if (userInfo.value.admin) {
    menus.forEach(e => {
          if ( e.menuCode == 'settings') {
              e.allshow='true'
         }
    });
  }
}
init()


const api = {
  logout: "/logout",
  getUseSpace: "/getUseSpace"
}
//修改头像
const updateAvatarRef = ref()
const updateAvatar = () => {
  updateAvatarRef.value.show(userInfo.value);
}
const reloadAvatar = () => {
  userInfo.value = proxy.Cookies.get("userInfo")
  timestamp.value = new Date().getTime()
}

const routerViewRef = ref()

//上传文件回调
const uploadCallbackHandler = () => {
  nextTick(() => {
    routerViewRef.value.reload();
    //TODO 更新用户空间
    getUseSpace()
  })
}
//修改密码
const updatePasswordRef = ref();
const updatepassword = () => {
  updatePasswordRef.value.shows1();
}
//退出登录
const logout = async () => {
  proxy.Confirm('你确定要退出吗', async () => {
    let result = await proxy.Request({
      url: api.logout
    })
    if (!result) {
      return;
    }
    proxy.Cookies.remove("userInfo")
    router.push("/")
  })
}
const jump = (item) => {
  if (!item.path || item.menuCode == currentMenu.value.menuCode) {
    return;
  }
  router.push(item.path)
}
const showUploder = ref(false);
const uploaderRef = ref();
//添加文件
const addFile = (data) => {
  const {file, filePid} = data;
  //父组件传子组件
  uploaderRef.value.addFile(file, filePid);
  showUploder.value = true;
};

const setMenu = (menuCode, path) => {
  const menu = menus.find((item) => {
    return item.menuCode === menuCode;
  });
  currentMenu.value = menu;
  currentPath.value = path;
};
//监听路由
watch(
    () => route,
    (newVal, oldVal) => {
      if (newVal.meta.menuCode) {
        setMenu(newVal.meta.menuCode, newVal.path);
      }
    },
    {immediate: true, deep: true}
);

//使用空间
const useSpaceInfo = ref({useSpace: 0, totalSpace: 1})
const getUseSpace = async () => {
  let result = await proxy.Request({
    url: api.getUseSpace,
    showLoading: false
  })
  if (!result) {
    return
  }
  useSpaceInfo.value = result.data
}
getUseSpace();
</script>

<style scoped>
.header {
  box-shadow: 0 3px 10px rgba(0, 0, 0, .06);
  height: 56px;
  padding-left: 24px;
  padding-right: 24px;
  position: relative;
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: space-between;

  .logo {
    display: flex;
    align-items: center;

    .icon-pan {
      font-size: 40px;
      color: #007fff;
    }

    .name {
      font-style: italic;
      font-size: 20px;
      font-weight: bold;
      margin-left: 5px;
    }
  }

  .right-panel {
    display: flex;
    align-items: center;

    .icon-transfer {
      cursor: pointer;
    }

    .user_info {
      margin-left: 10px;
      display: flex;
      align-items: center;
      cursor: pointer;

      .avatar {
        margin: 0px 5px 0px 5px;
      }

      .nick_name {
        color: #007fff;
      }
    }
  }
}

.body {
  display: flex;
  height: 687px;
  overflow: hidden;
  width: 100%;

  .left-sider {
    display: flex;
    background-color: #fff;
    border-right: 1px solid #f1f2f4;

    .menu-list {
      width: 80px;
      height: calc(100vh - 56px);
      box-shadow: 0 3px 10px 0 rgba(0, 0, 0, .06);
      border-right: 1px solid #f1f2f4;

      .menu-item {
        text-align: center;
        font-size: 14px;
        font-weight: bold;
        padding: 20px 0px;
        cursor: pointer;

        &:hover {
          background: #f3f3f3;
        }

        .iconfont {
          font-weight: normal;
          font-size: 18px;
        }
      }

      .active {
        .iconfont {
          color: #06a7ff;
        }

        .text {
          color: #06a7ff;
        }
      }
    }

    .menu-sub-list {
      width: 200px;
      border-right: 1px solid #f1f2f4;
      position: relative;
      padding: 20px 10px 0;

      .item-sub-menu {
        text-align: center;
        line-height: 40px;
        border-radius: 5px;
        cursor: pointer;

        &:hover {
          background: #f3f3f3;
        }

        .iconfont {
          font-size: 14px;
          margin-right: 20px;
        }

        .text {
          font-size: 14px;
        }
      }

      .active {
        background: #eef0fe;

        .iconfont {
          color: #06a7ff;
        }

        .text {
          color: #06a7ff;
        }
      }

      .txt {
        margin-top: 10px;
        font-size: 13px;
        color: #888
      }

      .space-info {
        position: absolute;
        bottom: 10px;
        width: 100%;
        padding: 0px 5px;

        .percent {
          padding-right: 10px;
        }

        .space-use {
          margin-top: 5px;
          color: #7e7e7e;
          display: flex;
          justify-content: space-around;

          .use {
            flex: 1;
          }

          .iconfont {
            cursor: pointer;
            margin-right: 20px;
            color: #05a1f5;
          }
        }
      }
    }
  }

  .body-content {
    flex: 1;
    width: 0;
    padding-left: 20px;
  }
}
</style>