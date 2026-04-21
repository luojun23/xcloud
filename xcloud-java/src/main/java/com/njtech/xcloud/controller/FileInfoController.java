package com.njtech.xcloud.controller;

import com.njtech.xcloud.entity.query.FileInfoQuery;
import com.njtech.xcloud.entity.vo.ResponseVO;
import com.njtech.xcloud.service.FileInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
}