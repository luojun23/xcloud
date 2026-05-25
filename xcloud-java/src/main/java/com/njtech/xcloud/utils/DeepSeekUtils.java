package com.njtech.xcloud.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class DeepSeekUtils {

    @Value("${ai.deepseek.api-key}")
    private String apiKey;

    @Value("${ai.deepseek.base-url}")
    private String baseUrl;

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    public String analyzeContent(String content) {
        String url = baseUrl + "/chat/completions";
        String systemPrompt = "# Role\n" +
                "你是一位拥有认知心理学背景的资深信息架构师。你的专长是从杂乱的语音转录文本中提取高价值信息，并进行逻辑重构。\n\n" +
                "# Goals\n" +
                "请忽略文本中的噪音，对内容进行深度降噪和逻辑精炼，最终输出一份结构清晰、语气专业的分析报告。\n\n" +
                "# Constraints\n" +
                "1. 必须严格遵守下方的输出格式。\n" +
                "2. 语气保持客观、理性、犀利。\n" +
                "3. 如果文本内容过短或无意义，直接输出'无法提取有效信息'。\n" +
                "4. 禁止输出任何开场白或结束语，直接输出 Markdown 内容。\n\n" +
                "# Output Format (Markdown)\n" +
                "## 核心摘要\n（精简概括视频到底讲了什么，直击本质）\n\n" +
                "## 深度洞察\n" +
                "### 1. [4-8字强观点标题]\n深度分析...\n\n" +
                "### 2. [第二个强观点标题]\n深度分析...\n\n" +
                "### 3. [第三个强观点标题]\n深度分析...\n\n" +
                "## 原始内容精选\n> \"引用有价值的原话\"\n\n" +
                "## 领域标签\n#标签1 #标签2 #标签3";

        JSONObject jsonBody = new JSONObject();
        jsonBody.put("model", "deepseek-ai/DeepSeek-R1-Distill-Qwen-32B");
        jsonBody.put("stream", false);

        JSONArray messages = new JSONArray();
        messages.add(JSONObject.of("role", "system", "content", systemPrompt));
        messages.add(JSONObject.of("role", "user", "content", content));
        jsonBody.put("messages", messages);

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return "❌ AI 请求失败: " + response.code() + " - " + response.body().string();
            }
            String resultJson = response.body().string();
            JSONObject jsonObject = JSON.parseObject(resultJson);
            return jsonObject.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
        } catch (IOException e) {
            e.printStackTrace();
            return "❌ 网络连接出错: " + e.getMessage();
        }
    }
}
