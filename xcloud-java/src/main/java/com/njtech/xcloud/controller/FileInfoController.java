package com.njtech.xcloud.controller;

import com.njtech.xcloud.annotation.GlobalInterceptor;
import com.njtech.xcloud.annotation.VerifyParam;
import com.njtech.xcloud.entity.constants.Constants;
import com.njtech.xcloud.entity.enums.FileCategoryEnum;
import com.njtech.xcloud.entity.po.FileInfo;
import com.njtech.xcloud.entity.query.FileInfoQuery;
import com.njtech.xcloud.entity.vo.FileInfoVO;
import com.njtech.xcloud.entity.vo.PaginationResultVO;
import com.njtech.xcloud.entity.vo.ResponseVO;
import com.njtech.xcloud.entity.vo.SessionWebUserVO;
import com.njtech.xcloud.service.FileInfoService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

/**
 * 文件信息表 Controller
 */
@RestController("fileInfoController")
@RequestMapping("/file")
public class FileInfoController extends ABaseController {

	@Resource
	private FileInfoService fileInfoService;

	/**
	 * 根据条件分页查询
	 */
	@GlobalInterceptor
	@RequestMapping("/loadDataList")
	public ResponseVO loadDataList(HttpSession session, FileInfoQuery query, String category) {
		// 从 session 获取当前登录用户
		SessionWebUserVO userInfo = (SessionWebUserVO) session.getAttribute(Constants.SESSION_WEB_USER);
		query.setUserId(userInfo.getUserId());

		// 只查询正常状态的文件（delFlag=2）
		query.setDelFlag(Constants.USING);

		// 根据 category 字符串映射 fileCategory
		FileCategoryEnum fileCategory = FileCategoryEnum.getByCode(category);
		if (null != fileCategory){
			query.setFileCategory(fileCategory.getCategory());
		}

		query.setOrderBy("last_update_time desc");

		// 分页查询
		PaginationResultVO<FileInfo> result = fileInfoService.findListByPage(query);

		// 将 PaginationResultVO<FileInfo> 转换为 PaginationResultVO<FileInfoVO>
		PaginationResultVO<FileInfoVO> voResult = convertPaginationResult(result, FileInfoVO.class);

		return getSuccessResponseVO(voResult);
	}


	@GlobalInterceptor(checkParams = true)
	@RequestMapping("/uploadFile")
	public ResponseVO uploadFile(HttpSession session,
								 String fileId,
								 MultipartFile file,
								 @VerifyParam(required = true) String fileName,
								 @VerifyParam(required = true) String fileMd5,
								 @VerifyParam(required = true) String filePid,
								 @VerifyParam(required = true) Integer chunkIndex,
								 @VerifyParam(required = true) Integer chunks
	) {
		SessionWebUserVO webUserVO = (SessionWebUserVO) session.getAttribute(Constants.SESSION_WEB_USER);
		fileInfoService.uploadFile(webUserVO, fileId, file, fileName, fileMd5, filePid, chunkIndex, chunks);
		return getSuccessResponseVO(null);
	}
}