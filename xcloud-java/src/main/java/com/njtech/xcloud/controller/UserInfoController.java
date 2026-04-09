package com.njtech.xcloud.controller;

import java.io.IOException;
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

/**
 *  Controller
 */
@RestController("UserInfoController")
public class UserInfoController extends ABaseController{

	@Resource
	private UserInfoService userInfoService;

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	@Resource
	private RedisUtils redisUtils;

	@Resource
	private UserInfoMapper userInfoMapper;
	/**
	 * 获取验证码图片流
	 * @param type 0:登录注册 1:邮箱验证码发送 默认0
	 * @param request HTTP请求
	 * @param response HTTP响应
	 */
	@GetMapping("/checkCode")
	public void getCheckCode(
			@RequestParam(value = "type", defaultValue = "0") Integer type,
			HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		// 生成验证码
		String code = CaptchaUtil.generateCode();
		
		// 生成验证码图片
		BufferedImage image = CaptchaUtil.generateImage(code);
		
		// 将验证码存入session
		HttpSession session = request.getSession();
		if (type == 0) {
			session.setAttribute("checkCode", code);
		} else if (type == 1) {
			session.setAttribute("emailCheckCode", code);
		}
		
		// 设置响应头
		response.setContentType("image/png");
		response.setHeader("Pragma", "No-cache");
		response.setHeader("Cache-Control", "no-cache");
		response.setDateHeader("Expires", 0);
		
		// 输出图片流
		ServletOutputStream outputStream = response.getOutputStream();
		ImageIO.write(image, "png", outputStream);
		outputStream.flush();
		outputStream.close();
	}

	@GetMapping("/test")
	public String test(HttpServletRequest request){
		HttpSession session = request.getSession();
		stringRedisTemplate.opsForValue().set("emailCheckCode", String.valueOf(session.getAttribute("emailCheckCode")));
		String test = stringRedisTemplate.opsForValue().get("emailCheckCode");
		return test;
	}

	@GetMapping("/test2")
	public UserInfo test2(HttpServletRequest request){
		UserInfo test = (UserInfo) userInfoMapper.selectByUserId("2117787425");
		return test;
	}

	/**
	 * 发送邮箱验证码
	 * @param email 接收方邮箱
	 * @param checkCode 图片验证码
	 * @param type 类型 0:注册 1:找回密码
	 * @param request HTTP请求
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
		return userInfoService.sendEmailCode(email,checkCode,request,type);
	}

	@RequestMapping("/register")
	public ResponseVO register(HttpSession session, String email, String nickName, String password, String checkCode, String emailCode) {
		try {
			if (!checkCode.equalsIgnoreCase((String) session.getAttribute("checkCode"))){
				throw new BusinessException("图片验证码错误");
			}
			userInfoService.register(email,nickName,password,checkCode,emailCode);
			return getSuccessResponseVO(null);
		} finally {
			session.removeAttribute("checkCode");
		}
	}


	@RequestMapping("/login")
	public ResponseVO login(HttpSession session, String email, String password, String checkCode) {
		try {
			if (!checkCode.equalsIgnoreCase((String) session.getAttribute("checkCode"))){
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
			if (!checkCode.equalsIgnoreCase((String) session.getAttribute("checkCode"))){
				throw new BusinessException("图片验证码错误");
			}
			userInfoService.retPassword(email,password);
			return getSuccessResponseVO(null);
		} finally {
			session.removeAttribute("checkCode");
		}
	}
}