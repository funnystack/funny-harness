---
name: harness-init-java
description: >
  Java 项目初始化脚手架，扫描项目并生成 AGENT.md + CLAUDE.md + docs/ 知识体系 + harness/ 自进化目录。
  覆盖项目结构、技术栈、API 设计模式与入口清单、数据库表字段推断（支持多数据源区分）、
  定时任务入口清单、数据访问模式、RPC调用方式推断（OpenFeign/Dubbo/RestTemplate/HttpClient）、
  事务模式、异常处理、隐性约定（hack/workaround/硬编码）、Git 历史考古、业务上下文提取。
  当用户说"初始化项目"、"生成 CLAUDE.md"、"项目脚手架"、"分析项目架构"、
  "harness init"、"项目上下文"、"我刚接手这个项目"、"帮我梳理一下这个项目"时触发。
  也适用于用户想了解一个陌生 Java 项目全貌、或者想为新项目建立 Claude 协作基线的场景。
  即使只是提到"这个项目结构是怎样的"或"帮我看看这个项目用了什么技术栈"也应触发。
---

# harness-init-java

你是一个 Java 项目分析脚手架。目标是扫描当前 Java 项目，建立一套结构化的项目知识体系，让 Claude 后续在这个项目中工作时能准确理解项目约束和风格。

## 为什么这件事重要

Claude 在一个新项目中工作时，最缺的不是代码能力，而是项目上下文。没有上下文，Claude 只能靠猜测，容易写出不符合项目风格的代码、踩到项目雷区、违反隐性约定。

这个 Skill 把项目初始化变成一个确定性流程，产出物不是一个大文件，而是一套分层知识体系。

## 文件创建映射

这个 Skill 会在目标项目中创建以下文件。执行前请先读取本 Skill 中对应的模板文件，然后根据扫描结果填充占位符后写入目标项目。

| 本 Skill 中的模板文件               | 目标项目中的位置                                 | 说明                               |
| ----------------------------------- | ------------------------------------------------ | ---------------------------------- |
| `AGENTS-template.md`                | `{项目根目录}/AGENTS.md`                         | 项目全景索引                       |
| `CLAUDE-project-template.md`        | `{项目根目录}/CLAUDE.md`                         | Claude 协作约束                    |
| `docs/project-structure.md`         | `{项目根目录}/docs/project-structure.md`         | 目录布局和模块关系                 |
| `docs/architecture.md`              | `{项目根目录}/docs/architecture.md`              | 核心约定大全（内联，不引用子文件） |
| `docs/architecture/tech-stack.md`   | `{项目根目录}/docs/architecture/tech-stack.md`   | 技术栈详情（按需参考）             |
| `docs/architecture/api-design.md`   | `{项目根目录}/docs/architecture/api-design.md`   | API 入口完整清单（按需参考）       |
| `docs/architecture/database.md`     | `{项目根目录}/docs/architecture/database.md`     | 数据库表字段清单（按需参考）       |
| `docs/architecture/jobs-design.md`  | `{项目根目录}/docs/architecture/jobs-design.md`  | 定时任务清单（按需参考）           |
| `docs/architecture/consumer.md`     | `{项目根目录}/docs/architecture/consumer.md`     | 消息消费者清单（按需参考）         |
| `docs/architecture/testing.md`      | `{项目根目录}/docs/architecture/testing.md`      | 测试规范（按需参考）               |
| `docs/architecture/development.md`  | `{项目根目录}/docs/architecture/development.md`  | 构建、测试、部署命令（按需参考）   |
| `docs/business-context.md`          | `{项目根目录}/docs/business-context.md`          | 业务上下文                         |
| `docs/implicit-contracts.md`        | `{项目根目录}/docs/implicit-contracts.md`        | 隐性约定                           |
| `harness/memory/episodic.md`        | `{项目根目录}/harness/memory/episodic.md`        | 情景记忆骨架                       |
| `harness/memory/procedural.md`      | `{项目根目录}/harness/memory/procedural.md`      | 程序记忆骨架                       |
| `harness/memory/lessons-learned.md` | `{项目根目录}/harness/memory/lessons-learned.md` | 失败经验骨架                       |
| `harness/trace/failures/`           | `{项目根目录}/harness/trace/failures/`           | 失败追踪目录                       |
| `scripts/validate.sh`               | `{项目根目录}/scripts/validate.sh`               | 统一验证管道                       |
| `scripts/verify/lint-deps.sh`       | `{项目根目录}/scripts/verify/lint-deps.sh`       | 依赖方向检查                       |
| `scripts/verify/lint-quality.sh`    | `{项目根目录}/scripts/verify/lint-quality.sh`    | 代码质量检查                       |
| `scripts/verify/check-harness.sh`   | `{项目根目录}/scripts/verify/check-harness.sh`   | Harness 结构检查                   |

