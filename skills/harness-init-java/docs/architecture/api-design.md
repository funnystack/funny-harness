## API 设计风格要求

- {统一使用 GET/POST / RESTful / 混合 / }
- **不使用** PUT/PATCH/DELETE
- 所有响应封装在`{response_wrapper_full_class_name}`，字段类型为`{字段列表}`
- 返回码约定如下：
  - `{code}`：{含义}
  - `{code}`：{含义}

## API 入口清单

### {Controller 类名}

**前缀**：`{request_mapping_prefix}`

**所属模块**：`{module_name}`

| HTTP 方法  | URL     | 方法名          | 功能描述                           |
| ---------- | ------- | --------------- | ---------------------------------- |
| {GET/POST} | `{url}` | `{method_name}` | {从方法注释或方法名推断的功能描述} |

{如果有 Swagger/OpenAPI 注解（@ApiOperation、@Operation），优先使用注解中的 summary 作为功能描述}
{每个 Controller 生成一个三级标题的表格，按模块分组}
