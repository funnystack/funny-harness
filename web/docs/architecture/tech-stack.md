## 技术栈

- 语言：Java 21
- 框架：Spring Boot 3.5.4
- 数据库：MySQL 5.7（mysql-connector-java 8.0.33）
- 缓存：未检测到 Redis 使用（pom.xml 中有 jedis 3.7.0 声明但代码中未引用）
- 消息队列：无
- 定时任务：Spring @EnableScheduling（Application.java 中启用）
- RPC：无（pom.xml 中有 spring-cloud-starter-openfeign 4.3.0 但代码中未使用）
- 远程调用方式：Apache HttpClient 4.5.13（pom.xml 声明，代码中暂未使用）
- 注册中心：无
- 配置中心：无
- ORM：MyBatis-Plus 3.5.9 + MyBatis 3.5.16
- 连接池：Druid 1.2.24
- 动态数据源：dynamic-datasource-spring-boot3-starter 4.3.1
- 分页插件：pagehelper-spring-boot 1.4.7（pom.xml 声明但代码中未使用）
- 其他：Lombok、Fastjson 1.2.83、Hutool 5.7.7（pom 声明未引用）、Freemarker 2.3.31（代码生成器用）
- 日志：Log4j2（排除了默认 logback）
