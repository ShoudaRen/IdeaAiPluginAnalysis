package com.example.demo.util;

import java.io.*;
import java.util.Properties;

/**
 * 配置管理器
 * 负责加载、保存和管理应用程序配置
 */
public class ConfigManager {
    private static ConfigManager instance;
    private Properties settings;
    private static final String SETTINGS_FILE = "cloud-dev-checker.properties";
    
    private ConfigManager() {
        this.settings = new Properties();
        loadSettings();
    }
    
    public static ConfigManager getInstance() {
        if (instance == null) {
            synchronized (ConfigManager.class) {
                if (instance == null) {
                    instance = new ConfigManager();
                }
            }
        }
        return instance;
    }
    
    private void loadSettings() {
        try {
            File settingsFile = new File(System.getProperty("user.home"), SETTINGS_FILE);
            if (settingsFile.exists()) {
                try (FileInputStream fis = new FileInputStream(settingsFile)) {
                    settings.load(fis);
                }
            } else {
                // 设置默认值
                setDefaultSettings();
            }
        } catch (IOException e) {
            System.err.println("加载配置文件失败: " + e.getMessage());
            setDefaultSettings();
        }
    }
    
    private void setDefaultSettings() {
        settings.setProperty("db.url", "jdbc:mysql://localhost:3306/cloud_dev_rules");
        settings.setProperty("db.username", "roo");
        settings.setProperty("db.password", "");
        settings.setProperty("db.enabled", "false");
        
        settings.setProperty("ai.api.url", "https://api.openai.com/v1/chat/completions");
        settings.setProperty("ai.api.key", "");
        settings.setProperty("ai.model", "gpt-3.5-turbo");
        settings.setProperty("ai.enabled", "false");
        
        settings.setProperty("report.output.dir", System.getProperty("user.home") + "/cloud-dev-reports");
        settings.setProperty("report.format", "html");
    }
    
    public void saveSettings() throws IOException {
        File settingsFile = new File(System.getProperty("user.home"), SETTINGS_FILE);
        try (FileOutputStream fos = new FileOutputStream(settingsFile)) {
            settings.store(fos, "云开发范式检查器配置");
        }
    }
    
    // 数据库配置
    public String getDatabaseUrl() {
        return settings.getProperty("db.url", "");
    }
    
    public String getDatabaseUsername() {
        return settings.getProperty("db.username", "");
    }
    
    public String getDatabasePassword() {
        return settings.getProperty("db.password", "");
    }
    
    public boolean isDatabaseEnabled() {
        return Boolean.parseBoolean(settings.getProperty("db.enabled", "false"));
    }

    // AI配置
    public String getAiApiUrl() {
        return settings.getProperty("ai.api.url", "");
    }
    
    public String getAiApiKey() {
        return settings.getProperty("ai.api.key", "");
    }
    
    public String getAiModel() {
        return settings.getProperty("ai.model", "deepseek/deepseek-chat-v3.1:free");
    }
    
    public boolean isAiEnabled() {
        return Boolean.parseBoolean(settings.getProperty("ai.enabled", "false"));
    }
    
    public void setAiConfig(String apiUrl, String apiKey, String model, boolean enabled) {
        settings.setProperty("ai.api.url", apiUrl);
        settings.setProperty("ai.api.key", apiKey);
        settings.setProperty("ai.model", model);
        settings.setProperty("ai.enabled", String.valueOf(enabled));
    }
    
    // 报告配置
    public String getReportOutputDir() {
        return settings.getProperty("report.output.dir", System.getProperty("user.home") + "/cloud-dev-reports");
    }
    
    public String getReportFormat() {
        return settings.getProperty("report.format", "html");
    }
    
    public void setReportConfig(String outputDir, String format) {
        settings.setProperty("report.output.dir", outputDir);
        settings.setProperty("report.format", format);
    }
    
    // 通用配置访问
    public String getProperty(String key) {
        return settings.getProperty(key);
    }
    
    public String getProperty(String key, String defaultValue) {
        return settings.getProperty(key, defaultValue);
    }
    
    public void setProperty(String key, String value) {
        settings.setProperty(key, value);
    }
    
    public Properties getAllSettings() {
        return new Properties(settings);
    }
    
    public void updateSettings(Properties newSettings) {
        settings.putAll(newSettings);
    }
    
    // 验证配置
    public boolean validateDatabaseConfig() {
        if (!isDatabaseEnabled()) {
            return true; // 如果未启用，则认为配置有效
        }
        
        String url = getDatabaseUrl();
        String username = getDatabaseUsername();
        
        return url != null && !url.trim().isEmpty() && 
               username != null && !username.trim().isEmpty();
    }
    
    public boolean validateAiConfig() {
        if (!isAiEnabled()) {
            return true; // 如果未启用，则认为配置有效
        }
        
        String apiUrl = getAiApiUrl();
        String apiKey = getAiApiKey();
        String model = getAiModel();
        
        return apiUrl != null && !apiUrl.trim().isEmpty() && 
               apiKey != null && !apiKey.trim().isEmpty() && 
               model != null && !model.trim().isEmpty();
    }
    
    public boolean isConfigurationValid() {
        return validateDatabaseConfig() && validateAiConfig();
    }
    
    // 重新加载配置
    public void reloadSettings() {
        loadSettings();
    }
    
    // 重置为默认配置
    public void resetToDefaults() {
        settings.clear();
        setDefaultSettings();
    }
}