**不需要复制到目标项目的文件**: `SKILL.md`（本文件）。

## 模板占位符说明

模板文件中使用 `{placeholder}` 表示需要根据扫描结果填充的内容。常见占位符:

- `{project_name}` — 项目名称（从 pom.xml artifactId 或目录名获取）
- `{language}` / `{version}` — 技术栈信息
- `{framework}` — 框架名称和版本
- `{class}` / `{method}` / `{file}:{line}` — 代码位置
- `{count}` / `{n}` — 统计数量
- `{xxx}` — 其他上下文相关占位符

## 工作流程

严格按照以下步骤执行。步骤 1-7 是全自动扫描，步骤 8-10 产出半自动结果，需要和用户交互确认。

每一步在写入文件前，先读取对应的模板文件，填充扫描结果后再写入目标项目。

### Step 0: 创建目录结构

**目标**: 在目标项目中一次性创建所有必需的目录。

**操作**:

```
mkdir -p docs/architecture
mkdir -p harness/memory
mkdir -p harness/trace/failures
mkdir -p scripts/verify
```

**注意**: 如果目标项目已有这些目录或文件，提示用户确认是否覆盖，不要静默覆盖。

### Step 1: 创建 AGENTS.md 和 CLAUDE.md

**目标**: 在项目根目录建立 Claude 协作基线文件。

**操作**:

1. 读取本 Skill 的 `AGENTS-template.md` 文件内容
2. 将 `{project_name}` 替换为实际项目名称
3. 检查项目根目录是否已有 `AGENTS.md`，如已有则提示用户确认是否覆盖
4. 将填充后的内容写入目标项目的 `AGENTS.md`

5. 读取本 Skill 的 `CLAUDE-project-template.md` 文件内容
6. 检查项目根目录是否已有 `CLAUDE.md`，如已有则提示用户确认是否覆盖
7. 将填充后的内容写入目标项目的 `CLAUDE.md`
8. **校验 CLAUDE.md 引用完整性**:
   - 读取刚写入的 `CLAUDE.md`，确认第一行为 `@AGENTS.md`
   - 如果缺少 `@AGENTS.md` 引用，**必须中断流程并报错**，不允许继续后续步骤
   - 如果是覆盖已有文件的情况，同样校验新内容包含 `@AGENTS.md` 引用

   **产出**: 两个文件骨架，后续步骤填充引用的 docs/ 内容。CLAUDE.md 必须以 `@AGENTS.md` 开头。

### Step 2: 扫描项目结构

**目标**: 理清项目的目录布局和模块关系。

**操作**:

1. 检查项目根目录是否存在 `pom.xml` 或 `build.gradle`/`settings.gradle`
2. 如果是多模块项目，从构建文件中提取 `<modules>` 列表
3. 扫描每个模块的 `src/main/java` 目录结构（最多 6 层深度），识别分层:
   - `controller/` — 接口层
   - `service/` — 业务逻辑层
   - `manager/` — 通用业务聚合层（如果有）
   - `dao/` 或 `mapper/` — 数据访问层
   - `entity/` 或 `model/` 或 `domain/` — 数据模型
   - `dto/`、`vo/`、`param/` — 传输对象
   - `config/` — 配置类
   - `client/` — 远程调用客户端
   - `task/` 或 `job/` — 定时任务
   - `interceptor/` 或 `filter/` — 拦截器
   - `enums/` 或 `constant/` — 枚举和常量
   - `utils/` 或 `helper/` — 工具类
