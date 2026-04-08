<template>
  <div class="login-body">
    <div class="bg">
      <div class="illustration">
        <div class="chart-card">
          <div class="chart-line"></div>
          <div class="chart-dots">
            <span></span>
            <span></span>
            <span></span>
          </div>
        </div>
        <div class="person">
          <div class="person-head"></div>
          <div class="person-body"></div>
          <div class="person-legs"></div>
        </div>
        <div class="pie-chart"></div>
      </div>
    </div>
    <div class="login-panel">
      <!-- 登录卡片 -->
      <div class="login-card" v-if="opType==1">
        <div class="login-title">
          XCloud云盘
        </div>
        <el-form
          class="login-form"
          :model="formData"
          :rules="rules"
          ref="formDataRef"
          @submit.prevent
        >
          <!-- 邮箱输入 -->
          <el-form-item prop="email">
            <el-input
              v-model.trim="formData.email"
              placeholder="请输入邮箱"
              size="large"
              clearable
            >
              <template #prefix>
                <span class="iconfont icon-account"></span>
              </template>
            </el-input>
          </el-form-item>

          <!-- 密码输入 -->
          <el-form-item prop="password">
            <el-input
              v-model="formData.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              show-password
            >
            <template #prefix>
                <span class="iconfont icon-password"></span>
              </template>
            </el-input>
          </el-form-item>

          <!-- 图片验证码 -->
          <el-form-item prop="checkCode">
            <div class="verify-code-row">
              <el-input
                v-model.trim="formData.checkCode"
                placeholder="请输入验证码"
                size="large"
                clearable
              >
              <template #prefix>
                <span class="iconfont icon-checkcode"></span>
              </template>
              </el-input>
              <img :src="checkCodeUrl" class="check-code-img" @click="changeCheckCode(0)"/>
            </div>
          </el-form-item>

          <!-- 记住我 -->
          <el-form-item class="remember-row">
            <el-checkbox v-model="formData.rememberMe">记住我</el-checkbox>
          </el-form-item>

          <!-- 登录按钮 -->
          <el-form-item>
            <el-button 
              type="primary" 
              class="login-btn" 
              size="large" 
              @click="doSubmit"
            >
              登 录
            </el-button>
          </el-form-item>

          <!-- 底部链接 -->
          <div class="bottom-links">
            <a href="javascript:void(0)" class="link-blue" @click="showPanel(2)">忘记密码?</a>
            <a href="javascript:void(0)" class="link-blue" @click="showPanel(0)">没有账号?</a>
          </div>

          <!-- 其他登录方式 -->
          <div class="other-login">
            <div class="divider">
              <span>其他登录方式</span>
            </div>
            <div class="social-login">
              <div class="social-item" @click="showQQLogin">
                <img src="@/assets/qq.jpg" alt="QQ" />
                <span>QQ登录</span>
              </div>
              <div class="social-item" @click="showWechatLogin">
                <div class="wechat-icon"></div>
                <span>微信登录</span>
              </div>
            </div>
          </div>
        </el-form>
      </div>

      <!-- 注册卡片 -->
      <div class="login-card" v-else-if="opType==0">
        <div class="login-title">注册账号</div>
        <el-form
          class="login-form"
          :model="formData"
          :rules="rules"
          ref="formDataRef"
          @submit.prevent
        >
          <el-form-item prop="email">
            <el-input
              v-model.trim="formData.email"
              placeholder="请输入邮箱"
              size="large"
              clearable
            >
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

                    <!-- 邮箱验证码 -->
          <el-form-item prop="emailCode">
            <div class="verify-code-row">
              <el-input
                v-model.trim="formData.emailCode"
                placeholder="请输入邮箱验证码"
                size="large"
                clearable
              >
                <template #prefix>
                <span class="iconfont icon-checkcode"></span>
                </template>
              </el-input>
              <el-button 
                type="primary" 
                size="large" 
                class="send-code-btn"
                :disabled="countdown > 0"
                @click="getEmailCode"
              >
                {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
              </el-button>
            </div>
          </el-form-item>

          <el-form-item prop="nickName">
            <el-input
              v-model.trim="formData.nickName"
              placeholder="请输入昵称"
              size="large"
              clearable
            >
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="RegisterPassword">
            <el-input
              v-model="formData.RegisterPassword"
              type="password"
              placeholder="请输入密码"
              size="large"
              show-password
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="ConfirmPassword">
            <el-input
              v-model="formData.ConfirmPassword"
              type="password"
              placeholder="请再次输入密码"
              size="large"
              show-password
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="checkCode">
            <div class="verify-code-row">
              <el-input
                v-model.trim="formData.checkCode"
                placeholder="请输入验证码"
                size="large"
                clearable
              >
                <template #prefix>
                <span class="iconfont icon-checkcode"></span>
                </template>
              </el-input>
              <img :src="checkCodeUrl" class="check-code-img" @click="changeCheckCode(0)"/>
            </div>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" class="login-btn" size="large" @click="doSubmit">
              注 册
            </el-button>
          </el-form-item>

          <div class="bottom-links center">
            <a href="javascript:void(0)" class="link-blue" @click="showPanel(1)">已有账号?去登录</a>
          </div>
        </el-form>
      </div>

      <!-- 重置密码卡片 -->
      <div class="login-card" v-else-if="opType==2">
        <div class="login-title">重置密码</div>
        <el-form
          class="login-form"
          :model="formData"
          :rules="rules"
          ref="formDataRef"
          @submit.prevent
        >
          <el-form-item prop="email">
            <el-input
              v-model.trim="formData.email"
              placeholder="请输入邮箱"
              size="large"
              clearable
            >
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <!-- 邮箱验证码 -->
          <el-form-item prop="emailCode">
            <div class="verify-code-row">
              <el-input
                v-model.trim="formData.emailCode"
                placeholder="请输入邮箱验证码"
                size="large"
                clearable
              >
                <template #prefix>
                  <el-icon><Message /></el-icon>
                </template>
              </el-input>
              <el-button 
                type="primary" 
                size="large" 
                class="send-code-btn"
                :disabled="countdown > 0"
                @click="getEmailCode"
              >
                {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
              </el-button>
            </div>
          </el-form-item>

          <el-form-item prop="RegisterPassword">
            <el-input
              v-model="formData.RegisterPassword"
              type="password"
              placeholder="请输入新密码"
              size="large"
              show-password
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="ConfirmPassword">
            <el-input
              v-model="formData.ConfirmPassword"
              type="password"
              placeholder="请再次输入新密码"
              size="large"
              show-password
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="checkCode">
            <div class="verify-code-row">
              <el-input
                v-model.trim="formData.checkCode"
                placeholder="请输入图片验证码"
                size="large"
                clearable
              >
                <template #prefix>
                  <el-icon><Key /></el-icon>
                </template>
              </el-input>
              <img :src="checkCodeUrl" class="check-code-img" @click="changeCheckCode(0)"/>
            </div>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" class="login-btn" size="large" @click="doSubmit">
              重置密码
            </el-button>
          </el-form-item>

          <div class="bottom-links center">
            <a href="javascript:void(0)" class="link-blue" @click="showPanel(1)">去登录</a>
          </div>
        </el-form>
      </div>
    </div>
    
    <!-- 发送邮箱验证码弹窗 -->
    <Dialog
      :show="dialogConfig4SendMailCode.show"
      :title="dialogConfig4SendMailCode.title"
      :buttons="dialogConfig4SendMailCode.buttons"
      width="400px"
      :showCancel="false"
      @close="dialogConfig4SendMailCode.show=false"
    >
      <el-form
        :model="formData4SendMailCode"
        :rules="rules"
        ref="formData4SendMailCodeRef"
        @submit.prevent
      >
        <el-form-item label="邮箱">
          {{ formData.email }}
        </el-form-item>
        <el-form-item label="图片验证码" prop="checkCode">
          <div class="check-code-panel">
            <el-input
              v-model.trim="formData4SendMailCode.checkCode"
              placeholder="请输入验证码"
              size="large"
              clearable
            />
            <img :src="checkCodeUrl4SendMailCode" class="check-code" @click="changeCheckCode(1)"/>
          </div>
        </el-form-item>
      </el-form>
    </Dialog>
  </div>
</template>


<script setup>
import {getCurrentInstance, nextTick, reactive, ref ,onMounted} from "vue";
import Dialog from "@/components/Dialog.vue";
import {useRouter,useRoute} from "vue-router";
import md5 from "js-md5"
import { User, Lock, Key, Message } from '@element-plus/icons-vue'

const router = useRouter();
const route = useRoute();
const { proxy } = getCurrentInstance();

/*操作类型 0:注册 1:登录 2:忘记密码*/
const opType = ref(1);
const showPanel = (type) =>{
  opType.value = type;
  restForm();
}
const ConfirmPassword = (rule,value,callback)=>{
  if (value!==formData.value.RegisterPassword){
      callback(new Error(rule.message));
  }else {
    callback();
  }
}

const formData = ref({});
const formDataRef = ref();
const rules = {
  email: [
    {required:true,message:"请输入邮箱"},
    {validator:proxy.Verify.email,message: "请输入正确的邮箱"}
  ],
  RegisterPassword: [
    {required:true,message:"请输入密码"},
    {validator:proxy.Verify.password,message: "密码必须包含字母、数字，且长度至少8位"}
  ],
  password:[
    {required:true,message:"请输入密码"},
    {validator:proxy.Verify.password,message: "密码必须包含字母、数字，且长度至少8位"}
  ],
  nickName: [
    {required:true,message:"请输入昵称"},
  ],
  ConfirmPassword: [
    {required:true,message:"请再次输入密码"},
    {validator:ConfirmPassword,message: "两次输入密码不一致"}
  ],
  checkCode: [
    {required:true,message:"请输入图片验证码"},
  ],
  remenberMe:[],
}

const api = {
  Register:"/register",
  checkCode:"/api/checkCode",
  sendEmailCode:"/sendEmailCode",
  login:"/login",
  resetPassword:"/resetPassword"
}
//获取验证码
const checkCodeUrl = ref(api.checkCode);
const checkCodeUrl4SendMailCode = ref(api.checkCode);

const changeCheckCode = (type) => {
  if (type==0){
    checkCodeUrl.value =
        api.checkCode + "?type=" +type + "&time=" +new Date().getTime();
  }else {
    checkCodeUrl4SendMailCode.value =
        api.checkCode + "?type=" +type + "&time=" +new Date().getTime();
  }
}

// 验证码倒计时
const countdown = ref(0);
const startCountdown = () => {
  countdown.value = 60;
  const timer = setInterval(() => {
    countdown.value--;
    if (countdown.value <= 0) {
      clearInterval(timer);
    }
  }, 1000);
}

const formData4SendMailCode = ref({});
const formData4SendMailCodeRef = ref();

const dialogConfig4SendMailCode = reactive({
  show: false,
  title: "发送邮箱验证码",
  buttons: [
    {
      type:"primary",
      text:"发送验证码",
      click:(e)=>{
        sendEmailCode();
      },
    },
  ],
});
//发送邮箱验证码
const sendEmailCode=()=>{
  formData4SendMailCodeRef.value.validate(async (valid)=>{
    if (!valid){
      return;
    }
    const params = Object.assign({},formData4SendMailCode.value);
    params.type = opType.value == 0?0:1;
    let result = await proxy.Request({
      url:api.sendEmailCode,
      params:params,
      errorCallback:()=>{
        changeCheckCode(1)
      }
    })
    if (!result){
      return
    }
    proxy.Message.success(result.info)
    dialogConfig4SendMailCode.show = false;
    startCountdown();
  })
}
const getEmailCode= ()=>{
  formDataRef.value.validateField("email", (valid) => {
    if (!valid){
      return;
    }
    dialogConfig4SendMailCode.show=true;
    nextTick(()=>{
      changeCheckCode(1);
      formData4SendMailCodeRef.value.resetFields();
      formData4SendMailCode.value={
        email:formData.value.email
      }
    })
  });
}
// 显示QQ登录提示
const showQQLogin = () => {
  proxy.Message.info("QQ登录接口开发中...");
}

// 显示微信登录提示
const showWechatLogin = () => {
  proxy.Message.info("微信登录接口开发中...");
}

const doSubmit=()=>{
  formDataRef.value.validate(async (valid)=>{
    if(!valid){
      return;
    }
    let params = {};
    Object.assign(params,formData.value);
    //注册 找回密码
    if (opType.value==0||opType.value==2){
      params.password = params.RegisterPassword;
      delete params.RegisterPassword;
      delete params.ConfirmPassword;
    }else if (opType.value==1){
      // 登录时使用密码
      let cookieLoginInfo = proxy.Cookies.get("loginInfo");
      let cookiePassword = cookieLoginInfo==null?null:cookieLoginInfo.password
      if (params.password!==cookiePassword){
        params.password = md5(params.password);
      }
    }
    let url = null;
    if (opType.value==0){
      url = api.Register
    }
    if (opType.value==1){
      url = api.login
    }
    if (opType.value==2){
      url = api.resetPassword
    }
    let result = await proxy.Request({
      url:url,
      params:params,
      errorCallback:()=>{
        changeCheckCode(0)
      }
    })
    if (!result){
      changeCheckCode(0)
      return;
    }
    if (opType.value==0){
      proxy.Message.success("注册成功")
      showPanel(1)
    }else if (opType.value==1){
      if (params.rememberMe){
        const loginInfo = {
          email:params.email,
          password:params.password,
          rememberMe: params.rememberMe
        };
        proxy.Cookies.set("loginInfo",loginInfo,"7d");
      }else {
        proxy.Cookies.remove("loginInfo");
      }
      proxy.Message.success("登录成功");
      //存储cookie
      proxy.Cookies.set("userInfo",result.data,0)
      //重定向到原始页面
      const redirectUrl = route.query.redirectUrl||"/home";
      router.push(redirectUrl);
    }else if(opType.value==2){
      proxy.Message.success("重置成功")
      showPanel(1)
    }
  })
}
onMounted(()=>{
  showPanel(1)
})
//重置表单
const restForm=()=>{
  changeCheckCode(0);
  formDataRef.value.resetFields();
  formData.value = {}
  countdown.value = 0;
  if (opType.value==1){
    let cookieLoginInfo = proxy.Cookies.get("loginInfo");
    if (cookieLoginInfo){
      formData.value = {
        email: cookieLoginInfo.email,
        password: cookieLoginInfo.password,
        rememberMe: cookieLoginInfo.rememberMe
      };
    }
  }
}
</script>

<style scoped>
.login-body {
  height: 100vh;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 100px;
  padding: 0 10%;

  .bg {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    height: 100%;

    .illustration {
      position: relative;
      width: 400px;
      height: 300px;

      .chart-card {
        position: absolute;
        left: 0;
        top: 50px;
        width: 200px;
        height: 140px;
        background: #fff;
        border-radius: 8px;
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
        padding: 20px;

        .chart-line {
          width: 100%;
          height: 4px;
          background: #409eff;
          border-radius: 2px;
          margin-bottom: 15px;
        }

        .chart-dots {
          display: flex;
          flex-direction: column;
          gap: 10px;

          span {
            width: 80%;
            height: 3px;
            background: #e4e7ed;
            border-radius: 2px;

            &:nth-child(1) { width: 60%; }
            &:nth-child(2) { width: 80%; }
            &:nth-child(3) { width: 50%; }
          }
        }
      }

      .person {
        position: absolute;
        left: 150px;
        top: 80px;
        z-index: 2;

        .person-head {
          width: 40px;
          height: 40px;
          background: #5b8ff9;
          border-radius: 50%;
          margin: 0 auto;
        }

        .person-body {
          width: 60px;
          height: 80px;
          background: #5b8ff9;
          border-radius: 30px 30px 0 0;
          margin-top: -10px;
        }

        .person-legs {
          display: flex;
          gap: 10px;
          justify-content: center;

          &::before,
          &::after {
            content: '';
            width: 20px;
            height: 50px;
            background: #303133;
            border-radius: 0 0 10px 10px;
          }
        }
      }

      .pie-chart {
        position: absolute;
        right: 50px;
        top: 30px;
        width: 100px;
        height: 100px;
        border-radius: 50%;
        background: conic-gradient(#67c23a 0deg 120deg, #e4e7ed 120deg 360deg);
        opacity: 0.6;
      }
    }
  }

  .login-panel {
    width: 400px;

    .login-card {
      background: #fff;
      border-radius: 8px;
      padding: 40px 35px;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);

      .login-title {
        text-align: center;
        font-size: 18px;
        font-weight: 600;
        color: #303133;
        margin-bottom: 25px;
      }

      .login-form {
        :deep(.el-input__wrapper) {
          border-radius: 4px;
          box-shadow: 0 0 0 1px #dcdfe6 inset;
          padding-left: 12px;

          &.is-focus {
            box-shadow: 0 0 0 1px #409eff inset;
          }
        }

        :deep(.el-input__inner) {
          height: 42px;
        }

        :deep(.el-input__prefix) {
          color: #909399;
          font-size: 16px;
          margin-right: 8px;
        }

        .verify-code-row {
          display: flex;
          gap: 10px;
          align-items: center;

          .el-input {
            flex: 1;
          }

          .check-code-img {
            height: 42px;
            border-radius: 4px;
            cursor: pointer;
            border: 1px solid #dcdfe6;
          }

          .send-code-btn {
            width: 110px;
            height: 42px;
            border-radius: 4px;
            font-size: 13px;
            padding: 0 10px;

            &:disabled {
              background: #a0cfff;
              border-color: #a0cfff;
            }
          }
        }

        .remember-row {
          margin-bottom: 10px;
          
          :deep(.el-checkbox__label) {
            color: #606266;
            font-size: 14px;
          }
        }

        .login-btn {
          width: 100%;
          height: 42px;
          border-radius: 4px;
          font-size: 15px;
          font-weight: 500;
          margin-top: 5px;
        }

        .bottom-links {
          display: flex;
          justify-content: space-between;
          margin-bottom: 20px;
          
          &.center {
            justify-content: center;
          }

          .link-blue {
            color: #409eff;
            font-size: 14px;
            text-decoration: none;

            &:hover {
              text-decoration: underline;
            }
          }
        }

        .other-login {
          .divider {
            display: flex;
            align-items: center;
            margin-bottom: 20px;

            &::before,
            &::after {
              content: '';
              flex: 1;
              height: 1px;
              background: #e4e7ed;
            }

            span {
              padding: 0 16px;
              color: #909399;
              font-size: 14px;
            }
          }

          .social-login {
            display: flex;
            justify-content: center;
            gap: 60px;

            .social-item {
              display: flex;
              flex-direction: column;
              align-items: center;
              gap: 8px;
              cursor: pointer;
              transition: opacity 0.3s;

              &:hover {
                opacity: 0.8;
              }

              img {
                width: 36px;
                height: 36px;
                border-radius: 50%;
              }

              .wechat-icon {
                width: 36px;
                height: 36px;
                background: #07c160;
                border-radius: 50%;
                display: flex;
                align-items: center;
                justify-content: center;

                &::before {
                  content: '微';
                  color: #fff;
                  font-size: 14px;
                }
              }

              span {
                font-size: 14px;
                color: #606266;
              }
            }
          }
        }
      }
    }
  }

  .check-code-panel {
    display: flex;
    gap: 12px;
    align-items: center;

    .el-input {
      flex: 1;
    }

    .check-code {
      height: 42px;
      border-radius: 4px;
      cursor: pointer;
      border: 1px solid #dcdfe6;
    }
  }
}
</style>