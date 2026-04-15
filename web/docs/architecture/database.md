# 数据库设计与SQL规范

## 命名规范

- 表名/字段名：`snake_case`（小写下划线）
- 主键：`bigint(20)` 自增（`@TableId(type = IdType.AUTO)`）
- 必备字段：`created_stime`（创建时间）、`modified_stime`（修改时间）、`is_del`（逻辑删除，`@TableLogic`）

## 敏感字段加密

以下字段必须加密存储，后缀 `_encrypt`：

- 手机号 → `mobile_encrypt` + `mobile_hash`
- 密码、身份证号、银行卡号等

## SQL 安全红线

- **禁止** 无 WHERE 的 UPDATE/DELETE
- **禁止** 跨库联表查询
- **禁止** SELECT \*
- **要求** 批量操作加 LIMIT
- **要求** 大表查询走索引

## 数据访问模式

### 当前 Mapper 清单

| Mapper                | 继承                            | 对应 Entity                   | Mapper XML                                |
| --------------------- | ------------------------------- | ----------------------------- | ----------------------------------------- |
| GoldPriceCaibaiMapper | `BaseMapper<GoldPriceCaibaiDO>` | GoldPriceCaibaiDO（菜百金价） | 有（仅 BaseResultMap + Base_Column_List） |
| MovieYinfansMapper    | `BaseMapper<MovieYinfansDO>`    | MovieYinfansDO（电影信息）    | 有（仅 BaseResultMap + Base_Column_List） |

### 查询方式统计

- **Mapper XML SQL**：2 个 XML 文件，目前只有 BaseResultMap 和 Base_Column_List，无自定义 SQL
- **MyBatis-Plus 方法调用**：ServiceImpl 继承模式（`ServiceImpl<XxxMapper, XxxDO>`）
- **LambdaQueryWrapper**：`GoldPriceCaibaiServiceImpl` 中使用（queryByDateRange、getLatestPrice）
- **QueryWrapper**：`MovieYinfansServiceImpl` 中使用（findRecentMovies）
- **JdbcTemplate**：未使用
- **多数据源**：pom.xml 引入了 dynamic-datasource-spring-boot3-starter，但代码中未使用 `@DS` 注解
- **分页方式**：pom.xml 声明了 PageHelper，但代码中未使用
- **Mapper XML 位置**：`src/main/resources/mapper/`

## 检查工具

运行 `/sql-risk-review` 进行自动化 SQL 风险检查。
