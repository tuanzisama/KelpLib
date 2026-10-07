package ink.tuanzi.kelpLib.api.terminable;

/**
 * 可绑定 {@link Terminable} 的注册表。插件实例即 consumer：
 * 通过 {@code KelpBukkit.lifecycle(plugin)}（Bukkit）或 {@code KelpVelocity.lifecycle(plugin)}（Velocity）
 * 获取与插件生命周期绑定的 consumer，插件 disable 时统一关闭全部绑定物（幂等）。
 */
public interface TerminableConsumer extends Terminable {

    /**
     * 绑定一个可关闭对象。若 consumer 已关闭，则立即关闭该对象。
     *
     * @param terminable 待绑定对象
     * @param <T>        对象类型（便于链式使用）
     * @return 被绑定的对象本身
     */
    <T extends Terminable> T bind(T terminable);
}
