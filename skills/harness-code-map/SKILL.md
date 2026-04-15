---
name: harness-code-map
description: >
  代码数据流追踪器。在需求启动前（如 /opsx:propose）必须调用，追踪 API 入口、定时任务入口和消息消费者入口的完整数据流向，
  辅助精准分析改动点。当用户准备提新需求、做变更分析、影响评估、代码追踪时触发。
  触发词：追踪数据流、分析影响范围、这个接口改了会影响什么、代码地图、code map、改动点分析、
  propose 前分析、需求前置分析、数据流向、调用链分析、消费者影响分析。
  在执行 /opsx:propose 或任何 OpenSpec 操作前，应当先使用此技能完成数据流分析。
  即使只是提到"我想改一下XXX"或"帮我看看这个需求的改动范围"也应触发。
---

# harness-code-map：代码数据流追踪器

## 为什么这件事重要

很多需求评审翻车，不是因为代码写错，而是改之前没有看清数据流向。改了一个 Service 方法，没想到三个 API 和两个定时任务都依赖它；改了一张表的结构，没想到有个异步任务在读这张表写入另一张表。

这个技能的目标是：**在需求启动之前，先把相关的数据流画清楚**，让你在 propose 的时候就知道改哪些文件、影响哪些链路。

## 前置条件

执行前先确认以下文件存在（由 `/harness-init-java` 生成）：

- `docs/architecture/api-design.md` — API 入口清单
- `docs/architecture/database.md` — 数据库表清单
- `docs/architecture/jobs-design.md` — 定时任务入口清单
- `docs/architecture/consumer.md` — 消息消费者入口清单

如果不存在，提示用户先执行 `/harness-init-java` 初始化项目知识体系。

## 工作流程

### Step 0: 理解需求范围

用户会给出一个需求描述（比如"给金价查询加一个缓存"、"电影提醒改成批量推送"）。

1. 从需求描述中提取**关键词**（业务名词、动词、模块名）
2. 用关键词在 `docs/architecture/api-design.md` 中匹配相关的 API 入口
3. 用关键词在 `docs/architecture/jobs-design.md` 中匹配相关的定时任务
4. 用关键词在 `docs/architecture/consumer.md` 中匹配相关的消息消费者
5. 用关键词在 `docs/architecture/database.md` 中匹配相关的表
6. 如果匹配不到明确的入口，用 AskUserQuestion 询问用户具体要改哪个接口或功能

**产出**：初步圈定的入口范围（API 列表 + 定时任务列表 + 消费者列表 + 涉及的表）。

### Step 1: 追踪 API 数据流

对 Step 0 中圈定的每个 API 入口，沿调用链逐层追踪。

#### 1.1 读取入口代码

从 Controller 方法开始：

1. 找到 Controller 类文件，读取入口方法
2. 提取方法调用的 Service 接口/方法

#### 1.2 逐层追踪 Service 调用链

对每个 Service 方法：

1. 找到 Service 实现类，读取方法体
2. 识别以下调用类型并继续追踪：

| 调用类型          | 识别特征                                                                    | 追踪方向                        |
| ----------------- | --------------------------------------------------------------------------- | ------------------------------- |
| 内部 Service 调用 | `xxxService.method()`                                                       | 继续进入该方法                  |
| Mapper/DAO 调用   | `xxxMapper.selectXxx()` / `xxxMapper.insertXxx()` / `xxxMapper.updateXxx()` | 记录操作的表和操作类型（读/写） |
| RPC 调用          | `xxxClient.method()` / `@DubboReference` 注入的调用                         | 记录远程服务名和方法            |
| MQ 发送           | `xxxTemplate.send()` / `xxxTemplate.convertAndSend()` / `producer.send()`   | 记录目标 Topic/Queue            |
| Redis 操作        | `redisTemplate.opsForValue()` / `@Cacheable` / `@CacheEvict`                | 记录缓存 Key 模式               |
| 定时调度          | `@Scheduled` / `xxlJobHandler`                                              | 记录关联的任务                  |

