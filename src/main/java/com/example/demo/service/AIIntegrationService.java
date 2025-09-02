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
    public List<RuleViolation> analyzeWithAI(MethodCallChain callChain, 
                                            List<RuleViolation> existingViolations,
                                            PromptManager promptManager) {
        List<RuleViolation> aiViolations = new ArrayList<>();
        
        try {
            // 提取违规类型
            List<String> violationTypes = new ArrayList<>();
            for (RuleViolation violation : existingViolations) {
                violationTypes.add(violation.getViolationType());
            }
            
            // 使用智能提示词管理器构建提示词
            String prompt = promptManager.buildSmartPrompt(callChain, violationTypes);
            String aiResponse = callAIAPI(prompt);
            aiViolations = parseAIResponse(aiResponse);
            
        } catch (Exception e) {
            logger.error("AI分析失败: {}", e.getMessage());
            // 返回空列表，不影响基础规则检查
        }
        
        return aiViolations;
    }
    
    /**
     * 使用AI分析方法调用链路并生成改进建议（兼容旧版本）
     */
    public List<RuleViolation> analyzeWithAI(MethodCallChain callChain, 
                                            List<RuleViolation> existingViolations) {
        PromptManager promptManager = new PromptManager();
        return analyzeWithAI(callChain, existingViolations, promptManager);
    }
    
    /**
     * 构建AI分析提示词
     */
    private String buildAnalysisPrompt(MethodCallChain callChain, 
                                      List<RuleViolation> existingViolations) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("你是一个Java代码规范专家，请分析以下方法调用链路是否符合云开发范式规范。\n\n");
        
        // 添加云开发范式规范说明（这里可以从数据库或配置文件加载）
        prompt.append("云开发范式规范要点：\n");
        prompt.append("1. 命名规范：方法名使用驼峰命名法，类名首字母大写\n");
        prompt.append("2. 层次架构：Controller -> Service -> DAO，不允许跨层调用\n");
        prompt.append("3. 方法签名：参数不超过5个，返回类型明确\n");
        prompt.append("4. 注解使用：正确使用Spring注解标识层次\n");
        prompt.append("5. 异常处理：统一异常处理机制\n\n");
        
        // 添加方法调用链路信息
        prompt.append("待分析的方法调用链路：\n");
        prompt.append(callChain.toTreeString());
        prompt.append("\n");
        
        // 添加已发现的违规信息
        if (!existingViolations.isEmpty()) {
            prompt.append("已发现的规范违规：\n");
            for (RuleViolation violation : existingViolations) {
                prompt.append("- ").append(violation.getDescription())
                      .append(" (位置: ").append(violation.getLocation()).append(")\n");
            }
            prompt.append("\n");
        }
        
        prompt.append("请分析并返回JSON格式的结果，包含以下字段：\n");
        prompt.append("- violations: 违规列表，每项包含type, description, location, suggestion, severity\n");
        prompt.append("- summary: 总体评估\n");
        prompt.append("- recommendations: 改进建议列表\n");
        
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
                throw new IOException("AI API调用失败: " + response.code());
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
        
        try {
            JsonObject response = gson.fromJson(aiResponse, JsonObject.class);
            
            if (response.has("violations")) {
                com.google.gson.JsonArray violationsArray = response.getAsJsonArray("violations");
                
                for (int i = 0; i < violationsArray.size(); i++) {
                    JsonObject violationObj = violationsArray.get(i).getAsJsonObject();
                    
                    RuleViolation violation = new RuleViolation();
                    violation.setViolationType(violationObj.get("type").getAsString());
                    violation.setDescription(violationObj.get("description").getAsString());
                    violation.setLocation(violationObj.get("location").getAsString());
                    violation.setSuggestion(violationObj.get("suggestion").getAsString());
                    violation.setSeverity(violationObj.get("severity").getAsString());
                    
                    violations.add(violation);
                }
            }
            
        } catch (Exception e) {
            logger.error("解析AI响应失败: {}", e.getMessage());
        }
        
        return violations;
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