package ink.tuanzi.kelpLib.bukkit;

import ink.tuanzi.kelpLib.api.KelpPlatform;
import ink.tuanzi.kelpLib.api.command.ArgumentResolver;
import ink.tuanzi.kelpLib.api.command.Commands;
import ink.tuanzi.kelpLib.api.config.ConfigEngine;
import ink.tuanzi.kelpLib.api.config.ConfigSerializer;
import ink.tuanzi.kelpLib.api.messenger.Messenger;
import ink.tuanzi.kelpLib.api.promise.PromiseFactory;
import ink.tuanzi.kelpLib.api.scheduler.KelpScheduler;
import ink.tuanzi.kelpLib.api.storage.StorageEngine;
import ink.tuanzi.kelpLib.KelpLib;
import ink.tuanzi.kelpLib.core.messenger.MessengerImpl;
import ink.tuanzi.kelpLib.api.terminable.CompositeTerminable;
import ink.tuanzi.kelpLib.api.terminable.Terminable;
import ink.tuanzi.kelpLib.api.terminable.TerminableConsumer;
import ink.tuanzi.kelpLib.api.text.Display;
import ink.tuanzi.kelpLib.api.text.LocaleResolver;
import ink.tuanzi.kelpLib.api.text.Messages;
import ink.tuanzi.kelpLib.bukkit.command.BukkitCommandsImpl;
import ink.tuanzi.kelpLib.bukkit.listener.Events;
import ink.tuanzi.kelpLib.bukkit.listener.ListenerRegistry;
import ink.tuanzi.kelpLib.bukkit.lifecycle.BukkitLifecycleConsumer;
import ink.tuanzi.kelpLib.bukkit.scheduler.FoliaScheduler;
import ink.tuanzi.kelpLib.bukkit.scheduler.PaperScheduler;
import ink.tuanzi.kelpLib.bukkit.scheduler.RegionSchedulerViewImpl;
import ink.tuanzi.kelpLib.bukkit.scheduler.SchedulerDetector;
import ink.tuanzi.kelpLib.bukkit.transport.PluginMessageTransport;
import ink.tuanzi.kelpLib.core.config.ConfigEngineImpl;
import ink.tuanzi.kelpLib.core.config.SerializerRegistryImpl;
import ink.tuanzi.kelpLib.core.promise.CorePromiseFactory;
import ink.tuanzi.kelpLib.core.storage.StorageEngineImpl;
import ink.tuanzi.kelpLib.core.text.AdventureDisplay;
import ink.tuanzi.kelpLib.core.text.MessagesImpl;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;

import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bukkit/Folia 平台服务实现（{@link KelpPlatform}）：
 * 调度（Paper/Folia 双实现探测切换）、Promise、配置、存储、Messenger、命令、展示、生命周期。
 */
public final class KelpPlatformBukkit implements KelpPlatform {

    private static final Logger LOGGER = Logger.getLogger("KelpLib");

    private final KelpLib plugin;
    private final CompositeTerminable rootLifecycle;
    private final ConcurrentHashMap<String, BukkitLifecycleConsumer> pluginLifecycles = new ConcurrentHashMap<>();

    private final KelpScheduler scheduler;
    private final RegionSchedulerViewImpl regions;
    private final CorePromiseFactory promiseFactory;
    private final SerializerRegistryImpl serializers;
    private final ConfigEngineImpl configEngine;
    private final StorageEngineImpl storageEngine;
    private final MessengerImplHolder messengerHolder = new MessengerImplHolder();
    private final Display display = new AdventureDisplay();
    private final BukkitCommandsImpl commands;
    private final ListenerRegistry listeners = new ListenerRegistry();
    private final LocaleResolver localeResolver = audience -> audience instanceof Player player ? player.locale() : null;

    public KelpPlatformBukkit(KelpLib plugin, CompositeTerminable rootLifecycle) {
        this.plugin = plugin;
        this.rootLifecycle = rootLifecycle;
        this.scheduler = SchedulerDetector.isFolia() ? new FoliaScheduler(plugin) : new PaperScheduler(plugin);
        this.regions = new RegionSchedulerViewImpl(plugin, SchedulerDetector.isFolia());
        this.promiseFactory = new CorePromiseFactory(scheduler.syncExecutor(), scheduler.asyncExecutor());
        rootLifecycle.bind(promiseFactory);
        this.serializers = new SerializerRegistryImpl();
        registerBukkitSerializers(serializers);
        this.configEngine = new ConfigEngineImpl(this::dataDirectory, scheduler, serializers);
        this.storageEngine = new StorageEngineImpl(this::dataDirectory, serializers);
        this.commands = new BukkitCommandsImpl(this);
    }

