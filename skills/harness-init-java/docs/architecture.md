## 技术栈概要

- 语言：{language} {version}
- 框架：{framework} {version}
- ORM：{orm_framework} {version}
- 数据库：{database} {version}
- 缓存：{cache} {version}
- 消息队列：{mq} {version}
- 定时任务：{job_framework} {version}
- RPC：{rpc_framework} {version}
- 远程调用方式：{rpc_calling_method}（主要），{rpc_calling_method_secondary}（辅助）
- 注册中心：{registry_framework} {version}
- 配置中心：{config_center_framework} {version}

## API 设计风格

- {统一使用 GET/POST / RESTful / 混合 / }
- **不使用** PUT/PATCH/DELETE
- 所有响应封装在`{response_wrapper_full_class_name}`，字段类型为`{字段列表}`
- 返回码约定如下：
  - `{code}`：{含义}
  - `{code}`：{含义}

## 数据访问约定

- 主导查询方式：{mapper_xml_or_mybatis_plus_or_mixed}
- 不提倡在Service中使用QueryWrapper/LambdaQueryWrapper访问数据库,优先使用Mapper XML的方式
- 多数据源：{是/否}
  {如果是多数据源:}
  - {数据源名称1}（`@DS("{ds_name}")`）：{描述}，{mapper_count} 个 Mapper
  - {数据源名称2}（`@DS("{ds_name}")`）：{描述}，{mapper_count} 个 Mapper
  - 跨数据源的表禁止使用多表链接查询
- 分页方式：{PageHelper / MyBatis-Plus Page / 无}
- Mapper XML 位置：`{mapper_xml_path}`

## 数据库命名规范

- 表名/字段名：`snake_case`（小写下划线）
- 主键：`bigint(20)` 自增
- 每个表必须要有 `created_stime`（默认当前时间）、`modified_stime`（自动更新）、`is_del`（逻辑删除）

### 敏感字段加密

以下字段必须加密存储，后缀 `_encrypt`：

- 密码、用户真实姓名、身份证号、银行卡号、个人住址、员工姓名、公司税务标识
- 手机号额外增加 `_hash` 字段：`mobile` → `mobile_encrypt` + `mobile_hash`

### SQL 安全红线

- **禁止** 无 WHERE 的 UPDATE/DELETE
- **禁止** 跨库联表查询
- **禁止** SELECT \*
- **要求** 批量操作加 LIMIT
- **要求** 大表查询走索引
- 运行 `/sql-risk-review` 进行自动化 SQL 风险检查

## 远程调用约定

- 主要调用方式：{rpc_calling_method}
  {根据主要方式补充:}
  {OpenFeign:}
  - Feign Client 存放位置：`{feign_client_package}`，共 `{feign_client_count}` 个 Client
  - Feign 拦截器：`{feign_interceptor_class}`（用于统一添加 header/token）
  - 新增远程调用时，在对应 Client 接口中添加方法，禁止在 Service 中直接使用 RestTemplate 或 HttpClient
    {Dubbo:}
  - Dubbo 配置方式：`{xml_or_annotation}`（XML配置/注解配置/混合）
  - Dubbo 角色判定：`{provider_or_consumer_or_both}`（生产者/消费者/混合）
    {如果是生产者:}
    - 暴露的服务位置：`{dubbo_service_package}`，共 `{dubbo_service_count}` 个服务
      {如果是消费者:}
    - 引用的服务位置：`{dubbo_reference_package}`，共 `{dubbo_reference_count}` 个引用
      {如果是混合:}
    - 暴露的服务位置：`{dubbo_service_package}`，共 `{dubbo_service_count}` 个服务
    - 引用的服务位置：`{dubbo_reference_package}`，共 `{dubbo_reference_count}` 个引用
  - Dubbo 协议：`{dubbo_protocol}`（dubbo/triple/rest）
    {如果是XML配置:}
  - Dubbo XML 配置文件：`{dubbo_xml_path}`
  - 新增远程调用时，在 XML 中添加 `<dubbo:reference` 配置，通过 setter 注入使用
    {如果是注解配置:}
  - 新增远程调用时，通过 `@DubboReference` 注入服务接口
    {RestTemplate:}
  - RestTemplate 配置位置：`{resttemplate_config_class}`
  - 调用封装位置：`{resttemplate_caller_package}`
  - 新增远程调用时，复用已有 RestTemplate Bean，统一走封装方法
    {HttpClient/OkHttp/手写工具:}
  - 工具类位置：`{http_util_class}`
  - 新增远程调用时，复用已有工具类方法，不要引入新的 HTTP 客户端
- 辅助调用方式：{rpc_calling_method_secondary_or_none}
- 调用约定：调用远程接口时需要打印调用前 url、参数、header，调用后需要打印接口返回结果
  {如果存在多种调用方式混用:}
- 注意：项目中存在多种远程调用方式混用，新增调用时应优先使用主要方式，避免进一步碎片化

## 事务约定

- 事务放置层级：{集中在 Manager层 / Service层 / 混合}
- 多表操作必须开启事务
- 事务内**禁止** RPC 调用、MQ 发送、Redis等IO操作
- 事务内禁止大批量查询数据，防止事务挂起时间过长,如有需要放在事务外处理
  {如果有 @DS + @Transactional 共存问题:}
- 事务内操作的Mapper不能有`@DS("{ds_name}")` 切换数据源的操作

## 全局异常处理约定

- 全局异常处理：`{global_exception_handler_full_class_name}`
- 自定义异常：
  - `{BusinessRuntimeException_full_name}`（{描述}，RuntimeException 子类）
  - `{BusinessException_full_name}`（{描述}，受检异常）
    {如果有其他异常类型，继续列出}

## 测试规范

- 最低要求：80% 覆盖率
- 必须测试的类型：单元测试、集成测试、端到端测试
- TDD 工作流：先写测试（RED）→ 写最小实现（GREEN）→ 重构（IMPROVE）→ 验证覆盖率
- 测试失败排查：检查隔离性 → 验证 mock → 修复实现而非测试

---

## 按需参考

以下文件包含详细的清单和参考数据，不会自动加载到上下文。当工作涉及相关领域时，请主动读取对应文件：

| 何时需要读取                 | 文件路径                           | 内容                                                   |
| ---------------------------- | ---------------------------------- | ------------------------------------------------------ |
| 开发或调试 API 接口时        | `docs/architecture/api-design.md`  | API 入口完整清单（每个 Controller 的 URL、方法、功能） |
| 涉及数据库表结构、新增字段时 | `docs/architecture/database.md`    | 数据表清单及字段说明（含多数据源分组）                 |
| 需要确认完整依赖版本时       | `docs/architecture/tech-stack.md`  | 完整技术栈依赖清单                                     |
| 开发或调试定时任务时         | `docs/architecture/jobs-design.md` | 定时任务入口清单及触发规则                             |
| 开发或调试消息消费者时       | `docs/architecture/consumer.md`    | 消息消费者入口清单                                     |
| 执行构建、部署、本地启动时   | `docs/architecture/development.md` | 构建、测试、部署命令                                   |
