package com.njtech.xcloud.controller;

import com.njtech.xcloud.annotation.GlobalInterceptor;
import com.njtech.xcloud.annotation.VerifyParam;
import com.njtech.xcloud.entity.constants.Constants;
import com.njtech.xcloud.entity.po.FileInfo;
import com.njtech.xcloud.entity.po.FileShare;
import com.njtech.xcloud.entity.po.UserInfo;
import com.njtech.xcloud.entity.query.FileInfoQuery;
import com.njtech.xcloud.entity.query.FileShareQuery;
import com.njtech.xcloud.entity.query.UserInfoQuery;
import com.njtech.xcloud.entity.vo.*;
import com.njtech.xcloud.service.FileInfoService;
import com.njtech.xcloud.service.FileShareService;
import com.njtech.xcloud.service.UserInfoService;
import com.njtech.xcloud.utils.StringTools;
import org.apache.catalina.User;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller
 */
@RestController("adminController")
@RequestMapping("/admin")
public class AdminController extends ABaseController {
    @Resource
    private FileInfoService fileInfoService;

    @Resource
    private UserInfoService userInfoService;

    @RequestMapping("/loadUserList")
    @GlobalInterceptor
    public ResponseVO getImage(UserInfoQuery userInfoQuery) {
        userInfoQuery.setOrderBy("join_time desc");
        PaginationResultVO resultVO = userInfoService.findListByPage(userInfoQuery);
        return getSuccessResponseVO(convertPaginationResult(resultVO, UserInfoVo.class));
    }

    @RequestMapping("/updateUserSpace")
    @GlobalInterceptor
    public ResponseVO updateUserSpace(@VerifyParam(required = true) String userId,
                                       @VerifyParam(required = true) Integer changeSpace) {
        userInfoService.updateUserSpace(userId, changeSpace);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/updateUserStatus")
    @GlobalInterceptor
    public ResponseVO updateUserStatus(@VerifyParam(required = true) String userId,
                                        @VerifyParam(required = true) Integer status) {
        userInfoService.updateUserStatus(userId, status);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/loadFileList")
    @GlobalInterceptor
    public ResponseVO loadFileList(FileInfoQuery fileInfoQuery) {
        fileInfoQuery.setOrderBy("last_update_time desc");
        PaginationResultVO resultVO = fileInfoService.findListByPage(fileInfoQuery);
        PaginationResultVO<FileInfoVO> voResult = convertPaginationResult(resultVO, FileInfoVO.class);
        if (voResult.getList() != null) {
            for (FileInfoVO vo : voResult.getList()) {
                if (!StringTools.isEmpty(vo.getUserId())) {
                    UserInfo userInfo = userInfoService.getUserInfoByUserId(vo.getUserId());
                    if (userInfo != null) {
                        vo.setNickName(userInfo.getNickName());
                    }
                }
            }
        }
        return getSuccessResponseVO(voResult);
    }

    @RequestMapping("/getFolderInfo")
    @GlobalInterceptor
    public ResponseVO getFolderInfo(String path) {
        List<FileInfo> folderList = fileInfoService.getFolderInfo4Admin(path);
        return getSuccessResponseVO(folderList);
    }

    @RequestMapping("/delFile")
    @GlobalInterceptor
    public ResponseVO delFile(@VerifyParam(required = true) String fileId) {
        String[] itemArray = fileId.split(",");
        for (String item : itemArray) {
            if (StringTools.isEmpty(item)) {
                continue;
            }
            String[] parts = item.split("_");
            if (parts.length != 2) {
                continue;
            }
            String userId = parts[0];
            String fileIdItem = parts[1];
            fileInfoService.delFileRecycle(userId, fileIdItem);
        }
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/createDownloadUrl/{userId}/{fileId}")
    @GlobalInterceptor
    public ResponseVO createDownloadUrl(@PathVariable("userId") String userId,
                                         @PathVariable("fileId") String fileId) {
        String downloadCode = fileInfoService.createDownloadUrl(userId, fileId);
        return getSuccessResponseVO(downloadCode);
    }

    @RequestMapping("/download/{downloadCode}")
    public void download(@PathVariable("downloadCode") String downloadCode,
                          HttpServletResponse response) {
        fileInfoService.download(downloadCode, response);
    }

    @RequestMapping("/getFile/{userId}/{fileId}")
    @GlobalInterceptor
    public void getFile(@PathVariable("userId") String userId,
                         @PathVariable("fileId") String fileId,
                         HttpServletResponse response) {
        fileInfoService.getFile(userId, fileId, response);
    }

    @RequestMapping("/ts/getVideo/{userId}/{fileId}")
    @GlobalInterceptor
    public void getVideoInfo(@PathVariable("userId") String userId,
                              @PathVariable("fileId") String fileId,
                              HttpServletResponse response) {
        fileInfoService.getVideoInfo(fileId, response);
    }

    @RequestMapping("/ts/{userId}/{fileId}/{tsName}")
    @GlobalInterceptor
    public void getVideo(@PathVariable("userId") String userId,
                          @PathVariable("fileId") String fileId,
                          @PathVariable("tsName") String tsName,
                          HttpServletResponse response) {
        fileInfoService.getVideo(fileId, tsName, response);
    }
}