    private static void registerBukkitSerializers(SerializerRegistryImpl registry) {
        registry.register(Location.class, new ConfigSerializer<Location>() {
            @Override
            public Location deserialize(String raw) {
                // world;x;y;z;yaw;pitch
                String[] parts = raw.split(";");
                World world = org.bukkit.Bukkit.getWorld(parts[0]);
                if (world == null) {
                    throw new IllegalArgumentException("Unknown world: " + parts[0]);
                }
                Location location = new Location(world, Double.parseDouble(parts[1]),
                        Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
                if (parts.length > 4) {
                    location.setYaw(Float.parseFloat(parts[4]));
                    location.setPitch(Float.parseFloat(parts[5]));
                }
                return location;
            }

            @Override
            public String serialize(Location location) {
                String world = location.getWorld() == null ? "" : location.getWorld().getName();
                return world + ";" + location.getX() + ";" + location.getY() + ";" + location.getZ()
                        + ";" + location.getYaw() + ";" + location.getPitch();
            }
        });
    }

    /** onEnable：注册依赖插件生命周期跟踪与默认传输。 */
    public void enable() {
        Events.subscribe(PluginDisableEvent.class)
                .filter(event -> event.getPlugin() != plugin)
                .handler(event -> {
                    BukkitLifecycleConsumer consumer = pluginLifecycles.remove(event.getPlugin().getName());
                    if (consumer != null) {
                        try {
                            consumer.close();
                        } catch (Throwable t) {
                            LOGGER.log(Level.WARNING, "Failed to close lifecycle of " + event.getPlugin().getName(), t);
                        }
                    }
                })
                .bindWith(rootLifecycle);
        // 默认插件消息传输（Bukkit↔Velocity 直连通道，M5）
        messengerHolder.registerIfAbsent(new PluginMessageTransport());
    }

    /** onDisable：统一关闭库内资源。 */
    public void disable() {
        rootLifecycle.close();
        storageEngine.close();
        messengerHolder.close();
    }

    @Override
    public String id() {
        return "bukkit";
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
        return messengerHolder.get();
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
        if (platformPlugin instanceof Plugin bukkitPlugin) {
            return bukkitPlugin.getDataFolder().toPath();
        }
        throw new IllegalArgumentException("Not a Bukkit plugin instance: " + platformPlugin);
    }

    @Override
    public TerminableConsumer lifecycle(Object platformPlugin) {
        if (!(platformPlugin instanceof Plugin bukkitPlugin)) {
            throw new IllegalArgumentException("Not a Bukkit plugin instance: " + platformPlugin);
        }
        return pluginLifecycles.computeIfAbsent(bukkitPlugin.getName(),
                name -> new BukkitLifecycleConsumer(bukkitPlugin));
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
    public boolean supportsRegionScheduler() {
        return true;
    }

    @Override
    public String describe() {
        return SchedulerDetector.isFolia() ? "bukkit/folia (regionized)" : "bukkit/paper";
    }

    /** 注册自定义参数解析器（转发给命令门面）。 */
    public <T> void registerArgumentResolver(Class<T> type, ArgumentResolver<T> resolver) {
        commands.registerArgumentResolver(type, resolver);
    }

    /** Listener 注册入口。 */
    public ListenerRegistry listeners() {
        return listeners;
    }

    /** 区域/实体作用域调度视图。 */
    public RegionSchedulerViewImpl regions() {
        return regions;
    }

    /** 延迟初始化的 Messenger 持有者（绑定共享序列化器与调度执行器）。 */
    private final class MessengerImplHolder {

        private volatile MessengerImpl instance;

        Messenger get() {
            MessengerImpl current = instance;
            if (current != null) {
                return current;
            }
            synchronized (this) {
                if (instance == null) {
                    instance = new MessengerImpl(serializers, scheduler.asyncExecutor());
                }
                return instance;
            }
        }

        void registerIfAbsent(ink.tuanzi.kelpLib.api.messenger.Transport transport) {
            get().registerTransport(transport);
        }

        void close() {
            MessengerImpl current = instance;
            if (current != null) {
                current.close();
            }
        }
    }
}
