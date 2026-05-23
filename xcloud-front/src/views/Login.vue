<template>
  <div class="login-body">
    <div class="left-section">
      <div class="logo">
        <span class="iconfont icon-yunshangchuan"></span>
        <span class="logo-text">XCloud云盘</span>
      </div>
      <div class="slogan">
        <h1>XCloud云盘</h1>
        <h2>电脑高效拍档 <span class="highlight">一键存爽快看</span></h2>
      </div>
      <div class="feature-cards">
        <div class="feature-card">
          <div class="feature-icon">
            <span class="iconfont icon-transfer"></span>
          </div>
          <div class="feature-title">分片秒传，极速下载</div>
          <div class="feature-desc">大文件切片、断点续传、秒传、上传进度、上传速度限制</div>
        </div>
        <div class="feature-card">
          <div class="feature-icon">
            <span class="iconfont icon-folder"></span>
          </div>
          <div class="feature-title">轻松存储，高效管理</div>
          <div class="feature-desc">新建目录、文件重命名、文件移动、文件分享、删除</div>
        </div>
        <div class="feature-card">
          <div class="feature-icon">
            <span class="iconfont icon-video"></span>
          </div>
          <div class="feature-title">在线预览，便捷高效</div>
          <div class="feature-desc">视频分片播放，pdf、excel、word等多种格式文件在线预览</div>
        </div>
      </div>
    </div>
    <div class="login-panel">
      <!-- 登录卡片 -->
      <div class="login-card" v-if="opType==1">
        <div class="login-title">XCloud云盘</div>
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
                @keyup.enter="doSubmit"
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
                <span class="iconfont icon-checkcode"></span>
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
      width="450px"
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

    <!-- 底部备案号 -->
    <div class="icp-footer">
      <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer">赣ICP备2025062598号</a>
    </div>
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
    {validator:proxy.Verify.nickName,message: "昵称长度最多14位"}
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
// QQ登录 - 获取授权URL并跳转
const showQQLogin = async () => {
  let result = await proxy.Request({
    url: "/qqLogin",
    errorCallback: () => {
      proxy.Message.error("QQ登录服务暂不可用");
    }
  });
  if (result) {
    window.location.href = result.data;
  }
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
  background: linear-gradient(135deg, #f0f5ff 0%, #e6f0ff 50%, #f5f0ff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 80px;
  padding: 0 8%;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: -20%;
    right: -10%;
    width: 600px;
    height: 600px;
    background: radial-gradient(circle, rgba(64, 158, 255, 0.08) 0%, transparent 70%);
    border-radius: 50%;
  }

  &::after {
    content: '';
    position: absolute;
    bottom: -30%;
    left: -10%;
    width: 500px;
    height: 500px;
    background: radial-gradient(circle, rgba(103, 194, 58, 0.06) 0%, transparent 70%);
    border-radius: 50%;
  }

  .left-section {
    flex: 1;
    max-width: 600px;
    position: relative;
    z-index: 1;

    .logo {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 60px;

      .icon-yunshangchuan {
        font-size: 32px;
        color: #007fff;
      }

      .logo-text {
        font-size: 20px;
        font-weight: 600;
        color: #303133;
      }
    }

    .slogan {
      margin-bottom: 50px;

      h1 {
        font-size: 42px;
        font-weight: 700;
        color: #303133;
        margin: 0 0 15px 0;
        line-height: 1.2;
      }

      h2 {
        font-size: 28px;
        font-weight: 400;
        color: #606266;
        margin: 0;
        line-height: 1.4;

        .highlight {
          color: #409eff;
          font-weight: 600;
          position: relative;

          &::after {
            content: '';
            position: absolute;
            bottom: 2px;
            left: 0;
            width: 100%;
            height: 8px;
            background: linear-gradient(120deg, rgba(64, 158, 255, 0.2) 0%, rgba(64, 158, 255, 0.05) 100%);
            border-radius: 4px;
            z-index: -1;
          }
        }
      }
    }

    .feature-cards {
      display: flex;
      gap: 20px;

      .feature-card {
        flex: 1;
        background: rgba(255, 255, 255, 0.7);
        backdrop-filter: blur(10px);
        border-radius: 12px;
        padding: 20px;
        border: 1px solid rgba(255, 255, 255, 0.8);
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
        transition: transform 0.3s, box-shadow 0.3s;

        &:hover {
          transform: translateY(-4px);
          box-shadow: 0 8px 30px rgba(0, 0, 0, 0.08);
        }

        .feature-icon {
          width: 44px;
          height: 44px;
          background: linear-gradient(135deg, #409eff 0%, #67c23a 100%);
          border-radius: 10px;
          display: flex;
          align-items: center;
          justify-content: center;
          margin-bottom: 14px;

          .iconfont {
            font-size: 60px;
            color: #fff;
          }
        }

        .feature-title {
          font-size: 15px;
          font-weight: 600;
          color: #303133;
          margin-bottom: 8px;
        }

        .feature-desc {
          font-size: 13px;
          color: #909399;
          line-height: 1.6;
        }
      }
    }
  }

  .login-panel {
    width: 420px;
    position: relative;
    z-index: 1;

    .login-card {
      background: #fff;
      border-radius: 16px;
      padding: 40px 40px;
      box-shadow: 0 8px 40px rgba(0, 0, 0, 0.08);

      .login-title {
        text-align: center;
        margin-bottom: 30px;
        font-size: 20px;
        font-weight: 600;
        color: #303133;
      }

      .login-form {
        :deep(.el-input__wrapper) {
          border-radius: 8px;
          box-shadow: none;
          background: #f5f7fa;
          padding-left: 14px;

          &.is-focus {
            background: #fff;
            box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
          }
        }

        :deep(.el-input__inner) {
          height: 44px;
          background: transparent;
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
            height: 44px;
            border-radius: 8px;
            cursor: pointer;
            border: none;
          }

          .send-code-btn {
            width: 110px;
            height: 44px;
            border-radius: 8px;
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
          height: 46px;
          border-radius: 10px;
          font-size: 15px;
          font-weight: 500;
          margin-top: 5px;
          background: linear-gradient(135deg, #409eff 0%, #3a8ee6 100%);
          border: none;
          box-shadow: 0 4px 14px rgba(64, 158, 255, 0.3);
          transition: transform 0.2s, box-shadow 0.2s;

          &:hover {
            transform: translateY(-1px);
            box-shadow: 0 6px 20px rgba(64, 158, 255, 0.4);
          }
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
              background: repeating-linear-gradient(
                90deg,
                #dcdfe6 0px,
                #dcdfe6 4px,
                transparent 4px,
                transparent 8px
              );
            }

            span {
              padding: 0 12px;
              color: #909399;
              font-size: 13px;
              white-space: nowrap;
            }
          }

          .social-login {
            display: flex;
            justify-content: center;

            .social-item {
              display: flex;
              align-items: center;
              gap: 10px;
              cursor: pointer;
              transition: opacity 0.3s;
              padding: 8px 20px;
              border-radius: 8px;

              &:hover {
                opacity: 0.8;
                background: #f5f7fa;
              }

              img {
                width: 28px;
                height: 28px;
                border-radius: 50%;
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

  .icp-footer {
    position: fixed;
    bottom: 20px;
    left: 0;
    right: 0;
    text-align: center;
    z-index: 1;

    a {
      color: #909399;
      font-size: 13px;
      text-decoration: none;
      transition: color 0.3s;

      &:hover {
        color: #606266;
      }
    }
  }
}
</style>