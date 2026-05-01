package com.njtech.xcloud.controller;

import com.njtech.xcloud.annotation.GlobalInterceptor;
import com.njtech.xcloud.annotation.VerifyParam;
import com.njtech.xcloud.entity.constants.Constants;
import com.njtech.xcloud.entity.po.FileInfo;
import com.njtech.xcloud.entity.query.FileShareQuery;
import com.njtech.xcloud.entity.po.FileShare;
import com.njtech.xcloud.entity.vo.FileShareVO;
import com.njtech.xcloud.entity.vo.PaginationResultVO;
import com.njtech.xcloud.entity.vo.ResponseVO;
import com.njtech.xcloud.entity.vo.SessionWebUserVO;
import com.njtech.xcloud.service.FileInfoService;
import com.njtech.xcloud.service.FileShareService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller
 */
@RestController("fileShareController")
@RequestMapping("/share")
public class FileShareController extends ABaseController {

    @Resource
    private FileShareService fileShareService;

    @Resource
    private FileInfoService fileInfoService;

    /**
     * 根据条件分页查询
     */
    @RequestMapping("/loadShareList")
    @GlobalInterceptor
    public ResponseVO loadShareList(HttpSession session, FileShareQuery query) {
        SessionWebUserVO webUserVO = (SessionWebUserVO) session.getAttribute(Constants.SESSION_WEB_USER);
        query.setOrderBy("share_time desc");
        query.setUserId(webUserVO.getUserId());
        PaginationResultVO<FileShare> result = fileShareService.findListByPage(query);

        List<FileShareVO> voList = new ArrayList<>();
        if (result != null && result.getList() != null) {
            for (FileShare share : result.getList()) {
                if (share == null) {
                    continue;
                }
                FileShareVO vo = new FileShareVO();
                vo.setShareId(share.getShareId());
                vo.setFileId(share.getFileId());
                vo.setUserId(share.getUserId());
                vo.setValidType(share.getValidType());
                vo.setExpireTime(share.getExpireTime());
                vo.setShareTime(share.getShareTime());
                vo.setCode(share.getCode());
                vo.setShowCount(share.getShowCount());

                if (share.getFileId() != null && share.getUserId() != null) {
                    FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(share.getFileId(), share.getUserId());
                    if (fileInfo != null) {
                        if (fileInfo.getFileName() != null) {
                            vo.setFileName(fileInfo.getFileName());
                        }
                        if (fileInfo.getFileCover() != null) {
                            vo.setFileCover(fileInfo.getFileCover());
                        }
                        if (fileInfo.getFolderType() != null) {
                            vo.setFolderType(fileInfo.getFolderType());
                        }
                        if (fileInfo.getFileCategory() != null) {
                            vo.setFileCategory(fileInfo.getFileCategory());
                        }
                        if (fileInfo.getFileType() != null) {
                            vo.setFileType(fileInfo.getFileType());
                        }
                    }
                }
                voList.add(vo);
            }
        }

        PaginationResultVO<FileShareVO> voResult = new PaginationResultVO<>();
        if (result != null) {
            voResult.setTotalCount(result.getTotalCount());
            voResult.setPageSize(result.getPageSize());
            voResult.setPageNo(result.getPageNo());
            voResult.setPageTotal(result.getPageTotal());
        }
        voResult.setList(voList);
        return getSuccessResponseVO(voResult);
    }

    @RequestMapping("/shareFile")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO shareFile(HttpSession session,
                                @VerifyParam(required = true) String fileId,
                                @VerifyParam(required = true) Integer validType,
                                @VerifyParam(required = true) Integer codeType,
                                String code) {
        SessionWebUserVO webUserVO = (SessionWebUserVO) session.getAttribute(Constants.SESSION_WEB_USER);
        FileShare fileShare = fileShareService.shareFile(webUserVO.getUserId(), fileId, validType, codeType, code);
        return getSuccessResponseVO(fileShare);
    }

    @RequestMapping("/cancelShare")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO cancelShare(HttpSession session,
                                  @VerifyParam(required = true) String shareIds) {
        SessionWebUserVO webUserVO = (SessionWebUserVO) session.getAttribute(Constants.SESSION_WEB_USER);
        fileShareService.cancelShare(webUserVO.getUserId(), shareIds);
        return getSuccessResponseVO(null);
    }
}