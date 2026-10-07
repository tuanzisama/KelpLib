package ink.tuanzi.kelpLib.bukkit.scheduler;

import ink.tuanzi.kelpLib.api.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * {@link RegionSchedulerView} 实现：Folia 走 RegionScheduler/EntityScheduler（含 retired 自动取消）；
 * Paper 上退化为普通同步任务（主线程），与 §5.3 一致。
 */
public final class RegionSchedulerViewImpl implements RegionSchedulerView {

    private final Plugin plugin;
    private final boolean folia;

    public RegionSchedulerViewImpl(Plugin plugin, boolean folia) {
        this.plugin = plugin;
        this.folia = folia;
    }

    @Override
    public ScheduledTask at(Location location, Consumer<ScheduledTask> task) {
        return atLater(location, Duration.ZERO, task);
    }

    @Override
    public ScheduledTask atLater(Location location, Duration delay, Consumer<ScheduledTask> task) {
        if (!folia) {
            return paperRunLater(delay, task);
        }
        BukkitScheduledTask handle = new BukkitScheduledTask();
        long ticks = PaperScheduler.toTicks(delay);
        Consumer<io.papermc.paper.threadedregions.scheduler.ScheduledTask> adapted = foliaTask -> {
            if (!handle.isCancelled()) {
                task.accept(handle);
            }
        };
        if (ticks <= 0) {
            Bukkit.getRegionScheduler().run(plugin, location, adapted);
        } else {
            Bukkit.getRegionScheduler().runDelayed(plugin, location, adapted, ticks);
        }
        return handle;
    }

    @Override
    public ScheduledTask forEntity(Entity entity, Consumer<ScheduledTask> task) {
        if (!folia) {
            return paperRunLater(Duration.ZERO, task);
        }
        BukkitScheduledTask handle = new BukkitScheduledTask();
        io.papermc.paper.threadedregions.scheduler.ScheduledTask scheduled =
                entity.getScheduler().run(plugin, foliaTask -> {
                    if (!handle.isCancelled()) {
                        task.accept(handle);
                    }
                }, handle::markCancelled); // 实体 retired（死亡/卸载）自动取消
        handle.attach(scheduled);
        return handle;
    }

    private ScheduledTask paperRunLater(Duration delay, Consumer<ScheduledTask> task) {
        BukkitScheduledTask handle = new BukkitScheduledTask();
        long ticks = PaperScheduler.toTicks(delay);
        Runnable body = () -> {
            if (!handle.isCancelled()) {
                task.accept(handle);
            }
        };
        if (ticks <= 0) {
            handle.attach(Bukkit.getScheduler().runTask(plugin, body));
        } else {
            handle.attach(Bukkit.getScheduler().runTaskLater(plugin, body, ticks));
        }
        return handle;
    }
}
