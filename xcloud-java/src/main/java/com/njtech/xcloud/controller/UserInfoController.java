package com.njtech.xcloud.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

import com.njtech.xcloud.config.RedisUtils;
import com.njtech.xcloud.entity.constants.Constants;
import com.njtech.xcloud.entity.enums.ResponseCodeEnum;
import com.njtech.xcloud.entity.query.UserInfoQuery;
import com.njtech.xcloud.entity.po.UserInfo;
import com.njtech.xcloud.entity.vo.ResponseVO;
import com.njtech.xcloud.entity.vo.SessionWebUserVO;
import com.njtech.xcloud.exception.BusinessException;
import com.njtech.xcloud.mappers.UserInfoMapper;
import com.njtech.xcloud.service.EmailCodeService;
import com.njtech.xcloud.service.UserInfoService;
import com.njtech.xcloud.service.impl.MinioServiceImpl;
import com.njtech.xcloud.utils.CaptchaUtil;
import com.njtech.xcloud.utils.StringTools;
import org.apache.catalina.User;
import org.apache.http.HttpRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.util.Map;

/**
 * Controller
 */
@RestController("UserInfoController")
public class UserInfoController extends ABaseController {

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private MinioServiceImpl minioService;

    @Resource
    private UserInfoMapper userInfoMapper;

    /**
     * 获取验证码图片流
     *
     * @param type     0:登录注册 1:邮箱验证码发送 默认0
     * @param request  HTTP请求
     * @param response HTTP响应
     */
    @GetMapping("/checkCode")
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
    public ResponseVO register(HttpSession session, String email, String nickName, String password, String checkCode, String emailCode) {
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
    public ResponseVO login(HttpSession session, String email, String password, String checkCode) {
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
     * @param userId   用户ID（此处作为文件名使用）
     * @param response HTTP响应
     */
    @GetMapping("/getAvatar/{userId}")
    public void getImage(
            @PathVariable("userId") String userId,
            HttpServletResponse response) {
        InputStream inputStream = null;
        ServletOutputStream outputStream = null;
        try {
            String fileName = userId + ".jpg";
            // 尝试从MinIO获取文件流
            try {
                inputStream = minioService.getFileStream(fileName);
            } catch (Exception e) {
                // 获取失败，使用默认图像
                inputStream = minioService.getFileStream("default_img.jpg");
            }

            // 设置响应头
            response.setContentType("image/jpeg");
            response.setHeader("Cache-Control", "max-age=86400"); // 缓存1天

            // 将文件流写入响应输出流
            outputStream = response.getOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException("获取图片失败: " + e.getMessage());
        } finally {
            // 关闭流            
			try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}