package com.funny.harness.common;

import java.io.Serializable;

/**
 */
public class BaseResult implements Serializable {

    public static final int CODE_SUCCESS = 0;

    public static final int CODE_FAILURE = -1;
    private Integer code;
    private String msg;

    public boolean isOK() {
        return code == 0;
    }

    public Integer getCode() {
        return code;
    }

    public BaseResult setCode(Integer code) {
        this.code = code;
        return this;
    }

    public String getMsg() {
        return msg;
    }

    public BaseResult setMsg(String msg) {
        this.msg = msg;
        return this;
    }

    public BaseResult setOK() {
        this.code = CODE_SUCCESS;
        return this;
    }

    @Override
    public String toString() {
        return "BaseResult:" + "code=" + code + ", msg=" + msg + "]";
    }

    public static BaseResult buildFailure(Integer errCode, String errMessage) {
        BaseResult response = new BaseResult();
        response.setCode(errCode);
        response.setMsg(errMessage);
        return response;
    }

    public static BaseResult build(){
        BaseResult response = new BaseResult();
        return response;
    }

}
