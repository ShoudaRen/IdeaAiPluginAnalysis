package com.example.demo.service;

import com.example.demo.model.MethodCallChain;
import com.example.demo.model.RuleViolation;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 缓存服务
 * 负责缓存方法调用链路分析结果和AI分析结果，提高性能
 */
public class CacheService {
    
    private static final CacheService instance = new CacheService();
    private final ConcurrentHashMap<String, CacheEntry> cache;
    private final ScheduledExecutorService scheduler;
    private final Gson gson;
    
    // 缓存配置
    private static final long DEFAULT_TTL = 30 * 60 * 1000; // 30分钟
    private static final long MAX_CACHE_SIZE = 1000; // 最大缓存条目数
    private static final long CLEANUP_INTERVAL = 10 * 60 * 1000; // 10分钟清理一次
    
    private CacheService() {
        this.cache = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.gson = new Gson();
        
        // 启动定期清理任务
        startCleanupTask();
    }
    
    public static CacheService getInstance() {
        return instance;
    }
    
    /**
     * 缓存方法调用链路分析结果
     */
    public void cacheCallChain(String methodKey, MethodCallChain callChain) {
        if (cache.size() >= MAX_CACHE_SIZE) {
            evictOldestEntries();
        }
        
        CacheEntry entry = new CacheEntry();
        entry.setType(CacheType.CALL_CHAIN);
        entry.setData(gson.toJson(callChain));
        entry.setTimestamp(System.currentTimeMillis());
        entry.setTtl(DEFAULT_TTL);
        
        cache.put("call_chain_" + methodKey, entry);
    }
    
    /**
     * 获取缓存的方法调用链路
     */
    public MethodCallChain getCachedCallChain(String methodKey) {
        CacheEntry entry = cache.get("call_chain_" + methodKey);
        if (entry != null && !entry.isExpired()) {
            Type type = new TypeToken<MethodCallChain>(){}.getType();
            return gson.fromJson(entry.getData(), type);
        }
        return null;
    }
    
    /**
     * 缓存AI分析结果
     */
    public void cacheAIAnalysis(String methodKey, List<RuleViolation> violations) {
        if (cache.size() >= MAX_CACHE_SIZE) {
            evictOldestEntries();
        }
        
        CacheEntry entry = new CacheEntry();
        entry.setType(CacheType.AI_ANALYSIS);
        entry.setData(gson.toJson(violations));
        entry.setTimestamp(System.currentTimeMillis());
        entry.setTtl(DEFAULT_TTL);
        
        cache.put("ai_analysis_" + methodKey, entry);
    }
    
    /**
     * 获取缓存的AI分析结果
     */
    public List<RuleViolation> getCachedAIAnalysis(String methodKey) {
        CacheEntry entry = cache.get("ai_analysis_" + methodKey);
        if (entry != null && !entry.isExpired()) {
            Type type = new TypeToken<List<RuleViolation>>(){}.getType();
            return gson.fromJson(entry.getData(), type);
        }
        return null;
    }
    
    /**
     * 缓存规则检查结果
     */
    public void cacheRuleCheck(String methodKey, List<RuleViolation> violations) {
        if (cache.size() >= MAX_CACHE_SIZE) {
            evictOldestEntries();
        }
        
        CacheEntry entry = new CacheEntry();
        entry.setType(CacheType.RULE_CHECK);
        entry.setData(gson.toJson(violations));
        entry.setTimestamp(System.currentTimeMillis());
        entry.setTtl(DEFAULT_TTL);
        
        cache.put("rule_check_" + methodKey, entry);
    }
    
    /**
     * 获取缓存的规则检查结果
     */
    public List<RuleViolation> getCachedRuleCheck(String methodKey) {
        CacheEntry entry = cache.get("rule_check_" + methodKey);
        if (entry != null && !entry.isExpired()) {
            Type type = new TypeToken<List<RuleViolation>>(){}.getType();
            return gson.fromJson(entry.getData(), type);
        }
        return null;
    }
    
    /**
     * 生成方法缓存键
     */
    public String generateMethodKey(String className, String methodName) {
        return className + "." + methodName;
    }
    
    /**
     * 检查是否有缓存
     */
    public boolean hasCachedResult(String methodKey) {
        return getCachedCallChain(methodKey) != null || 
               getCachedAIAnalysis(methodKey) != null || 
               getCachedRuleCheck(methodKey) != null;
    }
    