3. 递归追踪 Service 内部调用的其他 Service，直到所有叶子节点都是 Mapper/RPC/MQ/Redis

#### 1.3 记录事务边界

在追踪过程中，标记每个 `@Transactional` 的位置和范围，记录：

- 事务包含哪些 Mapper 操作（涉及哪些表）
- 事务内是否违规包含 RPC/MQ/Redis 操作

#### 1.4 产出 API 流向图

对每个 API 入口，输出如下格式的流向图：

```
[API] POST /api/movie/alert
  └── MovieYinfansController.alertMovie()
      └── IMovieYinfansService.findRecentMovies()
          └── MovieYinfansMapper.selectList()  → 读 movie_yinfans 表 (WHERE alerted=0)
      └── IMovieYinfansService.markAsAlerted()
          └── MovieYinfansMapper.updateById()  → 写 movie_yinfans 表 (SET alerted=1)
```

符号说明：

- `→ 读 xxx 表` — SELECT 操作
- `→ 写 xxx 表` — INSERT/UPDATE/DELETE 操作
- `→ 调用 xxx 服务` — RPC 远程调用
- `→ 发送 xxx 消息` — MQ 发送
- `→ 缓存 xxx` — Redis 读写
- `[事务]` — 标注事务边界

### Step 2: 追踪定时任务数据流

对 Step 0 中圈定的每个定时任务入口，沿调用链逐层追踪。

#### 2.1 读取任务入口代码

1. 找到定时任务类文件（在 `task/`、`job/`、`handler/` 目录下）
2. 读取任务执行方法

#### 2.2 追踪数据流向

追踪方式与 Step 1.2 相同，但额外关注：

| 特殊关注点 | 说明                                             |
| ---------- | ------------------------------------------------ |
| 数据来源   | 任务从哪些表读取数据（WHERE 条件是什么）         |
| 数据去向   | 处理后的数据写入哪些表/接口/MQ                   |
| 批量特征   | 是否有分页查询、循环处理、LIMIT 等批量逻辑       |
| 并发控制   | 是否有 `@DisallowConcurrentExecution` 或分布式锁 |
| 错误处理   | 失败时是重试、跳过、还是中断                     |

#### 2.3 产出定时任务流向图

对每个定时任务，输出如下格式的流向图：

```
[JOB] MovieAlertTaskHandler (XXL-Job, 每30分钟)
  ├── 数据来源:
  │   └── MovieYinfansMapper.selectList()  → 读 movie_yinfans 表 (WHERE alerted=0 AND is_del=0)
  ├── 处理逻辑:
  │   └── IMovieYinfansService.markAsAlerted()
  │       └── MovieYinfansMapper.updateById()  → 写 movie_yinfans 表 (SET alerted=1)
  └── 外部输出:
      └── 无
```

### Step 3: 追踪消息消费者数据流

对 Step 0 中圈定的每个消息消费者入口，沿调用链逐层追踪。

消费者是项目中容易被忽略的入口——它不像 API 那样有明确的 Controller，也不像定时任务那样有调度记录，但它同样会读写数据、调用 Service、触发下游操作。漏掉消费者，影响分析就不完整。

#### 3.1 读取消费者入口代码

1. 从 `docs/architecture/consumer.md` 中获取消费者类名和监听注解
2. 找到消费者类文件，读取消费方法

#### 3.2 追踪数据流向

追踪方式与 Step 1.2 相同，但额外关注：

| 特殊关注点 | 说明                                                       |
| ---------- | ---------------------------------------------------------- |
| 消息来源   | 从哪个 Queue/Topic 消费，消息体是什么结构                  |
| 消费逻辑   | 消费后做了什么——写表、调 RPC、发新消息、更新缓存           |
| ACK 模式   | 自动 ACK 还是手动 ACK，决定了失败时的重试行为              |
| 幂等性     | 是否有去重逻辑（如唯一索引、Redis 判重、状态机校验）       |
| 错误处理   | 消费失败时是重试入队、进死信队列、还是仅记录日志           |
| 并发模式   | 同一 Queue/Topic 有几个消费者实例，是竞争消费还是广播消费  |
| 事务边界   | 消费方法是否有 `@Transactional`——事务回滚与消息 ACK 的关系 |

