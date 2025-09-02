package com.example.demo.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * 数据库连接工具类
 * 使用HikariCP连接池管理数据库连接
 */
public class DatabaseUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseUtil.class);
    private static HikariDataSource dataSource;
    
    static {
        initializeDataSource();
    }
    
    /**
     * 初始化数据源
     */
    private static void initializeDataSource() {
        try {
            HikariConfig config = new HikariConfig();
            
            // 从配置文件或环境变量读取数据库配置
            Properties props = loadDatabaseProperties();
            
            config.setJdbcUrl(props.getProperty("db.url", "jdbc:mysql://localhost:3306/cloud_dev_rules"));
            config.setUsername(props.getProperty("db.username", "root"));
            config.setPassword(props.getProperty("db.password", "shouda"));
            config.setDriverClassName(props.getProperty("db.driver", "com.mysql.cj.jdbc.Driver"));
            
            // 连接池配置
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            
            // 连接测试
            config.setConnectionTestQuery("SELECT 1");
            
            dataSource = new HikariDataSource(config);
            
            logger.info("数据库连接池初始化成功");
            
        } catch (Exception e) {
            logger.error("数据库连接池初始化失败: {}", e.getMessage());
            // 不抛出异常，允许插件在没有数据库的情况下使用默认规则
        }
    }
    
    /**
     * 加载数据库配置
     */
    private static Properties loadDatabaseProperties() {
        Properties props = new Properties();
        
        try {
            // 尝试从类路径加载配置文件
            props.load(DatabaseUtil.class.getClassLoader().getResourceAsStream("database.properties"));
        } catch (Exception e) {
            logger.warn("无法加载database.properties文件，使用默认配置");
        }
        
        // 环境变量覆盖配置文件
        String dbUrl = System.getenv("DB_URL");
        if (dbUrl != null) {
            props.setProperty("db.url", dbUrl);
        }
        
        String dbUsername = System.getenv("DB_USERNAME");
        if (dbUsername != null) {
            props.setProperty("db.username", dbUsername);
        }
        
        String dbPassword = System.getenv("DB_PASSWORD");
        if (dbPassword != null) {
            props.setProperty("db.password", dbPassword);
        }
        
        return props;
    }
    
    /**
     * 获取数据库连接
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("数据源未初始化");
        }
        return dataSource.getConnection();
    }
    
    /**
     * 测试数据库连接
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            logger.error("数据库连接测试失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 关闭数据源
     */
    public static void closeDataSource() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("数据库连接池已关闭");
        }
    }
    
    /**
     * 创建数据库表（如果不存在）
     */
    public void createTablesIfNotExists() {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS cloud_dev_rules (
                id INT AUTO_INCREMENT PRIMARY KEY,
                rule_type VARCHAR(50) NOT NULL COMMENT '规则类型',
                rule_name VARCHAR(100) NOT NULL COMMENT '规则名称',
                rule_content TEXT NOT NULL COMMENT '规则内容(JSON格式)',
                description TEXT COMMENT '规则描述',
                is_active BOOLEAN DEFAULT TRUE COMMENT '是否启用',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_rule_type (rule_type),
                INDEX idx_is_active (is_active)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='云开发范式规则表';
            """;
        
        try (Connection conn = getConnection();
             var stmt = conn.createStatement()) {
            
            stmt.execute(createTableSQL);
            logger.info("数据库表创建成功");
            
        } catch (SQLException e) {
            logger.error("创建数据库表失败: {}", e.getMessage());
        }
    }
}