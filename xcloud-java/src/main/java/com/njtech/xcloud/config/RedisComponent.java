package com.njtech.xcloud.config;

import com.njtech.xcloud.dto.UserSpaceDto;
import com.njtech.xcloud.entity.constants.Constants;
import com.njtech.xcloud.entity.enums.ResponseCodeEnum;
import com.njtech.xcloud.entity.po.FileInfo;
import com.njtech.xcloud.entity.query.FileInfoQuery;
import com.njtech.xcloud.exception.BusinessException;
import com.njtech.xcloud.entity.po.UserInfo;
import com.njtech.xcloud.mappers.FileInfoMapper;
import com.njtech.xcloud.mappers.UserInfoMapper;

import javax.annotation.Resource;

/**
 * @ClassName : RedisComponent
 * @Description :
 * @Author : 罗君
 * @Date: 2026/4/22
 */
@org.springframework.stereotype.Component
public class RedisComponent {
    @Resource
    private RedisUtils redisUtils;

    @Resource
    private FileInfoMapper<FileInfo, FileInfoQuery> fileInfoMapper;

    @Resource
    private UserInfoMapper<UserInfo, ?> userInfoMapper;

    /**
     * 获取用户使用的空间
     *
     * @param userId
     * @return
     */
    public UserSpaceDto getUserSpaceUse(String userId) {
        UserSpaceDto spaceDto = (UserSpaceDto) redisUtils.get(Constants.REDIS_KEY_USER_SPACE_USE + userId);
        if (null == spaceDto) {
            spaceDto = new UserSpaceDto();
            // 查询当前用户已使用空间
            Long useSpace = this.fileInfoMapper.selectUseSpace(userId);
            spaceDto.setUseSpace(useSpace);
            spaceDto.setTotalSpace(5 * Constants.MB);
            redisUtils.set(Constants.REDIS_KEY_USER_SPACE_USE + userId, spaceDto, Constants.REDIS_KEY_EXPIRES_DAY);
        }
        return spaceDto;
    }

    /**
     * 检查用户磁盘空间是否充足
     *
     * @param userId
     * @param chunkSize
     * @param fileId
     */
    public void checkUserSpace(String userId, Long chunkSize, String fileId) {
        UserSpaceDto spaceDto = getUserSpaceUse(userId);
        Long totalSize = getTempSize(userId, fileId);
        if (totalSize + chunkSize + spaceDto.getUseSpace() > spaceDto.getTotalSpace()) {
            throw new BusinessException(ResponseCodeEnum.CODE_904);
        }
    }

    /**
     * 获取临时文件大小
     *
     * @param userId
     * @param fileId
     * @return
     */
    public Long getTempSize(String userId, String fileId) {
        Long size = getFileSizeFromRedis(Constants.REDIS_KEY_TEMP_SIZE + userId + ":" + fileId);
        return size == null ? 0L : size;
    }

    private Long getFileSizeFromRedis(String key) {
        Object sizeObj = redisUtils.get(key);
        if (sizeObj == null) {
            return 0L;
        }
        if (sizeObj instanceof Integer) {
            return ((Integer) sizeObj).longValue();
        } else if (sizeObj instanceof Long) {
            return (Long) sizeObj;
        }

        return 0L;
    }

    /**
     * 更新临时文件大小
     *
     * @param userId
     * @param fileId
     * @param chunkSize
     */
    public void updateTempSize(String userId, String fileId, Long chunkSize) {
        Long size = getTempSize(userId, fileId);
        redisUtils.set(Constants.REDIS_KEY_TEMP_SIZE + userId + ":" + fileId, size + chunkSize, Constants.REDIS_KEY_ONE_HOUR);
    }

    /**
     * 更新用户使用空间
     *
     * @param userId
     * @param totalSize
     */
    public void updateUserSpace(String userId, Long totalSize) {
        UserSpaceDto spaceDto = getUserSpaceUse(userId);
        Long newUseSpace = spaceDto.getUseSpace() + totalSize;
        if (newUseSpace < 0) {
            newUseSpace = 0L;
        }
        spaceDto.setUseSpace(newUseSpace);
        redisUtils.set(Constants.REDIS_KEY_USER_SPACE_USE + userId, spaceDto, Constants.REDIS_KEY_EXPIRES_DAY);

        // 同步更新数据库
        UserInfo updateInfo = new UserInfo();
        updateInfo.setUseSpace(newUseSpace);
        userInfoMapper.updateByUserId(updateInfo, userId);
    }
}
