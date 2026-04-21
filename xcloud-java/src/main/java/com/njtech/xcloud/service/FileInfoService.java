	package com.njtech.xcloud.service;

import java.util.List;

import com.njtech.xcloud.entity.query.FileInfoQuery;
import com.njtech.xcloud.entity.po.FileInfo;
import com.njtech.xcloud.entity.vo.PaginationResultVO;

import javax.servlet.http.HttpServletResponse;

import org.springframework.web.multipart.MultipartFile;


/**
 * 文件信息表 业务接口
 */
public interface FileInfoService {

	/**
	 * 根据条件查询列表
	 */
	List<FileInfo> findListByParam(FileInfoQuery param);

	/**
	 * 根据条件查询列表
	 */
	Integer findCountByParam(FileInfoQuery param);

	/**
	 * 分页查询
	 */
	PaginationResultVO<FileInfo> findListByPage(FileInfoQuery param);

	/**
	 * 新增
	 */
	Integer add(FileInfo bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<FileInfo> listBean);

	/**
	 * 批量新增/修改
	 */
	Integer addOrUpdateBatch(List<FileInfo> listBean);

	/**
	 * 多条件更新
	 */
	Integer updateByParam(FileInfo bean,FileInfoQuery param);

	/**
	 * 多条件删除
	 */
	Integer deleteByParam(FileInfoQuery param);

	/**
	 * 根据FileIdAndUserId查询对象
	 */
	FileInfo getFileInfoByFileIdAndUserId(String fileId,String userId);


	/**
	 * 根据FileIdAndUserId修改
	 */
	Integer updateFileInfoByFileIdAndUserId(FileInfo bean,String fileId,String userId);


	/**
	 * 根据FileIdAndUserId删除
	 */
	Integer deleteFileInfoByFileIdAndUserId(String fileId,String userId);

	/**
	 * 获取文件流并输出到响应
	 * @param userId 文件名
	 * @param response HTTP响应
	 */
	void getFile(String userId, HttpServletResponse response);

	/**
	 * 更新用户头像
	 * @param userId 用户ID
	 * @param avatar 头像文件
	 */
	void updateUserAvatar(String userId, MultipartFile avatar);

}