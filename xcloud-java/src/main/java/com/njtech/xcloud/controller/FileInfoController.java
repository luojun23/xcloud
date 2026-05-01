package com.njtech.xcloud.controller;

import com.njtech.xcloud.annotation.GlobalInterceptor;
import com.njtech.xcloud.annotation.VerifyParam;
import com.njtech.xcloud.dto.UploadResultDto;
import com.njtech.xcloud.entity.constants.Constants;
import com.njtech.xcloud.entity.enums.FileCategoryEnum;
import com.njtech.xcloud.entity.po.FileInfo;
import com.njtech.xcloud.entity.query.FileInfoQuery;
import com.njtech.xcloud.entity.vo.FileInfoVO;
import com.njtech.xcloud.entity.vo.PaginationResultVO;
import com.njtech.xcloud.entity.vo.ResponseVO;
import com.njtech.xcloud.entity.vo.SessionWebUserVO;
import com.njtech.xcloud.service.FileInfoService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;

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


	/**
	 * 上传文件 chunks 分片上传
	 */
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
		return getSuccessResponseVO(fileInfoService.uploadFile(webUserVO, fileId, file, fileName, fileMd5, filePid, chunkIndex, chunks));
	}


	/**
	 * 获取图片并以图片流形式输出
	 * 路径示例：/api/file/getImage//file/202604/xxx.jpg
	 * cover 值为 /file/202604/xxx.jpg（数据库中 file_cover 的相对路径）
	 */
	@GetMapping("/getImage/**")
	@GlobalInterceptor
	public void getImage(
			HttpServletRequest request,
			HttpServletResponse response) {
		// 从 URI 中提取 /getImage/ 之后的部分作为 cover 路径
		String uri = request.getRequestURI();
		String cover = uri.substring(uri.indexOf("/getImage/") + "/getImage/".length());
		fileInfoService.getImage(cover, response);
	}

	/**
	 * 获取视频 HLS m3u8 索引文件
	 */
	@GetMapping("/ts/getVideoInfo/{fileId}")
	@GlobalInterceptor
	public void getVideoInfo(@PathVariable("fileId") String fileId, HttpServletResponse response) {
		fileInfoService.getVideoInfo(fileId, response);
	}

	/**
	 * 获取视频 HLS ts 切片文件
	 */
	@GetMapping("/ts/{fileId}/{tsName}")
	@GlobalInterceptor
	public void getVideo(@PathVariable("fileId") String fileId,
						 @PathVariable("tsName") String tsName,
						 HttpServletResponse response) {
		fileInfoService.getVideo(fileId, tsName, response);
	}


	@PostMapping("/newFolder")
	@GlobalInterceptor(checkParams = true)
	public ResponseVO newFolder(HttpSession session,
								@VerifyParam(required = true) String fileName,
								@VerifyParam(required = true) String filePid) {
		SessionWebUserVO webUserVO = (SessionWebUserVO) session.getAttribute(Constants.SESSION_WEB_USER);
		FileInfo fileInfo = fileInfoService.newFolder(webUserVO.getUserId(), fileName, filePid);
		FileInfoVO fileInfoVO = new FileInfoVO();
		BeanUtils.copyProperties(fileInfo, fileInfoVO);
		return getSuccessResponseVO(fileInfoVO);
	}


	@PostMapping("/getFolderInfo")
	@GlobalInterceptor
	public ResponseVO getFolderInfo(HttpSession session, String path) {
		SessionWebUserVO webUserVO = (SessionWebUserVO) session.getAttribute(Constants.SESSION_WEB_USER);
		List<FileInfo> folderList = fileInfoService.getFolderInfo(webUserVO.getUserId(), path);
		return getSuccessResponseVO(folderList);
	}

	@PostMapping("/loadAllFolder")
	@GlobalInterceptor(checkParams = true)
	public ResponseVO loadAllFolder(HttpSession session,
									@VerifyParam(required = true) String filePid,
									String currentFileIds) {
		SessionWebUserVO webUserVO = (SessionWebUserVO) session.getAttribute(Constants.SESSION_WEB_USER);
		List<FileInfo> folderList = fileInfoService.loadAllFolder(webUserVO.getUserId(), filePid, currentFileIds);
		return getSuccessResponseVO(folderList);
	}

	@PostMapping("/changeFileFolder")
	@GlobalInterceptor(checkParams = true)
	public ResponseVO changeFileFolder(HttpSession session,
									   @VerifyParam(required = true) String fileIds,
									   @VerifyParam(required = true) String filePid) {
		SessionWebUserVO webUserVO = (SessionWebUserVO) session.getAttribute(Constants.SESSION_WEB_USER);
		fileInfoService.changeFileFolder(webUserVO.getUserId(), fileIds, filePid);
		return getSuccessResponseVO(null);
	}


	@PostMapping("/rename")
	@GlobalInterceptor(checkParams = true)
	public ResponseVO rename(HttpSession session,
							 @VerifyParam(required = true) String fileName,
							 @VerifyParam(required = true) String filePid,
							 @VerifyParam(required = true) String fileId) {
		SessionWebUserVO webUserVO = (SessionWebUserVO) session.getAttribute(Constants.SESSION_WEB_USER);
		FileInfo fileInfo = fileInfoService.rename(webUserVO.getUserId(), fileId, filePid, fileName);
		FileInfoVO fileInfoVO = new FileInfoVO();
		BeanUtils.copyProperties(fileInfo, fileInfoVO);
		return getSuccessResponseVO(fileInfoVO);
	}


	@RequestMapping("/getFile/{fileId}")
	@GlobalInterceptor(checkParams = true)
	public void getFile(HttpSession session,
						@PathVariable("fileId") String fileId,
						HttpServletResponse response) {
		SessionWebUserVO webUserVO = (SessionWebUserVO) session.getAttribute(Constants.SESSION_WEB_USER);
		fileInfoService.getFile(webUserVO.getUserId(), fileId, response);
	}
}