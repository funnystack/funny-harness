## 消息消费者规范

- 消息中间件：{RabbitMQ / Kafka / RocketMQ / 混合}
  {如果是 RabbitMQ:}
- 监听方式：`@RabbitListener(queues = "{queue_name}")`
- 序列化格式：{JSON / Java 序列化 / 其他}
  {如果是 Kafka:}
- 监听方式：`@KafkaListener(topics = "{topic_name}", groupId = "{group_id}")`
- 序列化格式：{JSON / Avro / Protobuf / 其他}
  {如果是 RocketMQ:}
- 监听方式：`@RocketMQMessageListener(topic = "{topic}", consumerGroup = "{group}")`
  {如果是混合:}
- 主要中间件：{主要消息中间件名称}
- 辅助中间件：{辅助消息中间件名称}

## 消费者入口清单

| 消费者类       | 监听注解                                  | Queue/Topic             | Group（如有）        | 功能描述                           |
| -------------- | ----------------------------------------- | ----------------------- | -------------------- | ---------------------------------- |
| `{class_name}` | `@{ListenerAnnotation}({queue_or_topic})` | `{queue_or_topic_name}` | `{group_id_or_dash}` | {从注释或方法名推断的消费逻辑描述} |

{注意：

- 如果消费者方法上有 `@Transactional`，在功能描述后标注（含事务）——这是风险点，事务内消费可能导致消息重试与事务回滚不一致
- 如果消费者有手动 ACK 模式（`channel.basicAck` / `Acknowledgment.acknowledge`），标注（手动ACK）
- 按 Queue/Topic 归类分组展示
- 如果同一 Queue/Topic 有多个消费者，标注消费模式（广播/竞争）
  }