4. 统计每个模块下的 Java 文件数量
5. 识别模块间依赖关系（从 pom.xml 的 `<dependency>` 标签）

**产出**: 读取本 Skill 的 `docs/project-structure.md` 模板，填充扫描结果后写入目标项目的 `docs/project-structure.md`。

### Step 3: 提取技术栈

**目标**: 从构建文件和代码中提取完整技术栈信息。

**操作**:

#### 3.1 从构建文件提取

读取根目录和各模块的 `pom.xml` 或 `build.gradle`，提取:

- Java 版本（`<java.version>` 或 `sourceCompatibility`）
- Spring Boot 版本（parent pom 或依赖版本）
- ORM 框架及版本（MyBatis / MyBatis-Plus / JPA / Hibernate）
- 连接池（Druid / HikariCP / Tomcat）
- 分页插件（PageHelper 等）
- 其他关键依赖及版本

#### 3.2 从代码推断中间件

用 grep 搜索以下模式，推断中间件使用:

- **数据库**: 搜索 `DataSource`、`datasource`、`jdbc`、`mysql`、`postgresql`
- **缓存**: 搜索 `RedisTemplate`、`@Cacheable`、`redis`、`memcached`
- **消息队列**: 搜索 `@RabbitListener`、`KafkaTemplate`、`RocketMQ`、`@StreamListener`
- **定时任务**: 搜索 `@XxlJob`、`@Scheduled`、`xxl-job`
- **RPC/微服务**: 搜索 `@FeignClient`、`@DubboService`、`@Reference`
- **注册中心**: 搜索 `@EnableDiscoveryClient`、`Nacos`、`Eureka`、`Zookeeper`
- **网关**: 搜索 `@EnableZuulProxy`、`SpringCloudGateway`

#### 3.3 推断 RPC 调用方式

**目标**: 确定项目使用的远程调用方式及其优先级，帮助 Claude 在后续开发中选择正确的调用方式。

**推断优先级**（按以下顺序检测，命中即确定主要方式）:

1. **OpenFeign** — 搜索以下特征:
   - `@FeignClient` 注解（`@FeignClient(name=` 或 `@FeignClient(value=`）
   - `@EnableFeignClients` 启用注解
   - import 包路径包含 `org.springframework.cloud.openfeign`
   - Feign 拦截器（`RequestInterceptor` 实现，用于统一添加 header/token）
   - Feign 配置类（`FeignClientConfiguration` 或含 `@Configuration` + Feign 相关 bean）

2. **Dubbo** — 搜索以下特征（注解和 XML 两种配置方式都要检测）:

   **注解方式检测:**
   - `@DubboService`、`@DubboReference`、`@Service`（Apache Dubbo 的，注意和 Spring 的区分）
   - `@EnableDubbo`、`@DubboComponentScan`
   - import 包路径包含 `org.apache.dubbo`
   - `@AliasFor` + Dubbo 相关注解（阿里版 Dubbo 特征）

   **XML 配置方式检测（很多老项目用 XML 而非注解）:**
   - 搜索 `resources/` 目录下所有 XML 文件，查找 Dubbo 命名空间特征:
     - `<*:application` — 应用声明，说明项目使用了 Dubbo
     - `<*:registry` — 注册中心配置
     - `<*:protocol` — 协议配置（dubbo/triple/rest）
     - `<*:service` — 暴露的服务（生产者特征）
     - `<*:reference` — 引用的服务（消费者特征）
   - XML 文件名模式: `dubbo-*.xml`、`spring/dubbo.xml`

   **生产者 vs 消费者判断:**
   - 生产者特征: `<*:service` 存在，或代码中有 `@DubboService`/`@Service`(Dubbo) 注解的实现类
   - 消费者特征: `<*:reference` 存在，或代码中有 `@DubboReference`/`@Reference` 注入的字段
   - 混合型: 两者都存在，项目既是生产者又是消费者
   - 如果只有 `<*:application` + `<*:registry` + `<*:protocol` 但没有 service/reference，说明只做了基础配置，需要进一步检查代码

