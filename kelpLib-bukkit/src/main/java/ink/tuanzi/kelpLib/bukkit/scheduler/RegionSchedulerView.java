package ink.tuanzi.kelpLib.bukkit.scheduler;

import ink.tuanzi.kelpLib.api.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.function.Consumer;

/**
 * 区域/实体作用域调度视图（仅 kelpLib-bukkit 导出；§5.2）。
 * Folia 下任务在目标区块/实体所属区域线程执行；Paper 上退化为普通同步任务（主线程）。
 * {@link #forEntity(Entity, Consumer)} 在 Folia 下跟随实体迁移，实体 retired 时自动取消。
 */
public interface RegionSchedulerView {

    /** 在目标位置所属区域线程（Paper 为主线程）执行任务。 */
    ScheduledTask at(Location location, Consumer<ScheduledTask> task);

    /** 延迟在目标位置所属区域线程执行任务（Duration 换算 tick）。 */
    ScheduledTask atLater(Location location, java.time.Duration delay, Consumer<ScheduledTask> task);

    /** 在实体所属线程执行任务（Folia 跟随实体迁移；Paper 为主线程）。 */
    ScheduledTask forEntity(Entity entity, Consumer<ScheduledTask> task);
}
