package com.njtech.xcloud.utils;

import com.njtech.xcloud.config.CosConfig;
import com.njtech.xcloud.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * @ClassName : COSUtils
 * @Description : 本地文件存储工具（原COS工具，现统一改为本地存储）
 * @Author : 罗君
 * @Date: 2026/4/23
 */
public class FileUtils {

    /**
     * 上传分片到本地
     */
    public static void uploadChunkToLocal(MultipartFile file, String relativePath) {
        String fullPath = getFullPath(relativePath);
        File targetFile = new File(fullPath);
        File parentDir = targetFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        try {
            file.transferTo(targetFile);
        } catch (IOException e) {
            throw new BusinessException("保存分片失败: " + e.getMessage());
        }
    }

    /**
     * 将本地临时分片合并到目标文件
     */
    public static void mergeLocalChunks(String tempRelativeDir, int chunks, String targetRelativePath) {
        String targetFullPath = getFullPath(targetRelativePath);
        File targetFile = new File(targetFullPath);
        File parentDir = targetFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(targetFile)) {
            for (int i = 0; i < chunks; i++) {
                String chunkFullPath = getFullPath(tempRelativeDir + "/" + i);
                File chunkFile = new File(chunkFullPath);
                if (!chunkFile.exists()) {
                    throw new BusinessException("分片文件不存在: " + chunkFullPath);
                }
                java.nio.file.Files.copy(chunkFile.toPath(), fos);
            }
        } catch (IOException e) {
            throw new BusinessException("合并分片失败: " + e.getMessage());
        }
    }

    /**
     * 删除本地临时分片
     */
    public static void deleteLocalChunks(String tempRelativeDir, int chunks) {
        for (int i = 0; i < chunks; i++) {
            String chunkFullPath = getFullPath(tempRelativeDir + "/" + i);
            File chunkFile = new File(chunkFullPath);
            if (chunkFile.exists()) {
                chunkFile.delete();
            }
        }
        // 尝试删除空目录
        File dir = new File(getFullPath(tempRelativeDir));
        if (dir.exists()) {
            dir.delete();
        }
    }

    /**
     * 获取本地文件完整路径
     */
    public static String getFullPath(String relativePath) {
        String basePath = CosConfig.PROJECT_FOLDER;
        if (basePath.endsWith("/") || basePath.endsWith("\\")) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }
        if (relativePath.startsWith("/") || relativePath.startsWith("\\")) {
            relativePath = relativePath.substring(1);
        }
        return basePath + "/" + relativePath;
    }
}