    /**
     * 清除特定方法的缓存
     */
    public void clearMethodCache(String methodKey) {
        cache.remove("call_chain_" + methodKey);
        cache.remove("ai_analysis_" + methodKey);
        cache.remove("rule_check_" + methodKey);
    }
    
    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        cache.clear();
    }
    
    /**
     * 获取缓存统计信息
     */
    public CacheStats getCacheStats() {
        CacheStats stats = new CacheStats();
        stats.setTotalEntries(cache.size());
        stats.setExpiredEntries(0);
        stats.setCallChainEntries(0);
        stats.setAiAnalysisEntries(0);
        stats.setRuleCheckEntries(0);
        
        for (CacheEntry entry : cache.values()) {
            if (entry.isExpired()) {
                stats.setExpiredEntries(stats.getExpiredEntries() + 1);
            }
            
            switch (entry.getType()) {
                case CALL_CHAIN:
                    stats.setCallChainEntries(stats.getCallChainEntries() + 1);
                    break;
                case AI_ANALYSIS:
                    stats.setAiAnalysisEntries(stats.getAiAnalysisEntries() + 1);
                    break;
                case RULE_CHECK:
                    stats.setRuleCheckEntries(stats.getRuleCheckEntries() + 1);
                    break;
            }
        }
        
        return stats;
    }
    
    /**
     * 启动定期清理任务
     */
    private void startCleanupTask() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                cleanupExpiredEntries();
            } catch (Exception e) {
                // 清理失败不影响主功能
            }
        }, CLEANUP_INTERVAL, CLEANUP_INTERVAL, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 清理过期条目
     */
    private void cleanupExpiredEntries() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    /**
     * 驱逐最旧的条目
     */
    private void evictOldestEntries() {
        if (cache.size() <= MAX_CACHE_SIZE * 0.8) {
            return; // 如果缓存大小在合理范围内，不进行清理
        }
        
        // 按时间戳排序，移除最旧的20%
        cache.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e1.getValue().getTimestamp(), e2.getValue().getTimestamp()))
                .limit((int) (MAX_CACHE_SIZE * 0.2))
                .forEach(entry -> cache.remove(entry.getKey()));
    }
    
    /**
     * 关闭缓存服务
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
    
    /**
     * 缓存条目
     */
    private static class CacheEntry {
        private CacheType type;
        private String data;
        private long timestamp;
        private long ttl;
        
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > ttl;
        }
        
        // Getters and Setters
        public CacheType getType() { return type; }
        public void setType(CacheType type) { this.type = type; }
        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        public long getTtl() { return ttl; }
        public void setTtl(long ttl) { this.ttl = ttl; }
    }
    
    /**
     * 缓存类型枚举
     */
    private enum CacheType {
        CALL_CHAIN,
        AI_ANALYSIS,
        RULE_CHECK
    }
    
    /**
     * 缓存统计信息
     */
    public static class CacheStats {
        private int totalEntries;
        private int expiredEntries;
        private int callChainEntries;
        private int aiAnalysisEntries;
        private int ruleCheckEntries;
        
        // Getters and Setters
        public int getTotalEntries() { return totalEntries; }
        public void setTotalEntries(int totalEntries) { this.totalEntries = totalEntries; }
        public int getExpiredEntries() { return expiredEntries; }
        public void setExpiredEntries(int expiredEntries) { this.expiredEntries = expiredEntries; }
        public int getCallChainEntries() { return callChainEntries; }
        public void setCallChainEntries(int callChainEntries) { this.callChainEntries = callChainEntries; }
        public int getAiAnalysisEntries() { return aiAnalysisEntries; }
        public void setAiAnalysisEntries(int aiAnalysisEntries) { this.aiAnalysisEntries = aiAnalysisEntries; }
        public int getRuleCheckEntries() { return ruleCheckEntries; }
        public void setRuleCheckEntries(int ruleCheckEntries) { this.ruleCheckEntries = ruleCheckEntries; }
        
        @Override
        public String toString() {
            return String.format("缓存统计: 总条目=%d, 过期=%d, 调用链路=%d, AI分析=%d, 规则检查=%d",
                    totalEntries, expiredEntries, callChainEntries, aiAnalysisEntries, ruleCheckEntries);
        }
    }
}
