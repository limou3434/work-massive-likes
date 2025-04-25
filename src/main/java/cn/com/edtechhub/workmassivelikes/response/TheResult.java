package cn.com.edtechhub.workmassivelikes.response;

import cn.com.edtechhub.workmassivelikes.enums.CodeBindMessage;

/**
 * 便捷响应体工具类
 * 1. 返回成功, 自动处理, {code: 20000, message: "成功", data: { 您来定夺 }}
 * 2. 返回失败, 自动处理, {code: xxxxx, message: "xxxxxxx: xxx"}
 *
 * @author <a href="https://github.com/limou3434">limou3434</a>
 */
public class TheResult {

    /**
     * 构造成功响应体
     *
     * @param data 数据
     * @param <T> data 的类型
     * @return 通用响应体对象
     */
    public static <T> BaseResponse<T> success(CodeBindMessage codeBindMessage, T data) {
        return new BaseResponse<>(codeBindMessage, data);
    }

    /**
     * 构造失败响应体
     *
     * @param codeBindMessage 错误-含义 枚举体
     * @return 通用响应体对象
     */
    public static <T> BaseResponse<T> error(CodeBindMessage codeBindMessage, String message) {
        return new BaseResponse<>(codeBindMessage, message);
    }

}