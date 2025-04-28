package cn.com.edtechhub.workmassivelikes.cache;

/**
 * 最小堆节点
 */
public class Node {

    final String key;

    final int count;

    Node(String key, int count) {
        this.key = key;
        this.count = count;
    }

}
