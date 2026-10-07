package ink.tuanzi.kelpLib.api.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 按 tick 分区分散大批量元素处理（helper 的 Bucket 思想，§6.3）：
 * Folia 下把大批量负载摊到多个 tick 执行，避免单 tick 卡顿。
 *
 * <pre>{@code
 * Bucket<Player> bucket = Bucket.of(20); // 分散到 20 个 tick
 * allPlayers.forEach(bucket::add);
 * // 每个 tick：bucket.poll() 返回当前分区的元素
 * }</pre>
 *
 * @param <T> 元素类型
 */
public final class Bucket<T> {

    private final List<List<T>> buckets;
    private final AtomicInteger cursor = new AtomicInteger();

    private Bucket(int bucketCount) {
        List<List<T>> lists = new ArrayList<>(bucketCount);
        for (int i = 0; i < bucketCount; i++) {
            lists.add(new ArrayList<>());
        }
        this.buckets = Collections.unmodifiableList(lists);
    }

    /** 创建指定分区数的桶。 */
    public static <T> Bucket<T> of(int bucketCount) {
        if (bucketCount < 1) {
            throw new IllegalArgumentException("bucketCount must be >= 1");
        }
        return new Bucket<>(bucketCount);
    }

    /** 添加元素（轮转写入下一分区；并发安全到"不丢元素"粒度）。 */
    public void add(T element) {
        int index = Math.floorMod(cursor.getAndIncrement(), buckets.size());
        buckets.get(index).add(element);
    }

    /**
     * 取出当前分区内容并推进游标（返回的列表随后会被清空复用，调用方不应长期持有）。
     *
     * @return 当前分区的元素快照
     */
    public List<T> poll() {
        int index = Math.floorMod(cursor.getAndIncrement(), buckets.size());
        List<T> bucket = buckets.get(index);
        List<T> snapshot = new ArrayList<>(bucket);
        bucket.clear();
        return snapshot;
    }

    /** 未消费的元素总数。 */
    public int size() {
        int total = 0;
        for (List<T> bucket : buckets) {
            total += bucket.size();
        }
        return total;
    }

    /** 清空全部分区。 */
    public void clear() {
        buckets.forEach(List::clear);
    }
}
