package com.njtech.xcloud.service;

import java.util.List;

import com.njtech.xcloud.entity.query.UserInfoQuery;
import com.njtech.xcloud.entity.po.UserInfo;
import com.njtech.xcloud.entity.vo.PaginationResultVO;
import com.njtech.xcloud.entity.vo.ResponseVO;
import com.njtech.xcloud.entity.vo.SessionWebUserVO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;


/**
 * 业务接口
 */
public interface UserInfoService {

    /**
     * 根据条件查询列表
     */
    List<UserInfo> findListByParam(UserInfoQuery param);

    /**
     * 根据条件查询列表
     */
    Integer findCountByParam(UserInfoQuery param);

    /**
     * 分页查询
     */
    PaginationResultVO<UserInfo> findListByPage(UserInfoQuery param);

    /**
     * 新增
     */
    Integer add(UserInfo bean);

    /**
     * 批量新增
     */
    Integer addBatch(List<UserInfo> listBean);

    /**
     * 批量新增/修改
     */
    Integer addOrUpdateBatch(List<UserInfo> listBean);

    /**
     * 多条件更新
     */
    Integer updateByParam(UserInfo bean, UserInfoQuery param);

    /**
     * 多条件删除
     */
    Integer deleteByParam(UserInfoQuery param);

    /**
     * 根据UserId查询对象
     */
    UserInfo getUserInfoByUserId(String userId);


    /**
     * 根据UserId修改
     */
    Integer updateUserInfoByUserId(UserInfo bean, String userId);


    /**
     * 根据UserId删除
     */
    Integer deleteUserInfoByUserId(String userId);


    /**
     * 根据Email查询对象
     */
    UserInfo getUserInfoByEmail(String email);


    /**
     * 根据Email修改
     */
    Integer updateUserInfoByEmail(UserInfo bean, String email);


    /**
     * 根据Email删除
     */
    Integer deleteUserInfoByEmail(String email);


    /**
     * 根据QqOpenId查询对象
     */
    UserInfo getUserInfoByQqOpenId(String qqOpenId);


    /**
     * 根据QqOpenId修改
     */
    Integer updateUserInfoByQqOpenId(UserInfo bean, String qqOpenId);


    /**
     * 根据QqOpenId删除
     */
    Integer deleteUserInfoByQqOpenId(String qqOpenId);


    /**
     * 根据NickName查询对象
     */
    UserInfo getUserInfoByNickName(String nickName);


    /**
     * 根据NickName修改
     */
    Integer updateUserInfoByNickName(UserInfo bean, String nickName);


    /**
     * 根据NickName删除
     */
    Integer deleteUserInfoByNickName(String nickName);

    ResponseVO<String> sendEmailCode(String email, String checkCode, HttpServletRequest request, Integer type);

    void register(String email, String nickName, String password, String checkCode, String emailCode);

    SessionWebUserVO login(String email, String password);

    void retPassword(String email, String password);

    void updateUserSpace(String userId, Integer changeSpace);

    void updateUserStatus(String userId, Integer status);

    /**
     * QQ登录：根据授权码完成整个OAuth流程，返回SessionWebUserVO
     *
     * @param code    QQ授权码
     * @param session HTTP会话
     * @return 登录用户信息
     */
    SessionWebUserVO qqLogin(String code, HttpSession session) throws Exception;
}