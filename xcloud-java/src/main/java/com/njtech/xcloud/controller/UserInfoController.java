package com.njtech.xcloud.controller;

import java.io.IOException;

import com.njtech.xcloud.annotation.GlobalInterceptor;
import com.njtech.xcloud.annotation.VerifyParam;
import com.njtech.xcloud.config.Appconfig;
import com.njtech.xcloud.config.RedisComponent;
import com.njtech.xcloud.dto.UserSpaceDto;
import com.njtech.xcloud.entity.constants.Constants;
import com.njtech.xcloud.entity.enums.VerifyRegexEnum;
import com.njtech.xcloud.entity.query.UserInfoQuery;
import com.njtech.xcloud.entity.vo.ResponseVO;
import com.njtech.xcloud.entity.vo.SessionWebUserVO;
import com.njtech.xcloud.exception.BusinessException;
import com.njtech.xcloud.mappers.UserInfoMapper;
import com.njtech.xcloud.service.FileInfoService;
import com.njtech.xcloud.service.UserInfoService;
import com.njtech.xcloud.utils.CaptchaUtil;
import com.njtech.xcloud.utils.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.net.URLEncoder;

/**
 * Controller
 */
@RestController("UserInfoController")
public class UserInfoController extends ABaseController {

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private FileInfoService fileInfoService;

    @Resource
    private UserInfoMapper userInfoMapper;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private Appconfig appconfig;

    private static final Logger logger = LoggerFactory.getLogger(UserInfoController.class);

    /**
     * 获取验证码图片流
     *
     * @param type     0:登录注册 1:邮箱验证码发送 默认0
     * @param request  HTTP请求
     * @param response HTTP响应
     */
    @GetMapping("/checkCode")
    @GlobalInterceptor(checkLogin = false)
    public void getCheckCode(
            @RequestParam(value = "type", defaultValue = "0") Integer type,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        // 生成验证码
        String checkCode = CaptchaUtil.generateCode();
        BufferedImage image = CaptchaUtil.generateImage(checkCode);

        // 将验证码存入session
        HttpSession session = request.getSession();
        if (type == 0) {
            // 登录/注册验证码
            session.setAttribute("checkCode", checkCode);
        } else {
            // 邮箱验证码
            session.setAttribute("emailCheckCode", checkCode);
        }

        // 设置响应头，禁止缓存
        response.setContentType("image/png");
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);

