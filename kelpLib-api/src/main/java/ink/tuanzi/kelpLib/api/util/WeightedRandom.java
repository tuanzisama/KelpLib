package ink.tuanzi.kelpLib.api.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/**
 * 加权随机（双平台，§6.3）。
 *
 * @param <T> 结果类型
 */
public final class WeightedRandom<T> {

    private record Entry<T>(T value, double weight) {
    }

    private final List<Entry<T>> entries;
    private final double total;

    private WeightedRandom(List<Entry<T>> entries) {
        this.entries = List.copyOf(entries);
        double sum = 0;
        for (Entry<T> entry : entries) {
            sum += entry.weight();
        }
        this.total = sum;
    }

    /** 由权重映射构建。 */
    public static <T> WeightedRandom<T> of(Map<T, Double> weights) {
        Builder<T> builder = builder();
        weights.forEach(builder::add);
        return builder.build();
    }

    /** 构建器。 */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /** 是否为空（无正权重条目）。 */
    public boolean isEmpty() {
        return total <= 0;
    }

    /** 以 ThreadLocalRandom 抽取。 */
    public T next() {
        return next(ThreadLocalRandom.current());
    }

    /** 以指定随机源抽取；空集抛 {@link IllegalStateException}。 */
    public T next(RandomGenerator random) {
        if (isEmpty()) {
            throw new IllegalStateException("WeightedRandom has no entries");
        }
        double r = random.nextDouble() * total;
        for (Entry<T> entry : entries) {
            r -= entry.weight();
            if (r < 0) {
                return entry.value();
            }
        }
        return entries.get(entries.size() - 1).value();
    }

    /** 构建器。 */
    public static final class Builder<T> {

        private final Map<T, Double> weights = new LinkedHashMap<>();

        /** 添加一个带权条目（权重 &le; 0 的条目被忽略）。 */
        public Builder<T> add(T value, double weight) {
            if (weight > 0) {
                weights.put(value, weight);
            }
            return this;
        }

        /** 构建。 */
        public WeightedRandom<T> build() {
            List<Entry<T>> entries = new ArrayList<>(weights.size());
            weights.forEach((value, weight) -> entries.add(new Entry<>(value, weight)));
            return new WeightedRandom<>(entries);
        }
    }
}
