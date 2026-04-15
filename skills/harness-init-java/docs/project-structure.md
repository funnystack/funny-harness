# 项目结构

## 构建工具

{maven/gradle}，{单模块/多模块}

## 模块依赖关系

{模块依赖图，用文字描述。如果是单模块项目，写"单模块项目，无模块间依赖"}

## 目录结构

{project_root}/
├── {module_a}/
│ └── src/main/java/com/xxx/{module_a}/
│ ├── controller/ ({count} 文件) — 接口层
│ ├── service/ ({count} 文件) — 业务逻辑层
│ ├── manager/ ({count} 文件) — 通用业务聚合层
│ ├── dao/ ({count} 文件) — 数据访问层
│ ├── entity/ ({count} 文件) — 数据模型
│ ├── dto/ ({count} 文件) — 传输对象
│ ├── config/ ({count} 文件) — 配置类
│ ├── client/ ({count} 文件) — 远程调用客户端
│ ├── enums/ ({count} 文件) — 枚举和常量
│ └── utils/ ({count} 文件) — 工具类
├── {module_b}/
│ └── ...
└── pom.xml

## 关键目录说明

| 目录/文件 | 说明   |
| --------- | ------ |
| {path}    | {说明} |
