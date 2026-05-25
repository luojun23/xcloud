package com.njtech.xcloud.strategy;

/**
 * AI 分析策略接口
 * 参数为 String 类型，兼容本地路径和网络 URL
 */
public interface AiAnalysisStrategy {

    /**
     * 将视频文件转换为文字
     * @param videoPath 视频路径或URL
     */
    String transcribe(String videoPath);

    /**
     * 对视频内容进行智能总结
     * @param videoPath 视频路径或URL
     */
    String generateSummary(String videoPath);
}
