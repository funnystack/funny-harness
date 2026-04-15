## API 设计风格要求

- 统一使用 GET/POST，不使用 PUT/PATCH/DELETE
- 所有响应封装在 `ApiResult<T>`（`com.funny.harness.common.ApiResult`），字段类型为 `code`(Integer)、`msg`(String)、`data`(T)
- 返回码约定如下：
  - `0`：成功（CODE_SUCCESS）
  - `-1`：失败（CODE_FAILURE）
