# 隐性业务约定与项目坑点

> 这是最容易导致"改完能跑、联调出错"的文档。所有口头约定、隐性规则、历史包袱都记录在这里。

## 记录格式

### YYYY-MM-DD：约定标题

- **约定内容**：{描述}
- **影响范围**：哪些模块受影响
- **违反后果**：出了什么事
- **来源**：口头/会议/事故复盘

## Hack / Workaround

未检测到 hack/workaround 标记。

## 硬编码魔法值

| #   | 位置                              | 值                                                  | 上下文                                           | 确认 |
| --- | --------------------------------- | --------------------------------------------------- | ------------------------------------------------ | ---- |
| 1   | `MovieYinfansServiceImpl.java:30` | `alerted = 0`                                       | 查询条件中硬编码未提醒状态                       | [ ]  |
| 2   | `MovieYinfansServiceImpl.java:44` | `movie.setAlerted(1)`                               | 设置已提醒状态硬编码                             | [ ]  |
| 3   | `CodeGenerator.java:26-28`        | `jdbc:mysql://localhost:3306/harness`, `root`, 密码 | 代码生成器中硬编码的数据库连接信息（含密码明文） | [ ]  |
| 4   | `Application.java:36`             | `System.out.println(...)`                           | 启动日志使用 System.out 而非 log                 | [ ]  |

## 非主流命名

| #   | 位置                          | 变量名                                                              | 建议                                                    | 确认 |
| --- | ----------------------------- | ------------------------------------------------------------------- | ------------------------------------------------------- | ---- |
| 1   | `CacheKeyConstant.java:11-12` | `LOCK_CREATIVE_COMBO_TASK_CACHE_KEY`, `LOCK_SPLIT_ADUNIT_CACHE_KEY` | 这些缓存 key 看起来是从其他项目复制来的，与当前业务无关 | [ ]  |

## TODO / FIXME

未检测到 TODO/FIXME 标记。

## 其他发现

| #   | 位置                                    | 描述                                                                        | 确认 |
| --- | --------------------------------------- | --------------------------------------------------------------------------- | ---- |
| 1   | `MovieYinfansServiceImpl.java:27-32`    | `findRecentMovies` 方法接收 `days` 参数但未使用，查询条件缺少时间过滤       | [ ]  |
| 2   | `MovieYinfansServiceImpl.java:41-46`    | `markAsAlerted` 方法使用循环逐条更新，建议改为批量更新                      | [ ]  |
| 3   | `MovieAlertTest.java` / `TaskTest.java` | 测试类引用了不存在的 `MovieAlertTaskHandler` 和 `GoldPriceAlertTaskHandler` | [ ]  |
| 4   | `WebConfig.java`                        | TokenInterceptor 相关代码已全部注释，WebMvcConfigurer 为空实现              | [ ]  |
| 5   | `pom.xml`                               | jedis、hutool、pagehelper 均已声明但未使用                                  | [ ]  |

## Git 历史考古

项目共有 2 个 commit：

1. **dbf9dd3** — 项目初始提交（commit message 为 "commit message"，无实际描述）。包含完整的项目脚手架、harness 工具链、规则文件、skill 定义等。
2. **3adc21a** — feat: 新增 RPC 调用方式推断功能。扩展了 harness-init-java skill 的 SKILL.md 和架构模板。

**关键发现**：

- 项目处于极早期阶段（2 个 commit），web 模块的 Java 业务代码是初始提交时带入的
- 业务代码（Controller/Service/DAO）由 MyBatis-Plus 代码生成器自动生成
- 测试类中引用了尚未实现的 TaskHandler 类，可能是预留的定时任务处理器
