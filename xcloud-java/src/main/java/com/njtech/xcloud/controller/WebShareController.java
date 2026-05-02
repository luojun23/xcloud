package com.njtech.xcloud.controller;

import com.njtech.xcloud.annotation.GlobalInterceptor;
import com.njtech.xcloud.annotation.VerifyParam;
import com.njtech.xcloud.entity.constants.Constants;
import com.njtech.xcloud.entity.po.FileInfo;
import com.njtech.xcloud.entity.po.FileShare;
import com.njtech.xcloud.entity.po.UserInfo;
import com.njtech.xcloud.entity.query.FileInfoQuery;
import com.njtech.xcloud.entity.vo.*;
import com.njtech.xcloud.exception.BusinessException;
import com.njtech.xcloud.service.FileInfoService;
import com.njtech.xcloud.service.FileShareService;
import com.njtech.xcloud.service.UserInfoService;
import com.njtech.xcloud.utils.StringTools;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @ClassName : WebShareController
 * @Description : 外部分享接口
 * @Author : 罗君
 * @Date: 2026/5/2
 */
@RestController("webShareController")
@RequestMapping("/showShare")
public class WebShareController extends ABaseController {

    @Resource
    private FileShareService fileShareService;

    @Resource
    private FileInfoService fileInfoService;

    @Resource
    private UserInfoService userInfoService;

    /**
     * 获取分享基本信息（无需提取码）
     */
    @RequestMapping("/getShareInfo")
    @GlobalInterceptor(checkLogin = false)
    public ResponseVO getShareInfo(@VerifyParam(required = true) String shareId) {
        FileShare fileShare = fileShareService.getFileShareByShareId(shareId);
        if (fileShare == null) {
            throw new BusinessException("分享不存在");
        }
        if (fileShare.getExpireTime() != null && fileShare.getExpireTime().before(new Date())) {
            throw new BusinessException("分享已过期");
        }

        UserInfo userInfo = userInfoService.getUserInfoByUserId(fileShare.getUserId());
        FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(fileShare.getFileId(), fileShare.getUserId());

        WebShareInfoVO webShareInfoVO = new WebShareInfoVO();
        webShareInfoVO.setUserId(fileShare.getUserId());
        webShareInfoVO.setNickName(userInfo != null ? userInfo.getNickName() : "");
        webShareInfoVO.setAvatar(userInfo != null ? userInfo.getQqAvatar() : "");
        webShareInfoVO.setFileName(fileInfo != null ? fileInfo.getFileName() : "");
        webShareInfoVO.setShareTime(fileShare.getShareTime());

        return getSuccessResponseVO(webShareInfoVO);
    }

    /**
     * 校验提取码
     */
    @RequestMapping("/checkShareCode")
    @GlobalInterceptor(checkLogin = false)
    public ResponseVO checkShareCode(HttpSession session,
                                     @VerifyParam(required = true) String shareId,
                                     @VerifyParam(required = true) String code) {
        session.setAttribute(Constants.SESSION_SHARE + "_" + shareId, shareId);
        fileShareService.checkShareCode(shareId, code);
        return getSuccessResponseVO(null);
    }

    /**
     * 获取分享登录信息（已校验提取码）
     */
    @RequestMapping("/getShareLoginInfo")
    @GlobalInterceptor(checkLogin = false)
    public ResponseVO getShareLoginInfo(HttpSession session, @VerifyParam(required = true) String shareId) {
        Object shareSession = session.getAttribute(Constants.SESSION_SHARE + "_" + shareId);
        if (shareSession == null) {
            return getSuccessResponseVO(null);
        }

        FileShare fileShare = fileShareService.getFileShareByShareId(shareId);
        if (fileShare == null) {
            throw new BusinessException("分享不存在");
        }
        if (fileShare.getExpireTime() != null && fileShare.getExpireTime().before(new Date())) {
            throw new BusinessException("分享已过期");
        }

        UserInfo userInfo = userInfoService.getUserInfoByUserId(fileShare.getUserId());
        FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(fileShare.getFileId(), fileShare.getUserId());

        WebShareInfoVO webShareInfoVO = new WebShareInfoVO();
        webShareInfoVO.setUserId(fileShare.getUserId());
        webShareInfoVO.setNickName(userInfo != null ? userInfo.getNickName() : "");
        webShareInfoVO.setAvatar(userInfo != null ? userInfo.getQqAvatar() : "");
        webShareInfoVO.setFileName(fileInfo != null ? fileInfo.getFileName() : "");
        webShareInfoVO.setShareTime(fileShare.getShareTime());

        // 判断当前登录用户是否是分享者本人
        SessionWebUserVO webUserVO = (SessionWebUserVO) session.getAttribute(Constants.SESSION_WEB_USER);
        boolean currentUser = webUserVO != null && webUserVO.getUserId().equals(fileShare.getUserId());
        webShareInfoVO.setCurrentUser(currentUser);

        return getSuccessResponseVO(webShareInfoVO);
    }