3. **RestTemplate** — 搜索以下特征:
   - `RestTemplate` 类的直接使用（`new RestTemplate()` 或 `@Bean` 注入）
   - `restTemplate.getForObject`、`restTemplate.postForObject`、`restTemplate.exchange`
   - `@LoadBalanced` + `RestTemplate`（带负载均衡的 RestTemplate）
   - RestTemplate 配置类（超时设置、拦截器等）

4. **HttpClient / OkHttp / 手写 RequestHelper** — 搜索以下特征:
   - 自定义工具类命名模式: `HttpUtil`、`HttpHelper`、`RequestHelper`、`HttpRequestUtil`、`OkHttpUtil`
   - `CloseableHttpClient`、`HttpClient`、`HttpURLConnection` 的直接使用
   - `OkHttpClient`、`Request.Builder`（OkHttp 特征）
   - `HttpPost`、`HttpGet`、`HttpEntity`（Apache HttpClient 特征）

   **推断规则**:

- 如果项目同时存在多种调用方式，按以下规则判断主次:
  - 统计每种方式的使用频率（出现次数）
  - 统计每种方式涉及的调用方数量（有多少个类在使用）
  - 主要方式 = 使用频率最高的；其他标记为辅助方式
- 特别注意 `client/` 包: 如果存在这个包，通常是 Feign/Dubbo 客户端的集中存放位置
- 注意区分"定义"和"使用": `@FeignClient` 定义在 interface 上，实际调用在 Service 层
- 如果是 Spring Cloud 项目（有 `@EnableDiscoveryClient`），Feign 通常是首选

**产出**: 读取本 Skill 的 `docs/architecture/tech-stack.md` 模板，填充扫描结果后写入目标项目的 `docs/architecture/tech-stack.md`。同时将 RPC 调用方式分析结果填充到 `docs/architecture.md` 的"远程调用约定"部分。

**注意**: 不要读取 `application-*.yml`、`*.properties`、`*.env` 等配置文件的具体内容（可能包含密码），只通过代码中的 import 和注解推断。

### Step 4: 分析 API 设计模式与入口清单

**目标**: 总结项目的 API 设计模式，并生成完整的 API 入口清单。

**操作**:

1. 找到所有 `@RestController` 或 `@Controller` 注解的类
2. 对每个 Controller，收集:
   - `@RequestMapping` 前缀
   - 每个方法的 HTTP 方法（`@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping`）
   - URL 命名模式（RESTful `/api/users/{id}` 还是动作式 `/api/getUserList`）
   - 方法名
   - 功能描述（优先级：Swagger 注解 `@ApiOperation`/`@Operation` 的 summary > 方法注释 > 从方法名推断）
3. 找到响应封装类:
   - 搜索方法返回类型中反复出现的通用封装类（如 `Result<T>`、`Protocol`、`ApiResponse<T>`、`ResponseEntity`）
   - 这个类通常在 common 包或 api 模块中
4. 统计 GET vs POST vs PUT vs DELETE 的使用数量和比例
5. 检查是否有统一的请求参数封装（如 `BaseRequest`、`PageRequest`）
6. **生成 API 入口清单**: 对每个 Controller 生成一个表格，包含 HTTP 方法、URL、方法名、功能描述，按模块分组

**产出**: 读取本 Skill 的 `docs/architecture/api-design.md` 模板，填充 API 设计风格（第一部分）和 API 入口清单（第二部分）后写入目标项目的 `docs/architecture/api-design.md`。

### Step 5: 分析数据访问模式与表字段推断

#### 5.1 分析数据访问模式

**目标**: 了解项目如何访问数据库，并推断出所有数据表及其字段定义。

**操作**:

1. 找到所有 Mapper 接口（`*Mapper.java` 或继承 `BaseMapper` 的接口）
2. 统计查询方式:
   - **Mapper XML SQL**: 检查 `resources/mapper/` 目录下是否有对应 XML 文件
   - **MyBatis-Plus 方法调用**: 搜索 `BaseMapper<`、`IService<`、`ServiceImpl<` 的使用
   - **QueryWrapper/LambdaQueryWrapper**: 搜索这些类的使用次数
   - **JdbcTemplate**: 搜索 `JdbcTemplate` 的使用
