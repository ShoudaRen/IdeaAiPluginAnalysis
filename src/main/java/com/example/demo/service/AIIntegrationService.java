package com.example.demo.service;

import com.example.demo.model.MethodCallChain;
import com.example.demo.model.RuleViolation;
import com.example.demo.util.ConfigManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * AI集成服务
 * 负责调用大模型API进行代码规范检查和建议生成
 */
public class AIIntegrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AIIntegrationService.class);
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final String apiUrl;
    private final String apiKey;
    
    public AIIntegrationService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
        
        // 从ConfigManager读取API配置
        ConfigManager config = ConfigManager.getInstance();
        this.apiUrl = !config.getAiApiUrl().isEmpty() ? 
                     config.getAiApiUrl() : 
                     "https://openrouter.ai/api/v1/chat/completions";
        this.apiKey = !config.getAiApiKey().isEmpty() ? 
                     config.getAiApiKey() : 
                     "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";
    }
    
    /**
     * 使用AI分析方法调用链路并生成改进建议
     */
    public List<RuleViolation> analyzeWithAI(MethodCallChain callChain) {
        List<RuleViolation> aiViolations = new ArrayList<>();
        
        try {
            
            // 使用本地提示词构建方法
            String prompt = buildAnalysisPrompt(callChain);
            String aiResponse = callAIAPI(prompt);
            aiViolations = parseAIResponse(aiResponse);
            
        } catch (Exception e) {
            logger.error("AI分析失败: {}", e.getMessage());

        }
        
        return aiViolations;
    }

    /**
     * 构建AI分析提示词
     */
    private String buildAnalysisPrompt(MethodCallChain callChain) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("你是一个Java代码规范专家，请分析以下方法调用链路是否符合云开发范式规范。\n\n");
        
        // 添加云开发范式规范说明（基于4层DDD架构）
        prompt.append("云开发范式规范要点：\n");
        prompt.append("1. 4层架构：Adapter(适配器层) -> Application(应用层) -> Domain(领域层) -> Infrastructure(基础设施层)\n");
        prompt.append("2. 层间依赖规则：\n");
        prompt.append("   - 适配器层：只能调用应用层和基础设施层的工具类\n");
        prompt.append("   - 应用层：只能调用领域层和基础设施层\n");
        prompt.append("   - 领域层：只能调用基础设施层，通过support接口实现依赖反转\n");
        prompt.append("   - 基础设施层：只能调用领域层，实现support接口\n");
        prompt.append("3. 分包结构：\n");
        prompt.append("   - adapter包：Web控制器、消息适配器、定时任务等\n");
        prompt.append("   - application包：api和service分包，应用逻辑编排\n");
        prompt.append("   - domain包：实体、值对象、领域服务、support接口\n");
        prompt.append("   - infrastructure包：mapper、repository、config、util、supportimpl等\n");
        prompt.append("4. 命名规范：方法名使用驼峰命名法，类名首字母大写\n");
        prompt.append("5. 方法签名：参数不超过5个，返回类型明确\n");
        prompt.append("6. 注解使用：正确使用Spring注解标识层次\n\n");
        
        // 添加方法调用链路信息
        prompt.append("待分析的方法调用链路：\n");
        prompt.append(callChain.toTreeString());
        prompt.append("\n");
        
        prompt.append("请深度分析此调用链路，重点关注：\n");
        prompt.append("1. 层间调用是否符合依赖规则\n");
        prompt.append("2. 是否存在跨层调用或违反依赖反转\n");
        prompt.append("3. 方法职责是否合理分配\n");
        prompt.append("4. 是否遵循领域驱动设计原则\n\n");
        
        prompt.append("请返回JSON格式的结果，包含以下字段：\n");
        prompt.append("- violations: 违规列表，每项包含type, description, location, suggestion, severity\n");
        prompt.append("- summary: 总体评估和架构健康度\n");
        prompt.append("- recommendations: 具体改进建议，包含重构方案\n");
        prompt.append("- architecture_advice: 基于4层架构的设计建议\n");
        
        return prompt.toString();
    }
    
    /**
     * 调用AI API
     */
    private String callAIAPI(String prompt) throws IOException {
        ConfigManager config = ConfigManager.getInstance();
        
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", config.getAiModel());
        requestBody.addProperty("max_tokens", 2000);
        requestBody.addProperty("temperature", 0.3);
        
        // 构建消息
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        
        com.google.gson.JsonArray messages = new com.google.gson.JsonArray();
        messages.add(message);
        requestBody.add("messages", messages);
        
        RequestBody body = RequestBody.create(
            requestBody.toString(),
            MediaType.get("application/json; charset=utf-8")
        );
        
        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("AI API调用失败: " + response);
            }
            
            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            
            return jsonResponse.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();
        }
    }
    
    /**
     * 解析AI响应
     */
    private List<RuleViolation> parseAIResponse(String aiResponse) {
        List<RuleViolation> violations = new ArrayList<>();
        
        logger.info("AI响应内容: {}", aiResponse);
        
        try {
            JsonObject response = gson.fromJson(aiResponse, JsonObject.class);
            logger.info("JSON解析成功，检查violations字段...");
            
            if (response.has("violations")) {
                com.google.gson.JsonArray violationsArray = response.getAsJsonArray("violations");
                logger.info("找到violations数组，长度: {}", violationsArray.size());
                
                for (int i = 0; i < violationsArray.size(); i++) {
                    JsonObject violationObj = violationsArray.get(i).getAsJsonObject();
                    
                    RuleViolation violation = new RuleViolation();
                    violation.setViolationType(getJsonString(violationObj, "type", "UNKNOWN"));
                    violation.setDescription(getJsonString(violationObj, "description", "AI检测到问题"));
                    violation.setLocation(getJsonString(violationObj, "location", "位置未知"));
                    violation.setSuggestion(getJsonString(violationObj, "suggestion", "请查看AI建议"));
                    violation.setSeverity(getJsonString(violationObj, "severity", "medium"));
                    
                    violations.add(violation);
                    logger.info("添加违规: {}", violation.getDescription());
                }
            } else {
                logger.info("AI响应中没有violations字段，可能代码符合规范");
            }
            
        } catch (Exception e) {
            logger.error("解析AI响应失败: {}", e.getMessage());
            logger.error("AI原始响应: {}", aiResponse);
            
            // 解析失败时，创建一个包含AI分析结果的违规项
            RuleViolation violation = new RuleViolation();
            violation.setViolationType("AI_ANALYSIS");
            violation.setDescription("AI分析完成，请查看详细报告");
            violation.setLocation("整体调用链路");
            violation.setSuggestion("AI已完成分析，详细建议请查看报告");
            violation.setSeverity("info");
            violations.add(violation);
        }
        
        logger.info("最终返回违规数量: {}", violations.size());
        return violations;
    }
    
    /**
     * 安全获取JSON字符串值
     */
    private String getJsonString(JsonObject obj, String key, String defaultValue) {
        try {
            if (obj.has(key) && !obj.get(key).isJsonNull()) {
                return obj.get(key).getAsString();
            }
        } catch (Exception e) {
            logger.warn("获取JSON字段失败: {}", key);
        }
        return defaultValue;
    }
    
    /**
     * 生成改进建议文档
     */
    public String generateImprovementDocument(MethodCallChain callChain, 
                                            List<RuleViolation> allViolations) {
        try {
            String prompt = buildDocumentPrompt(callChain, allViolations);
            return callAIAPI(prompt);
        } catch (Exception e) {
            logger.error("生成改进文档失败: {}", e.getMessage());
            return generateFallbackDocument(callChain, allViolations);
        }
    }
    
    /**
     * 构建文档生成提示词
     */
    private String buildDocumentPrompt(MethodCallChain callChain, 
                                      List<RuleViolation> violations) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("请根据以下代码分析结果，生成一份详细的代码规范检查报告：\n\n");
        
        prompt.append("方法调用链路：\n");
        prompt.append(callChain.toTreeString());
        prompt.append("\n");
        
        prompt.append("发现的问题：\n");
        for (RuleViolation violation : violations) {
            prompt.append(violation.toReportString()).append("\n");
        }
        
        prompt.append("\n请生成包含以下内容的报告：\n");
        prompt.append("1. 执行摘要\n");
        prompt.append("2. 问题详细分析\n");
        prompt.append("3. 具体修改建议\n");
        prompt.append("4. 最佳实践推荐\n");
        
        return prompt.toString();
    }
    
    /**
     * 生成备用文档（当AI不可用时）
     */
    private String generateFallbackDocument(MethodCallChain callChain, 
                                          List<RuleViolation> violations) {
        StringBuilder doc = new StringBuilder();
        
        doc.append("# 代码规范检查报告\n\n");
        doc.append("## 执行摘要\n");
        doc.append("检查方法: ").append(callChain.getRootMethod().getMethodSignature()).append("\n");
        doc.append("发现问题数: ").append(violations.size()).append("\n\n");
        
        doc.append("## 问题详情\n");
        for (RuleViolation violation : violations) {
            doc.append(violation.toReportString()).append("\n");
        }
        
        return doc.toString();
    }
}