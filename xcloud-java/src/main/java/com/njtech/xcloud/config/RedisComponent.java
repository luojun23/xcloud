package com.njtech.xcloud.config;

import com.njtech.xcloud.dto.UserSpaceDto;
import com.njtech.xcloud.entity.constants.Constants;
import com.njtech.xcloud.entity.po.FileInfo;
import com.njtech.xcloud.entity.query.FileInfoQuery;
import com.njtech.xcloud.mappers.FileInfoMapper;

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
            // 查询dan用户已使用空间
            Long useSpace = this.fileInfoMapper.selectUseSpace(userId);
            spaceDto.setUseSpace(useSpace);
            spaceDto.setTotalSpace(5 * Constants.MB);
            redisUtils.set(Constants.REDIS_KEY_USER_SPACE_USE + userId, spaceDto, Constants.REDIS_KEY_EXPIRES_DAY);
        }
        return spaceDto;
    }
}