    /**
     * 加载分享文件列表
     */
    @RequestMapping("/loadFileList")
    @GlobalInterceptor(checkLogin = false)
    public ResponseVO loadFileList(FileInfoQuery query, @VerifyParam(required = true) String shareId) {
        FileShare fileShare = fileShareService.getFileShareByShareId(shareId);
        if (fileShare == null) {
            throw new BusinessException("分享不存在");
        }
        if (fileShare.getExpireTime() != null && fileShare.getExpireTime().before(new Date())) {
            throw new BusinessException("分享已过期");
        }

        String userId = fileShare.getUserId();
        String shareFileId = fileShare.getFileId();
        FileInfo shareFileInfo = fileInfoService.getFileInfoByFileIdAndUserId(shareFileId, userId);
        if (shareFileInfo == null) {
            throw new BusinessException("分享文件不存在");
        }

        if ("0".equals(query.getFilePid())) {
            if (shareFileInfo.getFolderType() != null && shareFileInfo.getFolderType() == 1) {
                // 分享的是文件夹，查询文件夹下的内容
                query.setUserId(userId);
                query.setFilePid(shareFileId);
                query.setDelFlag(Constants.USING);
                query.setOrderBy("last_update_time desc");
                PaginationResultVO result = fileInfoService.findListByPage(query);
                return getSuccessResponseVO(convertPaginationResult(result, FileInfoVO.class));
            } else {
                // 分享的是文件，直接返回该文件
                List<FileInfoVO> list = new ArrayList<>();
                FileInfoVO vo = new FileInfoVO();
                BeanUtils.copyProperties(shareFileInfo, vo);
                list.add(vo);
                PaginationResultVO<FileInfoVO> result = new PaginationResultVO<>();
                result.setList(list);
                result.setTotalCount(1);
                result.setPageSize(query.getPageSize() == null ? 15 : query.getPageSize());
                result.setPageNo(query.getPageNo() == null ? 1 : query.getPageNo());
                result.setPageTotal(1);
                return getSuccessResponseVO(result);
            }
        } else {
            // 已在文件夹内部，正常查询
            query.setUserId(userId);
            query.setDelFlag(Constants.USING);
            query.setOrderBy("last_update_time desc");
            PaginationResultVO result = fileInfoService.findListByPage(query);
            return getSuccessResponseVO(convertPaginationResult(result, FileInfoVO.class));
        }
    }

    /**
     * 获取分享目录导航信息
     */
    @RequestMapping("/getFolderInfo")
    @GlobalInterceptor(checkLogin = false)
    public ResponseVO getFolderInfo(String path, @VerifyParam(required = true) String shareId) {
        FileShare fileShare = fileShareService.getFileShareByShareId(shareId);
        if (fileShare == null) {
            throw new BusinessException("分享不存在");
        }
        if (StringTools.isEmpty(path)) {
            return getSuccessResponseVO(new ArrayList<>());
        }
        List<FileInfo> folderList = fileInfoService.getFolderInfo(fileShare.getUserId(), path);
        return getSuccessResponseVO(folderList);
    }

    /**
     * 获取文件流（预览）
     */
    @RequestMapping("/getFile/{shareId}/{fileId}")
    @GlobalInterceptor(checkLogin = false)
    public void getFile(@PathVariable("shareId") String shareId,
                        @PathVariable("fileId") String fileId,
                        HttpServletResponse response) {
        FileShare fileShare = fileShareService.getFileShareByShareId(shareId);
        if (fileShare == null) {
            throw new BusinessException("分享不存在");
        }
        fileInfoService.getFile(fileShare.getUserId(), fileId, response);
    }

    /**
     * 获取视频 HLS m3u8 索引文件
     */
    @RequestMapping("/ts/getVideo/{shareId}/{fileId}")
    @GlobalInterceptor(checkLogin = false)
    public void getVideoInfo(@PathVariable("shareId") String shareId,
                             @PathVariable("fileId") String fileId,
                             HttpServletResponse response) {
        FileShare fileShare = fileShareService.getFileShareByShareId(shareId);
        if (fileShare == null) {
            throw new BusinessException("分享不存在");
        }
        fileInfoService.getVideoInfo(fileId, response);
    }

    /**
     * 获取视频 HLS ts 切片文件
     */
    @RequestMapping("/ts/{shareId}/{fileId}/{tsName}")
    @GlobalInterceptor(checkLogin = false)
    public void getVideo(@PathVariable("shareId") String shareId,
                         @PathVariable("fileId") String fileId,
                         @PathVariable("tsName") String tsName,
                         HttpServletResponse response) {
        FileShare fileShare = fileShareService.getFileShareByShareId(shareId);
        if (fileShare == null) {
            throw new BusinessException("分享不存在");
        }
        fileInfoService.getVideo(fileId, tsName, response);
    }

    /**
     * 创建下载链接
     */
    @RequestMapping("/createDownloadUrl/{shareId}/{fileId}")
    @GlobalInterceptor(checkLogin = false)
    public ResponseVO createDownloadUrl(@PathVariable("shareId") String shareId,
                                        @PathVariable("fileId") String fileId) {
        FileShare fileShare = fileShareService.getFileShareByShareId(shareId);
        if (fileShare == null) {
            throw new BusinessException("分享不存在");
        }
        String downloadCode = fileInfoService.createDownloadUrl(fileShare.getUserId(), fileId);
        return getSuccessResponseVO(downloadCode);
    }

    /**
     * 下载文件
     */
    @RequestMapping("/download/{downloadCode}")
    @GlobalInterceptor(checkLogin = false)
    public void download(@PathVariable("downloadCode") String downloadCode,
                         HttpServletResponse response) {
        fileInfoService.download(downloadCode, response);
    }
}