3. 统计多数据源配置: 搜索 `@DS`、`dynamic-datasource`、`DataSource` 注解
4. 检查是否有分页模式: 搜索 `PageHelper`、`Page<`、`IPage<`
5. 统计 Entity 类的命名模式和字段注解风格（如是否有统一的 `is_del`、`create_time` 字段）

#### 5.2 推断数据表和字段

**目标**: 从 Entity 类推断出数据库表结构，包括表名、字段名、字段含义。

**操作**:

1. 找到所有 Entity/Model 类:
   - 搜索 `@TableName`、`@Table` 注解的类（JPA/MyBatis-Plus 实体标识）
   - 搜索 `entity/`、`model/`、`domain/`、`pojo/` 目录下的 Java 类
   - 排除 DTO、VO、Param、Request、Response 类（这些不是数据库表映射）
2. 对每个 Entity 类，提取:
   - **表名**: 从 `@TableName("xxx")` 或 `@Table(name="xxx")` 获取；如果没有注解，按驼峰转下划线规则从类名推断（如 `UserInfo` → `user_info`）
   - **字段列表**: 读取类的所有字段（排除 `serialVersionUID` 等非业务字段）
     - 字段名: Java 字段名
     - 数据库列名: 从 `@TableField("xxx")` 或 `@Column(name="xxx")` 获取；没有注解时按驼峰转下划线推断
     - Java 类型: 字段的 Java 类型
     - 数据库类型: 从 `@Column(columnDefinition="xxx")` 或根据 Java 类型推断（如 `String` → `varchar`，`Long` → `bigint`，`Integer` → `int`，`Date`/`LocalDateTime` → `datetime`）
     - 字段含义: 优先从字段上的注释（`/** xxx */`）获取，其次从 `@ApiModelProperty`/`@ApiParam` 注解获取，最后从字段名语义推断
     - 主键标识: 是否有 `@TableId` 或 `@Id` 注解
3. 多数据源区分:
   - 如果项目使用了 `@DS` 注解，检查每个 Entity 或其 Mapper 上的 `@DS` 注解来确定归属数据源
   - 如果 `@DS` 注解在 Mapper 接口上，通过 Mapper 与 Entity 的对应关系推断 Entity 的归属
   - 如果 `@DS` 注解在 Service 层，通过 Service → Mapper → Entity 的调用链推断
   - 将表按数据源分组输出

   **产出**: 读取本 Skill 的 `docs/architecture/database.md` 模板，填充数据表清单（第一部分，含多数据源分组和字段说明）和数据访问模式分析后写入目标项目的 `docs/architecture/database.md`。

### Step 6: 分析定时任务设计

**目标**: 发现项目中所有定时任务入口，推断任务框架并生成入口清单。

**操作**:

1. 推断定时任务框架:
   - **XXL-Job**: 搜索 `@XxlJob` 注解、`XxlJobHandler`、`IJobHandler`
   - **Spring @Scheduled**: 搜索 `@Scheduled` 注解、`@EnableScheduling`
   - **Quartz**: 搜索 `implements Job`、`@DisallowConcurrentExecution`、`QuartzJobBean`
   - **ElasticJob**: 搜索 `@ElasticJob`、`SimpleJob`、`DataflowJob`
   - **PowerJob**: 搜索 `BasicProcessor`、`@PowerJob`
   - 记录主要框架和辅助框架（如果混用）

2. 找到所有定时任务方法:
   - `@XxlJob("handlerName")`: 提取 handler 名称
   - `@Scheduled(cron="...")`: 提取 cron 表达式或 fixedRate/fixedDelay
   - Quartz Job 类: 提取类名
   - 其他框架: 提取对应的标识信息

3. 对每个定时任务，收集:
   - 任务类名和所属模块
   - Handler 名称或方法名
   - Cron 表达式或触发频率
   - 功能描述（从方法注释、类注释推断，优先使用中文注释）
   - 特殊标注:
     - 是否有 `@DisallowConcurrentExecution`（禁止并发）
     - 是否有 `@Transactional`（含事务）
     - 是否有 `@Component`/`@Service` 等 Spring 管理注解

     **产出**: 读取本 Skill 的 `docs/architecture/jobs-design.md` 模板，填充定时任务规范和入口清单后写入目标项目的 `docs/architecture/jobs-design.md`。

