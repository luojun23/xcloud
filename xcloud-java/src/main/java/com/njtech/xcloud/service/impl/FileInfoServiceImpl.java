package com.njtech.xcloud.service.impl;

import com.njtech.xcloud.config.RedisComponent;
import com.njtech.xcloud.config.RedisUtils;
import com.njtech.xcloud.dto.UploadResultDto;
import com.njtech.xcloud.entity.constants.Constants;
import com.njtech.xcloud.entity.enums.FileFolderTypeEnums;
import com.njtech.xcloud.entity.enums.PageSize;
import com.njtech.xcloud.entity.enums.UploadStatusEnums;
import com.njtech.xcloud.entity.po.FileInfo;
import com.njtech.xcloud.entity.query.FileInfoQuery;
import com.njtech.xcloud.entity.query.SimplePage;
import com.njtech.xcloud.entity.vo.PaginationResultVO;
import com.njtech.xcloud.entity.vo.SessionWebUserVO;
import com.njtech.xcloud.exception.BusinessException;
import com.njtech.xcloud.mappers.FileInfoMapper;
import com.njtech.xcloud.service.FileInfoService;
import com.njtech.xcloud.utils.FileUtils;
import com.njtech.xcloud.utils.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;


/**
 * 文件信息表 业务接口实现
 */
@Service("fileInfoService")
public class FileInfoServiceImpl implements FileInfoService {

	@Resource
	private FileInfoMapper<FileInfo, FileInfoQuery> fileInfoMapper;

	@Resource
	private RedisUtils redisUtils;

	@Resource
	private RedisComponent redisComponent;

	@Resource
	private ApplicationContext applicationContext;

