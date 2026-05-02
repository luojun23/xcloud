import './assets/base.scss'
import './assets/icon/iconfont.css'
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
//引入element-plus
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
//引入cookie
import cookies from 'vue-cookies'

//引入代码高亮
import HlsVuePlugin from '@highlightjs/vue-plugin'
import "highlight.js/styles/atom-one-light.css"
import "highlight.js/lib/common"

//自定义组件
import Dialog from "@/components/Dialog.vue";
import Avatar from "@/components/Avatar.vue";
import Table from "@/components/Table.vue";
import Icon from "@/components/Icon.vue";
import NoData from "@/components/NoData.vue";
import FolderSelect from "@/components/FolderSelect.vue"
import Navigation from "@/components/Navigation.vue"
import Preview from "@/components/preview/Preview.vue"
import Window from "@/components/Window.vue"

import verify from "@/utils/Verify";
import Message from "@/utils/Message";
import Request from "@/utils/Request";
import Confirm from "@/utils/Confirm";
import Utils from "@/utils/Utils";

const app = createApp(App)

app.component("Dialog",Dialog)
app.component("Avatar",Avatar)
app.component("Table",Table)
app.component("Icon",Icon)
app.component("NoData",NoData)
app.component("FolderSelect",FolderSelect)
app.component("Navigation",Navigation)
app.component("Preview",Preview)
app.component("Window",Window)
//配置全局组件
app.config.globalProperties.Verify=verify
app.config.globalProperties.Message=Message
app.config.globalProperties.Request=Request
app.config.globalProperties.Cookies=cookies
app.config.globalProperties.Confirm=Confirm
app.config.globalProperties.Utils=Utils

app.config.globalProperties.globalInfo={
    avatarUrl:"/api/getAvatar/",
    imageUrl:"/api/file/getImage/",
}
app.use(router)
app.use(ElementPlus,{size:"medium"})
app.use(HlsVuePlugin)
app.mount('#app')