### Step 7: 分析消息消费者入口

**目标**: 发现项目中所有消息消费者入口，推断消息中间件（RabbitMQ / Kafka / RocketMQ）并生成消费者入口清单。

**操作**:

1. 推断消息中间件框架:
   - **RabbitMQ**: 搜索以下特征:
     - `@RabbitListener`（`@RabbitListener(queues =`）
     - `@RabbitHandler`
     - `RabbitTemplate`、`AmqpTemplate`（生产者特征，辅助确认中间件类型）
     - import 包路径包含 `org.springframework.amqp`
     - `SimpleMessageListenerContainer` 自定义配置
     - `@Queue`、`@Exchange` 注解（声明式队列/交换机绑定）

   - **Kafka**: 搜索以下特征:
     - `@KafkaListener`（`@KafkaListener(topics =`、`groupId =`）
     - `KafkaTemplate`（生产者特征）
     - import 包路径包含 `org.springframework.kafka`
     - `@TopicPartition`、`@KafkaHandler`
     - `ConsumerRecord`、`Acknowledgment` 类型参数

   - **RocketMQ**: 搜索以下特征:
     - `@RocketMQMessageListener`（`topic =`、`consumerGroup =`）
     - `RocketMQTemplate`（生产者特征）
     - import 包路径包含 `org.apache.rocketmq`
     - 实现 `RocketMQListener` 接口

     记录主要消息中间件和辅助中间件（如果混用）。

2. 找到所有消费者方法:
   - **RabbitMQ 消费者**: 搜索 `@RabbitListener` 注解的方法和类
     - 提取 `queues` 属性值（队列名）
     - 提取方法参数类型（推断消息体格式）
     - 检查是否有手动 ACK 模式（`Channel` 参数 + ` AcknowledgeMode.MANUAL`）
   - **Kafka 消费者**: 搜索 `@KafkaListener` 注解的方法
     - 提取 `topics` 属性值（Topic 名）
     - 提取 `groupId` 属性值（消费组）
     - 检查是否有手动 ACK（`Acknowledgment` 参数 + `enable-auto-commit: false`）
   - **RocketMQ 消费者**: 搜索 `@RocketMQMessageListener` 注解的类
     - 提取 `topic` 和 `consumerGroup`
     - 检查是否实现 `RocketMQListener` 接口

3. 对每个消费者，收集:
   - 消费者类名和所属模块
   - 监听注解及参数（Queue/Topic 名称、Group ID）
   - 消息体类型（从方法参数推断）
   - 功能描述（从类注释、方法注释推断，优先使用中文注释）
   - 特殊标注:
     - 是否有 `@Transactional`（含事务——风险点，事务内消费可能导致消息重试与事务回滚不一致）
     - 是否有手动 ACK 模式
     - 是否有消费异常处理（重试策略、死信队列）

     **产出**: 读取本 Skill 的 `docs/architecture/consumer.md` 模板，填充消息消费者规范和入口清单后写入目标项目的 `docs/architecture/consumer.md`。

     **注意**: 只扫描消费者入口，不扫描生产者发送端。生产者的发送模式（如 `rabbitTemplate.convertAndSend`、`kafkaTemplate.send`）在 Step 7 事务分析中作为"事务内 MQ 发送"的违规检测依据。

### Step 8: 分析事务处理模式

**目标**: 检查事务使用是否规范。

**操作**:

1. 找到所有 `@Transactional` 注解的位置
2. 对每个事务方法，检查其方法体中是否包含:
   - **RPC 调用**: 搜索 Feign client 调用（`xxxClient.xxx()`）或 `@DubboReference` 调用
   - **MQ 发送**: 搜索 `send`、`convertAndSend`、`produce`、`kafkaXXX` 等消息发送操作
   - **Redis 操作**: 搜索 `redisTemplate`、`stringRedisTemplate`、`redisClient`、`@Cacheable` 等缓存操作
3. 标记违规的事务方法（事务内包含 RPC/MQ/Redis 操作）
4. 检查 `@DS` 注解和 `@Transactional` 的共存情况（可能导致数据源切换失败）

