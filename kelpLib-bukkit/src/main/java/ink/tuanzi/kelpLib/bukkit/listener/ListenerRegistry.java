package ink.tuanzi.kelpLib.bukkit.listener;

import ink.tuanzi.kelpLib.api.terminable.Terminable;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 传统 {@code @EventHandler} 监听器的 Terminable 绑定（§6.2 与原生互操作路径）。
 */
public final class ListenerRegistry {

    /** 注册监听器并返回可纳入生命周期的句柄（close 即 unregisterAll）。 */
    public Terminable register(Plugin plugin, Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        return new ListenerHandle(listener);
    }

    private static final class ListenerHandle implements Terminable {

        private final Listener listener;
        private final AtomicBoolean closed = new AtomicBoolean(false);

        private ListenerHandle(Listener listener) {
            this.listener = listener;
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                HandlerList.unregisterAll(listener);
            }
        }

        @Override
        public boolean isClosed() {
            return closed.get();
        }
    }
}
