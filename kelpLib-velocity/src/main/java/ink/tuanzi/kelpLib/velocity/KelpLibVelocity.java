package ink.tuanzi.kelpLib.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import ink.tuanzi.kelpLib.api.Kelp;
import ink.tuanzi.kelpLib.api.terminable.CompositeTerminable;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * KelpLib Velocity 代理插件入口（velocity-plugin.json：id=kelplib）。
 *
 * <p>依赖方经 {@code @Plugin(dependencies = @Dependency(id = "kelplib"))} 声明，
 * 在其插件实例中注入 {@code KelpLibVelocity} 获取 {@code Kelp} 门面（§4）。
 * {@link ProxyInitializeEvent} 完成门面初始化，{@link ProxyShutdownEvent} 统一关闭（P9）。</p>
 */
@Plugin(
        id = "kelplib",
        name = "KelpLib",
        version = "1.0.0",
        description = "Shared utility & extension API library for the utoverse server family.",
        authors = {"evenwan"}
)
public final class KelpLibVelocity {

    private static volatile KelpLibVelocity instance;

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;

    private CompositeTerminable rootLifecycle;
    private KelpPlatformVelocity platform;

    /** Velocity 依赖注入构造（ProxyServer + slf4j Logger + @DataDirectory）。 */
    public KelpLibVelocity(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        instance = this;
        proxy.getEventManager().register(this, new LifecycleListener());
    }

    /** 取代理端插件实例（依赖方注入本类型后也可直接持有，无需静态访问）。 */
    public static KelpLibVelocity getInstance() {
        KelpLibVelocity current = instance;
        if (current == null) {
            throw new IllegalStateException("KelpLib is not loaded yet");
        }
        return current;
    }

    private void initialize() {
        rootLifecycle = CompositeTerminable.create();
        platform = new KelpPlatformVelocity(proxy, logger, dataDirectory, rootLifecycle);
        Kelp.init(platform);
        platform.enable();
        logger.info("KelpLib initialized (platform=velocity)");
    }

    private void shutdown() {
        if (platform != null) {
            platform.disable();
        }
        if (rootLifecycle != null) {
            rootLifecycle.close();
        }
        Kelp.init(null);
        instance = null;
        logger.info("KelpLib disabled");
    }

    public ProxyServer proxy() {
        return proxy;
    }

    public Logger logger() {
        return logger;
    }

    public Path dataDirectory() {
        return dataDirectory;
    }

    /** 生命周期事件监听（初始化/关服）。 */
    private final class LifecycleListener {

        @Subscribe
        public void onInitialize(ProxyInitializeEvent event) {
            initialize();
        }

        @Subscribe
        public void onShutdown(ProxyShutdownEvent event) {
            shutdown();
        }
    }
}
