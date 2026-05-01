package com.njtech.xcloud.controller;

import com.njtech.xcloud.entity.enums.FileFolderTypeEnums;
import com.njtech.xcloud.entity.enums.FileTypeEnums;
import com.njtech.xcloud.entity.enums.ResponseCodeEnum;
import com.njtech.xcloud.entity.po.FileInfo;
import com.njtech.xcloud.entity.query.FileInfoQuery;
import com.njtech.xcloud.entity.vo.PaginationResultVO;
import com.njtech.xcloud.entity.vo.ResponseVO;
import com.njtech.xcloud.exception.BusinessException;
import com.njtech.xcloud.mappers.FileInfoMapper;
import com.njtech.xcloud.utils.StringTools;
import org.springframework.beans.BeanUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class ABaseController {

    @Resource
    private FileInfoMapper<FileInfo, FileInfoQuery> fileInfoMapper;

    protected static final String STATUC_SUCCESS = "success";

    protected static final String STATUC_ERROR = "error";

    protected <T> ResponseVO getSuccessResponseVO(T t) {
        ResponseVO<T> responseVO = new ResponseVO<>();
        responseVO.setStatus(STATUC_SUCCESS);
        responseVO.setCode(ResponseCodeEnum.CODE_200.getCode());
        responseVO.setInfo(ResponseCodeEnum.CODE_200.getMsg());
        responseVO.setData(t);
        return responseVO;
    }

    protected <T> ResponseVO getBusinessErrorResponseVO(BusinessException e, T t) {
        ResponseVO vo = new ResponseVO();
        vo.setStatus(STATUC_ERROR);
        if (e.getCode() == null) {
            vo.setCode(ResponseCodeEnum.CODE_600.getCode());
        } else {
            vo.setCode(e.getCode());
        }
        vo.setInfo(e.getMessage());
        vo.setData(t);
        return vo;
    }

    protected <T> ResponseVO getServerErrorResponseVO(T t) {
        ResponseVO vo = new ResponseVO();
        vo.setStatus(STATUC_ERROR);
        vo.setCode(ResponseCodeEnum.CODE_500.getCode());
        vo.setInfo(ResponseCodeEnum.CODE_500.getMsg());
        vo.setData(t);
        return vo;
    }

    /**
     * 将 PaginationResultVO<S> 转换为 PaginationResultVO<T>
     * 通过 BeanUtils.copyProperties 逐个拷贝 list 中对象的属性
     *
     * @param source      源分页结果
     * @param targetClass 目标VO的Class
     * @return 转换后的分页结果
     */
    protected <S, T> PaginationResultVO<T> convertPaginationResult(PaginationResultVO<S> source, Class<T> targetClass) {
        List<T> voList = new ArrayList<>();
        try {
            for (S item : source.getList()) {
                T target = targetClass.getDeclaredConstructor().newInstance();
                BeanUtils.copyProperties(item, target);
                voList.add(target);
            }
        } catch (Exception e) {
            throw new BusinessException("分页数据转换失败: " + e.getMessage());
        }
        return new PaginationResultVO<>(
                source.getTotalCount(),
                source.getPageSize(),
                source.getPageNo(),
                source.getPageTotal(),
                voList
        );
    }


    protected void rename(String fileName, String filePid, String fileId, String userId) {
        FileInfo fileInfo = fileInfoMapper.selectByFileId(fileId);
        String suffix = StringTools.getFileSuffixWithPoint(fileInfo.getFileName());
        fileName = fileName + suffix;
        //校验
        checkFileName(filePid,fileName,userId, fileInfo.getFolderType());
        FileInfo updateFileInfo = new FileInfo();
        updateFileInfo.setFileName(fileName);
        updateFileInfo.setLastUpdateTime(new Date());
        fileInfoMapper.updateByFileIdAndUserId(updateFileInfo,fileId,userId);
    }

    private void checkFileName(String filePid, String fileName, String userId, Integer folderType) {
        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(userId);
        query.setFileName(fileName);
        query.setFileType(folderType);
        query.setFilePid(filePid);
        Integer count = fileInfoMapper.selectCount(query);
        if (count > 1) {
            throw new BusinessException("此目录下已存在同名文件,请修改文件名");
        }
    }
}
