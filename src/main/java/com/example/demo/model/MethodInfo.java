package com.example.demo.model;

import java.util.List;

/**
 * 方法信息模型
 * 包含方法的基本信息，用于云开发范式检查
 */
public class MethodInfo {
    private String methodName;
    private String className;
    private String returnType;
    private List<String> parameters;
    private List<String> annotations;
    private String packageName;
    private String layerType; // 控制层、服务层、数据层等
    
    // Getters and Setters
    public String getMethodName() {
        return methodName;
    }
    
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
    
    public String getClassName() {
        return className;
    }
    
    public void setClassName(String className) {
        this.className = className;
        // 根据类名推断包名
        if (className != null && className.contains(".")) {
            int lastDotIndex = className.lastIndexOf(".");
            this.packageName = className.substring(0, lastDotIndex);
        }
    }
    
    public String getReturnType() {
        return returnType;
    }
    
    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }
    
    public List<String> getParameters() {
        return parameters;
    }
    
    public void setParameters(List<String> parameters) {
        this.parameters = parameters;
    }
    
    public List<String> getAnnotations() {
        return annotations;
    }
    
    public void setAnnotations(List<String> annotations) {
        this.annotations = annotations;
    }
    
    public String getPackageName() {
        return packageName;
    }
    
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
    
    public String getLayerType() {
        return layerType;
    }
    
    public void setLayerType(String layerType) {
        this.layerType = layerType;
    }
    
    /**
     * 生成方法签名字符串
     */
    public String getMethodSignature() {
        StringBuilder signature = new StringBuilder();
        signature.append(returnType).append(" ");
        signature.append(className).append(".");
        signature.append(methodName).append("(");
        
        if (parameters != null && !parameters.isEmpty()) {
            signature.append(String.join(", ", parameters));
        }
        
        signature.append(")");
        return signature.toString();
    }
    
    @Override
    public String toString() {
        return "MethodInfo{" +
                "methodName='" + methodName + '\'' +
                ", className='" + className + '\'' +
                ", returnType='" + returnType + '\'' +
                ", parameters=" + parameters +
                ", annotations=" + annotations +
                ", layerType='" + layerType + '\'' +
                '}';
    }
}