	public static final Logger logger = LoggerFactory.getLogger(FileInfoServiceImpl.class);
	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<FileInfo> findListByParam(FileInfoQuery param) {
		return this.fileInfoMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(FileInfoQuery param) {
		return this.fileInfoMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<FileInfo> findListByPage(FileInfoQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<FileInfo> list = this.findListByParam(param);
		PaginationResultVO<FileInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(FileInfo bean) {
		return this.fileInfoMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<FileInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.fileInfoMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<FileInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.fileInfoMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(FileInfo bean, FileInfoQuery param) {
		StringTools.checkParam(param);
		return this.fileInfoMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(FileInfoQuery param) {
		StringTools.checkParam(param);
		return this.fileInfoMapper.deleteByParam(param);
	}

	/**
	 * 根据FileIdAndUserId获取对象
	 */
	@Override
	public FileInfo getFileInfoByFileIdAndUserId(String fileId, String userId) {
		return this.fileInfoMapper.selectByFileIdAndUserId(fileId, userId);
	}

	/**
	 * 根据FileIdAndUserId修改
	 */
	@Override
	public Integer updateFileInfoByFileIdAndUserId(FileInfo bean, String fileId, String userId) {
		return this.fileInfoMapper.updateByFileIdAndUserId(bean, fileId, userId);
	}

	/**
	 * 根据FileIdAndUserId删除
	 */
	@Override
	public Integer deleteFileInfoByFileIdAndUserId(String fileId, String userId) {
		return this.fileInfoMapper.deleteByFileIdAndUserId(fileId, userId);
	}

	/**
	 * 获取文件流并输出到响应
	 * @param userId 用户ID
	 * @param response HTTP响应
	 */
	@Override
	public void getFile(String userId, HttpServletResponse response) {
		String avatarPath = FileUtils.getFullPath(Constants.FILE_FOLDER_AVATAR + userId + ".jpg");
		File avatarFile = new File(avatarPath);
		if (!avatarFile.exists()) {
			avatarPath = FileUtils.getFullPath(Constants.FILE_FOLDER_AVATAR + "default_avatar.jpg");
			avatarFile = new File(avatarPath);
		}

		try (FileInputStream inputStream = new FileInputStream(avatarFile);
			 ServletOutputStream outputStream = response.getOutputStream()) {
			response.setContentType("image/jpeg");
			response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
			response.setHeader("Pragma", "no-cache");
			response.setDateHeader("Expires", 0);

			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
			}
			outputStream.flush();
		} catch (Exception e) {
			e.printStackTrace();
			throw new BusinessException("获取图片失败: " + e.getMessage());
		}
	}

	@Override
	public void updateUserAvatar(String userId, MultipartFile avatar) {
		try {
			// 参数校验
			if (avatar == null || avatar.isEmpty()) {
				throw new BusinessException("头像文件不能为空");
			}

			String avatarPath = FileUtils.getFullPath(Constants.FILE_FOLDER_AVATAR + userId + ".jpg");
			File avatarFile = new File(avatarPath);
			File parentDir = avatarFile.getParentFile();
			if (parentDir != null && !parentDir.exists()) {
				parentDir.mkdirs();
			}

			avatar.transferTo(avatarFile);
			logger.info("上传头像成功: " + avatarPath);

		} catch (IOException e) {
			e.printStackTrace();
			throw new BusinessException("更新头像失败: " + e.getMessage());
		}
	}

	@Override
	public UploadResultDto uploadFile(SessionWebUserVO webUserVO, String fileId, MultipartFile file,
												  String fileName, String fileMd5, String filePid,
												  Integer chunkIndex, Integer chunks) {
		UploadResultDto result = new UploadResultDto();
		String userId = webUserVO.getUserId();
		Date curDate = new Date();

		// === 第一块分片：秒传检查 ===
		if (chunkIndex == 0) {
			FileInfoQuery query = new FileInfoQuery();
			query.setFileMd5(fileMd5);
			query.setDelFlag(Constants.USING);
			List<FileInfo> dbFileList = this.fileInfoMapper.selectList(query);

			if (!dbFileList.isEmpty()) {
				// 秒传：复制已有记录
				FileInfo dbFile = dbFileList.get(0);
				String newFileId = StringTools.getRandomString(Constants.TEN);

				FileInfo newFile = new FileInfo();
				BeanUtils.copyProperties(dbFile, newFile);
				newFile.setFileId(newFileId);
				newFile.setUserId(userId);
				newFile.setFilePid(filePid);
				newFile.setFileName(autoRename(userId, filePid, fileName));
				newFile.setCreateTime(curDate);
				newFile.setLastUpdateTime(curDate);
				newFile.setStatus(Constants.TRANSFER_SUCCESS);
				newFile.setDelFlag(Constants.USING);
				this.fileInfoMapper.insert(newFile);

				// 更新用户使用空间
				redisComponent.updateUserSpace(userId, newFile.getFileSize());

				result.setFileId(newFileId);
				result.setStatus(UploadStatusEnums.UPLOAD_SECONDS.getCode());
				return result;
			}
		}

		// === 非秒传：分片上传逻辑 ===
		// 生成或确认 fileId
		if (StringTools.isEmpty(fileId)) {
			fileId = StringTools.getRandomString(Constants.TEN);
		}

		Long chunkSize = file.getSize();

		// 检查用户磁盘空间
		redisComponent.checkUserSpace(userId, chunkSize, fileId);

		// 上传分片到本地临时目录
		String tempKey = Constants.FILE_FOLDER_TEMP + userId + fileId + "/" + chunkIndex;
		FileUtils.uploadChunkToLocal(file, tempKey);

		// 更新 Redis 临时文件大小
		redisComponent.updateTempSize(userId, fileId, chunkSize);

		// 不是最后一块，直接返回
		if (chunkIndex < chunks - 1) {
			result.setFileId(fileId);
			result.setStatus(UploadStatusEnums.UPLOADING.getCode());
			return result;
		}

		// === 最后一块分片 ===
		Long totalSize = redisComponent.getTempSize(userId, fileId);

		Integer fileType = getFileType(fileName);
		Integer fileCategory = getFileCategory(fileName);

		// 自动重命名：检查同目录下是否存在同名文件
		fileName = autoRename(userId, filePid, fileName);

		// 保存文件信息到数据库
		FileInfo fileInfo = new FileInfo();
		fileInfo.setFileId(fileId);
		fileInfo.setUserId(userId);
		fileInfo.setFileMd5(fileMd5);
		fileInfo.setFilePid(filePid);
		fileInfo.setFileName(fileName);
		fileInfo.setFileSize(totalSize);
		fileInfo.setCreateTime(curDate);
		fileInfo.setLastUpdateTime(curDate);
		fileInfo.setFolderType(FileFolderTypeEnums.FILE.getType()); // 文件
		fileInfo.setFileCategory(fileCategory);
		fileInfo.setFileType(fileType);
		fileInfo.setStatus(Constants.TRANSFER_ING); // 转码中（等待合并）
		fileInfo.setDelFlag(Constants.USING);

		this.fileInfoMapper.insert(fileInfo);

		// 更新用户使用空间
		redisComponent.updateUserSpace(userId, totalSize);

		// 清除 Redis 临时大小
		redisUtils.del(Constants.REDIS_KEY_TEMP_SIZE + userId + ":" + fileId);

		// 异步合并分片（使用 CompletableFuture 确保真正异步执行，避免 Spring 代理失效导致同步阻塞）
		asyncMergeChunks(userId, fileId, fileName, chunks);

		result.setFileId(fileId);
		result.setStatus(UploadStatusEnums.UPLOAD_FINISH.getCode());
		return result;
	}

	/**
	 * 异步合并分片（包装方法，解决 Lambda 变量捕获问题）
	 */
	private void asyncMergeChunks(String userId, String fileId, String fileName, Integer chunks) {
		CompletableFuture.runAsync(() -> mergeChunks(userId, fileId, fileName, chunks));
	}

	/**
	 * 异步合并分片
	 */
	@Async
	@Override
	public void mergeChunks(String userId, String fileId, String fileName, Integer chunks) {
		try {
			String tempRelativeDir = Constants.FILE_FOLDER_TEMP + userId + fileId;

			// 合并后的文件路径
			String datePath = new SimpleDateFormat("yyyyMM").format(new Date());
			String fileSuffix = StringTools.getFileSuffix(fileName);
			String formalRelativePath = Constants.FILE_FOLDER_FILE + datePath + "/" + fileId
					+ (fileSuffix.isEmpty() ? "" : "." + fileSuffix);

			// 合并分片到正式文件
			FileUtils.mergeLocalChunks(tempRelativeDir, chunks, formalRelativePath);

			// 更新数据库 filePath 和状态
			FileInfo updateInfo = new FileInfo();
			updateInfo.setFilePath(formalRelativePath);
			updateInfo.setStatus(Constants.TRANSFER_SUCCESS);
			this.fileInfoMapper.updateByFileIdAndUserId(updateInfo, fileId, userId);

			// 删除本地临时分片
			FileUtils.deleteLocalChunks(tempRelativeDir, chunks);

			logger.info("文件合并完成: fileId={}, formalPath={}", fileId, formalRelativePath);

		} catch (Exception e) {
			logger.error("合并分片失败: fileId=" + fileId, e);
			// 更新状态为转码失败
			FileInfo updateInfo = new FileInfo();
			updateInfo.setStatus(Constants.TRANSFER_FAIL);
			this.fileInfoMapper.updateByFileIdAndUserId(updateInfo, fileId, userId);
			throw new BusinessException("文件转码失败");
		}
	}



	/**
	 * 根据文件名获取文件类型
	 * 1:视频 2:音频 3:图片 4:pdf 5:doc 6:excel 7:txt 8:code 9:zip 10:其他
	 */
	private Integer getFileType(String fileName) {
		String suffix = StringTools.getFileSuffix(fileName).toLowerCase();
		switch (suffix) {
			case "mp4": case "avi": case "rmvb": case "mkv": case "mov":
				return 1;
			case "mp3": case "wav": case "wma": case "mp2": case "flac":
			case "midi": case "ra": case "ape": case "aac": case "cda":
				return 2;
			case "jpeg": case "jpg": case "png": case "gif": case "bmp":
			case "dds": case "psd": case "pdt": case "webp": case "xmp":
			case "svg": case "tiff":
				return 3;
			case "pdf":
				return 4;
			case "doc": case "docx":
				return 5;
			case "xls": case "xlsx":
				return 6;
			case "txt":
				return 7;
			case "java": case "py": case "cpp": case "c": case "js":
			case "html": case "css": case "xml": case "json": case "sql":
				return 8;
			case "zip": case "rar": case "7z": case "tar": case "gz":
				return 9;
			default:
				return 10;
		}
	}

	/**
	 * 根据文件名获取文件分类
	 * 1:视频 2:音频 3:图片 4:文档 5:其他
	 */
	private Integer getFileCategory(String fileName) {
		String suffix = StringTools.getFileSuffix(fileName).toLowerCase();
		switch (suffix) {
			case "mp4": case "avi": case "rmvb": case "mkv": case "mov":
				return 1;
			case "mp3": case "wav": case "wma": case "mp2": case "flac":
			case "midi": case "ra": case "ape": case "aac": case "cda":
				return 2;
			case "jpeg": case "jpg": case "png": case "gif": case "bmp":
			case "dds": case "psd": case "pdt": case "webp": case "xmp":
			case "svg": case "tiff":
				return 3;
			case "pdf": case "doc": case "docx": case "xls": case "xlsx": case "txt":
				return 4;
			default:
				return 5;
		}
	}

	/**
	 * 自动重命名：检查同目录下是否存在同名文件，存在则自动添加序号
	 *
	 * @param userId   用户ID
	 * @param filePid  父目录ID
	 * @param fileName 原始文件名
	 * @return 可用的文件名
	 */
	private String autoRename(String userId, String filePid, String fileName) {
		FileInfoQuery query = new FileInfoQuery();
		query.setUserId(userId);
		query.setFilePid(filePid);
		query.setDelFlag(Constants.USING);
		query.setFolderType(FileFolderTypeEnums.FILE.getType());

		String fileSuffix = StringTools.getFileSuffix(fileName);
		String nameWithoutSuffix = fileName;
		if (!fileSuffix.isEmpty()) {
			nameWithoutSuffix = fileName.substring(0, fileName.lastIndexOf("."));
		}

		String finalFileName = fileName;
		query.setFileName(finalFileName);
		Integer count = this.fileInfoMapper.selectCount(query);
		if (count == 0) {
			return fileName;
		}

		int index = 1;
		do {
			if (fileSuffix.isEmpty()) {
				finalFileName = nameWithoutSuffix + " (" + index + ")";
			} else {
				finalFileName = nameWithoutSuffix + " (" + index + ")." + fileSuffix;
			}
			query.setFileName(finalFileName);
			count = this.fileInfoMapper.selectCount(query);
			index++;
		} while (count > 0);

		return finalFileName;
	}
}