package com.example.demo.service;

import com.example.demo.model.MethodCallChain;
import com.example.demo.model.MethodInfo;
import com.example.demo.model.RuleViolation;
import com.example.demo.util.DatabaseUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 云开发范式规则引擎
 * 负责加载和执行云开发范式规则，检查代码是否符合规范
 */
public class CloudDevelopmentRuleEngine {
    
    private final DatabaseUtil databaseUtil;
    private final Gson gson;
    private Map<String, Object> rules;
    
    public CloudDevelopmentRuleEngine() {
        this.databaseUtil = new DatabaseUtil();
        this.gson = new Gson();
        this.rules = new HashMap<String, Object>();
        loadRulesFromDatabase();
    }
    
    /**
     * 从数据库加载云开发范式规则
     */
    private void loadRulesFromDatabase() {
        String sql = "SELECT rule_type, rule_content FROM cloud_dev_rules WHERE is_active = 1";
        
        try (Connection conn = databaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                String ruleType = rs.getString("rule_type");
                String ruleContent = rs.getString("rule_content");
                
                Type type = new TypeToken<Map<String, Object>>(){}.getType();
                @SuppressWarnings("unchecked")
                Map<String, Object> ruleMap = gson.fromJson(ruleContent, type);
                rules.put(ruleType, ruleMap);
            }
            
        } catch (SQLException e) {
            // 如果数据库连接失败，使用默认规则
            loadDefaultRules();
        }
    }
    
    /**
     * 加载默认规则（当数据库不可用时）
     */
    private void loadDefaultRules() {
        // 命名规范
        Map<String, Object> namingRules = new HashMap<String, Object>();
        namingRules.put("method_naming_pattern", "^[a-z][a-zA-Z0-9]*$");
        namingRules.put("class_naming_pattern", "^[A-Z][a-zA-Z0-9]*$");
        namingRules.put("constant_naming_pattern", "^[A-Z][A-Z0-9_]*$");
        rules.put("naming", namingRules);
        
        // 层次架构规范
        Map<String, Object> layerRules = new HashMap<String, Object>();
        layerRules.put("controller_package_pattern", ".*\\.controller\\.*");
        layerRules.put("service_package_pattern", ".*\\.service\\.*");
        layerRules.put("dao_package_pattern", ".*\\.(dao|repository)\\.*");
        layerRules.put("controller_annotations", Arrays.asList("RestController", "Controller"));
        layerRules.put("service_annotations", Arrays.asList("Service", "Component"));
        rules.put("layer", layerRules);
        
        // 方法签名规范
        Map<String, Object> signatureRules = new HashMap<String, Object>();
        signatureRules.put("max_parameters", 5);
        signatureRules.put("required_controller_annotations", Arrays.asList("RequestMapping", "GetMapping", "PostMapping"));
        rules.put("signature", signatureRules);
    }
    
    /**
     * 检查方法调用链路是否符合云开发范式
     */
    public List<RuleViolation> checkCallChain(MethodCallChain callChain) {
        List<RuleViolation> violations = new ArrayList<>();
        
        // 检查根方法
        if (callChain.getRootMethod() != null) {
            violations.addAll(checkMethod(callChain.getRootMethod()));
        }
        
        // 检查调用链中的所有方法
        for (MethodInfo method : callChain.getAllMethods()) {
            violations.addAll(checkMethod(method));
        }
        
        // 检查层次调用规范
        violations.addAll(checkLayerCallPattern(callChain));
        
        return violations;
    }
    
    /**
     * 检查单个方法是否符合规范
     */
    private List<RuleViolation> checkMethod(MethodInfo method) {
        List<RuleViolation> violations = new ArrayList<>();
        
        // 检查命名规范
        violations.addAll(checkNamingRules(method));
        
        // 检查方法签名规范
        violations.addAll(checkSignatureRules(method));
        
        // 检查层次规范
        violations.addAll(checkLayerRules(method));
        
        return violations;
    }
    
    /**
     * 检查命名规范
     */
    private List<RuleViolation> checkNamingRules(MethodInfo method) {
        List<RuleViolation> violations = new ArrayList<>();
        @SuppressWarnings("unchecked")
        Map<String, Object> namingRules = (Map<String, Object>) rules.get("naming");
        
        if (namingRules != null) {
            String methodPattern = (String) namingRules.get("method_naming_pattern");
            if (methodPattern != null && !Pattern.matches(methodPattern, method.getMethodName())) {
                violations.add(new RuleViolation(
                    "NAMING_VIOLATION",
                    "方法名不符合命名规范",
                    method.getMethodSignature(),
                    "方法名应该以小写字母开头，使用驼峰命名法",
                    "high"
                ));
            }
        }
        
        return violations;
    }
    
    /**
     * 检查方法签名规范
     */
    private List<RuleViolation> checkSignatureRules(MethodInfo method) {
        List<RuleViolation> violations = new ArrayList<>();
        @SuppressWarnings("unchecked")
        Map<String, Object> signatureRules = (Map<String, Object>) rules.get("signature");
        
        if (signatureRules != null) {
            Integer maxParams = (Integer) signatureRules.get("max_parameters");
            if (maxParams != null && method.getParameters() != null && 
                method.getParameters().size() > maxParams) {
                violations.add(new RuleViolation(
                    "SIGNATURE_VIOLATION",
                    "方法参数过多",
                    method.getMethodSignature(),
                    "建议将参数封装为对象或拆分方法",
                    "medium"
                ));
            }
        }
        
        return violations;
    }
    
    /**
     * 检查层次规范
     */
    private List<RuleViolation> checkLayerRules(MethodInfo method) {
        List<RuleViolation> violations = new ArrayList<>();
        
        // 根据包名判断层次
        String packageName = method.getPackageName();
        if (packageName != null) {
            if (packageName.contains(".controller.")) {
                method.setLayerType("CONTROLLER");
            } else if (packageName.contains(".service.")) {
                method.setLayerType("SERVICE");
            } else if (packageName.contains(".dao.") || packageName.contains(".repository.")) {
                method.setLayerType("DAO");
            }
        }
        
        return violations;
    }
    
    /**
     * 检查层次调用模式（基于云开发范式4层架构）
     */
    private List<RuleViolation> checkLayerCallPattern(MethodCallChain callChain) {
        List<RuleViolation> violations = new ArrayList<>();
        
        // 构建调用关系图
        Map<String, Set<String>> callGraph = buildCallGraph(callChain);
        
        // 检查层间依赖规则
        for (MethodInfo method : callChain.getAllMethods()) {
            String callerLayer = method.getLayerType();
            String callerSignature = method.getMethodSignature();
            
            // 获取该方法直接调用的其他方法
            Set<String> directCalls = callGraph.get(callerSignature);
            if (directCalls == null) continue;
            
            for (String calledSignature : directCalls) {
                MethodInfo calledMethod = findMethodBySignature(callChain, calledSignature);
                if (calledMethod == null) continue;
                
                String calleeLayer = calledMethod.getLayerType();
                
                // 检查违规的层间调用
                String violationMessage = checkLayerDependency(callerLayer, calleeLayer);
                if (violationMessage != null) {
                    violations.add(new RuleViolation(
                        "LAYER_VIOLATION",
                        violationMessage,
                        callerSignature + " -> " + calledSignature,
                        getLayerDependencyAdvice(callerLayer, calleeLayer),
                        "high"
                    ));
                }
            }
        }
        
        return violations;
    }
    
    /**
     * 构建调用关系图
     */
    private Map<String, Set<String>> buildCallGraph(MethodCallChain callChain) {
        Map<String, Set<String>> graph = new HashMap<>();
        
        // 根据调用深度构建调用关系
        Map<Integer, List<MethodInfo>> callsByDepth = callChain.getCallsByDepth();
        
        for (int depth = 0; depth < callChain.getMaxDepth(); depth++) {
            List<MethodInfo> currentLevel = callsByDepth.get(depth);
            List<MethodInfo> nextLevel = callsByDepth.get(depth + 1);
            
            if (currentLevel != null && nextLevel != null) {
                for (MethodInfo caller : currentLevel) {
                    String callerSignature = caller.getMethodSignature();
                    Set<String> calls = graph.computeIfAbsent(callerSignature, k -> new HashSet<>());
                    
                    for (MethodInfo callee : nextLevel) {
                        calls.add(callee.getMethodSignature());
                    }
                }
            }
        }
        
        return graph;
    }
    
    /**
     * 根据方法签名查找方法信息
     */
    private MethodInfo findMethodBySignature(MethodCallChain callChain, String signature) {
        for (MethodInfo method : callChain.getAllMethods()) {
            if (signature.equals(method.getMethodSignature())) {
                return method;
            }
        }
        return null;
    }
    
    /**
     * 检查层间依赖是否合规
     * 返回违规描述，null表示合规
     */
    private String checkLayerDependency(String callerLayer, String calleeLayer) {
        // 云开发范式4层架构依赖规则：
        // 1. 适配器层 -> 应用层
        // 2. 应用层 -> 领域层
        // 3. 基础设施层 -> 领域层
        // 4. 所有层都可以依赖基础设施层的工具类和通用类
        
        if ("ADAPTER".equals(callerLayer)) {
            if (!"APPLICATION".equals(calleeLayer) && !"INFRASTRUCTURE".equals(calleeLayer)) {
                return "适配器层违规调用：适配器层只能调用应用层或基础设施层的工具类";
            }
        } else if ("APPLICATION".equals(callerLayer)) {
            if (!"DOMAIN".equals(calleeLayer) && !"INFRASTRUCTURE".equals(calleeLayer)) {
                return "应用层违规调用：应用层只能调用领域层或基础设施层";
            }
        } else if ("DOMAIN".equals(callerLayer)) {
            if ("ADAPTER".equals(calleeLayer) || "APPLICATION".equals(calleeLayer)) {
                return "领域层违规调用：领域层不能调用适配器层或应用层";
            }
        } else if ("INFRASTRUCTURE".equals(callerLayer)) {
            if ("ADAPTER".equals(calleeLayer) || "APPLICATION".equals(calleeLayer)) {
                return "基础设施层违规调用：基础设施层不能调用适配器层或应用层";
            }
        }
        
        return null; // 合规
    }
    
    /**
     * 获取层间依赖建议
     */
    private String getLayerDependencyAdvice(String callerLayer, String calleeLayer) {
        if ("ADAPTER".equals(callerLayer)) {
            return "适配器层应该调用应用层进行业务编排，通过应用层间接访问其他层";
        } else if ("APPLICATION".equals(callerLayer)) {
            return "应用层应该调用领域层处理核心业务逻辑，通过领域层的support接口访问基础设施层";
        } else if ("DOMAIN".equals(callerLayer)) {
            return "领域层应该通过support接口定义依赖，由基础设施层实现具体功能";
        } else if ("INFRASTRUCTURE".equals(callerLayer)) {
            return "基础设施层应该实现领域层定义的support接口，不应该主动调用上层";
        }
        
        return "请遵循云开发范式的层间依赖规则";
    }
}