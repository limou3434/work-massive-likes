package cn.com.edtechhub.workmassivelikes.cache;

/**
 * 描述 hash 表中的桶结构
 *
 * @author <a href="https://github.com/limou3434">limou3434</a>
 */
public class Bucket {

    /**
     * 指纹
     */
    long fingerprint;

    /**
     * 计数
     */
    int count;

}
