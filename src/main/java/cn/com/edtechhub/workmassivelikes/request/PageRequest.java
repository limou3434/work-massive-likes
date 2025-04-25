package cn.com.edtechhub.workmassivelikes.request;

import lombok.Data;

/**
 * 分页请求
 *
 * @author <a href="https://github.com/limou3434">limou3434</a>
 */
@Data
public class PageRequest {

    /**
     * 当前页号
     */
    private int pageCurrent = 1;

    /**
     * 页面大小
     */
    private int pageSize = 10000; // Integer.MAX_VALUE 不好设置, 一是可能全量时消耗过大, 二是可能超出 ES 的最大查询值

    /**
     * 排序字段
     */
    private String sortField = "account";

    /**
     * 排序顺序
     */
    private String sortOrder = "ascend"; // "descend"

}