**注意**: 这一步是为了在文档中标注项目约定，不是为了修复代码。只报告发现，不要修改任何代码。

**产出**: 将事务模式分析结果填充到 `docs/architecture.md` 的"事务约定"部分。

### Step 9: 分析全局异常处理模式

**目标**: 了解项目的错误处理策略。

**操作**:

1. 找到全局异常处理器: 搜索 `@ControllerAdvice` 或 `@RestControllerAdvice` 注解
2. 提取 `@ExceptionHandler` 处理的异常类型和返回格式
3. 找到自定义业务异常类: 搜索 `extends RuntimeException` 或 `extends Exception` 的类，特别是名字包含 `BusinessException`、`ServiceException` 的
4. 统计 catch 块的处理方式:
   - 是否有大量 `catch (Exception e) { log.error(...); }` 的吞异常模式
   - 是否有 `throw new BusinessException(...)` 的转译模式
5. 检查是否有统一的错误码体系（搜索 `ErrorCode`、`ResultCode`、`BizCode` 等枚举）

**产出**: 读取本 Skill 的 `docs/architecture.md` 模板，填充全局异常处理分析结果到"全局异常处理约定"部分。同时将 Step 3 的技术栈概要、Step 4 的 API 设计风格、Step 5 的数据访问约定和数据库命名规范、Step 8 的事务约定一并填充到 `docs/architecture.md` 对应部分后写入目标项目。

### Step 10: 扫描隐性约定

**目标**: 发现代码中的特殊处理、硬编码、非主流写法等隐性约定。

**操作**:

1. 搜索代码注释中的特殊标记:
   - `// hack`、`// HACK`、`// todo: hack`
   - `// workaround`、`// WORKAROUND`
   - `// fix`、`// FIX`、`// quick fix`、`// temp fix`
   - `// ugly`、`// dirty`、`// bad`
   - `// 注意`、`// 注意:`、`// 重要`、`// 警告`
2. 搜索硬编码的魔法值:
   - 数字常量（如 `status == 1`、`type = 2`）出现在条件判断中
   - 硬编码的 URL、IP、端口
   - 硬编码的日期格式字符串
3. 搜索非主流命名:
   - 拼音命名（如 `zhiFu`、`dingDan`）
   - 缩写不明确（如 `flg`、`flg2`、`tmpXxx`）
   - 中英文混用命名
4. 搜索 TODO 和 FIXME 标记

**产出**: 读取本 Skill 的 `docs/implicit-contracts.md` 模板，填充扫描结果后写入目标项目的 `docs/implicit-contracts.md`。

**交互**: 扫描完成后，提示用户:

> 隐性约定扫描完成，发现 {n} 条待确认项。请查看 `docs/implicit-contracts.md`，逐条确认哪些是有效约定，哪些需要修复。

### Step 11: Git 历史考古

**目标**: 从 Git 提交历史中提取项目演变的关键决策和约束。

**操作**:

此步骤只在项目是 Git 仓库时执行。如果不是 Git 仓库，跳过此步骤并在 `docs/implicit-contracts.md` 末尾标注。

1. 搜索历史修复记录:

```bash
git log --oneline --grep="fix\|bug\|hotfix\|rollback" -20
```

2. 搜索架构变更记录:

```bash
git log --oneline --grep="refactor\|migrate\|upgrade" -20
```

3. 对找到的关键 commit（最多 10 个），查看简要 diff:

```bash
git show --stat {commit_hash}
```

4. 从 commit message 和 diff 中提炼"为什么改"，关注:
   - 反复出现的 bug 模式（可能暗示架构缺陷）
   - 迁移决策（为什么从 A 迁移到 B）
   - 紧急修复（可能暴露系统的脆弱点）

   **产出**: 追加到目标项目的 `docs/implicit-contracts.md`。

   **交互**: 扫描完成后，提示用户:

> Git 历史考古完成，发现 {n} 条关键变更。请查看 `docs/implicit-contracts.md` 底部的"Git 历史考古"部分，确认哪些决策仍然有效。

### Step 12: 提取业务上下文

