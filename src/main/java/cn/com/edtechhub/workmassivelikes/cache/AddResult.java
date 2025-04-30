package cn.com.edtechhub.workmassivelikes.cache;

import lombok.Data;

/**
 * 描述对 TopK 结构新增后的返回结果
 *
 * @author <a href="https://github.com/limou3434">limou3434</a>
 */
@Data
public class AddResult {

    /**
     * 被挤出的 key
     */
    private final String expelledKey;

    /**
     * 当前 key 是否进入 TopK
     */
    private final boolean isHotKey;

    /**
     * 当前操作的 key
     */
    private final String currentKey;

    public AddResult(String expelledKey, boolean isHotKey, String currentKey) {
        this.expelledKey = expelledKey;
        this.isHotKey = isHotKey;
        this.currentKey = currentKey;
    }

}
