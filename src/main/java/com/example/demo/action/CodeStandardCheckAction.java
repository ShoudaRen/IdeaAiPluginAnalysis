package com.example.demo.action;

import com.example.demo.service.CallChainAnalyzer;
import com.example.demo.service.CloudDevelopmentRuleEngine;
import com.example.demo.service.AIIntegrationService;
import com.example.demo.service.CacheService;
import com.example.demo.service.PromptManager;
import com.example.demo.model.MethodCallChain;
import com.example.demo.model.RuleViolation;
import com.example.demo.ui.CheckResultDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import org.jetbrains.annotations.NotNull;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.util.List;
import java.util.ArrayList;

/**
 * 代码规范检查Action
 * 提供代码检查功能，检查当前方法是否符合云开发范式
 */
public class CodeStandardCheckAction extends AnAction {

    /**
     * 执行代码规范检查
     * @param methodName 方法名
     * @param className 类名
     * @param project 项目对象
     */
    public void performCheck(String methodName, String className, Project project) {
        if (methodName == null || methodName.trim().isEmpty()) {
            showErrorDialog("请提供有效的方法名", "错误");
            return;
        }
        
        if (className == null || className.trim().isEmpty()) {
            showErrorDialog("请提供有效的类名", "错误");
            return;
        }
        
        // 生成缓存键
        String methodKey = CacheService.getInstance().generateMethodKey(className, methodName);
        
        // 检查缓存
        if (CacheService.getInstance().hasCachedResult(methodKey)) {
            showCachedResults(methodKey);
            return;
        }
        
        // 使用进度管理器执行检查   
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "云开发规范检查", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setText("开始分析方法调用链路...");
                    indicator.setFraction(0.1);
                    
                    // 1. 分析方法调用链路
                    CallChainAnalyzer analyzer = new CallChainAnalyzer(project);
                    MethodCallChain callChain = analyzer.analyzeCallChain(methodName, className);
                    
                    // 缓存调用链路结果
                    CacheService.getInstance().cacheCallChain(methodKey, callChain);
                    
                    indicator.setText("执行规则检查...");
                    indicator.setFraction(0.3);
                    
                    // 2. 执行云开发范式规则检查
                    CloudDevelopmentRuleEngine ruleEngine = new CloudDevelopmentRuleEngine();
                    List<RuleViolation> violations = ruleEngine.checkCallChain(callChain);
                    
                    // 缓存规则检查结果
                    CacheService.getInstance().cacheRuleCheck(methodKey, violations);
                    
                    indicator.setText("AI智能分析...");
                    indicator.setFraction(0.6);
                    
                    // 3. 使用AI进行深度分析
                    AIIntegrationService aiService = new AIIntegrationService();
                    PromptManager promptManager = new PromptManager();
                    
                    // 提取违规类型用于智能提示词构建
                    List<String> violationTypes = new ArrayList<>();
                    for (RuleViolation violation : violations) {
                        violationTypes.add(violation.getViolationType());
                    }
                    
                    List<RuleViolation> aiViolations = aiService.analyzeWithAI(callChain, violations, promptManager);
                    
                    // 缓存AI分析结果
                    CacheService.getInstance().cacheAIAnalysis(methodKey, aiViolations);
                    
                    indicator.setText("生成检查报告...");
                    indicator.setFraction(0.9);
                    
                    // 合并所有违规信息
                    List<RuleViolation> allViolations = new ArrayList<>(violations);
                    allViolations.addAll(aiViolations);
                    
                    // 4. 生成检查报告
                    String report = generateReport(callChain, allViolations, aiService);
                    
                    indicator.setFraction(1.0);
                    
                    // 在EDT线程中显示结果
                    SwingUtilities.invokeLater(() -> showResults(callChain, allViolations, report));
                    
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> 
                        showErrorDialog("检查过程中发生错误: " + ex.getMessage(), "错误"));
                }
            }
        });
    }
    
    /**
     * 显示缓存的结果
     */
    private void showCachedResults(String methodKey) {
        MethodCallChain callChain = CacheService.getInstance().getCachedCallChain(methodKey);
        List<RuleViolation> violations = CacheService.getInstance().getCachedRuleCheck(methodKey);
        List<RuleViolation> aiViolations = CacheService.getInstance().getCachedAIAnalysis(methodKey);
        
        if (callChain != null && violations != null) {
            List<RuleViolation> allViolations = new ArrayList<>(violations);
            if (aiViolations != null) {
                allViolations.addAll(aiViolations);
            }
            
            AIIntegrationService aiService = new AIIntegrationService();
            String report = generateReport(callChain, allViolations, aiService);
            showResults(callChain, allViolations, report);
        } else {
            showErrorDialog("缓存数据不完整，请重新检查", "错误");
        }
    }
    
    /**
     * 生成检查报告
     */
    private String generateReport(MethodCallChain callChain, List<RuleViolation> violations, 
                                 AIIntegrationService aiService) {
        if (violations.isEmpty()) {
            return "恭喜！该方法调用链路完全符合云开发范式规范。";
        }
        
        StringBuilder report = new StringBuilder();
        report.append("=== 云开发规范检查报告 ===\n\n");
        report.append("检查方法: ").append(callChain.getRootMethod().getMethodSignature()).append("\n");
        report.append("调用链路深度: ").append(callChain.getMaxDepth()).append("\n");
        report.append("涉及方法数: ").append(callChain.getTotalMethodCount()).append("\n");
        report.append("发现问题数: ").append(violations.size()).append("\n\n");
        
        // 按严重程度分组显示问题
        report.append("=== 问题详情 ===\n");
        violations.stream()
                .sorted((v1, v2) -> Integer.compare(v2.getSeverityLevel(), v1.getSeverityLevel()))
                .forEach(violation -> {
                    report.append(violation.toReportString()).append("\n");
                });
        
        // 尝试生成AI改进建议
        try {
            String aiDocument = aiService.generateImprovementDocument(callChain, violations);
            report.append("\n=== AI改进建议 ===\n");
            report.append(aiDocument);
        } catch (Exception e) {
            report.append("\n=== 改进建议 ===\n");
            report.append("请根据上述问题逐一修改代码，确保符合云开发范式规范。");
        }
        
        return report.toString();
    }
    
    /**
     * 显示检查结果
     */
    private void showResults(MethodCallChain callChain, List<RuleViolation> violations, String report) {
        // 使用新的结果展示对话框
        CheckResultDialog dialog = new CheckResultDialog(callChain, violations, report);
        dialog.show();
    }
    
    /**
     * 显示信息对话框
     */
    private void showInfoDialog(String message, String title) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * 显示错误对话框
     */
    private void showErrorDialog(String message, String title) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            showErrorDialog("无法获取当前项目", "错误");
            return;
        }
        
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            showErrorDialog("请在编辑器中选择一个方法", "错误");
            return;
        }
        
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (psiFile == null) {
            showErrorDialog("无法获取当前文件信息", "错误");
            return;
        }
        
        // 获取光标位置的元素
        int offset = editor.getCaretModel().getOffset();
        PsiElement element = psiFile.findElementAt(offset);
        
        if (element == null) {
            showErrorDialog("请将光标放在方法内部", "错误");
            return;
        }
        
        // 查找包含当前元素的方法
        PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
        if (method == null) {
            showErrorDialog("请将光标放在方法内部", "错误");
            return;
        }
        
        // 获取类信息
        PsiClass psiClass = PsiTreeUtil.getParentOfType(method, PsiClass.class);
        if (psiClass == null) {
            showErrorDialog("无法获取类信息", "错误");
            return;
        }
        
        String methodName = method.getName();
        String className = psiClass.getQualifiedName();
        
        if (className == null) {
            className = psiClass.getName();
        }
        
        // 执行检查
        performCheck(methodName, className, project);
    }
}