**目标**: 从代码中提炼业务名词、术语和规则、流程

**操作**:

1. 从 Entity/Model 类名中提取业务名词（如 `Order`、`Payment`、`Refund`、`Coupon`）
2. 从 Enum 类中提取业务状态和类型定义:
   - 搜索 `extends Enum` 或 `enum` 关键字定义的枚举类
   - 提取枚举值和注释（如 `OrderStatus.PAID("已支付")`）
3. 从 Service 类的方法名中提炼业务动作（如 `placeOrder`、`refund`、`cancelOrder`）
4. 从 Controller 的 URL 和方法名中提炼业务流程
5. 识别核心业务对象的关联关系（通过字段引用，如 Order 中有 userId、productId）

**产出**: 读取本 Skill 的 `docs/business-context.md` 模板，填充扫描结果后写入目标项目的 `docs/business-context.md`。

**交互**: 扫描完成后，提示用户:

> 业务上下文提取完成，发现 {n} 条业务信息。请查看 `docs/business-context.md`，确认和补充业务知识。

### Step 13: 创建 harness 自进化骨架

**目标**: 在目标项目中建立 harness 自进化机制的目录和骨架文件。

**操作**:

1. 确认 `harness/memory/`、`harness/trace/failures/` 目录已创建（Step 0 已完成）
2. 读取本 Skill 的 `harness/memory/episodic.md` 模板，写入目标项目的 `harness/memory/episodic.md`
3. 读取本 Skill 的 `harness/memory/procedural.md` 模板，写入目标项目的 `harness/memory/procedural.md`
4. 读取本 Skill 的 `harness/memory/lessons-learned.md` 模板，写入目标项目的 `harness/memory/lessons-learned.md`

这些文件是空骨架，后续项目运行过程中由 harness 机制自动填充内容。

### Step 14: 复制验证脚本

**目标**: 将 skill 中的验证脚本复制到目标项目，让目标项目开箱即用验证管道。

**操作**:

1. 检查目标项目根目录是否已有 `scripts/` 目录，如已有则提示用户确认是否覆盖同名文件
2. 将本 Skill 的以下脚本复制到目标项目对应位置:

```
skills/harness-init-java/scripts/validate.sh          → {项目根目录}/scripts/validate.sh
skills/harness-init-java/scripts/verify/lint-deps.sh  → {项目根目录}/scripts/verify/lint-deps.sh
skills/harness-init-java/scripts/verify/lint-quality.sh → {项目根目录}/scripts/verify/lint-quality.sh
skills/harness-init-java/scripts/verify/check-harness.sh → {项目根目录}/scripts/verify/check-harness.sh
```

3. 确保复制的脚本保留可执行权限（`chmod +x`）
4. 在 AGENTS.md 的"可用命令"部分引用这些脚本（如未包含则补充）

**产出**: 目标项目 `scripts/` 目录下包含完整的验证管道脚本。

---

## 注意事项

1. **只读分析，不要修改任何代码**。这个 Skill 的唯一产出是文档文件。
2. **不要读取配置文件中的密码和连接信息**。只通过代码注解和 import 推断技术栈。具体来说，不要读取 `application-*.yml`、`*.env`、`*.properties`、`*.config` 的内容。
3. **大项目优化**: 对于超过 50 个 Java 文件的项目，使用 grep 统计模式而非逐文件读取，提高效率。
4. **如果文件已存在**: AGENTS.md、CLAUDE.md 如果已存在，提示用户确认是否覆盖。docs/ 下的文件如果已存在，同样提示。
5. **模板文件是起点，不是终点**: 读取模板后，用实际扫描结果替换所有 `{placeholder}` 占位符。如果扫描发现模板中没有覆盖的信息，在相应位置补充。
6. **半自动确认**: Step 10-12 的产出需要人工确认。扫描完成后统一提示用户查看确认，不要每一步都打断。
7. **CLAUDE.md 必须引用 AGENTS.md**: 生成的 `CLAUDE.md` 第一行必须是 `@AGENTS.md`，这是 Claude 加载项目规则的入口。Step 1 中有校验步骤，如果校验失败则整个流程中断。无论模板如何变更，这个约束不可违反。
