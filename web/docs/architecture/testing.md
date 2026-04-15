# 测试规范

## 最低要求：80% 覆盖率

### 必须测试的类型

1. **单元测试** — 独立函数、工具类、组件
2. **集成测试** — API 端点、数据库操作
3. **端到端测试** — 关键用户流程

### TDD 工作流

1. 先写测试（RED）
2. 运行测试 — 应该失败
3. 写最小实现（GREEN）
4. 运行测试 — 应该通过
5. 重构（IMPROVE）
6. 验证覆盖率（80%+）

### 测试失败排查

1. 检查测试隔离性
2. 验证 mock 是否正确
3. 修复实现，不是修复测试（除非测试本身有错）

## 当前测试状态

- 测试框架：JUnit 4（@Test + @RunWith SpringRunner）
- 测试类：2 个（MovieAlertTest、TaskTest）
- 测试类型：集成测试（@SpringBootTest），依赖 Spring 上下文启动
- 注意：测试类引用了 `MovieAlertTaskHandler` 和 `GoldPriceAlertTaskHandler`，但这两个类在源码中不存在，测试可能无法通过
- 注意：Application.java 中有 `System.out.println`（启动信息），应替换为日志
