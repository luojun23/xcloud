package com.njtech.xcloud.service.impl;

import com.njtech.xcloud.config.Appconfig;
import com.njtech.xcloud.config.RedisUtils;
import com.njtech.xcloud.dto.UserSpaceDto;
import com.njtech.xcloud.entity.constants.Constants;
import com.njtech.xcloud.entity.enums.PageSize;
import com.njtech.xcloud.entity.enums.ResponseCodeEnum;
import com.njtech.xcloud.entity.enums.UserStatusEnum;
import com.njtech.xcloud.entity.po.UserInfo;
import com.njtech.xcloud.entity.query.SimplePage;
import com.njtech.xcloud.entity.query.UserInfoQuery;
import com.njtech.xcloud.entity.vo.PaginationResultVO;
import com.njtech.xcloud.entity.vo.ResponseVO;
import com.njtech.xcloud.entity.vo.SessionWebUserVO;
import com.njtech.xcloud.exception.BusinessException;
import com.njtech.xcloud.mappers.UserInfoMapper;
import com.njtech.xcloud.service.UserInfoService;
import com.njtech.xcloud.utils.StringTools;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;


/**
 * 业务接口实现
 */
@Service("userInfoService")
public class UserInfoServiceImpl implements UserInfoService {

    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private JavaMailSender javaMailSender;

    @Resource
    private Appconfig appconfig;

    private static final Logger logger = LoggerFactory.getLogger(UserInfoServiceImpl.class);

    /**
     * 根据条件查询列表
     */
    @Override
    public List<UserInfo> findListByParam(UserInfoQuery param) {
        return this.userInfoMapper.selectList(param);
    }

    /**
     * 根据条件查询列表
     */
    @Override
    public Integer findCountByParam(UserInfoQuery param) {
        return this.userInfoMapper.selectCount(param);
    }

