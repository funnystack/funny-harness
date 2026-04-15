# harness-critic 分析报告模板

## 概览

- **分析周期**: {start_date} ~ {end_date}
- **记录总数**: {total_records}
- **涉及 change 数**: {change_count}
- **高优问题数**: {high_priority_count}

## 问题分布

| 问题类型 | 次数 | 占比 | 趋势 |
|---------|------|------|------|
| {type_1} | {count_1} | {pct_1}% | ↑/↓/→ |
| {type_2} | {count_2} | {pct_2}% | ↑/↓/→ |

## 模式识别

### 跨 change 反复出现的模式

{pattern_1}: 在 {n} 个 change 中出现，涉及 {files}

### 新出现的模式

{new_pattern}: 首次出现，需要持续关注

## 硬度升级建议

| 条目 | 当前状态 | 出现次数 | 建议升级到 | 理由 |
|------|---------|---------|-----------|------|
| {item_1} | {current} | {count} | {target} | {reason} |

## 行动项

| # | 行动 | 负责方 | 优先级 | 状态 |
|---|------|-------|-------|------|
| 1 | {action_1} | 人/Agent | P0/P1/P2 | 待确认 |
| 2 | {action_2} | 人/Agent | P0/P1/P2 | 待确认 |
