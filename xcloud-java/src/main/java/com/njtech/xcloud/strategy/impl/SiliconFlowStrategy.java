package com.njtech.xcloud.strategy.impl;

import com.njtech.xcloud.entity.constants.Constants;
import com.njtech.xcloud.strategy.AiAnalysisStrategy;
import com.njtech.xcloud.utils.SiliconFlowAsrUtils;
import com.njtech.xcloud.utils.SiliconFlowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 基于 SiliconFlow 的 AI 分析策略
 * - ASR 语音识别：TeleAI/TeleSpeechASR
 * - 智能总结：DeepSeek-R1-Distill-Qwen-32B
 */
@Component("defaultAiStrategy")
public class SiliconFlowStrategy implements AiAnalysisStrategy {

    @Autowired
    private SiliconFlowAsrUtils siliconFlowAsrUtils;

    @Autowired
    private SiliconFlowUtils siliconFlowUtils;

    @Value("${tool.ffmpeg.dir:ffmpeg}")
    private String ffmpegDir;

    @Value("${project.folder}")
    private String projectFolder;

    @Override
    public String transcribe(String videoPath) {
        return processVideoToText(videoPath);
    }

    @Override
    public String generateSummary(String videoPath) {
        String text = processVideoToText(videoPath);
        if (text.startsWith("❌")) return text;
        return siliconFlowUtils.analyzeContent("请对以下视频提取的文字进行总结，不需要废话，直接列出核心观点：\n" + text);
    }

    private String processVideoToText(String inputPath) {
        if (inputPath == null || inputPath.isEmpty()) return "❌ 路径为空";

        if (!inputPath.startsWith("http")) {
            File localFile = new File(inputPath);
            if (!localFile.exists()) return "❌ 磁盘找不到文件: " + inputPath;
        }

        String outputMp3Path = projectFolder + Constants.FILE_FOLDE_TRANSLATE_AUDIO + File.separator + "temp_" + UUID.randomUUID() + ".mp3";

        try {
            boolean success = extractAudio(inputPath, outputMp3Path);
            if (!success) return "❌ FFmpeg 转换失败 (可能是网络超时或文件损坏)";
            return siliconFlowAsrUtils.audioToText(outputMp3Path);
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ 处理异常: " + e.getMessage();
        }/* finally {
            File mp3 = new File(outputMp3Path);
            if (mp3.exists()) mp3.delete();
        }*/
    }

    /**
     * 公开的音频提取方法，供 Controller 直接调用（下载音频功能）
     */
    public boolean extractAudioPublic(String inputPath, String outputPath) {
        return extractAudio(inputPath, outputPath);
    }

    private boolean extractAudio(String inputPath, String outputPath) {
        Process process = null;
        try {
            // 确保输出目录存在
            new File(outputPath).getParentFile().mkdirs();

            List<String> command = new ArrayList<>();
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

            System.out.println("[FFmpeg] 执行命令: " + String.join(" ", command));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true); // stderr 合并到 stdout

            process = pb.start();

            // 读取 FFmpeg 输出（不读取会导致进程阻塞）
            StringBuilder ffmpegLog = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    ffmpegLog.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(15, java.util.concurrent.TimeUnit.MINUTES);

            if (finished) {
                int exitCode = process.exitValue();
                if (exitCode != 0) {
                    System.err.println("[FFmpeg] 转换失败，exitCode=" + exitCode + "，日志:\n" + ffmpegLog);
                    return false;
                }
                // 检查输出文件是否真的生成
                File out = new File(outputPath);
                if (!out.exists() || out.length() == 0) {
                    System.err.println("[FFmpeg] 输出文件不存在或为空: " + outputPath);
                    return false;
                }
                System.out.println("[FFmpeg] 转换成功，输出文件大小: " + out.length() + " bytes");
                return true;
            } else {
                process.destroyForcibly();
                System.err.println("[FFmpeg] 超时（15分钟），已强制终止");
                return false;
            }
        } catch (Exception e) {
            System.err.println("[FFmpeg] 异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }
}
