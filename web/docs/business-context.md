## 业务上下文

> 以下内容由 harness-init-java 从代码中提取，请 Owner 逐条确认和补充。

## 核心业务名词

| 名词     | 对应类                   | 说明                                                 | 确认 |
| -------- | ------------------------ | ---------------------------------------------------- | ---- |
| 菜百金价 | `GoldPriceCaibaiDO`      | 菜百品牌金价数据，含足金、铂金、投资金等多个品类价格 | [ ]  |
| 电影信息 | `MovieYinfansDO`         | yinfans 电影表，含电影名、描述、分类、封面、详情链接 | [ ]  |
| 电影提醒 | `MovieYinfansDO.alerted` | 电影是否已提醒标记（0=未提醒，1=已提醒）             | [ ]  |

## 业务状态流转

| 业务对象 | 状态字段            | 流转路径                   |
| -------- | ------------------- | -------------------------- |
| 电影信息 | `alerted` (Integer) | 0（未提醒） -> 1（已提醒） |

## 核心业务动作

| 动作               | Service 方法                                 | 涉及实体          | 确认 |
| ------------------ | -------------------------------------------- | ----------------- | ---- |
| 查询日期范围内金价 | `IGoldPriceCaibaiService.queryByDateRange()` | GoldPriceCaibaiDO | [ ]  |
| 查询最近N天金价    | `IGoldPriceCaibaiService.queryRecentDays()`  | GoldPriceCaibaiDO | [ ]  |
| 获取最新金价       | `IGoldPriceCaibaiService.getLatestPrice()`   | GoldPriceCaibaiDO | [ ]  |
| 查询最近新电影     | `IMovieYinfansService.findRecentMovies()`    | MovieYinfansDO    | [ ]  |
| 标记电影已提醒     | `IMovieYinfansService.markAsAlerted()`       | MovieYinfansDO    | [ ]  |

## 业务规则（从代码推断）

| #   | 规则                                                   | 来源位置                                | 确认 |
| --- | ------------------------------------------------------ | --------------------------------------- | ---- |
| 1   | 金价查询按价格日期升序排列                             | `GoldPriceCaibaiServiceImpl.java:27`    | [ ]  |
| 2   | 最新金价取价格日期最大的一条记录                       | `GoldPriceCaibaiServiceImpl.java:41-44` | [ ]  |
| 3   | 电影提醒只查询未提醒（alerted=0）的记录                | `MovieYinfansServiceImpl.java:30`       | [ ]  |
| 4   | 逻辑删除：所有实体均使用 is_del 字段（0=正常，1=删除） | Entity 类 @TableLogic 注解              | [ ]  |
