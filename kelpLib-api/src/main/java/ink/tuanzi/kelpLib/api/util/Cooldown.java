package ink.tuanzi.kelpLib.api.util;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 并发安全冷却器（按 UUID 键控，双平台可用，§6.3）。
 *
 * <pre>{@code
 * Cooldown cd = Cooldown.of(Duration.ofSeconds(30));
 * if (cd.tryAcquire(player.getUniqueId())) { // 放行
 * } else { // 剩余时间提示：cd.remaining(player.getUniqueId())
 * }
 * }</pre>
 */
public final class Cooldown {

    private final Duration duration;
    private final Map<UUID, Long> expiry = new ConcurrentHashMap<>();

    private Cooldown(Duration duration) {
        this.duration = duration;
    }

    /** 创建固定时长的冷却器。 */
    public static Cooldown of(Duration duration) {
        return new Cooldown(duration);
    }

    /**
     * 尝试获取一次冷却配额。
     *
     * @param id 键（玩家 UUID 等）
     * @return 允许执行返回 true（并开始冷却）；冷却中返回 false
     */
    public boolean tryAcquire(UUID id) {
        long now = System.currentTimeMillis();
        long expires = now + duration.toMillis();
        Long current = expiry.get(id);
        if (current != null && current > now) {
            return false;
        }
        expiry.put(id, expires);
        return true;
    }

    /** 是否处于冷却中。 */
    public boolean isCooling(UUID id) {
        Long expires = expiry.get(id);
        return expires != null && expires > System.currentTimeMillis();
    }

    /** 剩余冷却时长（不在冷却中返回 {@link Duration#ZERO}）。 */
    public Duration remaining(UUID id) {
        Long expires = expiry.get(id);
        if (expires == null) {
            return Duration.ZERO;
        }
        long left = expires - System.currentTimeMillis();
        return left <= 0 ? Duration.ZERO : Duration.ofMillis(left);
    }

    /** 重置（清除）某键的冷却。 */
    public void reset(UUID id) {
        expiry.remove(id);
    }

    /** 清除全部冷却。 */
    public void clear() {
        expiry.clear();
    }

    /** 清理已过期的键（可低频调用以免长驻内存）。 */
    public void clearExpired() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> it = expiry.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue() <= now) {
                it.remove();
            }
        }
    }
}
