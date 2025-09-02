# 云开发范式检查插件

## 项目简介

这是一个IntelliJ IDEA插件，用于检查Java代码是否符合云开发范式规范。插件通过分析方法调用链路，结合规则引擎和AI大模型，自动识别代码中不符合规范的地方，并提供具体的修改建议。

## 主要功能

### 1. 方法调用链路分析
- 自动分析方法的调用关系
- 构建完整的调用链路树
- 支持多层级调用追踪（最大深度5层）

### 2. 云开发范式规则检查
- **命名规范检查**：方法名、类名、常量命名规范
- **层次架构检查**：Controller -> Service -> DAO 调用规范
- **方法签名检查**：参数数量、返回类型规范
- **注解使用检查**：Spring注解正确使用
- **跨层调用检查**：防止Controller直接调用DAO等违规操作

### 3. AI智能分析
- 集成大模型API进行深度代码分析
- 生成个性化的改进建议
- 支持复杂场景的规范判断

### 4. 详细报告生成
- 生成完整的检查报告
- 按严重程度分类显示问题
- 提供具体的修改建议和最佳实践

## 使用方法

### 1. 安装插件
1. 下载插件jar包
2. 在IDEA中选择 `File -> Settings -> Plugins`
3. 点击齿轮图标，选择 `Install Plugin from Disk`
4. 选择下载的jar包进行安装
5. 重启IDEA

### 2. 配置数据库（可选）
1. 创建MySQL数据库 `cloud_dev_rules`
2. 修改 `src/main/resources/database.properties` 中的数据库连接信息
3. 或者设置环境变量：
   - `DB_URL`: 数据库连接URL
   - `DB_USERNAME`: 数据库用户名
   - `DB_PASSWORD`: 数据库密码

### 3. 配置AI服务（可选）
设置环境变量：
- `AI_API_URL`: AI服务API地址
- `AI_API_KEY`: AI服务API密钥

### 4. 使用插件
1. 在Java文件中，将光标放在要检查的方法内部
2. 右键点击，选择 "检查云开发规范"
3. 等待分析完成，查看检查报告

## 技术架构

### 核心组件

1. **CodeStandardCheckAction**: 右键菜单入口，协调各个服务组件
2. **CallChainAnalyzer**: 方法调用链路分析器，使用PSI API分析代码结构
3. **CloudDevelopmentRuleEngine**: 规则引擎，执行云开发范式规则检查
4. **AIIntegrationService**: AI集成服务，调用大模型API进行智能分析
5. **DatabaseUtil**: 数据库工具类，管理规则数据的存储和加载

### 数据模型

1. **MethodInfo**: 方法信息模型，包含方法签名、参数、注解等信息
2. **MethodCallChain**: 方法调用链路模型，表示完整的调用关系
3. **RuleViolation**: 规则违规模型，描述具体的违规问题和建议

### 技术栈

- **开发语言**: Java 17
- **插件框架**: IntelliJ Platform SDK
- **数据库**: MySQL + HikariCP连接池
- **HTTP客户端**: OkHttp
- **JSON处理**: Gson
- **日志**: SLF4J + Logback

## 云开发范式规范

### 1. 命名规范
- 方法名：小写字母开头，驼峰命名法
- 类名：大写字母开头，驼峰命名法
- 常量：全大写，下划线分隔

### 2. 层次架构
- **Controller层**: 处理HTTP请求，调用Service层
- **Service层**: 业务逻辑处理，调用DAO层
- **DAO层**: 数据访问，与数据库交互
- **禁止跨层调用**: Controller不能直接调用DAO

### 3. 方法签名
- 参数数量不超过5个
- 返回类型明确
- 合理使用泛型

### 4. 注解使用
- Controller层：`@RestController`、`@Controller`
- Service层：`@Service`、`@Component`
- DAO层：`@Repository`
- 请求映射：`@RequestMapping`、`@GetMapping`、`@PostMapping`等

## 项目问题分析与改进

### 已识别的问题

#### 1. 技术实现问题
- **PSI API集成不完整**: 原始版本只使用模拟数据，无法进行真实的代码分析
- **AI提示词过长**: 100多页的规范文档作为提示词会导致API调用失败
- **缺乏缓存机制**: 每次检查都重新分析，性能较差
- **错误处理不完善**: 当AI服务不可用时用户体验较差

#### 2. 架构设计问题
- **缺乏智能提示词管理**: 无法根据检查内容动态选择相关规范
- **结果展示过于简单**: 只使用简单的消息框，缺乏交互性
- **配置复杂**: 需要手动配置多个服务

#### 3. 用户体验问题
- **缺乏进度提示**: 长时间分析时用户不知道进度
- **结果展示不直观**: 无法快速定位问题
- **缺乏导出功能**: 无法保存检查报告

### 已实施的改进方案

#### 1. 完善PSI API集成
- ✅ 重写 `CallChainAnalyzer` 类，使用真实的PSI API分析代码
- ✅ 添加循环调用检测，防止无限递归
- ✅ 实现智能层次推断，自动识别Controller/Service/DAO层
- ✅ 添加异常处理，PSI分析失败时回退到模拟模式

