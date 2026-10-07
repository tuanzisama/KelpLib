package ink.tuanzi.kelpLib;

import ink.tuanzi.kelpLib.api.terminable.CompositeTerminable;
import ink.tuanzi.kelpLib.bukkit.KelpPlatformBukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * KelpLib Bukkit/Folia 插件入口（load: STARTUP）。
 *
 * <p>{@code onLoad()} 完成门面初始化（同为 STARTUP 的依赖方在其 onLoad 中亦可安全获取，§4）；
 * {@code onEnable()} 注册内置命令、插件消息传输与依赖插件生命周期跟踪；
 * {@code onDisable()} 统一关闭库内一切资源（P9 零残留）。</p>
 */
public final class KelpLib extends JavaPlugin {

    private static volatile KelpLib instance;

    private CompositeTerminable rootLifecycle;
    private KelpPlatformBukkit platform;

    /** 取 KelpLib 插件实例。 */
    public static KelpLib getInstance() {
        KelpLib current = instance;
        if (current == null) {
            throw new IllegalStateException("KelpLib is not loaded yet");
        }
        return current;
    }

    @Override
    public void onLoad() {
        instance = this;
        rootLifecycle = CompositeTerminable.create();
        platform = new KelpPlatformBukkit(this, rootLifecycle);
        ink.tuanzi.kelpLib.api.Kelp.init(platform);
        getLogger().info("KelpLib initialized (platform=" + platform.describe() + ")");
    }

    @Override
    public void onEnable() {
        platform.enable();
        getLogger().info("KelpLib enabled");
    }

    @Override
    public void onDisable() {
        platform.disable();
        ink.tuanzi.kelpLib.api.Kelp.init(null);
        instance = null;
        getLogger().info("KelpLib disabled");
    }
}
