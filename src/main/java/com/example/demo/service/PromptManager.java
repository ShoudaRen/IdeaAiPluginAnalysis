package com.example.demo.service;

import com.example.demo.model.MethodCallChain;
import com.example.demo.model.MethodInfo;
import com.example.demo.util.DatabaseUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 提示词管理服务
 * 负责管理AI提示词，解决大文档提示词过长的问题
 */
public class PromptManager {
    
    private final DatabaseUtil databaseUtil;
    private final Gson gson;
    private final Map<String, String> promptCache;
    private final int maxPromptLength = 4000; // 最大提示词长度
    
    public PromptManager() {
        this.databaseUtil = new DatabaseUtil();
        this.gson = new Gson();
        this.promptCache = new HashMap<>();
    }
    
    /**
     * 构建智能提示词
     * 根据检查内容动态选择相关的规范片段
     */
    public String buildSmartPrompt(MethodCallChain callChain, List<String> violationTypes) {

        //todo
        StringBuilder prompt = new StringBuilder();
        
        // 1. 基础提示词
        prompt.append("你是一个Java代码规范专家，请分析以下方法调用链路是否符合云开发范式规范。\n\n");
        
        // 2. 根据违规类型选择相关的规范片段
        Set<String> relevantRules = selectRelevantRules(violationTypes);
        prompt.append("相关规范要点：\n");
        for (String rule : relevantRules) {
            prompt.append(rule).append("\n");
        }
        prompt.append("\n");
        
        // 3. 添加方法调用链路信息（简化版）
        prompt.append("待分析的方法调用链路：\n");
        prompt.append(buildSimplifiedCallChain(callChain));
        prompt.append("\n");
        
        // 4. 检查提示词长度，如果过长则进一步简化
        if (prompt.length() > maxPromptLength) {
            prompt = new StringBuilder(truncatePrompt(prompt.toString()));
        }
        
        // 5. 添加分析要求
        prompt.append("请分析并返回JSON格式的结果，包含以下字段：\n");
        prompt.append("- violations: 违规列表，每项包含type, description, location, suggestion, severity\n");
        prompt.append("- summary: 总体评估\n");
        prompt.append("- recommendations: 改进建议列表\n");
        
        return prompt.toString();
    }
    
    /**
     * 根据违规类型选择相关的规范片段
     */
    private Set<String> selectRelevantRules(List<String> violationTypes) {
        Set<String> relevantRules = new HashSet<>();
        
        // 从数据库加载规则片段
        Map<String, String> ruleFragments = loadRuleFragments();
        
        // 根据违规类型选择相关规则
        for (String violationType : violationTypes) {
            switch (violationType.toLowerCase()) {
                case "naming":
                case "naming_violation":
                    relevantRules.add("1. 命名规范：方法名使用驼峰命名法，类名首字母大写，常量全大写");
                    break;
                case "layer":
                case "layer_violation":
                    relevantRules.add("2. 4层架构：Adapter(适配器) -> Application(应用) -> Domain(领域) -> Infrastructure(基础设施)");
                    relevantRules.add("   - 适配器层：只能调用应用层和基础设施层工具类");
                    relevantRules.add("   - 应用层：只能调用领域层和基础设施层");
                    relevantRules.add("   - 领域层：通过support接口调用基础设施层");
                    relevantRules.add("   - 基础设施层：实现领域层support接口");
                    break;
                case "signature":
                case "signature_violation":
                    relevantRules.add("3. 方法签名：参数不超过5个，返回类型明确");
                    break;
                case "annotation":
                case "annotation_violation":
                    relevantRules.add("4. 注解使用：正确使用Spring注解标识层次");
                    break;
                default:
                    relevantRules.add("5. 通用规范：遵循SOLID原则，保持代码简洁");
            }
        }
        
        // 如果没有特定违规类型，添加基础规范
        if (relevantRules.isEmpty()) {
            relevantRules.add("1. 命名规范：方法名使用驼峰命名法，类名首字母大写");
            relevantRules.add("2. 4层架构：Adapter -> Application -> Domain -> Infrastructure");
            relevantRules.add("3. 方法签名：参数不超过5个，返回类型明确");
        }
        
        return relevantRules;
    }
    