#### 2. 智能提示词管理
- ✅ 新增 `PromptManager` 服务，解决大文档提示词问题
- ✅ 根据违规类型动态选择相关规范片段
- ✅ 实现提示词长度控制，自动截断过长内容
- ✅ 添加规则片段缓存，提高性能

#### 3. 缓存机制优化
- ✅ 新增 `CacheService` 服务，缓存分析结果
- ✅ 支持多种缓存类型：调用链路、规则检查、AI分析
- ✅ 实现自动过期清理和LRU淘汰策略
- ✅ 添加缓存统计和监控功能

#### 4. 用户体验提升
- ✅ 新增 `CheckResultDialog` 对话框，提供更好的结果展示
- ✅ 实现多选项卡界面：概览、问题详情、调用链路、详细报告
- ✅ 添加进度管理器，显示分析进度
- ✅ 支持报告导出功能

#### 5. 错误处理和降级策略
- ✅ 完善异常处理机制，确保服务可用性
- ✅ 实现AI服务不可用时的降级策略
- ✅ 添加数据库连接失败时的默认规则支持

### 性能优化

#### 缓存策略
- 方法调用链路分析结果缓存30分钟
- AI分析结果缓存30分钟
- 规则检查结果缓存30分钟
- 自动清理过期缓存，最大缓存1000条记录

#### 智能分析
- 根据违规类型选择相关规范，减少提示词长度
- 调用链路分析限制最大深度为5层
- 每层最多显示3个方法调用，避免信息过载

## 扩展开发

### 添加新的规则

1. 在数据库中添加新规则：
```sql
INSERT INTO cloud_dev_rules (rule_type, rule_name, rule_content, rule_summary, description) 
VALUES ('custom', '自定义规则', '{"pattern": "规则内容"}', '规则摘要', '规则描述');
```

2. 在 `CloudDevelopmentRuleEngine` 中添加检查逻辑

3. 在 `PromptManager` 中添加规则摘要支持

### 自定义AI提示词

1. 修改 `PromptManager.getDefaultRuleSummary()` 方法添加新的规则摘要
2. 在数据库中配置 `rule_summary` 字段
3. 使用 `PromptManager.buildSmartPrompt()` 方法构建智能提示词

### 添加新的缓存类型

1. 在 `CacheService.CacheType` 枚举中添加新类型
2. 实现对应的缓存和获取方法
3. 在统计信息中添加新类型的计数

## 常见问题

### Q: 插件无法连接数据库怎么办？
A: 插件会自动使用内置的默认规则，不影响基本功能。检查数据库配置或网络连接。

### Q: AI分析功能不可用？
A: 检查AI API配置，确保API密钥正确且有足够的配额。插件会继续使用规则引擎进行检查。

### Q: 如何添加自定义规则？
A: 可以通过数据库添加规则，或者修改 `loadDefaultRules()` 方法添加内置规则。

### Q: 插件性能如何优化？
A: 插件已内置缓存机制，会自动缓存分析结果30分钟。可以通过调整 `CallChainAnalyzer` 的 `maxDepth` 参数来控制分析深度。

### Q: 为什么AI提示词会被截断？
A: 为了避免API调用失败，插件会自动控制提示词长度在4000字符以内。这是通过智能选择相关规范片段实现的。

### Q: 如何查看缓存统计信息？
A: 可以通过 `CacheService.getInstance().getCacheStats()` 方法获取缓存统计信息。

### Q: 插件支持哪些IDE版本？
A: 插件支持IntelliJ IDEA 2023.2.5及以上版本，兼容性范围：232-242.*

## 技术架构更新

### 新增组件

1. **PromptManager**: 智能提示词管理服务
   - 根据违规类型动态选择相关规范
   - 控制提示词长度，避免API调用失败
   - 支持规则片段缓存

2. **CacheService**: 缓存管理服务
   - 多类型缓存支持（调用链路、规则检查、AI分析）
   - 自动过期清理和LRU淘汰
   - 缓存统计和监控

3. **CheckResultDialog**: 结果展示对话框
   - 多选项卡界面设计
   - 交互式问题展示
   - 报告导出功能

### 改进的组件

1. **CallChainAnalyzer**: 完善PSI API集成
   - 真实代码分析替代模拟数据
   - 循环调用检测
   - 智能层次推断

2. **AIIntegrationService**: 智能提示词支持
   - 集成PromptManager
   - 优化API调用策略
   - 改进错误处理

3. **CodeStandardCheckAction**: 用户体验优化
   - 进度管理器集成
   - 缓存机制支持
   - 异步处理优化

## 贡献指南

1. Fork 项目
2. 创建特性分支
3. 提交更改
4. 推送到分支
5. 创建 Pull Request

## 许可证

本项目采用 MIT 许可证。

## 联系方式

如有问题或建议，请通过以下方式联系：
- 邮箱: support@yourcompany.com
- 项目地址: https://github.com/yourcompany/cloud-dev-standard-checker