#### 3.3 产出消费者流向图

对每个消费者，输出如下格式的流向图：

```
[CONSUMER] OrderPaySuccessListener (RabbitMQ, queue: order.pay.success)
  ├── 消息来源: order.pay.success Queue，消息体: OrderPayEvent
  ├── 消费逻辑:
  │   └── IOrderService.handlePaySuccess()
  │       ├── OrderMapper.updateById()  → 写 order 表 (SET status=PAID)
  │       ├── ICouponService.unlockCoupon()
  │       │   └── CouponMapper.updateById()  → 写 coupon 表 (SET status=USED)
  │       └── INotifyService.sendPaySuccessNotify()  → 调用通知服务
  ├── ACK 模式: 自动
  ├── 幂等性: 通过 order.status 状态机校验
  └── 外部输出:
      └── 无
```

### Step 4: 反向追踪（表视角）

根据 Step 0 圈定的表，反向查找所有访问入口。

#### 4.1 从 Mapper 入手

1. 找到该表对应的 Mapper 接口
2. 读取 Mapper XML（如果有自定义 SQL），分析 SQL 类型：
   - SELECT：读取操作
   - INSERT：写入操作
   - UPDATE：更新操作
   - DELETE：删除操作
3. 对 Mapper 中的每个方法，搜索哪些 Service 调用了它
4. 对每个 Service 方法，搜索哪些 Controller、定时任务或消费者调用了它

#### 4.2 产出表访问矩阵

```
[表] movie_yinfans

| 访问入口 | 操作类型 | Service 方法 | SQL 条件 |
|---------|---------|-------------|---------|
| POST /api/movie/alert | 读 | IMovieYinfansService.findRecentMovies() | WHERE alerted=0 AND is_del=0 |
| POST /api/movie/alert | 写 | IMovieYinfansService.markAsAlerted() | SET alerted=1 WHERE id=? |
| [JOB] MovieAlertTaskHandler | 读 | IMovieYinfansService.findRecentMovies() | WHERE alerted=0 AND is_del=0 |
| [JOB] MovieAlertTaskHandler | 写 | IMovieYinfansService.markAsAlerted() | SET alerted=1 WHERE id=? |
```

### Step 5: 生成影响分析报告

汇总 Step 1-4 的追踪结果，生成最终的影响分析报告。

#### 报告结构

```markdown
# 代码数据流追踪报告

## 需求描述

{用户的原始需求描述}

## 追踪范围

- 涉及 API: {n} 个
- 涉及定时任务: {n} 个
- 涉及消息消费者: {n} 个
- 涉及数据表: {n} 个
- 涉及 RPC 调用: {n} 个
- 涉及 MQ: {n} 个

## API 数据流

{Step 1 的每个 API 流向图}

## 定时任务数据流

{Step 2 的每个定时任务流向图}

## 消费者数据流

{Step 3 的每个消费者流向图}

## 表访问矩阵

{Step 4 的表访问矩阵}

## 改动点清单

基于追踪结果，以下是需要关注的改动点：

| #   | 改动类型 | 位置        | 说明         | 风险等级   |
| --- | -------- | ----------- | ------------ | ---------- |
| 1   | {类型}   | {文件:行号} | {需要改什么} | {高/中/低} |

改动类型说明：

- 入口变更：需要修改 API 参数/返回值
- 逻辑变更：Service 内部逻辑需要调整
- 数据变更：SQL/Mapper 需要修改
- 新增逻辑：需要新增 Service 方法或 Mapper 方法
- 影响链路：虽然不需要改，但需要回归测试

## 风险提示

{基于追踪结果识别的风险点，例如：}

- 修改 xx 表结构会影响 {n} 个 API 和 {n} 个定时任务 和 {n} 个消费者入口
- xx 方法被 {n} 个入口调用，修改时需保持兼容
- xx 定时任务有并发控制要求，改动时注意幂等性
- xx 事务内包含 RPC 调用，属于违规模式，建议一并修复
- xx 消费者使用自动 ACK，消费失败会丢失消息，建议改为手动 ACK 或加死信队列
- xx 消费者无幂等保护，重复消费可能导致数据异常
```

