package com.example.demo.model;

/**
 * 规则违规模型
 * 表示代码中违反云开发范式的具体问题
 */
public class RuleViolation {
    private String violationType;    // 违规类型
    private String description;      // 违规描述
    private String location;         // 违规位置（方法签名等）
    private String suggestion;       // 修改建议
    private String severity;         // 严重程度：high, medium, low
    private String ruleReference;    // 规则参考
    
    public RuleViolation() {}
    
    public RuleViolation(String violationType, String description, String location, 
                        String suggestion, String severity) {
        this.violationType = violationType;
        this.description = description;
        this.location = location;
        this.suggestion = suggestion;
        this.severity = severity;
    }
    
    // Getters and Setters
    public String getViolationType() {
        return violationType;
    }
    
    public void setViolationType(String violationType) {
        this.violationType = violationType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public String getSuggestion() {
        return suggestion;
    }
    
    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }
    
    public String getSeverity() {
        return severity;
    }
    
    public void setSeverity(String severity) {
        this.severity = severity;
    }
    
    public String getRuleReference() {
        return ruleReference;
    }
    
    public void setRuleReference(String ruleReference) {
        this.ruleReference = ruleReference;
    }
    
    /**
     * 获取严重程度的数值表示（用于排序）
     */
    public int getSeverityLevel() {
        switch (severity != null ? severity.toLowerCase() : "low") {
            case "high": return 3;
            case "medium": return 2;
            case "low": return 1;
            default: return 1;
        }
    }
    
    /**
     * 生成违规报告的文本格式
     */
    public String toReportString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(severity.toUpperCase()).append("] ");
        sb.append(description).append("\n");
        sb.append("位置: ").append(location).append("\n");
        sb.append("建议: ").append(suggestion).append("\n");
        if (ruleReference != null) {
            sb.append("规则参考: ").append(ruleReference).append("\n");
        }
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return "RuleViolation{" +
                "violationType='" + violationType + '\'' +
                ", description='" + description + '\'' +
                ", location='" + location + '\'' +
                ", severity='" + severity + '\'' +
                '}';
    }
}