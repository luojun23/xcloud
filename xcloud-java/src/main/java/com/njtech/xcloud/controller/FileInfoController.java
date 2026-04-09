package com.njtech.xcloud.controller;

import java.util.List;
import java.util.Map;

import com.njtech.xcloud.entity.query.FileInfoQuery;
import com.njtech.xcloud.entity.po.FileInfo;
import com.njtech.xcloud.entity.vo.ResponseVO;
import com.njtech.xcloud.service.FileInfoService;
import com.njtech.xcloud.service.impl.MinioServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 文件信息表 Controller
 */
@RestController("fileInfoController")
@RequestMapping("/fileInfo")
public class FileInfoController extends ABaseController{

	@Resource
	private FileInfoService fileInfoService;
	/**
	 * 根据条件分页查询
	 */
	@RequestMapping("/loadDataList")
	public ResponseVO loadDataList(FileInfoQuery query){
		return getSuccessResponseVO(fileInfoService.findListByPage(query));
	}

	@Autowired
	private MinioServiceImpl minioService;

	/**
	 * 上传文件
	 *
	 * @param file 文件
	 * @return 上传结果
	 */
/*	@PostMapping("/upload")
	public Result<Map<String, String>> uploadFile(
			@ApiParam("文件") @RequestParam("file") MultipartFile file) {
		String fileName = minioService.uploadFile(file);
		Map<String, String> result = new HashMap<>();
		result.put("fileName", fileName);
		return Result.success("文件上传成功", result);
	}

	*//**
	 * 获取文件URL
	 *
	 * @param fileName 文件名
	 * @return 文件URL
	 *//*
	@GetMapping("/url")
	public Result<Map<String, String>> getFileUrl(
			@ApiParam("文件名") @RequestParam("fileName") String fileName) {
		String url = minioService.getFileUrl(fileName);
		Map<String, String> result = new HashMap<>();
		result.put("url", url);
		return Result.success("获取文件URL成功", result);
	}

	*//**
	 * 删除文件
	 *
	 * @param fileName 文件名
	 * @return 删除结果
	 *//*
	@ApiOperation("删除文件")
	@DeleteMapping("/delete")
	public Result<Void> deleteFile(
			@ApiParam("文件名") @RequestParam("fileName") String fileName) {
		minioService.deleteFile(fileName);
		return Result.success("文件删除成功", null);
	}*/
}