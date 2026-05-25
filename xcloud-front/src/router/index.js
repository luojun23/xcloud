import {createRouter, createWebHistory} from 'vue-router'
import cookies from 'vue-cookies'
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'Login',
      component: ()=> import('../views/Login.vue')
    },
    {
      path: '/test',
      name: 'Test',
      component: ()=> import('../views/Test.vue')
    },
    {
      path: '/home',
      name: 'Home',
      component: ()=> import('../views/Home.vue'),
      children:[
        {
          path:'/home',
          redirect:"/main/all"
        },
        {
          path:'/main/:category',
          name:'首页',
          meta:{
            needLogin:true,
            menuCode:"main"
          },
          component:()=>import("../views/main/Main.vue")
        },
        {
          path:'/ai',
          name:'AI分析',
          meta:{
            needLogin:true,
            menuCode:"ai"
          },
          component:()=>import("../views/ai/AiAnalysis.vue")
        },
        {
          path:'/myshare',
          name:'我的分享',
          meta:{
            needLogin:true,
            menuCode:"share"
          },
          component:()=>import("../views/share/Share.vue")
        },
        {
          path:'/recycle',
          name:'回收站',
          meta:{
            needLogin:true,
            menuCode:"recycle"
          },
          component:()=>import("../views/recycle/Recycle.vue")
        },
        {
          path:'/settings/userList',
          name:'用户管理',
          meta:{
            needLogin:true,
            menuCode:"settings"
          },
          component:()=>import("../views/admin/UserList.vue")
        },
        {
          path:'/settings/fileList',
          name:'用户文件',
          meta:{
            needLogin:true,
            menuCode:"settings"
          },
          component:()=>import("../views/admin/FileList.vue")
        }
      ]
    },
    {
      path: '/qqCallback',
      name: 'QQCallback',
      component: ()=> import('../views/QQCallback.vue')
    },
    {
      path: '/shareCheck/:shareId',
      name: '分享校验',
      component: ()=> import('../views/webShare/shareCheck.vue')
    },
    {
      path: '/share/:shareId',
      name: '分享',
      component: ()=> import('../views/webShare/Share.vue')
    }
  ]
})

router.beforeEach((to, from, next)=>{
  const userInfo = cookies.get("userInfo")
  if (to.meta.needLogin!=null&&to.meta.needLogin&&userInfo==null){
    router.push("/")
  }
  next();
})

export default router
