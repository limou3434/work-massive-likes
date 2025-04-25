package cn.com.edtechhub.workmassivelikes.response;

import cn.com.edtechhub.workmassivelikes.enums.CodeBindMessage;
import lombok.Data;

import java.io.Serializable;

/**
 * 通用响应体描述类
 * 确保所有的接口返回都按照下面的类型结构进行返回, 用户只需要定义这个类型在接口返回值即可, 在接口中使用 ThResult 工具来放回响应即可
 *
 * @author <a href="https://github.com/limou3434">limou3434</a>
 */
@Data
public class BaseResponse<T> implements Serializable {

    /**
     * 状态
     */
    private int code;

    /**
     * 含义
     */
    private String message;

    /**
     * 数据
     */
    private T data;

    /**
     * 外部构造方法, 快速封装 成功 响应体
     *
     * @param codeBindMessage 错误-含义 枚举体
     * @param data 数据
     */
    public BaseResponse(CodeBindMessage codeBindMessage, T data) {
        this.code = codeBindMessage.getCode();
        this.message = codeBindMessage.getMessage();
        this.data = data;
    }

    /**
     * 外部构造方法, 快速封装 错误 响应体
     *
     * @param codeBindMessage 错误-含义 枚举体
     * @param message 消息
     */
    public BaseResponse(CodeBindMessage codeBindMessage, String message) {
        this.code = codeBindMessage.getCode();
        this.message = codeBindMessage.getMessage() + ": " + message;
        this.data = null;
    }

    /**
     * 内部构造方法, 快速封装 临时 响应体(仅供测试)
     *
     * @param code 状态
     * @param message 含义
     * @param data 数据
     */
    private BaseResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
}