package com.njtech.xcloud.service.impl;

import com.njtech.xcloud.config.CosConfig;
import com.njtech.xcloud.entity.enums.PageSize;
import com.njtech.xcloud.entity.po.FileInfo;
import com.njtech.xcloud.entity.query.FileInfoQuery;
import com.njtech.xcloud.entity.query.SimplePage;
import com.njtech.xcloud.entity.vo.PaginationResultVO;
import com.njtech.xcloud.entity.vo.SessionWebUserVO;
import com.njtech.xcloud.exception.BusinessException;
import com.njtech.xcloud.mappers.FileInfoMapper;
import com.njtech.xcloud.service.FileInfoService;
import com.njtech.xcloud.utils.StringTools;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;


/**
 * 文件信息表 业务接口实现
 */
@Service("fileInfoService")
public class FileInfoServiceImpl implements FileInfoService {

	@Resource
	private FileInfoMapper<FileInfo, FileInfoQuery> fileInfoMapper;

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
	 * 获取COS客户端
	 */
	private COSClient getCosClient() {
		COSCredentials cred = new BasicCOSCredentials(CosConfig.ACCESS_KEY_ID, CosConfig.ACCESS_KEY_SECRET);
		// region 格式只需要是 ap-nanjing 这样的地域标识
		ClientConfig clientConfig = new ClientConfig(new Region(CosConfig.END_POINT));
		return new COSClient(cred, clientConfig);
	}

	/**
	 * 获取文件流并输出到响应
	 * @param userId 用户ID
	 * @param response HTTP响应
	 */
	@Override
	public void getFile(String userId, HttpServletResponse response) {
		COSClient cosClient = null;
		COSObject cosObject = null;
		InputStream inputStream = null;
		ServletOutputStream outputStream = null;

		try {
			cosClient = getCosClient();

			// 尝试获取用户头像文件
			String key = "/file/avatar/" + userId + ".jpg";
			try {
				GetObjectRequest getObjectRequest = new GetObjectRequest(CosConfig.BUCKET_NAME, key);
				cosObject = cosClient.getObject(getObjectRequest);
			} catch (CosServiceException e) {
				// 文件不存在，使用默认头像
				GetObjectRequest getObjectRequest = new GetObjectRequest(CosConfig.BUCKET_NAME, "/file/avatar/default_avatar.jpg");
				cosObject = cosClient.getObject(getObjectRequest);
			}

			// 获取文件流
			inputStream = cosObject.getObjectContent();

			// 设置响应头，禁用缓存确保头像及时更新
			response.setContentType("image/jpeg");
			response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
			response.setHeader("Pragma", "no-cache");
			response.setDateHeader("Expires", 0);

			// 将文件流写入响应输出流
			outputStream = response.getOutputStream();
			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
			}
			outputStream.flush();
		} catch (Exception e) {
			e.printStackTrace();
			throw new BusinessException("获取图片失败: " + e.getMessage());
		} finally {
			// 关闭资源
			try {
				if (inputStream != null) {
					inputStream.close();
				}
				if (outputStream != null) {
					outputStream.close();
				}
				if (cosObject != null) {
					cosObject.close();
				}
				if (cosClient != null) {
					cosClient.shutdown();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void updateUserAvatar(String userId, MultipartFile avatar) {
		COSClient cosClient = null;
		try {
			// 参数校验
			if (avatar == null || avatar.isEmpty()) {
				throw new BusinessException("头像文件不能为空");
			}

			cosClient = getCosClient();
			String key = "/file/avatar/" + userId + ".jpg";

			// 1. 先尝试删除旧头像（如果存在）
			try {
				cosClient.deleteObject(CosConfig.BUCKET_NAME, key);
				logger.info("删除旧头像成功: " + key);
			} catch (CosServiceException e) {
				// 文件不存在，忽略错误
				logger.error("旧头像不存在或删除失败: " + e.getMessage());
			}

			// 2. 上传新头像
			InputStream inputStream = avatar.getInputStream();
			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentType(avatar.getContentType());
			metadata.setContentLength(avatar.getSize());

			PutObjectRequest putObjectRequest = new PutObjectRequest(
					CosConfig.BUCKET_NAME,
					key,
					inputStream,
					metadata
			);
			cosClient.putObject(putObjectRequest);
			logger.info("上传新头像成功: " + key);

		} catch (IOException e) {
			e.printStackTrace();
			throw new BusinessException("更新头像失败: " + e.getMessage());
		} finally {
			if (cosClient != null) {
				cosClient.shutdown();
			}
		}
	}

	@Override
	public void uploadFile(SessionWebUserVO webUserVO, String fileId, MultipartFile file, String fileName, String fileMd5, String filePid, Integer chunkIndex, Integer chunks) {

	}
}