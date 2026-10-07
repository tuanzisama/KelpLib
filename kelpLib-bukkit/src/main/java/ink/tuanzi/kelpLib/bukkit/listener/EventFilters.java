package ink.tuanzi.kelpLib.bukkit.listener;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 预置事件过滤器（§6.2）：ignoreCancelled、playerHasPermission、ignoreSameBlock 等。
 */
public final class EventFilters {

    private EventFilters() {
    }

    /** 忽略已取消的事件（对非 Cancellable 事件恒通过）。 */
    public static <E extends Event> Predicate<E> ignoreCancelled() {
        return event -> !(event instanceof Cancellable cancellable) || !cancellable.isCancelled();
    }

    /** 仅放行具有指定权限的玩家事件（无法判定玩家的事件恒拒绝）。 */
    public static <E extends Event> Predicate<E> playerHasPermission(String permission) {
        return event -> {
            Player player = playerOf(event);
            return player != null && player.hasPermission(permission);
        };
    }

    /** 忽略同一玩家对同一方块的连续重复事件（按最近一次记录判断）。 */
    public static <E extends Event> Predicate<E> ignoreSameBlock(
            Function<E, Player> player, Function<E, Block> block) {
        Map<UUID, String> lastBlock = new ConcurrentHashMap<>();
        return event -> {
            Player p = player.apply(event);
            Block b = block.apply(event);
            if (p == null || b == null) {
                return true;
            }
            String key = b.getWorld().getName() + ";" + b.getX() + ";" + b.getY() + ";" + b.getZ();
            String previous = lastBlock.put(p.getUniqueId(), key);
            return !key.equals(previous);
        };
    }

    private static Player playerOf(Event event) {
        if (event instanceof PlayerEvent playerEvent) {
            return playerEvent.getPlayer();
        }
        return null;
    }
}