    /**
     * 构建简化的调用链路描述
     */
    private String buildSimplifiedCallChain(MethodCallChain callChain) {
        StringBuilder chain = new StringBuilder();
        
        if (callChain.getRootMethod() != null) {
            chain.append("根方法: ").append(callChain.getRootMethod().getMethodSignature()).append("\n");
        }
        
        // 只显示前3层调用，避免过长
        Map<Integer, List<MethodInfo>> callsByDepth = callChain.getCallsByDepth();
        int maxDepth = Math.min(3, callChain.getMaxDepth());
        
        for (int depth = 1; depth <= maxDepth; depth++) {
            List<MethodInfo> methods = callsByDepth.get(depth);
            if (methods != null && !methods.isEmpty()) {
                chain.append("第").append(depth).append("层调用: ");
                for (int i = 0; i < Math.min(3, methods.size()); i++) { // 每层最多显示3个方法
                    chain.append(methods.get(i).getMethodSignature());
                    if (i < Math.min(3, methods.size()) - 1) {
                        chain.append(", ");
                    }
                }
                chain.append("\n");
            }
        }
        
        return chain.toString();
    }
    
    /**
     * 截断过长的提示词
     */
    private String truncatePrompt(String prompt) {
        if (prompt.length() <= maxPromptLength) {
            return prompt;
        }
        
        // 保留开头和结尾，中间部分简化
        int headerLength = 500;
        int footerLength = 200;
        int middleLength = maxPromptLength - headerLength - footerLength;
        
        String header = prompt.substring(0, headerLength);
        String footer = prompt.substring(prompt.length() - footerLength);
        
        return header + "\n... (内容已简化) ...\n" + footer;
    }
    
    /**
     * 从数据库加载规则片段
     */
    private Map<String, String> loadRuleFragments() {
        Map<String, String> fragments = new HashMap<>();
        
        String sql = "SELECT rule_type, rule_summary FROM cloud_dev_rules WHERE is_active = 1";
        
        try (Connection conn = databaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                String ruleType = rs.getString("rule_type");
                String ruleSummary = rs.getString("rule_summary");
                fragments.put(ruleType, ruleSummary);
            }
            
        } catch (SQLException e) {
            // 如果数据库连接失败，使用默认规则片段
            loadDefaultRuleFragments(fragments);
        }
        
        return fragments;
    }
    
    /**
     * 加载默认规则片段
     */
    private void loadDefaultRuleFragments(Map<String, String> fragments) {
        fragments.put("naming", "命名规范：方法名使用驼峰命名法，类名首字母大写，常量全大写");
        fragments.put("layer", "4层架构：Adapter(适配器) -> Application(应用) -> Domain(领域) -> Infrastructure(基础设施)，严格遵循层间依赖规则");
        fragments.put("signature", "方法签名：参数不超过5个，返回类型明确");
        fragments.put("annotation", "注解使用：正确使用Spring注解标识层次");
        fragments.put("exception", "异常处理：统一异常处理机制");
    }
    
    /**
     * 获取规则摘要（用于快速参考）
     */
    public String getRuleSummary(String ruleType) {
        if (promptCache.containsKey(ruleType)) {
            return promptCache.get(ruleType);
        }
        
        String summary = loadRuleSummaryFromDatabase(ruleType);
        if (summary == null) {
            summary = getDefaultRuleSummary(ruleType);
        }
        
        promptCache.put(ruleType, summary);
        return summary;
    }
    
    /**
     * 从数据库加载规则摘要
     */
    private String loadRuleSummaryFromDatabase(String ruleType) {
        String sql = "SELECT rule_summary FROM cloud_dev_rules WHERE rule_type = ? AND is_active = 1";
        
        try (Connection conn = databaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, ruleType);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("rule_summary");
                }
            }
            
        } catch (SQLException e) {
            // 数据库查询失败
        }
        
        return null;
    }
    
    /**
     * 获取默认规则摘要
     */
    private String getDefaultRuleSummary(String ruleType) {
        switch (ruleType.toLowerCase()) {
            case "naming":
                return "命名规范：方法名使用驼峰命名法，类名首字母大写，常量全大写";
            case "layer":
                return "4层架构：Adapter(适配器) -> Application(应用) -> Domain(领域) -> Infrastructure(基础设施)，严格遵循层间依赖规则";
            case "signature":
                return "方法签名：参数不超过5个，返回类型明确";
            case "annotation":
                return "注解使用：正确使用Spring注解标识层次";
            case "exception":
                return "异常处理：统一异常处理机制";
            default:
                return "通用规范：遵循SOLID原则，保持代码简洁";
        }
    }
    
    /**
     * 清理缓存
     */
    public void clearCache() {
        promptCache.clear();
    }
}
