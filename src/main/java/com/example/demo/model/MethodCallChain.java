package com.example.demo.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 方法调用链路模型
 * 表示一个方法及其调用的所有方法的层次结构
 */
public class MethodCallChain {
    private MethodInfo rootMethod;
    private Map<Integer, List<MethodInfo>> callsByDepth;
    private List<MethodInfo> allMethods;
    
    public MethodCallChain() {
        this.callsByDepth = new HashMap<>();
        this.allMethods = new ArrayList<>();
    }
    
    public MethodInfo getRootMethod() {
        return rootMethod;
    }
    
    public void setRootMethod(MethodInfo rootMethod) {
        this.rootMethod = rootMethod;
        if (rootMethod != null) {
            this.allMethods.add(rootMethod);
        }
    }
    
    /**
     * 添加方法调用
     * @param method 被调用的方法
     * @param depth 调用深度
     */
    public void addMethodCall(MethodInfo method, int depth) {
        callsByDepth.computeIfAbsent(depth, k -> new ArrayList<>()).add(method);
        allMethods.add(method);
    }
    
    /**
     * 获取指定深度的方法调用
     */
    public List<MethodInfo> getMethodsAtDepth(int depth) {
        return callsByDepth.getOrDefault(depth, new ArrayList<>());
    }
    
    /**
     * 获取按深度分组的方法调用
     */
    public Map<Integer, List<MethodInfo>> getCallsByDepth() {
        return new HashMap<>(callsByDepth);
    }
    
    /**
     * 获取所有方法
     */
    public List<MethodInfo> getAllMethods() {
        return new ArrayList<>(allMethods);
    }
    
    /**
     * 获取调用链路的最大深度
     */
    public int getMaxDepth() {
        return callsByDepth.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
    }
    
    /**
     * 获取调用链路的总方法数
     */
    public int getTotalMethodCount() {
        return allMethods.size();
    }
    
    /**
     * 生成调用链路的文本表示
     */
    public String toTreeString() {
        StringBuilder sb = new StringBuilder();
        
        if (rootMethod != null) {
            sb.append("Root: ").append(rootMethod.getMethodSignature()).append("\n");
        }
        
        for (int depth = 1; depth <= getMaxDepth(); depth++) {
            List<MethodInfo> methods = getMethodsAtDepth(depth);
            for (MethodInfo method : methods) {
                // 添加缩进表示层级
                for (int i = 0; i < depth; i++) {
                    sb.append("  ");
                }
                sb.append("├─ ").append(method.getMethodSignature()).append("\n");
            }
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return "MethodCallChain{" +
                "rootMethod=" + (rootMethod != null ? rootMethod.getMethodName() : "null") +
                ", totalMethods=" + getTotalMethodCount() +
                ", maxDepth=" + getMaxDepth() +
                '}';
    }
}