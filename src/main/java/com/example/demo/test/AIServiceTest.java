package com.example.demo.test;

import com.example.demo.service.AIIntegrationService;
import com.example.demo.model.MethodCallChain;
import com.example.demo.model.MethodInfo;
import com.example.demo.model.RuleViolation;

import java.util.ArrayList;
import java.util.List;

/**
 * AI服务测试类
 */
public class AIServiceTest {

    public static void main(String[] args) {
        System.out.println("=== AI服务测试 ===");

        // 创建测试调用链
        MethodCallChain testChain = createTestCallChain();

        // 测试AI分析
        AIIntegrationService aiService = new AIIntegrationService();
        List<RuleViolation> violations = aiService.analyzeWithAI(testChain);

        System.out.println("AI分析结果:");
        System.out.println("违规数量: " + violations.size());

        for (RuleViolation violation : violations) {
            System.out.println("- 类型: " + violation.getViolationType());
            System.out.println("  描述: " + violation.getDescription());
            System.out.println("  位置: " + violation.getLocation());
            System.out.println("  建议: " + violation.getSuggestion());
            System.out.println("  严重程度: " + violation.getSeverity());
            System.out.println();
        }

        // 测试生成改进文档
        try {
            String document = aiService.generateImprovementDocument(testChain, violations);
            System.out.println("=== AI改进文档 ===");
            System.out.println(document);
        } catch (Exception e) {
            System.out.println("生成改进文档失败: " + e.getMessage());
        }
    }

    private static MethodCallChain createTestCallChain() {
        // 创建根方法 (Controller)
        MethodInfo rootMethod = new MethodInfo();
        rootMethod.setMethodName("getGroupMembers");
        rootMethod.setClassName("com.test.controller.GroupController");
        rootMethod.setReturnType("Result<List<User>>");
        rootMethod.setParameters(new ArrayList<String>() {{
            add("Long groupId");
            add("HttpServletRequest request");
        }});
        rootMethod.setLayerType("ADAPTER");

        // 创建Service方法
        MethodInfo serviceMethod = new MethodInfo();
        serviceMethod.setMethodName("getGroupMembers");
        serviceMethod.setClassName("com.test.service.GroupService");
        serviceMethod.setReturnType("List<User>");
        serviceMethod.setParameters(new ArrayList<String>() {{
            add("Long groupId");
        }});
        serviceMethod.setLayerType("APPLICATION");


        return null;
    }
}
