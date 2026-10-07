package ink.tuanzi.kelpLib.velocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import ink.tuanzi.kelpLib.api.KelpPlatform;
import ink.tuanzi.kelpLib.api.command.ArgumentResolver;
import ink.tuanzi.kelpLib.api.command.Commands;
import ink.tuanzi.kelpLib.api.config.ConfigEngine;
import ink.tuanzi.kelpLib.api.messenger.Messenger;
import ink.tuanzi.kelpLib.api.promise.PromiseFactory;
import ink.tuanzi.kelpLib.api.scheduler.KelpScheduler;
import ink.tuanzi.kelpLib.api.storage.StorageEngine;
import ink.tuanzi.kelpLib.api.terminable.CompositeTerminable;
import ink.tuanzi.kelpLib.api.terminable.Terminable;
import ink.tuanzi.kelpLib.api.terminable.TerminableConsumer;
import ink.tuanzi.kelpLib.api.text.Display;
import ink.tuanzi.kelpLib.api.text.LocaleResolver;
import ink.tuanzi.kelpLib.api.text.Messages;
import ink.tuanzi.kelpLib.core.config.ConfigEngineImpl;
import ink.tuanzi.kelpLib.core.config.SerializerRegistryImpl;
import ink.tuanzi.kelpLib.core.messenger.MessengerImpl;
import ink.tuanzi.kelpLib.core.promise.CorePromiseFactory;
import ink.tuanzi.kelpLib.core.storage.StorageEngineImpl;
import ink.tuanzi.kelpLib.core.text.AdventureDisplay;
import ink.tuanzi.kelpLib.core.text.MessagesImpl;
import ink.tuanzi.kelpLib.velocity.command.VelocityCommandsImpl;
import ink.tuanzi.kelpLib.velocity.scheduler.VelocityPlatformScheduler;
import ink.tuanzi.kelpLib.velocity.transport.PluginMessageTransport;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Velocity 平台服务实现（{@link KelpPlatform}）：调度（VelocityScheduler 池）、
 * Promise、配置（dataDirectory 根）、存储、Messenger（Redis/插件消息传输）、命令、展示子集。
 */
public final class KelpPlatformVelocity implements KelpPlatform {

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private final CompositeTerminable rootLifecycle;
    private final ConcurrentHashMap<String, TerminableConsumer> pluginLifecycles = new ConcurrentHashMap<>();

    private final VelocityPlatformScheduler scheduler;
    private final CorePromiseFactory promiseFactory;
    private final SerializerRegistryImpl serializers;
    private final ConfigEngineImpl configEngine;
    private final StorageEngineImpl storageEngine;
    private final MessengerImpl messenger;
    private final Display display = new AdventureDisplay();
    private final VelocityCommandsImpl commands;
    private final LocaleResolver localeResolver = audience -> {
        if (audience instanceof Player player) {
            try {
                return player.getSettings().getLocale();
            } catch (Throwable ignored) {
                return null;
            }
        }
        return null;
    };

    KelpPlatformVelocity(ProxyServer proxy, Logger logger, Path dataDirectory, CompositeTerminable rootLifecycle) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.rootLifecycle = rootLifecycle;
        this.scheduler = new VelocityPlatformScheduler(proxy, KelpLibVelocity.getInstance());
        this.promiseFactory = new CorePromiseFactory(scheduler.syncExecutor(), scheduler.asyncExecutor());
        rootLifecycle.bind(promiseFactory);
        this.serializers = new SerializerRegistryImpl();
        this.configEngine = new ConfigEngineImpl(plugin -> dataDirectory, scheduler, serializers);
        this.storageEngine = new StorageEngineImpl(plugin -> dataDirectory, serializers);
        this.messenger = new MessengerImpl(serializers, scheduler.asyncExecutor());
        this.commands = new VelocityCommandsImpl(proxy, this::bindLifecycle);
    }

    /** enable：注册默认插件消息传输（Bukkit↔Velocity 直连通道）。 */
    public void enable() {
        messenger.registerTransport(new PluginMessageTransport(proxy));
    }

    /** disable：统一关闭库内资源（P9）。 */
    public void disable() {
        rootLifecycle.close();
        storageEngine.close();
        messenger.close();
    }

    private void bindLifecycle(Terminable terminable) {
        rootLifecycle.bind(terminable);
    }

    @Override
    public String id() {
        return "velocity";
    }

    @Override
    public KelpScheduler scheduler() {
        return scheduler;
    }

    @Override
    public Commands commands() {
        return commands;
    }

    @Override
    public Messenger messenger() {
        return messenger;
    }

    @Override
    public Display display() {
        return display;
    }

    @Override
    public ConfigEngine configEngine() {
        return configEngine;
    }

    @Override
    public StorageEngine storageEngine() {
        return storageEngine;
    }

    @Override
    public PromiseFactory promiseFactory() {
        return promiseFactory;
    }

    @Override
    public Path dataDirectory(Object platformPlugin) {
        return dataDirectory;
    }

    @Override
    public TerminableConsumer lifecycle(Object platformPlugin) {
        String key = platformPlugin == null ? "unknown" : platformPlugin.getClass().getName();
        return pluginLifecycles.computeIfAbsent(key, name -> new CompositeTerminable() {
            private final CompositeTerminable composite = CompositeTerminable.create();

            @Override
            public <T extends Terminable> T bind(T terminable) {
                return composite.bind(terminable);
            }

            @Override
            public void close() {
                composite.close();
            }

            @Override
            public boolean isClosed() {
                return composite.isClosed();
            }
        });
    }

    @Override
    public LocaleResolver defaultLocaleResolver() {
        return localeResolver;
    }

    @Override
    public Messages.Builder messages(Object platformPlugin) {
        return new MessagesImpl.Builder(dataDirectory(platformPlugin), localeResolver);
    }

    @Override
    public String describe() {
        return "velocity (proxy)";
    }

    /** 注册自定义参数解析器（转发给命令门面）。 */
    public <T> void registerArgumentResolver(Class<T> type, ArgumentResolver<T> resolver) {
        commands.registerArgumentResolver(type, resolver);
    }
}
