package com.njtech.xcloud.strategy.impl;

import com.njtech.xcloud.strategy.AiAnalysisStrategy;
import com.njtech.xcloud.utils.AliyunAsrUtils;
import com.njtech.xcloud.utils.DeepSeekUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component("defaultAiStrategy")
public class AliyunDeepSeekStrategy implements AiAnalysisStrategy {

    @Autowired
    private AliyunAsrUtils aliyunAsrUtils;

    @Autowired
    private DeepSeekUtils deepSeekUtils;

    @Value("${tool.ffmpeg.dir:ffmpeg}")
    private String ffmpegDir;

    @Override
    public String transcribe(String videoPath) {
        return processVideoToText(videoPath);
    }

    @Override
    public String generateSummary(String videoPath) {
        String text = processVideoToText(videoPath);
        if (text.startsWith("❌")) return text;
        return deepSeekUtils.analyzeContent("请对以下视频提取的文字进行总结，不需要废话，直接列出核心观点：\n" + text);
    }

    private String processVideoToText(String inputPath) {
        if (inputPath == null || inputPath.isEmpty()) return "❌ 路径为空";

        if (!inputPath.startsWith("http")) {
            File localFile = new File(inputPath);
            if (!localFile.exists()) return "❌ 磁盘找不到文件: " + inputPath;
        }

        String outputMp3Path = System.getProperty("java.io.tmpdir") + File.separator + "temp_" + UUID.randomUUID() + ".mp3";

        try {
            boolean success = extractAudio(inputPath, outputMp3Path);
            if (!success) return "FFmpeg 转换失败 (可能是网络超时或文件损坏)";
            String text = aliyunAsrUtils.audioToText(outputMp3Path);
            return text;
        } catch (Exception e) {
            e.printStackTrace();
            return "处理异常: " + e.getMessage();
        } finally {
            File mp3 = new File(outputMp3Path);
            if (mp3.exists()) mp3.delete();
        }
    }

    private boolean extractAudio(String inputPath, String outputPath) {
        Process process = null;
        try {
            List<String> command = new ArrayList<>();
            // 优先使用配置的 ffmpeg 路径
            String ffmpegExe = "ffmpeg";
            if (ffmpegDir != null && !ffmpegDir.isEmpty()) {
                ffmpegExe = ffmpegDir + File.separator + "ffmpeg";
            }
            command.add(ffmpegExe);
            command.add("-y");
            command.add("-i");
            command.add(inputPath);
            command.add("-vn");
            command.add("-acodec");
            command.add("libmp3lame");
            command.add("-q:a");
            command.add("2");
            command.add(outputPath);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);

            process = pb.start();
            boolean finished = process.waitFor(15, java.util.concurrent.TimeUnit.MINUTES);

            if (finished) {
                return process.exitValue() == 0;
            } else {
                process.destroyForcibly();
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }
}
