package com.nan.waveform.common.result;
import lombok.Data;

/**
 * @author nan chao
 * @since 2026/6/9 21:05
 */

/**
 * 全局统一响应体，后端统一返回结果
 */

@Data
public class Result<T> {
    private Integer code;       // 状态码：200成功，500失败
    private String message;     // 提示信息
    private T data;             // 响应数据
    private long timestamp;     // 接口响应时间戳

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    // 成功快捷方法 (无数据)
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        return result;
    }

    // 成功快捷方法 (有数据)
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    // 失败快捷方法
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    // 默认失败方法
    public static <T> Result<T> error(String message) {
        return error(500, message);
    }
}