#### 改动点清单的优先级

在列出改动点时，按以下优先级排列：

1. **数据层**（Mapper/SQL） — 最底层，改动影响面最大，优先分析
2. **Service 层** — 业务逻辑，核心改动点
3. **入口层**（Controller/Job/Consumer） — 改动最小但最直观
4. **外部依赖**（RPC/MQ/Redis） — 需要协调的改动

## 追踪策略

### 小需求（涉及 1-3 个入口）

直接逐层追踪，读取每个相关文件，产出完整流向图。

### 大需求（涉及 4+ 个入口或跨模块）

采用**分层追踪**策略：

1. 第一层：只读 Controller → Service 接口签名（不读实现），快速建立调用拓扑
2. 第二层：对关键 Service 读取实现，追踪到 Mapper/RPC/MQ
3. 第三层：对可疑点深入读取 Mapper XML 分析 SQL 细节

### 超大项目（100+ Java 文件）

使用 grep 统计模式快速定位，避免逐文件读取：

- `grep -r "表名\|Entity类名" --include="*.java"` 查找所有引用
- `grep -r "方法名" --include="*.java"` 查找调用方
- 重点关注 `service/impl/`、`mapper/`、`controller/`、`task/` 目录

## 输出位置

追踪报告不写入项目文件，直接在对话中输出。原因：

- 这是需求分析阶段的临时产物
- 报告内容会直接反馈到后续的 `/opsx:propose` 工件中
- 避免在项目中产生需要维护的临时文件

如果用户要求保存，写入 `docs/code-map-{需求名}.md`。

## 与 OpenSpec 的衔接

此技能产出的追踪结果应直接用于：

1. **`/opsx:propose` 中的 proposal.md** — "改动范围" 和 "影响分析" 部分
2. **design.md** — 技术方案中的"数据流变更"部分
3. **tasks.md** — 任务拆分依据，确保每个改动点都有对应任务

### 两种调用方式

**方式 1：独立调用（用户主动）**

用户说"帮我分析一下这个需求的改动范围"、"追踪一下这个接口的数据流"时，独立运行此技能。

```
/harness-code-map → 追踪数据流 → 分析影响
        ↓
/opsx:propose → 基于追踪结果生成 proposal + design + tasks
        ↓
人工审批 → /opsx:apply → 实现
```

**方式 2：被 propose 自动调用（强制前置）**

当用户执行 `/opsx:propose` 时，propose 命令的 Step 1 会自动触发此技能。你不需要返回"请先运行 harness-code-map"，而是直接在此技能中执行完整的追踪流程，将结果留在上下文中供 propose 后续步骤使用。

这种模式下：

- 不需要额外输出"建议工作流"等提示语
- 直接产出追踪报告，供 propose 的 Step 2+ 直接引用
- 追踪报告中的改动点清单就是 tasks.md 任务拆分的直接输入

## 注意事项

1. **不要修改任何代码**。这个技能只做分析，只读不写。
2. **不要读取配置文件内容**（application-_.yml、_.properties、\*.env），只通过代码注解和 import 推断。
3. **追踪深度控制在合理范围**。如果调用链超过 5 层，在报告中标注"深层调用，建议人工确认"。
4. **优先读取 docs/ 中的已有知识**，避免重复扫描。docs/ 中没有的信息再从代码推断。
5. **如果项目未初始化**（缺少 docs/），提示用户先执行 `/harness-init-java`。
