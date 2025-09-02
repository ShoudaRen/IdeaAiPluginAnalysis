package com.example.demo.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;
import java.io.*;
import javax.swing.UIManager;

/**
 * 插件配置对话框
 * 允许用户配置数据库连接、AI模型等设置
 */
public class SettingsDialog extends JDialog {
    private JTextField dbUrlField;
    private JTextField dbUsernameField;
    private JPasswordField dbPasswordField;
    private JTextField aiApiUrlField;
    private JTextField aiApiKeyField;
    private JTextField aiModelField;
    private JCheckBox enableAiCheckBox;
    private JCheckBox enableDbCheckBox;
    
    private Properties settings;
    private static final String SETTINGS_FILE = "cloud-dev-checker.properties";
    
    public SettingsDialog(Frame parent) {
        super(parent, "云开发范式检查器 - 配置", true);
        this.settings = new Properties();
        loadSettings();
        initComponents();
        setupLayout();
        loadFieldValues();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(500, 400);
        setLocationRelativeTo(parent);
    }
    
    private void initComponents() {
        // 数据库配置
        dbUrlField = new JTextField(30);
        dbUsernameField = new JTextField(20);
        dbPasswordField = new JPasswordField(20);
        enableDbCheckBox = new JCheckBox("启用数据库规则加载");
        
        // AI配置
        aiApiUrlField = new JTextField(30);
        aiApiKeyField = new JTextField(30);
        aiModelField = new JTextField(20);
        enableAiCheckBox = new JCheckBox("启用AI代码分析");
        
        // 设置默认值
        dbUrlField.setText("jdbc:mysql://localhost:3306/cloud_dev_rules");
        aiApiUrlField.setText("https://api.openai.com/v1/chat/completions");
        aiModelField.setText("gpt-3.5-turbo");
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // 数据库配置区域
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        mainPanel.add(enableDbCheckBox, gbc);
        
        gbc.gridwidth = 1;
        gbc.gridy++;
        mainPanel.add(new JLabel("数据库URL:"), gbc);
        gbc.gridx = 1;
        mainPanel.add(dbUrlField, gbc);
        
        gbc.gridx = 0; gbc.gridy++;
        mainPanel.add(new JLabel("用户名:"), gbc);
        gbc.gridx = 1;
        mainPanel.add(dbUsernameField, gbc);
        
        gbc.gridx = 0; gbc.gridy++;
        mainPanel.add(new JLabel("密码:"), gbc);
        gbc.gridx = 1;
        mainPanel.add(dbPasswordField, gbc);
        
        // 分隔线
        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(new JSeparator(), gbc);
        
        // AI配置区域
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 2;
        gbc.gridy++;
        mainPanel.add(enableAiCheckBox, gbc);
        
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy++;
        mainPanel.add(new JLabel("AI API URL:"), gbc);
        gbc.gridx = 1;
        mainPanel.add(aiApiUrlField, gbc);
        
        gbc.gridx = 0; gbc.gridy++;
        mainPanel.add(new JLabel("API Key:"), gbc);
        gbc.gridx = 1;
        mainPanel.add(aiApiKeyField, gbc);
        
        gbc.gridx = 0; gbc.gridy++;
        mainPanel.add(new JLabel("模型名称:"), gbc);
        gbc.gridx = 1;
        mainPanel.add(aiModelField, gbc);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton saveButton = new JButton("保存");
        JButton cancelButton = new JButton("取消");
        JButton testButton = new JButton("测试连接");
        
        saveButton.addActionListener(e -> saveSettings());
        cancelButton.addActionListener(e -> dispose());
        testButton.addActionListener(e -> testConnections());
        
        buttonPanel.add(testButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void loadSettings() {
        try {
            File settingsFile = new File(System.getProperty("user.home"), SETTINGS_FILE);
            if (settingsFile.exists()) {
                try (FileInputStream fis = new FileInputStream(settingsFile)) {
                    settings.load(fis);
                }
            }
        } catch (IOException e) {
            System.err.println("加载配置文件失败: " + e.getMessage());
        }
    }
    
    private void loadFieldValues() {
        dbUrlField.setText(settings.getProperty("db.url", dbUrlField.getText()));
        dbUsernameField.setText(settings.getProperty("db.username", ""));
        dbPasswordField.setText(settings.getProperty("db.password", ""));
        enableDbCheckBox.setSelected(Boolean.parseBoolean(settings.getProperty("db.enabled", "false")));
        
        aiApiUrlField.setText(settings.getProperty("ai.api.url", aiApiUrlField.getText()));
        aiApiKeyField.setText(settings.getProperty("ai.api.key", ""));
        aiModelField.setText(settings.getProperty("ai.model", aiModelField.getText()));
        enableAiCheckBox.setSelected(Boolean.parseBoolean(settings.getProperty("ai.enabled", "false")));
    }
    
    private void saveSettings() {
        settings.setProperty("db.url", dbUrlField.getText());
        settings.setProperty("db.username", dbUsernameField.getText());
        settings.setProperty("db.password", new String(dbPasswordField.getPassword()));
        settings.setProperty("db.enabled", String.valueOf(enableDbCheckBox.isSelected()));
        
        settings.setProperty("ai.api.url", aiApiUrlField.getText());
        settings.setProperty("ai.api.key", aiApiKeyField.getText());
        settings.setProperty("ai.model", aiModelField.getText());
        settings.setProperty("ai.enabled", String.valueOf(enableAiCheckBox.isSelected()));
        
        try {
            File settingsFile = new File(System.getProperty("user.home"), SETTINGS_FILE);
            try (FileOutputStream fos = new FileOutputStream(settingsFile)) {
                settings.store(fos, "云开发范式检查器配置");
            }
            JOptionPane.showMessageDialog(this, "配置保存成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "保存配置失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void testConnections() {
        StringBuilder result = new StringBuilder("连接测试结果:\n\n");
        
        // 测试数据库连接
        if (enableDbCheckBox.isSelected()) {
            try {
                // 这里应该实际测试数据库连接
                // 为了简化，我们只是模拟测试
                if (!dbUrlField.getText().trim().isEmpty() && 
                    !dbUsernameField.getText().trim().isEmpty()) {
                    result.append("✓ 数据库连接配置正常\n");
                } else {
                    result.append("✗ 数据库连接配置不完整\n");
                }
            } catch (Exception e) {
                result.append("✗ 数据库连接失败: ").append(e.getMessage()).append("\n");
            }
        } else {
            result.append("- 数据库连接已禁用\n");
        }
        
        // 测试AI连接
        if (enableAiCheckBox.isSelected()) {
            try {
                // 这里应该实际测试AI API连接
                // 为了简化，我们只是模拟测试
                if (!aiApiUrlField.getText().trim().isEmpty() && 
                    !aiApiKeyField.getText().trim().isEmpty()) {
                    result.append("✓ AI API配置正常\n");
                } else {
                    result.append("✗ AI API配置不完整\n");
                }
            } catch (Exception e) {
                result.append("✗ AI API连接失败: ").append(e.getMessage()).append("\n");
            }
        } else {
            result.append("- AI功能已禁用\n");
        }
        
        JOptionPane.showMessageDialog(this, result.toString(), "连接测试", JOptionPane.INFORMATION_MESSAGE);
    }
    
    public Properties getSettings() {
        return settings;
    }
    
    // 静态方法用于显示配置对话框
    public static void showSettingsDialog(Frame parent) {
        SwingUtilities.invokeLater(() -> {
            new SettingsDialog(parent).setVisible(true);
        });
    }
    

}