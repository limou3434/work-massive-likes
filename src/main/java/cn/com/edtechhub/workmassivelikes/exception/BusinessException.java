package cn.com.edtechhub.workmassivelikes.exception;

import cn.com.edtechhub.workmassivelikes.enums.CodeBindMessage;
import lombok.Getter;

/**
 * 业务内异常类
 *
 * @author <a href="https://github.com/limou3434">limou3434</a>
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误-含义
     */
    CodeBindMessage codeBindMessage;

    /**
     * 详细信息
     */
    String exceptionMessage;

    /**
     * 构造异常对象
     *
     * @param codeBindMessage 错误-含义 枚举体
     * @param exceptionMessage 详细信息
     */
    public BusinessException(CodeBindMessage codeBindMessage, String exceptionMessage) {
        this.codeBindMessage = codeBindMessage;
        this.exceptionMessage = exceptionMessage;
    }

}