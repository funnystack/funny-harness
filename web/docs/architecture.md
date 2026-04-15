## 技术栈

@docs/architecture/tech-stack.md

## 数据库及SQL规范

@docs/architecture/database.md

## 测试规范

@docs/architecture/testing.md

## API 设计模式

@docs/architecture/api-design.md

## 构建、测试命令

@docs/architecture/development.md

## 数据访问约定

- 主导查询方式：MyBatis-Plus ServiceImpl 继承 + LambdaQueryWrapper/QueryWrapper
- 不提倡在Service中使用QueryWrapper/LambdaQueryWrapper访问数据库，优先使用Mapper XML的方式
- 多数据源：pom.xml 已引入 dynamic-datasource 但未使用，预留能力
- 分页方式：pom.xml 已引入 PageHelper 但未使用
- Mapper XML 位置：`src/main/resources/mapper/`

## 远程调用约定

- 主要调用方式：暂未使用（pom.xml 已引入 OpenFeign 4.3.0 和 Apache HttpClient 4.5.13，但代码中均未使用）
- 注意：项目中存在多种远程调用依赖（Feign + HttpClient），新增远程调用时应优先使用 Feign，避免进一步碎片化
- 调用约定：调用远程接口时需要打印调用前 url、参数、header，调用后需要打印接口返回结果

## 事务约定

- 事务放置层级：暂未使用 @Transactional
- 多表操作必须开启事务
- 事务内**禁止** RPC 调用、MQ 发送、Redis等IO操作
- 事务内禁止大批量查询数据，防止事务挂起时间过长，如有需要放在事务外处理

## 全局异常处理约定

- 全局异常处理：**未检测到** `@ControllerAdvice` 或 `@RestControllerAdvice`
- 自定义异常：**未检测到** BusinessException 或 ServiceException
- 建议：项目尚在早期阶段，需要补充全局异常处理和统一错误码体系
