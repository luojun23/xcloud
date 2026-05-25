-- ============================================================
-- XCloud-AI 数据库迁移脚本
-- 数据库: easypan
-- 说明: 为 file_info 表新增 AI 视频分析相关字段
--       ai_summary    -> AI 智能总结（DeepSeek 生成的 Markdown 报告）
--       transcript_text -> 语音转文字全文（阿里 ASR 提取的文本）
-- 执行顺序: 先执行增量 ALTER，若新建库则执行完整建表
-- ============================================================

-- ============================
-- 方式一：增量迁移（已有数据库执行）
-- ============================

ALTER TABLE `file_info`
    ADD COLUMN `ai_summary` TEXT COMMENT 'AI 智能总结（DeepSeek 生成的 Markdown 格式分析报告）' AFTER `del_flag`,
    ADD COLUMN `transcript_text` MEDIUMTEXT COMMENT '语音转文字全文（阿里 ASR 提取的完整文本）' AFTER `ai_summary`;


-- ============================
-- 方式二：完整建表（新建数据库执行）
-- ============================

/*
CREATE DATABASE IF NOT EXISTS `easypan` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `easypan`;
*/

-- -----------------------------------------------------------
-- 表1: user_info  用户信息表（原 xcloud，无修改）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_info` (
    `user_id`          VARCHAR(64)   NOT NULL COMMENT '用户ID',
    `nick_name`        VARCHAR(50)   NOT NULL COMMENT '昵称',
    `email`            VARCHAR(100)  NOT NULL COMMENT '邮箱',
    `password`         VARCHAR(100)  NOT NULL COMMENT '密码(MD5)',
    `avatar`           VARCHAR(200)  DEFAULT NULL COMMENT '头像路径',
    `join_time`        DATETIME      NOT NULL COMMENT '加入时间',
    `last_login_time`  DATETIME      DEFAULT NULL COMMENT '最后登录时间',
    `status`           TINYINT       NOT NULL DEFAULT 1 COMMENT '0:禁用 1:启用',
    `use_space`        BIGINT        NOT NULL DEFAULT 0 COMMENT '已使用空间(字节)',
    `total_space`      BIGINT        NOT NULL DEFAULT 5368709120 COMMENT '总空间(字节), 默认5GB',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';


-- -----------------------------------------------------------
-- 表2: email_code  邮箱验证码表（原 xcloud，无修改）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `email_code` (
    `code_id`    INT           NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `email`      VARCHAR(100)  NOT NULL COMMENT '邮箱',
    `code`       VARCHAR(10)   NOT NULL COMMENT '验证码',
    `create_time` DATETIME     NOT NULL COMMENT '创建时间',
    `status`     TINYINT       NOT NULL DEFAULT 0 COMMENT '0:未使用 1:已使用',
    PRIMARY KEY (`code_id`),
    KEY `idx_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮箱验证码表';


-- -----------------------------------------------------------
-- 表3: file_info  文件信息表（核心表，新增 AI 字段）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `file_info` (
    `file_id`          VARCHAR(64)    NOT NULL COMMENT '文件ID',
    `user_id`          VARCHAR(64)    NOT NULL COMMENT '用户ID',
    `file_md5`         VARCHAR(32)    DEFAULT NULL COMMENT '文件MD5',
    `file_pid`         VARCHAR(64)    NOT NULL COMMENT '父级ID',
    `file_size`        BIGINT         NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
    `file_name`        VARCHAR(200)   NOT NULL COMMENT '文件名',
    `file_cover`       VARCHAR(200)   DEFAULT NULL COMMENT '文件封面路径',
    `file_path`        VARCHAR(200)   DEFAULT NULL COMMENT '文件存储路径',
    `create_time`      DATETIME       NOT NULL COMMENT '创建时间',
    `last_update_time` DATETIME       NOT NULL COMMENT '最后更新时间',
    `folder_type`      TINYINT        NOT NULL DEFAULT 0 COMMENT '文件类型 0:文件 1:目录',
    `file_category`    TINYINT        DEFAULT NULL COMMENT '文件分类 1:视频 2:音频 3:图片 4:文档 5:其他',
    `file_type`        TINYINT        DEFAULT NULL COMMENT '文件格式 1:视频 2:音频 3:图片 4:pdf 5:doc 6:excel 7:txt 8:code 9:zip 10:其他',
    `status`           TINYINT        NOT NULL DEFAULT 0 COMMENT '转码状态 0:转码中 1:转码失败 2:转码成功',
    `recovery_time`    DATETIME       DEFAULT NULL COMMENT '进入回收站时间',
    `del_flag`         TINYINT        NOT NULL DEFAULT 2 COMMENT '标记删除 0:已删除 1:回收站 2:正常',
    -- ========== 以下为 XCloud-AI 新增字段 ==========
    `ai_summary`       TEXT           DEFAULT NULL COMMENT 'AI 智能总结（DeepSeek 生成的 Markdown 格式分析报告）',
    `transcript_text`  MEDIUMTEXT     DEFAULT NULL COMMENT '语音转文字全文（阿里 ASR 提取的完整文本，最大16MB）',
    PRIMARY KEY (`file_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_file_pid` (`file_pid`),
    KEY `idx_file_md5` (`file_md5`),
    KEY `idx_del_flag` (`del_flag`),
    KEY `idx_file_category` (`file_category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件信息表';


-- -----------------------------------------------------------
-- 表4: file_share  文件分享表（原 xcloud，无修改）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `file_share` (
    `share_id`      VARCHAR(64)    NOT NULL COMMENT '分享ID',
    `user_id`       VARCHAR(64)    NOT NULL COMMENT '分享用户ID',
    `file_id`       VARCHAR(64)    NOT NULL COMMENT '文件ID',
    `share_time`    DATETIME       NOT NULL COMMENT '分享时间',
    `expire_time`   DATETIME       DEFAULT NULL COMMENT '过期时间(空则永久)',
    `valid_type`    TINYINT        NOT NULL DEFAULT 0 COMMENT '有效期类型 0:永久 1:1天 2:7天 3:30天',
    `share_code`    VARCHAR(20)    DEFAULT NULL COMMENT '提取码',
    `share_url`     VARCHAR(200)   DEFAULT NULL COMMENT '分享链接',
    PRIMARY KEY (`share_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_file_id` (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件分享表';
