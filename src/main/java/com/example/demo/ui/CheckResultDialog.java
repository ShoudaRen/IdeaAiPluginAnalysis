package com.example.demo.ui;

import com.example.demo.model.MethodCallChain;
import com.example.demo.model.RuleViolation;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * 检查结果展示对话框
 * 提供更好的用户体验来展示代码规范检查结果
 */
public class CheckResultDialog extends DialogWrapper {
    
    private final MethodCallChain callChain;
    private final List<RuleViolation> violations;
    private final String report;
    
    public CheckResultDialog(MethodCallChain callChain, List<RuleViolation> violations, String report) {
        super(true);
        this.callChain = callChain;
        this.violations = violations;
        this.report = report;
        
        setTitle("云开发规范检查结果");
        setResizable(true);
        init();
    }
    
    @Override
    protected JComponent createCenterPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setPreferredSize(new Dimension(800, 600));
        
        // 创建选项卡面板
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // 1. 概览选项卡
        tabbedPane.addTab("概览", createOverviewPanel());
        
        // 2. 问题详情选项卡
        tabbedPane.addTab("问题详情", createViolationsPanel());
        
        // 3. 调用链路选项卡
        tabbedPane.addTab("调用链路", createCallChainPanel());
        
        // 4. 详细报告选项卡
        tabbedPane.addTab("详细报告", createReportPanel());
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        return mainPanel;
    }
    
    /**
     * 创建概览面板
     */
    private JComponent createOverviewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        // 统计信息
        JPanel statsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // 检查方法
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        statsPanel.add(new JLabel("检查方法:"), gbc);
        gbc.gridx = 1;
        statsPanel.add(new JLabel(callChain.getRootMethod().getMethodSignature()), gbc);
        
        // 调用深度
        gbc.gridx = 0; gbc.gridy = 1;
        statsPanel.add(new JLabel("调用深度:"), gbc);
        gbc.gridx = 1;
        statsPanel.add(new JLabel(String.valueOf(callChain.getMaxDepth())), gbc);
        
        // 涉及方法数
        gbc.gridx = 0; gbc.gridy = 2;
        statsPanel.add(new JLabel("涉及方法数:"), gbc);
        gbc.gridx = 1;
        statsPanel.add(new JLabel(String.valueOf(callChain.getTotalMethodCount())), gbc);
        
        // 发现问题数
        gbc.gridx = 0; gbc.gridy = 3;
        statsPanel.add(new JLabel("发现问题数:"), gbc);
        gbc.gridx = 1;
        JLabel violationCountLabel = new JLabel(String.valueOf(violations.size()));
        if (violations.isEmpty()) {
            violationCountLabel.setForeground(Color.GREEN);
        } else {
            violationCountLabel.setForeground(Color.RED);
        }
        statsPanel.add(violationCountLabel, gbc);
        
        panel.add(statsPanel, BorderLayout.NORTH);
        
        // 总体评估
        JTextArea summaryArea = new JTextArea();
        summaryArea.setEditable(false);
        summaryArea.setBackground(panel.getBackground());
        summaryArea.setFont(summaryArea.getFont().deriveFont(Font.PLAIN, 12));
        
        if (violations.isEmpty()) {
            summaryArea.setText("✅ 恭喜！该方法调用链路完全符合云开发范式规范。\n\n" +
                    "所有检查项都通过了验证，代码质量良好。");
            summaryArea.setForeground(Color.GREEN);
        } else {
            summaryArea.setText("❌ 发现 " + violations.size() + " 个问题需要修复。\n\n" +
                    "请查看\"问题详情\"选项卡了解具体问题和修改建议。");
            summaryArea.setForeground(Color.RED);
        }
        
        panel.add(new JBScrollPane(summaryArea), BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * 创建问题详情面板
     */
    private JComponent createViolationsPanel() {
        if (violations.isEmpty()) {
            JPanel panel = new JPanel(new BorderLayout());
            JLabel label = new JLabel("✅ 没有发现任何问题！", SwingConstants.CENTER);
            label.setFont(label.getFont().deriveFont(Font.BOLD, 16));
            label.setForeground(Color.GREEN);
            panel.add(label, BorderLayout.CENTER);
            return panel;
        }
        
        // 创建表格
        String[] columnNames = {"类型", "描述", "位置", "严重程度", "建议"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        for (RuleViolation violation : violations) {
            Object[] row = {
                violation.getViolationType(),
                violation.getDescription(),
                violation.getLocation(),
                violation.getSeverity(),
                violation.getSuggestion()
            };
            model.addRow(row);
        }
        
        JBTable table = new JBTable(model);
        table.setRowHeight(30);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        
        // 设置列宽
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(2).setPreferredWidth(150);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);
        table.getColumnModel().getColumn(4).setPreferredWidth(200);
        
        return new JBScrollPane(table);
    }
    
    /**
     * 创建调用链路面板
     */
    private JComponent createCallChainPanel() {
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setText(callChain.toTreeString());
        
        return new JBScrollPane(textArea);
    }
    
    /**
     * 创建详细报告面板
     */
    private JComponent createReportPanel() {
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        textArea.setText(report);
        
        return new JBScrollPane(textArea);
    }
    
    @Override
    protected JComponent createSouthPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        // 导出报告按钮
        JButton exportButton = new JButton("导出报告");
        exportButton.addActionListener(e -> exportReport());
        panel.add(exportButton);
        
        // 重新检查按钮
        JButton recheckButton = new JButton("重新检查");
        recheckButton.addActionListener(e -> {
            close(OK_EXIT_CODE);
            // 这里可以触发重新检查
        });
        panel.add(recheckButton);
        
        // 关闭按钮
        JButton closeButton = new JButton("关闭");
        closeButton.addActionListener(e -> close(OK_EXIT_CODE));
        panel.add(closeButton);
        
        return panel;
    }
    
    /**
     * 导出报告
     */
    private void exportReport() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new java.io.File("云开发规范检查报告.txt"));
        
        if (fileChooser.showSaveDialog(getContentPanel()) == JFileChooser.APPROVE_OPTION) {
            try {
                java.io.File file = fileChooser.getSelectedFile();
                java.nio.file.Files.write(file.toPath(), report.getBytes("UTF-8"));
                JOptionPane.showMessageDialog(getContentPanel(), "报告已导出到: " + file.getAbsolutePath());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(getContentPanel(), "导出失败: " + e.getMessage(), 
                        "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