        // 将图片写入响应输出流
        ServletOutputStream out = response.getOutputStream();
        ImageIO.write(image, "png", out);
        out.flush();
        out.close();
    }

    /**
     * 发送邮箱验证码
     *
     * @param email     接收方邮箱
     * @param checkCode 图片验证码
     * @param type      类型 0:注册 1:找回密码
     * @param request   HTTP请求
     * @return 响应结果
     */
    @RequestMapping("/sendEmailCode")
    @GlobalInterceptor(checkLogin = false)
    public ResponseVO<String> sendEmailCode(
            @RequestParam(value = "email") String email,
            @RequestParam(value = "checkCode") String checkCode,
            @RequestParam(value = "type", defaultValue = "0") Integer type,
            HttpServletRequest request) {

        // 校验邮箱格式
        if (StringTools.isEmpty(email)) {
            throw new BusinessException("邮箱不能为空");
        }
        // 校验图片验证码
        HttpSession session = request.getSession();
        String sessionCheckCode = (String) session.getAttribute("emailCheckCode");
        if (StringTools.isEmpty(sessionCheckCode)) {
            throw new BusinessException("图片验证码已过期，请重新获取");
        }
        if (!sessionCheckCode.equalsIgnoreCase(checkCode)) {
            throw new BusinessException("图片验证码错误");
        }

        UserInfoQuery userInfoQuery = new UserInfoQuery();
        userInfoQuery.setEmail(email);
        Integer count = userInfoService.findCountByParam(userInfoQuery);

        // 根据类型判断
        if (type == 0) {
            // 注册：检查邮箱是否已存在
            if (count > 0) {
                throw new BusinessException("该邮箱已被注册");
            }
        } else if (type == 1) {
            // 找回密码：检查邮箱是否存在
            if (count == 0) {
                throw new BusinessException("该邮箱未注册");
            }
        } else {
            throw new BusinessException("参数错误");
        }
        return userInfoService.sendEmailCode(email, checkCode, request, type);
    }

    @RequestMapping("/register")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public ResponseVO register(HttpSession session,
                               @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL, max = 150) String email,
                               @VerifyParam(required = true,max = 14) String nickName,
                               @VerifyParam(required = true, regex = VerifyRegexEnum.PASSWORD,min = 8,max = 18) String password,
                               @VerifyParam(required = true) String checkCode,
                               @VerifyParam(required = true) String emailCode) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute("checkCode"))) {
                throw new BusinessException("图片验证码错误");
            }
            userInfoService.register(email, nickName, password, checkCode, emailCode);
            return getSuccessResponseVO(null);
        } finally {
            session.removeAttribute("checkCode");
        }
    }


    @RequestMapping("/login")
    @GlobalInterceptor(checkLogin = false)
    public ResponseVO login(HttpSession session,
                            @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL) String email,
                            @VerifyParam(required = true, regex = VerifyRegexEnum.PASSWORD) String password,
                            String checkCode) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute("checkCode"))) {
                throw new BusinessException("图片验证码错误");
            }
            SessionWebUserVO sessionWebUserVO = userInfoService.login(email, password);
            session.setAttribute(Constants.SESSION_WEB_USER, sessionWebUserVO);
            return getSuccessResponseVO(sessionWebUserVO);
        } finally {
            session.removeAttribute("checkCode");
        }
    }

    @RequestMapping("/resetPwd")
    @GlobalInterceptor
    public ResponseVO retPassword(HttpSession session, String email, String password, String checkCode) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute("checkCode"))) {
                throw new BusinessException("图片验证码错误");
            }
            userInfoService.retPassword(email, password);
            return getSuccessResponseVO(null);
        } finally {
            session.removeAttribute("checkCode");
        }
    }

    /**
     * 获取图片并以图片流形式输出
     *
     * @param userId 用户ID（此处作为文件名使用）
     */
    @GetMapping("/getAvatar/{userId}")
    public void getAvatar(
            @PathVariable("userId") String userId,
            HttpServletResponse response) {
        fileInfoService.getFile(userId, response);
    }

    @RequestMapping("/logout")
    @GlobalInterceptor
    public ResponseVO logout(HttpSession session) {
        session.invalidate();
        return getSuccessResponseVO(null);
    }

    @PostMapping("/updateUserAvatar")
    @GlobalInterceptor
    public ResponseVO updateUserAvatar(MultipartFile avatar, HttpSession session) {
        SessionWebUserVO sessionWebUserVO = (SessionWebUserVO) session.getAttribute(Constants.SESSION_WEB_USER);
        String userId = sessionWebUserVO.getUserId();
        fileInfoService.updateUserAvatar(userId, avatar);
        return getSuccessResponseVO(null);
    }

    @GetMapping("/updatePassword")
    @GlobalInterceptor
    public void updatePassword(
            @PathVariable("userId") String userId,
            HttpServletResponse response) {
        fileInfoService.getFile(userId, response);
    }

    @RequestMapping("/getUseSpace")
    @GlobalInterceptor
    public ResponseVO getUseSpace(HttpSession session) {
        SessionWebUserVO sessionWebUserVO = (SessionWebUserVO) session.getAttribute(Constants.SESSION_WEB_USER);
        UserSpaceDto userSpaceUse = redisComponent.getUserSpaceUse(sessionWebUserVO.getUserId());
        return getSuccessResponseVO(userSpaceUse);
    }

    /**
     * 获取QQ授权URL，前端跳转到QQ登录页
     */
    @RequestMapping("/qqLogin")
    @GlobalInterceptor(checkLogin = false)
    public ResponseVO<String> qqLogin() {
        String state = StringTools.getRandomNumber(Constants.TEN);
        String redirectUrl;
        try {
            redirectUrl = URLEncoder.encode(appconfig.getQqUrlRedirect(), "UTF-8");
        } catch (Exception e) {
            throw new BusinessException("回调地址编码失败");
        }
        String url = String.format(appconfig.getQqUrlAuthorization(),
                appconfig.getQqAppId(), redirectUrl, state);
        return getSuccessResponseVO(url);
    }
 
    /**
     * QQ OAuth回调接口
     * 前端携带QQ返回的code调用此接口，后端完成OAuth流程并返回用户信息
     */
    @RequestMapping("/qqCallback")
    @GlobalInterceptor(checkLogin = false)
    public ResponseVO<SessionWebUserVO> qqCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            HttpServletRequest request,
            HttpSession session) throws Exception {
        SessionWebUserVO sessionWebUserVO = userInfoService.qqLogin(code, session);
        return getSuccessResponseVO(sessionWebUserVO);
    }
}