    /**
     * 分页查询方法
     */
    @Override
    public PaginationResultVO<UserInfo> findListByPage(UserInfoQuery param) {
        int count = this.findCountByParam(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

        SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
        param.setSimplePage(page);
        List<UserInfo> list = this.findListByParam(param);
        PaginationResultVO<UserInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    /**
     * 新增
     */
    @Override
    public Integer add(UserInfo bean) {
        return this.userInfoMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<UserInfo> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.userInfoMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或者修改
     */
    @Override
    public Integer addOrUpdateBatch(List<UserInfo> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.userInfoMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 多条件更新
     */
    @Override
    public Integer updateByParam(UserInfo bean, UserInfoQuery param) {
        StringTools.checkParam(param);
        return this.userInfoMapper.updateByParam(bean, param);
    }

    /**
     * 多条件删除
     */
    @Override
    public Integer deleteByParam(UserInfoQuery param) {
        StringTools.checkParam(param);
        return this.userInfoMapper.deleteByParam(param);
    }

    /**
     * 根据UserId获取对象
     */
    @Override
    public UserInfo getUserInfoByUserId(String userId) {
        return this.userInfoMapper.selectByUserId(userId);
    }

    /**
     * 根据UserId修改
     */
    @Override
    public Integer updateUserInfoByUserId(UserInfo bean, String userId) {
        return this.userInfoMapper.updateByUserId(bean, userId);
    }

    /**
     * 根据UserId删除
     */
    @Override
    public Integer deleteUserInfoByUserId(String userId) {
        return this.userInfoMapper.deleteByUserId(userId);
    }

    /**
     * 根据Email获取对象
     */
    @Override
    public UserInfo getUserInfoByEmail(String email) {
        return this.userInfoMapper.selectByEmail(email);
    }

    /**
     * 根据Email修改
     */
    @Override
    public Integer updateUserInfoByEmail(UserInfo bean, String email) {
        return this.userInfoMapper.updateByEmail(bean, email);
    }

    /**
     * 根据Email删除
     */
    @Override
    public Integer deleteUserInfoByEmail(String email) {
        return this.userInfoMapper.deleteByEmail(email);
    }

    @Override
    public UserInfo getUserInfoByQqOpenId(String qqOpenId) {
        return null;
    }

    @Override
    public Integer updateUserInfoByQqOpenId(UserInfo bean, String qqOpenId) {
        return null;
    }

    @Override
    public Integer deleteUserInfoByQqOpenId(String qqOpenId) {
        return null;
    }

    @Override
    public UserInfo getUserInfoByNickName(String nickName) {
        return null;
    }

    @Override
    public Integer updateUserInfoByNickName(UserInfo bean, String nickName) {
        return null;
    }

    @Override
    public Integer deleteUserInfoByNickName(String nickName) {
        return null;
    }

    @Override
    public ResponseVO<String> sendEmailCode(String email, String checkCode, HttpServletRequest request, Integer type) {
        ResponseVO<String> responseVO = new ResponseVO<>();
        try {
            // 生成6位数字验证码
            String emailCode = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));

            // 发送邮件
            sendEmail(email, emailCode, type);

            // 保存验证码到Redis，有效期15分钟
            String redisKey = "email_code:" + email;
            redisUtils.set(redisKey, emailCode, 15 * 60);

            responseVO.setStatus("success");
            responseVO.setCode(ResponseCodeEnum.CODE_200.getCode());
            responseVO.setInfo("验证码已发送至邮箱，请注意查收");

            // 清除session中的图片验证码
            request.getSession().removeAttribute("emailCheckCode");

        } catch (BusinessException e) {
            responseVO.setStatus("error");
            responseVO.setCode(ResponseCodeEnum.CODE_600.getCode());
            responseVO.setInfo(e.getMessage());
        } catch (Exception e) {
            responseVO.setStatus("error");
            responseVO.setCode(ResponseCodeEnum.CODE_500.getCode());
            responseVO.setInfo("发送邮件失败，请稍后重试");
            e.printStackTrace();
        }
        return responseVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(String email, String nickName, String password, String checkCode, String emailCode) {
        UserInfo userInfo = userInfoMapper.selectByEmail(email);
        if (userInfo != null) {
            throw new BusinessException("邮箱已存在");
        }
        UserInfo userInfo1 = userInfoMapper.selectByNickName(nickName);
        if (userInfo1 != null) {
            throw new BusinessException("昵称已存在");
        }
        //校验邮箱验证码
        String redisCode = (String) redisUtils.get("email_code:" + email);
        if (redisCode == null) {
            throw new BusinessException("邮箱验证码已过期，请重新获取");
        }
        if (!redisCode.equals(emailCode)) {
            throw new BusinessException("邮箱验证码错误");
        }
        UserInfo userInfo2 = new UserInfo();
        userInfo2.setUserId(StringTools.getRandomNumber(Constants.TEN));
        userInfo2.setNickName(nickName);
        userInfo2.setEmail(email);
        userInfo2.setPassword(StringTools.Md5(password));
        userInfo2.setJoinTime(new Date());
        userInfo2.setStatus(Constants.ONE); // 1:正常 0:禁用
        userInfo2.setUseSpace(0L);
        userInfo2.setTotalSpace(5 * 1024 * 1024L);
        userInfoMapper.insert(userInfo2);
        redisUtils.del("email_code:" + email);
    }

    @Override
    public SessionWebUserVO login(String email, String password) {
        UserInfo userInfo = userInfoMapper.selectByEmail(email);
        if (userInfo == null || !userInfo.getPassword().equals(password)){
            throw new BusinessException("邮箱或密码错误");
        }
        if (UserStatusEnum.NORMAL.getNum() == userInfo.getStatus()){
            throw new BusinessException("用户状态异常,请联系管理员");
        }
        UserInfo userInfo1 = new UserInfo();
        userInfo1.setLastLoginTime(new Date());
        userInfoMapper.updateByEmail(userInfo1, email);

        SessionWebUserVO sessionWebUserVO = new SessionWebUserVO();
        sessionWebUserVO.setUserId(userInfo.getUserId());
        sessionWebUserVO.setNickName(userInfo.getNickName());
        boolean b = ArrayUtils.contains(appconfig.getAdminEmails().split(","), email);
        sessionWebUserVO.setAdmin(b);
        //TODO 查询用户空间使用情况并存储到Redis
        UserSpaceDto userSpaceDto = new UserSpaceDto();
        userSpaceDto.setUseSpace(0L);
        userSpaceDto.setTotalSpace((long) userInfo.getTotalSpace());
        redisUtils.set(Constants.REDIS_KEY_USER_SPACE_USE + userInfo.getUserId(), userSpaceDto, Constants.REDIS_KEY_EXPIRES_DAY);
        return sessionWebUserVO;
    }

    @Override
    public void retPassword(String email, String password) {
        UserInfo userInfo = userInfoMapper.selectByEmail(email);
        if (userInfo == null) {
            throw new BusinessException("邮箱不存在");
        }
        userInfo.setPassword(StringTools.Md5(password));
        userInfoMapper.updateByEmail(userInfo, email);

    }

    /**
     * 发送邮件
     *
     * @param toEmail   接收方邮箱
     * @param emailCode 邮箱验证码
     */
    private void sendEmail(String toEmail, String emailCode, int type) throws Exception {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // 设置发件人（从配置文件中读取）
            helper.setFrom(appconfig.getFromEmail());
            helper.setTo(toEmail);

            String subject;
            String content;
            if (type == 0) {
                subject = "【Xcloud】注册验证码";
                content = buildEmailContent("注册", emailCode);
            } else {
                subject = "【Xcloud】找回密码验证码";
                content = buildEmailContent("找回密码", emailCode);
            }

            helper.setSubject(subject);
            helper.setText(content, true);

            javaMailSender.send(mimeMessage);
        } catch (Exception e) {
            logger.error("发送邮件失败", e);
            throw new BusinessException("发送邮件失败");
        }
    }

    /**
     * 构建邮件内容
     *
     * @param typeName  类型名称
     * @param emailCode 验证码
     * @return HTML格式的邮件内容
     */
    private String buildEmailContent(String typeName, String emailCode) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>");
        sb.append("<html>");
        sb.append("<head>");
        sb.append("<meta charset='UTF-8'>");
        sb.append("<title>邮箱验证码</title>");
        sb.append("<style>");
        sb.append("body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 20px; }");
        sb.append(".container { max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }");
        sb.append(".header { text-align: center; border-bottom: 2px solid #007bff; padding-bottom: 20px; margin-bottom: 30px; }");
        sb.append(".header h1 { color: #333333; margin: 0; font-size: 24px; }");
        sb.append(".content { line-height: 1.6; color: #555555; }");
        sb.append(".code-box { background-color: #f8f9fa; border: 2px dashed #007bff; padding: 20px; text-align: center; margin: 25px 0; border-radius: 5px; }");
        sb.append(".code { font-size: 32px; font-weight: bold; color: #007bff; letter-spacing: 5px; }");
        sb.append(".footer { text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eeeeee; color: #999999; font-size: 12px; }");
        sb.append("</style>");
        sb.append("</head>");
        sb.append("<body>");
        sb.append("<div class='container'>");
        sb.append("<div class='header'>");
        sb.append("<h1>").append(typeName).append("验证码</h1>");
        sb.append("</div>");
        sb.append("<div class='content'>");
        sb.append("<p>尊敬的用户，您好！</p>");
        sb.append("<p>您正在进行").append(typeName).append("操作，验证码如下：</p>");
        sb.append("<div class='code-box'>");
        sb.append("<span class='code'>").append(emailCode).append("</span>");
        sb.append("</div>");
        sb.append("<p>验证码有效期为15分钟，请尽快完成操作。如非本人操作，请忽略此邮件。</p>");
        sb.append("</div>");
        sb.append("<div class='footer'>");
        sb.append("<p>此邮件由系统自动发送，请勿回复</p>");
        sb.append("</div>");
        sb.append("</div>");
        sb.append("</body>");
        sb.append("</html>");
        return sb.toString();
    }
}