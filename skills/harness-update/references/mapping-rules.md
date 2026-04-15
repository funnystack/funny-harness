# 变更类型与文档映射规则

## 路径匹配规则

文件路径匹配到以下模式时，触发对应的文档更新。

| 文件路径模式 | 目标文档 | 更新内容 |
|-------------|---------|---------|
| `*Controller.java` | `docs/architecture/api-design.md` | 新增/修改/删除的端点信息 |
| `*Service.java` / `*ServiceImpl.java` | `docs/architecture.md` (业务逻辑层) | 新增的业务方法或逻辑变更 |
| `*Mapper.java` / `*Dao.java` | `docs/architecture/database.md` | 新增的查询方法或数据访问模式 |
| `*Entity.java` / `*DO.java` / `*PO.java` | `docs/architecture/database.md` | 字段变更（新增、修改、删除） |
| `*DTO.java` / `*VO.java` / `*Param.java` | `docs/architecture/api-design.md` | 请求/响应结构变更 |
| `*Enum.java` / `*Constant*.java` | `docs/business-context.md` | 枚举值或常量定义变更 |
| `*Config.java` / `*Configuration.java` | `docs/architecture/tech-stack.md` | 新增的配置类或中间件配置 |
| `*Interceptor.java` / `*Filter.java` | `docs/architecture.md` | 新增的拦截器或过滤器 |
| `pom.xml` / `build.gradle` | `docs/architecture/tech-stack.md` | 依赖变更（新增、升级、删除） |
| `src/main/java/**/new_package/**` | `docs/project-structure.md` | 新增包/目录结构 |

## 内容匹配规则

diff 内容包含以下关键词时，触发对应的文档更新。

| 关键词模式 | 目标文档 | 更新内容 |
|-----------|---------|---------|
| `import RedisTemplate` / `@Cacheable` | `docs/architecture/tech-stack.md` | 新增 Redis 缓存使用 |
| `import KafkaTemplate` / `@RabbitListener` | `docs/architecture/tech-stack.md` | 新增消息队列使用 |
| `@FeignClient` / `@DubboService` | `docs/architecture/tech-stack.md` | 新增 RPC 调用 |
| `@Transactional` | `docs/architecture.md` (事务章节) | 新增或修改事务边界 |
| `@DS` / `dynamic-datasource` | `docs/architecture/database.md` | 多数据源变更 |
| `@Scheduled` / `@XxlJob` | `docs/architecture/tech-stack.md` | 新增定时任务 |
| `@RestController` / `@Controller` | `docs/architecture/api-design.md` | 新增 Controller 类 |
| `CREATE TABLE` / `ALTER TABLE` | `docs/architecture/database.md` | 表结构变更 |
| `extends RuntimeException` | `docs/architecture.md` (异常处理) | 新增自定义异常 |

## 不触发更新的变更

以下变更类型不影响项目文档：

| 变更类型 | 说明 |
|---------|------|
| `*Test.java` | 测试文件变更 |
| `*Spec.java` | 测试规格文件 |
| `src/test/**` | 测试目录下的任何变更 |
| `*.md` | 文档文件本身的变更（避免循环） |
| `harness/**` | harness 目录下的变更 |
| `.claude/**` | Claude 配置变更 |
| `README.md` | README 变更 |

## 优先级排序

多个文档需要更新时，按以下优先级排序：

1. **P0** — 架构约束相关（分层违规、事务模式变更）
2. **P1** — API 契约相关（端点新增/修改、DTO 变更）
3. **P2** — 技术栈相关（依赖变更、中间件新增）
4. **P3** — 其他（项目结构、业务上下文）
