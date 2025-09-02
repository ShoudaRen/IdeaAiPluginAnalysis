package com.example.demo.test;

import com.example.demo.service.CloudDevelopmentRuleEngine;
import com.example.demo.service.CacheService;
import com.example.demo.service.PromptManager;

/**
 * 简单测试类
 * 用于验证核心功能是否正常工作
 */
public class SimpleTest {
    
    public static void main(String[] args) {
        System.out.println("=== 云开发范式检查器插件简单测试 ===");
        
        try {
            // 测试规则引擎
            System.out.println("1. 测试规则引擎...");
            CloudDevelopmentRuleEngine engine = new CloudDevelopmentRuleEngine();
            System.out.println("   ✓ 规则引擎初始化成功");
            
            // 测试缓存服务
            System.out.println("2. 测试缓存服务...");
            CacheService cache = CacheService.getInstance();
            System.out.println("   ✓ 缓存服务初始化成功");
            System.out.println("   - 缓存统计: " + cache.getCacheStats());
            
            // 测试提示词管理器
            System.out.println("3. 测试提示词管理器...");
            PromptManager promptManager = new PromptManager();
            System.out.println("   ✓ 提示词管理器初始化成功");
            
            System.out.println("\n=== 所有核心组件测试通过 ===");
            
        } catch (Exception e) {
            System.out.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
