package com.hinadt.miaocha.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 数据源连接测试结果DTO */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatasourceConnectionTestResultDTO {

    /** 连接是否成功 */
    private boolean success;

    /** 错误信息（连接失败时提供用户友好的提示） */
    private String errorMessage;

    /** 创建成功的连接测试结果 */
    public static DatasourceConnectionTestResultDTO success() {
        return new DatasourceConnectionTestResultDTO(true, null);
    }

    /**
     * 创建失败的连接测试结果
     *
     * @param errorMessage 用户友好的错误信息
     */
    public static DatasourceConnectionTestResultDTO failure(String errorMessage) {
        return new DatasourceConnectionTestResultDTO(false, errorMessage);
    }
}
