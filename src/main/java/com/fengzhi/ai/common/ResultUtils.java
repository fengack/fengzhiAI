package com.fengzhi.ai.common;

/**
 * 返回工具类
 *
 */
public class ResultUtils {

    /**
     * 成功
     *
     * @param data
     * @param <T>
     * @return
     */
    public static <T> com.fengzhi.ai.common.BaseResponse<T> success(T data) {
        return new com.fengzhi.ai.common.BaseResponse<>(0, data, "ok");
    }

    /**
     * 失败
     *
     * @param errorCode
     * @return
     */
    public static com.fengzhi.ai.common.BaseResponse error(ErrorCode errorCode) {
        return new com.fengzhi.ai.common.BaseResponse<>(errorCode);
    }

    /**
     * 失败
     *
     * @param code
     * @param message
     * @return
     */
    public static com.fengzhi.ai.common.BaseResponse error(int code, String message) {
        return new com.fengzhi.ai.common.BaseResponse(code, null, message);
    }

    /**
     * 失败
     *
     * @param errorCode
     * @return
     */
    public static com.fengzhi.ai.common.BaseResponse error(ErrorCode errorCode, String message) {
        return new com.fengzhi.ai.common.BaseResponse(errorCode.getCode(), null, message);
    }
}
