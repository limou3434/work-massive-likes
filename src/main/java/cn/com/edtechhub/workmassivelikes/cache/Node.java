package cn.com.edtechhub.workmassivelikes.cache;

/**
 * 描述最小堆节点结构
 *
 * @author <a href="https://github.com/limou3434">limou3434</a>
 */
public class Node {

    final String key;

    final int count;

    Node(String key, int count) {
        this.key = key;
        this.count = count;
    }

}
