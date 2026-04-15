package com.funny.harness.common;

/**
 */
public class ApiResult<T> extends BaseResult {

    private T data;

    public static <T> ApiResult<T> of(T data) {
        ApiResult<T> singleResponse = new ApiResult<>();
        singleResponse.setData(data);
        return singleResponse;
    }

    public T getData() {
        return data;
    }

    public ApiResult setData(T data) {
        this.data = data;
        return this;
    }

    public ApiResult setSuccessData(T data) {
        this.data = data;
        this.setOK();
        return this;
    }

    public static ApiResult buildFailure(Integer errCode, String errMessage) {
        ApiResult response = new ApiResult();
        response.setCode(errCode);
        response.setMsg(errMessage);
        return response;
    }

    public static ApiResult build(){
        ApiResult response = new ApiResult();
        return response;
    }

    public static ApiResult fail(String message) {
        ApiResult commonResult = new ApiResult();
        commonResult.setCode(CODE_FAILURE);
        commonResult.setMsg(message);
        return commonResult;
    }

    public static <T> ApiResult succ() {
        ApiResult commonResult = new ApiResult();
        commonResult.setCode(CODE_SUCCESS);
        return commonResult;
    }

    public static <T> ApiResult succ(T data) {
        ApiResult commonResult = succ();
        commonResult.setData(data);
        return commonResult;
    }

    public static <T> ApiResult succ(T data, String msg) {
        ApiResult commonResult = succ();
        commonResult.setData(data);
        commonResult.setMsg(msg);
        return commonResult;
    }


}
