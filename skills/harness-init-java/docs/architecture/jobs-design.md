## 定时任务规范

- 定时任务框架：{XXL-Job / Spring @Scheduled / Quartz / 其他}
  {如果是 XXL-Job:}
- 任务注册方式：`@XxlJob("{handler_name}")`
- 任务调度中心：{由配置管理，不要读取具体地址}
  {如果是 @Scheduled:}
- 任务触发方式：`@Scheduled({cron/fixedRate/fixedDelay})`
  {如果是 Quartz:}
- 任务注册方式：实现 `Job` 接口 + `@DisallowConcurrentExecution`

## 定时任务入口清单

| 任务类         | Handler/方法名             | Cron 表达式         | 功能描述                           |
| -------------- | -------------------------- | ------------------- | ---------------------------------- |
| `{class_name}` | `{handler_or_method_name}` | `{cron_expression}` | {从注释或方法名推断的任务功能描述} |

{注意：

- 如果有 @DisallowConcurrentExecution 或 XXL-Job 的阻塞策略，在功能描述后标注（禁止并发）
- 如果方法上有 @Transactional，在功能描述后标注（含事务）
- 按模块或业务域分组展示
  }
