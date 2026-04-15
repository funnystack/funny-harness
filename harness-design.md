# Harness 整体设计文档

## 设计哲学
funny-harness 的底层逻辑是：**环境设计的投入回报远高于 prompt 调优**。

一套好的 Harness 能让普通模型产出可靠的代码，而没有 Harness 的顶级模型照样会在同样的坑里反复栽。

## 三大支柱

### 支柱一：可读性
- AGENTS.md 做导航地图，详细知识放 docs/
- 渐进披露，不全量注入
- OpenSpec specs/ 作为活文档

### 支柱二：防御机制
- 硬约束（hooks、permissions）优先于软约束（提示词）
- 状态机锁定执行阶段
- 写入前预验证
- 失败循环检测（3次自动停止）

### 支柱三：反馈回路
- 实现和评审分离（不同 Agent）
- 错误经验持久化（harness/memory/）
- Critic → Refiner 自进化循环
- 重复模式编译成脚本

## 自进化机制

```
Agent 执行 → 验证抓到问题 → Critic 分析模式 → Refiner 更新规则 → 下一个 Agent 受益
```

### 三层记忆
1. **情景记忆**（episodic.md）— 具体事件和教训
2. **程序记忆**（procedural.md）— 成功操作步骤模式
3. **失败经验**（lessons-learned.md）— 供 Critic 分析

### 轨迹编译
同一类任务成功执行 3 次以上且步骤高度一致 → 编译为确定性脚本。
棘轮效应：每个被编译的模式都变成永久基础设施。
