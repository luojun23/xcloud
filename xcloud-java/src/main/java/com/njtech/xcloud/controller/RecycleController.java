package com.njtech.xcloud.controller;

import com.njtech.xcloud.annotation.GlobalInterceptor;
import com.njtech.xcloud.annotation.VerifyParam;
import com.njtech.xcloud.entity.constants.Constants;
import com.njtech.xcloud.entity.po.FileInfo;
import com.njtech.xcloud.entity.query.FileInfoQuery;
import com.njtech.xcloud.entity.vo.FileInfoVO;
import com.njtech.xcloud.entity.vo.PaginationResultVO;
import com.njtech.xcloud.entity.vo.ResponseVO;
import com.njtech.xcloud.entity.vo.SessionWebUserVO;
import com.njtech.xcloud.service.FileInfoService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName : RecycleController
 * @Description :
 * @Author : 罗君
 * @Date: 2026/5/1
 */
@RestController("recycleController")
@RequestMapping("/recycle")
public class RecycleController extends ABaseController{

    @Resource
    private FileInfoService fileInfoService;
    /**
     * 根据条件分页查询
     */
    @GlobalInterceptor
    @RequestMapping("/loadRecycleList")
    public ResponseVO loadRecycleList(HttpSession session, FileInfoQuery query, String category) {
        // 从 session 获取当前登录用户
        SessionWebUserVO userInfo = (SessionWebUserVO) session.getAttribute(Constants.SESSION_WEB_USER);
        query.setUserId(userInfo.getUserId());
        query.setDelFlag(Constants.RECYCLE);
        query.setOrderBy("recovery_time desc");

        // 不分页查询所有回收站文件
        List<FileInfo> allList = fileInfoService.findListByParam(query);

        // 过滤掉父目录也在回收站中的文件（只显示顶级被删除的项）
        List<FileInfo> filteredList = allList.stream().filter(fileInfo -> {
            String filePid = fileInfo.getFilePid();
            if ("0".equals(filePid)) {
                return true;
            }
            FileInfo parent = fileInfoService.getFileInfoByFileIdAndUserId(filePid, userInfo.getUserId());
            return parent == null || !Constants.RECYCLE.equals(parent.getDelFlag());
        }).collect(Collectors.toList());

        // 手动分页
        int pageNo = query.getPageNo() == null ? 1 : query.getPageNo();
        int pageSize = query.getPageSize() == null ? 15 : query.getPageSize();
        int totalCount = filteredList.size();
        int pageTotal = (totalCount + pageSize - 1) / pageSize;
        int start = (pageNo - 1) * pageSize;
        List<FileInfo> pageList = new ArrayList<>();
        if (start < totalCount) {
            int end = Math.min(start + pageSize, totalCount);
            pageList = filteredList.subList(start, end);
        }

        PaginationResultVO<FileInfo> result = new PaginationResultVO<>();
        result.setTotalCount(totalCount);
        result.setPageSize(pageSize);
        result.setPageNo(pageNo);
        result.setPageTotal(pageTotal);
        result.setList(pageList);

        // 将 PaginationResultVO<FileInfo> 转换为 PaginationResultVO<FileInfoVO>
        PaginationResultVO<FileInfoVO> voResult = convertPaginationResult(result, FileInfoVO.class);

        return getSuccessResponseVO(voResult);
    }

    @RequestMapping("/recoverFile")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO recoverFile(HttpSession session,
                                   @VerifyParam(required = true) String fileIds) {
        SessionWebUserVO webUserVO = (SessionWebUserVO) session.getAttribute(Constants.SESSION_WEB_USER);
        fileInfoService.recoverFile(webUserVO.getUserId(), fileIds);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/delFile")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO delFile(HttpSession session,
                               @VerifyParam(required = true) String fileIds) {
        SessionWebUserVO webUserVO = (SessionWebUserVO) session.getAttribute(Constants.SESSION_WEB_USER);
        fileInfoService.delFileRecycle(webUserVO.getUserId(), fileIds);
        return getSuccessResponseVO(null);
    }
}
