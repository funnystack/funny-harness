# 项目结构

## 构建工具

Maven，单模块

## 模块依赖关系

单模块项目，无模块间依赖

## 目录结构

web/
└── src/main/java/com/funny/harness/
├── web/controller/ (2 文件) — 接口层
│ ├── HealthController.java
│ └── MovieYinfansController.java
├── service/ (2 文件) — 业务逻辑层（接口）
│ ├── IGoldPriceCaibaiService.java
│ └── IMovieYinfansService.java
├── service/impl/ (2 文件) — 业务逻辑层（实现）
│ ├── GoldPriceCaibaiServiceImpl.java
│ └── MovieYinfansServiceImpl.java
├── dao/entity/ (2 文件) — 数据模型
│ ├── GoldPriceCaibaiDO.java
│ └── MovieYinfansDO.java
├── dao/mapper/ (2 文件) — 数据访问层
│ ├── GoldPriceCaibaiMapper.java
│ └── MovieYinfansMapper.java
├── dao/ (1 文件) — 代码生成器
│ └── CodeGenerator.java
├── common/ (2 文件) — 通用响应封装
│ ├── ApiResult.java
│ └── BaseResult.java
├── common/consts/ (2 文件) — 常量
│ ├── BaseConsts.java
│ └── CacheKeyConstant.java
├── web/config/ (1 文件) — 配置类
│ └── WebConfig.java
└── Application.java — 启动类
└── src/test/java/com/funny/harness/
├── MovieAlertTest.java
└── TaskTest.java

## 关键目录说明

| 目录/文件                                                | 说明                                              |
| -------------------------------------------------------- | ------------------------------------------------- |
| `src/main/resources/mapper/`                             | MyBatis Mapper XML 文件                           |
| `src/main/java/com/funny/harness/dao/CodeGenerator.java` | MyBatis-Plus 代码生成器，新增表时运行生成骨架代码 |
| `src/main/resources/log4j2/`                             | Log4j2 日志配置                                   |
