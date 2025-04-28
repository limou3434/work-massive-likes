package cn.com.edtechhub.workmassivelikes.cache;

import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * TopK 算法声明
 */
public interface TopK {

    /**
     * 向 TopK 数据结构中添加/更新元素(本质是增加空桶或更新计数)
     */
    AddResult add(String key, int increment);

    /**
     * 这个方法返回当前 TopK 数据结构中按某种排序方式排列的前 K 个元素列表
     */
    List<Item> list();

    /**
     * 包含从 TopK 数据结构中不符合前 k 个条件后被踢出的元素阻塞队列
     */
    BlockingQueue<Item> expelled();

    /**
     * 元素随着时间的流逝需要逐渐不再活跃的全局热度降低方法
     */
    void fading();

    /**
     * 返回当前 TopK 数据结构中元素的总增量(总流量)
     */
    long total();

}
