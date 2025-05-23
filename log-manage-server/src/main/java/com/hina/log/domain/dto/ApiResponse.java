package com.hina.log.domain.dto;

import com.hina.log.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 统一API响应对象
 */
@Getter
@Setter
@Schema(description = "API统一响应格式")
public class ApiResponse<T> {
    @Schema(description = "响应状态码", example = "00000")
    private String code;

    @Schema(description = "响应消息", example = "操作成功")
    private String message;

    @Schema(description = "响应数据")
    private T data;

    private ApiResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功结果
     *
     * @param data 数据
     * @param <T>  数据类型
     * @return 结果
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    /**
     * 成功结果（无数据）
     *
     * @return 结果
     */
    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    /**
     * 失败结果
     *
     * @param code    错误码
     * @param message 错误信息
     * @param <T>     数据类型
     * @return 结果
     */
    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    /**
     * 失败结果
     *
     * @param errorCode 错误码
     * @param <T>       数据类型
     * @return 结果
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return error(errorCode.getCode(), errorCode.getMessage());
    }

    /**
     * 失败结果
     *
     * @param errorCode   错误码
     * @param errorDetail 错误详情
     * @param <T>         数据类型
     * @return 结果
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String errorDetail) {
        return error(errorCode.getCode(), errorDetail);
    }
}