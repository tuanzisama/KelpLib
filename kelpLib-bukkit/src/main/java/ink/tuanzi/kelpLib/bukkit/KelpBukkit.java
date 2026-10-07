package ink.tuanzi.kelpLib.bukkit;

import ink.tuanzi.kelpLib.KelpLib;
import ink.tuanzi.kelpLib.api.Kelp;
import ink.tuanzi.kelpLib.api.scheduler.KelpScheduler;
import ink.tuanzi.kelpLib.api.terminable.TerminableConsumer;
import ink.tuanzi.kelpLib.bukkit.listener.ListenerRegistry;
import ink.tuanzi.kelpLib.bukkit.scheduler.RegionSchedulerView;

/**
 * KelpLib Bukkit 侧门面（平台专属视图，§4）：区域/实体调度视图、传统监听器注册、
 * 业务插件生命周期 consumer。平台无关能力仍走 {@link Kelp}。
 */
public final class KelpBukkit {

    private KelpBukkit() {
    }

    public static KelpPlatformBukkit platform() {
        return (KelpPlatformBukkit) Kelp.platform();
    }

    /** KelpLib 插件实例。 */
    public static KelpLib plugin() {
        return KelpLib.getInstance();
    }

    /** 调度抽象（GLOBAL/ASYNC，Paper/Folia 自动适配）。 */
    public static KelpScheduler scheduler() {
        return Kelp.scheduler();
    }

    /** 区域/实体作用域调度视图（Folia 区域线程；Paper 主线程退化）。 */
    public static RegionSchedulerView regions() {
        return platform().regions();
    }

    /** 传统 {@code @EventHandler} 监听器注册（Terminable 绑定）。 */
    public static ListenerRegistry listeners() {
        return platform().listeners();
    }

    /**
     * 业务插件的生命周期 consumer：绑定物随插件 disable 自动关闭。
     *
     * @param platformPlugin 业务插件实例
     */
    public static TerminableConsumer lifecycle(Object platformPlugin) {
        return Kelp.platform().lifecycle(platformPlugin);
    }
}
