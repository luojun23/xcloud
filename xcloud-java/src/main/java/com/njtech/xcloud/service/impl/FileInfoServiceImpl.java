package com.njtech.xcloud.service.impl;

import com.njtech.xcloud.config.RedisComponent;
import com.njtech.xcloud.config.RedisUtils;
import com.njtech.xcloud.dto.UploadResultDto;
import com.njtech.xcloud.entity.constants.Constants;
import com.njtech.xcloud.entity.enums.*;
import com.njtech.xcloud.entity.po.FileInfo;
import com.njtech.xcloud.entity.query.FileInfoQuery;
import com.njtech.xcloud.entity.query.SimplePage;
import com.njtech.xcloud.entity.vo.PaginationResultVO;
import com.njtech.xcloud.entity.vo.SessionWebUserVO;
import com.njtech.xcloud.exception.BusinessException;
import com.njtech.xcloud.mappers.FileInfoMapper;
import com.njtech.xcloud.service.FileInfoService;
import com.njtech.xcloud.utils.FileUtils;
import com.njtech.xcloud.utils.ProcessUtils;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


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
     *
     * @param userId   用户ID
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

    /**
     * 获取缩略图/图片并以流形式输出到响应
     *
     * @param cover    图片相对路径（如 /file/202604/xxx_.jpg）
     * @param response HTTP响应
     */
    @Override
    public void getImage(String cover, HttpServletResponse response) {
        if (cover == null || cover.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String fullPath = FileUtils.getFullPath(cover);
        File imageFile = new File(fullPath);

        if (!imageFile.exists()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 根据后缀判断 Content-Type
        String contentType = "image/jpeg";
        String lowerName = imageFile.getName().toLowerCase();
        if (lowerName.endsWith(".png")) {
            contentType = "image/png";
        } else if (lowerName.endsWith(".gif")) {
            contentType = "image/gif";
        } else if (lowerName.endsWith(".webp")) {
            contentType = "image/webp";
        }

        response.setContentType(contentType);
        response.setHeader("Cache-Control", "max-age=86400");
        response.setHeader("Pragma", "cache");

        try (FileInputStream inputStream = new FileInputStream(imageFile);
             ServletOutputStream outputStream = response.getOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        } catch (Exception e) {
            logger.error("获取图片失败: cover={}", cover, e);
        }
    }

    /**
     * 新建文件夹
     */
    @Override
    public FileInfo newFolder(String userId, String fileName, String filePid) {
        Date curDate = new Date();

        // 自动重命名（同目录下不能有同名文件夹）
        fileName = autoRename(userId, filePid, fileName);

        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileId(StringTools.getRandomString(Constants.TEN));
        fileInfo.setUserId(userId);
        fileInfo.setFilePid(filePid);
        fileInfo.setFileName(fileName);
        fileInfo.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        fileInfo.setFileCategory(0);
        fileInfo.setFileType(0);
        fileInfo.setStatus(Constants.TRANSFER_SUCCESS);
        fileInfo.setDelFlag(Constants.USING);
        fileInfo.setCreateTime(curDate);
        fileInfo.setLastUpdateTime(curDate);

        this.fileInfoMapper.insert(fileInfo);
        return fileInfo;
    }

    /**
     * 获取目录导航信息
     */
    @Override
    public List<FileInfo> getFolderInfo(String userId, String path) {
        List<FileInfo> folderList = new ArrayList<>();
        if (StringTools.isEmpty(path)) {
            return folderList;
        }
        String[] folderIds = path.split("/");
        for (String folderId : folderIds) {
            if (StringTools.isEmpty(folderId)) {
                continue;
            }
            FileInfo folder = this.fileInfoMapper.selectByFileIdAndUserId(folderId, userId);
            if (folder != null && folder.getFolderType() != null
                    && folder.getFolderType().equals(FileFolderTypeEnums.FOLDER.getType())) {
                folderList.add(folder);
            }
        }
        return folderList;
    }

    /**
     * 获取文件夹列表（移动文件时选择目标目录）
     */
    @Override
    public List<FileInfo> loadAllFolder(String userId, String filePid, String currentFileIds) {
        FileInfoQuery query = new FileInfoQuery();
        query.setFilePid(filePid);
        query.setUserId(userId);
        query.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        query.setDelFlag(Constants.USING);
        query.setOrderBy("create_time desc");
        List<FileInfo> folderList = this.fileInfoMapper.selectList(query);

        // 排除正在移动的文件/文件夹本身
        if (!StringTools.isEmpty(currentFileIds)) {
            Set<String> excludeIds = new HashSet<>(Arrays.asList(currentFileIds.split(",")));
            folderList = folderList.stream()
                    .filter(f -> !excludeIds.contains(f.getFileId()))
                    .collect(Collectors.toList());
        }

        return folderList;
    }

    /**
     * 移动文件/文件夹到目标目录
     */
    @Override
    public void changeFileFolder(String userId, String fileIds, String filePid) {
        if (StringTools.isEmpty(fileIds) || StringTools.isEmpty(filePid)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        // 校验目标文件夹是否存在（filePid 为 "0" 表示根目录，不需要校验）
        if (!"0".equals(filePid)) {
            FileInfo targetFolder = this.fileInfoMapper.selectByFileIdAndUserId(filePid, userId);
            if (targetFolder == null || !FileFolderTypeEnums.FOLDER.getType().equals(targetFolder.getFolderType())) {
                throw new BusinessException("目标文件夹不存在");
            }
        }

        String[] fileIdArray = fileIds.split(",");
        for (String fileId : fileIdArray) {
            if (StringTools.isEmpty(fileId)) {
                continue;
            }

            FileInfo fileInfo = this.fileInfoMapper.selectByFileIdAndUserId(fileId, userId);
            if (fileInfo == null) {
                continue;
            }

            // 检查目标目录下是否有同名文件/文件夹
            FileInfoQuery query = new FileInfoQuery();
            query.setFilePid(filePid);
            query.setFileName(fileInfo.getFileName());
            query.setUserId(userId);
            query.setDelFlag(Constants.USING);
            List<FileInfo> list = this.fileInfoMapper.selectList(query);
            if (list != null && !list.isEmpty()) {
                boolean hasOther = list.stream().anyMatch(f -> !f.getFileId().equals(fileId));
                if (hasOther) {
                    throw new BusinessException("目标目录下已存在同名文件: " + fileInfo.getFileName());
                }
            }

            FileInfo updateInfo = new FileInfo();
            updateInfo.setFilePid(filePid);
            updateInfo.setLastUpdateTime(new Date());
            this.fileInfoMapper.updateByFileIdAndUserId(updateInfo, fileId, userId);
        }
    }

    /**
     * 重命名文件/文件夹
     */
    @Override
    public FileInfo rename(String userId, String fileId, String filePid, String fileName) {
        FileInfo fileInfo = this.fileInfoMapper.selectByFileIdAndUserId(fileId, userId);
        if (fileInfo == null) {
            throw new BusinessException("文件不存在");
        }

        // 同目录下检查同名（排除自己）
        FileInfoQuery query = new FileInfoQuery();
        query.setFilePid(filePid);
        query.setFileName(fileName);
        query.setUserId(userId);
        query.setDelFlag(Constants.USING);
        List<FileInfo> list = this.fileInfoMapper.selectList(query);
        if (list != null && !list.isEmpty()) {
            boolean hasOther = list.stream().anyMatch(f -> !f.getFileId().equals(fileId));
            if (hasOther) {
                throw new BusinessException("文件名已存在");
            }
        }

        FileInfo updateInfo = new FileInfo();
        updateInfo.setFileName(fileName);
        updateInfo.setLastUpdateTime(new Date());
        this.fileInfoMapper.updateByFileIdAndUserId(updateInfo, fileId, userId);

        return this.fileInfoMapper.selectByFileIdAndUserId(fileId, userId);
    }

    /**
     * 获取文件流（预览/下载），支持 Range 分段请求
     */
    @Override
    public void getFile(String userId, String fileId, HttpServletResponse response) {
        FileInfo fileInfo = this.fileInfoMapper.selectByFileIdAndUserId(fileId, userId);
        if (fileInfo == null) {
            throw new BusinessException("文件不存在");
        }
        String filePath = FileUtils.getFullPath(fileInfo.getFilePath());
        File file = new File(filePath);
        if (!file.exists()) {
            throw new BusinessException("文件不存在");
        }

        // 根据后缀设置 Content-Type
        String suffix = StringTools.getFileSuffix(fileInfo.getFileName());
        if (suffix == null) {
            suffix = "";
        }
        String contentType = getContentType(suffix);
        response.setContentType(contentType);
        response.setHeader("Accept-Ranges", "bytes");

        long fileLength = file.length();
        long start = 0;
        long end = fileLength - 1;

        // 解析 Range 请求头
        String rangeHeader = null;
        try {
            rangeHeader = org.springframework.web.context.request.RequestContextHolder
                    .getRequestAttributes() != null
                    ? ((org.springframework.web.context.request.ServletRequestAttributes)
                    org.springframework.web.context.request.RequestContextHolder.getRequestAttributes())
                    .getRequest().getHeader("Range")
                    : null;
        } catch (Exception ignored) {
        }

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String rangeValue = rangeHeader.substring(6);
            int dashIndex = rangeValue.indexOf('-');
            try {
                if (dashIndex > 0) {
                    start = Long.parseLong(rangeValue.substring(0, dashIndex));
                    if (dashIndex < rangeValue.length() - 1) {
                        end = Long.parseLong(rangeValue.substring(dashIndex + 1));
                    }
                } else if (dashIndex == 0) {
                    // bytes=-500 最后 500 字节
                    long suffixLength = Long.parseLong(rangeValue.substring(1));
                    start = fileLength - suffixLength;
                }
                if (end >= fileLength) {
                    end = fileLength - 1;
                }
                long contentLength = end - start + 1;
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
                response.setHeader("Content-Length", String.valueOf(contentLength));
            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                response.setHeader("Content-Range", "bytes */" + fileLength);
                return;
            }
        } else {
            response.setHeader("Content-Length", String.valueOf(fileLength));
        }

        try (RandomAccessFile raf = new RandomAccessFile(file, "r");
             OutputStream out = response.getOutputStream()) {
            raf.seek(start);
            byte[] buffer = new byte[4096];
            long remaining = end - start + 1;
            int len;
            while (remaining > 0 && (len = raf.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                out.write(buffer, 0, len);
                remaining -= len;
            }
            out.flush();
        } catch (Exception e) {
            logger.error("读取文件失败: fileId={}, path={}", fileId, filePath, e);
        }
    }

    private String getContentType(String suffix) {
        switch (suffix.toLowerCase()) {
            case "mp3": return "audio/mpeg";
            case "mp4": return "video/mp4";
            case "pdf": return "application/pdf";
            case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "pptx": return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "txt": return "text/plain";
            case "png": return "image/png";
            case "jpg": case "jpeg": return "image/jpeg";
            case "gif": return "image/gif";
            default: return "application/octet-stream";
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

        String fileSuffix = StringTools.getFileSuffix(fileName);
        FileTypeEnums fileTypeEnums = FileTypeEnums.getFileTypeBySuffix("." + fileSuffix);
        Integer fileType = fileTypeEnums.getType();
        Integer fileCategory = fileTypeEnums.getCategory().getCategory();

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
        boolean transferFlag = true;
        String coverPath = null;
        try {
            String tempRelativeDir = Constants.FILE_FOLDER_TEMP + userId + fileId;

            // 合并后的文件路径
            String datePath = new SimpleDateFormat("yyyyMM").format(new Date());
            String fileSuffix = StringTools.getFileSuffix(fileName);
            String formalRelativePath = Constants.FILE_FOLDER_FILE + datePath + "/" + userId + fileId
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

            // 视频文件：同步切割 + 生成缩略图
            FileTypeEnums fileTypeEnums = FileTypeEnums.getFileTypeBySuffix("." + fileSuffix);
            if (fileTypeEnums.getCategory().getCategory() == 1) {
                // 视频：切片 + 缩略图
                String videoPathAbsolute = FileUtils.getFullPath(formalRelativePath);
                String outPath = Constants.FILE_FOLDER_FILE + datePath + "/" + userId + fileId;
                videoCut(outPath, videoPathAbsolute, fileId);

                coverPath = outPath + ".png";
                String coverPathAbsolute = FileUtils.getFullPath(coverPath);
                generateThumbnail(videoPathAbsolute, coverPathAbsolute, Constants.THUMB_WIDTH, Constants.THUMB_HEIGHT, true);
            } else if (fileTypeEnums.getCategory().getCategory() == 3) {
                // 图片：缩略图（将 . 替换为 _. 与原图区分）
                String thumbRelativePath = formalRelativePath.replace(".", "_.");
                coverPath = thumbRelativePath;
                String coverPathAbsolute = FileUtils.getFullPath(thumbRelativePath);
                String imagePathAbsolute = FileUtils.getFullPath(formalRelativePath);
                generateThumbnail(imagePathAbsolute, coverPathAbsolute, Constants.THUMB_WIDTH, Constants.THUMB_HEIGHT, false);
            }

            if (coverPath != null) {
                updateInfo.setFileCover(coverPath);
                this.fileInfoMapper.updateByFileIdAndUserId(updateInfo, fileId, userId);
            }
        } catch (Exception e) {
            logger.error("合并分片失败: fileId=" + fileId, e);
            transferFlag = false;
            throw new BusinessException("文件转码失败");
        } finally {
            // 仅在失败时更新状态，成功时已在前面更新过（避免覆盖 filePath）
            if (!transferFlag) {
                FileInfo updateInfo = new FileInfo();
                updateInfo.setStatus(Constants.TRANSFER_FAIL);
                this.fileInfoMapper.updateByFileIdAndUserId(updateInfo, fileId, userId);
            }
        }
    }


    /**
     * 视频文件切割：调用 ffmpeg 生成 m3u8 + ts 切片
     *
     * @param outPath   切片输出目录（相对路径）
     * @param videoPath 视频文件完整路径
     * @param fileId    文件ID
     */
    private void videoCut(String outPath, String videoPath, String fileId) {
        String fullPath = FileUtils.getFullPath(outPath);
        File tsFolder = new File(fullPath);
        if (!tsFolder.exists()) {
            tsFolder.mkdirs();
        }

        final String CMD_TRANSFER_2TS = "ffmpeg -y -i \"%s\" -c copy -bsf:v h264_mp4toannexb \"%s\"";
        final String CMD_TRANSFER_2TS_FALLBACK = "ffmpeg -y -i \"%s\" -c:v libx264 -preset ultrafast -crf 23 -c:a aac -b:a 128k \"%s\"";

        String tsPath = fullPath + "/" + Constants.TS_NAME;

        // 生成 .ts（先尝试 copy，失败则降级为转码）
        String cmd = String.format(CMD_TRANSFER_2TS, videoPath, tsPath);
        try {
            ProcessUtils.executeCommand(cmd, true);
        } catch (BusinessException e) {
            logger.warn("视频copy失败，尝试转码, fileId={}, error={}", fileId, e.getMessage());
            cmd = String.format(CMD_TRANSFER_2TS_FALLBACK, videoPath, tsPath);
            ProcessUtils.executeCommand(cmd, true);
        }

        // 生成标准 HLS m3u8 和切片 .ts（hls.js 需要标准 HLS 格式）
        String m3u8Path = fullPath + "/" + Constants.M3U8_NAME;
        String tsPattern = fullPath + "/" + fileId + "_%04d.ts";
        cmd = String.format(
                "ffmpeg -y -i \"%s\" -c copy -map 0 -f hls -hls_time 10 -hls_list_size 0 -hls_segment_filename \"%s\" \"%s\"",
                tsPath, tsPattern, m3u8Path);
        ProcessUtils.executeCommand(cmd, true);

        // 删除 index.ts
        new File(tsPath).delete();

        logger.info("视频切片完成, fileId={}", fileId);
    }

    /**
     * 生成缩略图（支持视频和图片）
     *
     * @param sourcePath        源文件绝对路径
     * @param coverPathAbsolute 缩略图输出绝对路径
     * @param width             缩略图宽度（null 则不缩放）
     * @param height            缩略图高度（null 则不缩放）
     * @param isVideo           true=视频（取第1秒帧），false=图片
     */
    private void generateThumbnail(String sourcePath, String coverPathAbsolute, Integer width, Integer height, boolean isVideo) {
        try {
            String cmd;
            if (isVideo) {
                // 视频：取第1秒帧，按指定宽高比缩放
                cmd = String.format("ffmpeg -i \"%s\" -y -vframes 1 -vf scale=%d:%d/a \"%s\"", sourcePath, width, height, coverPathAbsolute);
            } else {
                // 图片：指定宽度，高度按比例自动计算
                cmd = String.format("ffmpeg -i \"%s\" -vf scale=%d:-1 \"%s\" -y", sourcePath, width, coverPathAbsolute);
            }

            ProcessUtils.executeCommand(cmd, 60);
            logger.info("缩略图生成完成: isVideo={}, path={}", isVideo, coverPathAbsolute);
        } catch (Exception e) {
            logger.error("缩略图生成失败: isVideo={}", isVideo, e);
        }
    }

    /**
     * 获取视频 HLS m3u8 索引文件（修改 ts 路径为 API 路径后返回）
     */
    @Override
    public void getVideoInfo(String fileId, HttpServletResponse response) {
        FileInfo fileInfo = this.fileInfoMapper.selectByFileId(fileId);
        if (fileInfo == null || fileInfo.getFilePath() == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // m3u8 文件路径：与视频文件同目录下的 index.m3u8
        String videoPath = fileInfo.getFilePath();
        String m3u8RelativePath = videoPath.substring(0, videoPath.lastIndexOf(".")) + "/" + Constants.M3U8_NAME;
        String m3u8AbsolutePath = FileUtils.getFullPath(m3u8RelativePath);
        File m3u8File = new File(m3u8AbsolutePath);

        if (!m3u8File.exists()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            String content = new String(java.nio.file.Files.readAllBytes(m3u8File.toPath()), java.nio.charset.StandardCharsets.UTF_8);
            // 将 m3u8 中的 ts 相对路径替换为 /api/file/ts/{fileId}/xxx.ts
            String apiPrefix = "/api/file/ts/" + fileId + "/";
            String[] lines = content.split("\\r?\\n");
            StringBuilder sb = new StringBuilder();
            for (String line : lines) {
                String trimLine = line.trim();
                if (trimLine.isEmpty() || trimLine.startsWith("#")) {
                    sb.append(line).append("\n");
                } else {
                    // ts 文件名（只取文件名部分）
                    String tsName = new File(trimLine).getName();
                    sb.append(apiPrefix).append(tsName).append("\n");
                }
            }

            response.setContentType("application/vnd.apple.mpegurl");
            response.setHeader("Cache-Control", "max-age=86400");
            response.getWriter().write(sb.toString());
        } catch (Exception e) {
            logger.error("读取 m3u8 失败: fileId={}", fileId, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 获取视频 HLS ts 切片文件
     */
    @Override
    public void getVideo(String fileId, String tsName, HttpServletResponse response) {
        FileInfo fileInfo = this.fileInfoMapper.selectByFileId(fileId);
        if (fileInfo == null || fileInfo.getFilePath() == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String videoPath = fileInfo.getFilePath();
        String tsDir = videoPath.substring(0, videoPath.lastIndexOf("."));
        String tsRelativePath = tsDir + "/" + tsName;
        String tsAbsolutePath = FileUtils.getFullPath(tsRelativePath);
        File tsFile = new File(tsAbsolutePath);

        if (!tsFile.exists()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType("video/mp2t");
        response.setHeader("Cache-Control", "max-age=86400");

        try (FileInputStream inputStream = new FileInputStream(tsFile);
             ServletOutputStream outputStream = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        } catch (Exception e) {
            logger.error("读取 ts 切片失败: fileId={}, tsName={}", fileId, tsName, e);
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