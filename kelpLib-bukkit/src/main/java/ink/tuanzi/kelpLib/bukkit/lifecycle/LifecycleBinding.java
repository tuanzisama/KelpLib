package ink.tuanzi.kelpLib.bukkit.lifecycle;

import org.bukkit.plugin.Plugin;

/**
 * 暴露 consumer 所属插件实例（供 Events.bindWith 等推断注册主体）。
 */
public interface LifecycleBinding {

    /** 所属插件。 */
    Plugin plugin();
}
