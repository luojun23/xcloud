package com.njtech.xcloud.dto;

import lombok.Data;

/**
 * @ClassName : DownloadFileDto
 * @Description :
 * @Author : 罗君
 * @Date: 2026/5/1
 */
@Data
public class DownloadFileDto {
    private String downloadCode;
    private String fileName;
    private String filePath;
}
