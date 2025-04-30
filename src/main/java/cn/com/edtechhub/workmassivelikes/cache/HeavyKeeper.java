package cn.com.edtechhub.workmassivelikes.cache;

import cn.hutool.core.util.HashUtil;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * TopK 算法实现(参考 https://github.com/go-kratos/aegis/tree/main/topk 实现)
 *
 * @author <a href="https://github.com/limou3434">limou3434</a>
 */
public class HeavyKeeper implements TopK {

    /**
     * 当前 TopK 数据结构中的总增量次数(统计整体流量强度, 每次 add 都且只会增加)
     */
    private long total;

    /**
     * 哈希表, 实际结构是一个二维数组
     */
    private final Bucket[][] buckets;

    /**
     * 第一维的大小, 代表哈希表中的层数
     */
    private final int depth;

    /**
     * 第二维的大小, 代表每层中桶的数量
     */
    private final int width;

    /**
     * 衰减系数表
     */
    private final double[] lookupTable;

    /**
     * 衰减系数表的大小
     */
    private static final int LOOKUP_TABLE_SIZE = 256;

    /**
     * 最小堆, 用于存储 TopK 数据结构中的元素
     */
    private final PriorityQueue<Node> minHeap;

    /**
     * 被挤出的元素阻塞队列
     */
    private final BlockingQueue<Item> expelledQueue;

    /**
     * TopK 的 k 值
     */
    private final int k;

    /**
     * 最小计数, 当元素的计数小于此值时, 不会进入 TopK
     */
    private final int minCount;

    /**
     * 随机数生成器
     */
    private final Random random;

    /**
     * 初始化一个 HeavyKeeper 实例
     */
    public HeavyKeeper(int k, int width, int depth, double decay, int minCount) {
        this.k = k;
        this.width = width;
        this.depth = depth;
        this.minCount = minCount;
        this.minHeap = new PriorityQueue<>(Comparator.comparingInt(n -> n.count));
        this.expelledQueue = new LinkedBlockingQueue<>();
        this.random = new Random();
        this.total = 0;

        this.lookupTable = new double[LOOKUP_TABLE_SIZE];
        for (int i = 0; i < LOOKUP_TABLE_SIZE; i++) {
            lookupTable[i] = Math.pow(decay, i);
        }

        this.buckets = new Bucket[depth][width];
        for (int i = 0; i < depth; i++) {
            for (int j = 0; j < width; j++) {
                buckets[i][j] = new Bucket();
            }
        }
    }

    @Override
    public AddResult add(String key, int increment) {
        int maxCount = 0; // 用于追踪当前元素在所有桶中计数的最大值, 用于后续判断是否进入 TopK

        byte[] keyBytes = key.getBytes(); // 将一个字符串 key 转换为字节数组
        long itemFingerprint = hash(keyBytes); // 计算 key 的 hash 指纹

        // 遍历哈希表的每一层来映射哈希桶
        for (int i = 0; i < depth; i++) {
            // 用不同哈希算法
            // TODO: 实际上每一层都需要使用不同的哈希算法, 不过这里我们简单一些, 每一层都只使用同种哈希算法

            // 获得映射后的桶
            int bucketNumber = Math.abs(hash(keyBytes)) % width; // 计算桶的编号, 用于确定要访问的桶
            Bucket bucket = buckets[i][bucketNumber]; // 获取桶

            // 具体的映射过程
            synchronized (bucket) {
                // 如果此时桶为空, 则将此元素添加到桶中
                if (bucket.count == 0) {
                    bucket.fingerprint = itemFingerprint;
                    bucket.count = increment; // 可以自定义增量来增加计数
                    maxCount = Math.max(maxCount, increment);
                }
                // 如果此时桶不空, 并且不是冲突, 则更新计数
                else if (bucket.fingerprint == itemFingerprint) {
                    bucket.count += increment;
                    maxCount = Math.max(maxCount, bucket.count);
                }
                // 如果此时桶不空, 并且发生冲突, 则衰减计数(旨在让占据哈希桶的元素如果不是热点就不要占用过渡)
                else {
                    for (int j = 0; j < increment; j++) {
                        // 取得衰减系数
                        double decay = bucket.count < LOOKUP_TABLE_SIZE ?
                                lookupTable[bucket.count] : // 根据桶中的当前计数值决定衰减系数
                                lookupTable[LOOKUP_TABLE_SIZE - 1]; // 如果桶中的计数值超过了衰减系数表的大小则使用最大的衰减系数

                        // 使用随机数决定是否执行衰减
                        if (random.nextDouble() < decay) {
                            bucket.count--;
                            if (bucket.count == 0) { //  如果计数减少到 0 表示桶中的元素已经被完全衰减
                                bucket.fingerprint = itemFingerprint; // 更新桶的指纹为当前元素的指纹
                                bucket.count = increment - j; // 设置计数为剩余的增量
                                maxCount = Math.max(maxCount, bucket.count); // 更新最大计数
                                break;
                            }
                        }
                    }
                }
            }
        }

        // 更新总流量
        synchronized (this) {
            total += increment;
        }

        // 如果本元素的最大计数小于最小计数, 则表示此元素不会进入 TopK, 直接返回
        if (maxCount < minCount) {
            return new AddResult(null, false, null);
        }

        // 如果本元素的最大计数大于或等于最小计数, 则表示此元素会进入 TopK, 进行相应的操作
        synchronized (minHeap) {
            boolean isHot = false; // 用于标记当前元素是否进入 TopK
            String expelled = null; // 用于记录被挤出的元素

            // 检查当前 key 是否已经在最小堆里面
            Optional<Node> existing = minHeap
                    .stream()
                    .filter(n -> n.key.equals(key))
                    .findFirst();

            // 如果当前 key 已经在最小堆里面, 则将其移出最小堆然后更新计数重新添加到最小堆中
            if (existing.isPresent()) {
                minHeap.remove(existing.get());
                minHeap.add(new Node(key, maxCount));
                isHot = true;
            }
            // 如果当前 key 不在最小堆里面
            else {
                if (
                        minHeap.size() < k || // 如果最小堆未满
                                maxCount >= Objects.requireNonNull(minHeap.peek()).count // 或者当前元素的计数大于等于最小堆中最小的元素的计数
                ) { // 则将当前元素添加到最小堆中
                    Node newNode = new Node(key, maxCount);
                    // 如果是通过第二个条件进来的, 并且最小堆已经满了, 则将最小堆中的最小元素移出最小堆, 并将其添加到被挤出的元素阻塞队列中
                    if (minHeap.size() >= k) {
                        expelled = minHeap.poll().key; // 从小顶堆里删除弹出一个最小元素, 这里还拿到它的 key
                        expelledQueue.offer(new Item(expelled, maxCount)); // 把被踢出去的元素用当前最大计数包装成一个新 Item 放进另一个队列里
                    }
                    minHeap.add(newNode);
                    isHot = true;
                }
                // 如果最小堆已经满了, 并且当前元素的计数小于最小堆中最小的元素的计数, 则不会进入 TopK, 直接返回
            }

            // 最终返回 AddResult 对象, 包含被挤出的元素, 当前元素是否进入 TopK, 以及当前元素的 key
            return new AddResult(expelled, isHot, key);
        }
    }

    @Override
    public List<Item> list() {
        synchronized (minHeap) {
            List<Item> result = new ArrayList<>(minHeap.size());
            for (Node node : minHeap) {
                result.add(new Item(node.key, node.count));
            }
            result.sort((a, b) -> Integer.compare(b.count(), a.count()));
            return result;
        }
    }

    @Override
    public BlockingQueue<Item> expelled() {
        return expelledQueue;
    }

    @Override
    public void fading() {
        // 遍历哈希表的每一层, 对每个桶进行衰减
        for (Bucket[] row : buckets) {
            for (Bucket bucket : row) {
                synchronized (bucket) {
                    bucket.count = bucket.count >> 1;
                }
            }
        }

        // 对最小堆进行衰减
        synchronized (minHeap) {
            PriorityQueue<Node> newHeap = new PriorityQueue<>(Comparator.comparingInt(n -> n.count));
            for (Node node : minHeap) {
                newHeap.add(new Node(node.key, node.count >> 1));
            }
            minHeap.clear(); // 清空最小堆
            minHeap.addAll(newHeap); // 将新的最小堆设置到最小堆中
        }

        // 对总流量进行衰减
        total = total >> 1;
    }

    @Override
    public long total() {
        return total;
    }

    /**
     * 计算指纹的所有哈希算法
     */
    private static int hash(byte[] data) { // TODO: 可以再多传一个层级来区分使用不同的哈希算法
        return HashUtil.murmur32(data);
    }

}

