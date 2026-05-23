package com.njtech.xcloud.utils;

import com.njtech.xcloud.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName : ProcessUtils
 * @Description : 系统命令执行工具
 * @Author : 罗君
 * @Date: 2026/4/23
 */
public class ProcessUtils {

    private static final Logger logger = LoggerFactory.getLogger(ProcessUtils.class);

    /**
     * 执行系统命令（Windows 下使用 cmd /c）
     *
     * @param command 命令字符串
     * @param timeout 超时时间（秒）
     * @return 命令输出
     */
    public static String executeCommand(String command, Integer timeout) {
        return executeCommand(command, timeout, true);
    }

    /**
     * 执行系统命令
     *
     * @param command  命令字符串
     * @param printLog 是否打印日志
     * @return 命令输出
     */
    public static String executeCommand(String command, boolean printLog) {
        return executeCommand(command, null, printLog);
    }

    private static String executeCommand(String command, Integer timeout, boolean printLog) {
        if (printLog) {
            logger.info("执行命令: {}", command);
        }
        ProcessBuilder processBuilder = new ProcessBuilder();

         String os = System.getProperty("os.name").toLowerCase();
         if (os.contains("win")) {
             //Windows:
             processBuilder.command("cmd", "/c", command);
         } else {
             //Linux:
             processBuilder.command("bash", "-c", command);
         }
        // 合并错误输出到标准输出，防止stderr缓冲区满导致子进程挂起
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();

            // 异步读取输出，防止缓冲区满导致子进程阻塞死锁
            StringBuilder outputBuilder = new StringBuilder();
            Thread readerThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        outputBuilder.append(line).append("\n");
                    }
                } catch (IOException e) {
                    logger.error("读取命令输出异常", e);
                }
            });
            readerThread.start();

            boolean finished;
            if (timeout != null && timeout > 0) {
                finished = process.waitFor(timeout, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    throw new BusinessException("命令执行超时: " + command);
                }
            } else {
                process.waitFor();
            }

            // 等待读取线程完成（最多再等5秒）
            readerThread.join(5000);

            String output = outputBuilder.toString();
            int exitValue = process.exitValue();
            if (exitValue != 0) {
                if (printLog) {
                    logger.error("命令执行失败, exitCode={}, output={}", exitValue, output);
                }
                throw new BusinessException("命令执行失败: " + output);
            }
            if (printLog) {
                logger.info("命令执行完成, exitCode={}", exitValue);
            }
            return output;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            if (printLog) {
                logger.error("执行命令异常", e);
            }
            throw new BusinessException("执行命令异常: " + e.getMessage());
        }
    }

    private static String readStream(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }
}
