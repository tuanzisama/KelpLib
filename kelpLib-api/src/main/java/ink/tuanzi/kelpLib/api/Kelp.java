package ink.tuanzi.kelpLib.api;

import ink.tuanzi.kelpLib.api.command.Commands;
import ink.tuanzi.kelpLib.api.config.ConfigEngine;
import ink.tuanzi.kelpLib.api.messenger.Messenger;
import ink.tuanzi.kelpLib.api.scheduler.KelpScheduler;
import ink.tuanzi.kelpLib.api.storage.StorageEngine;
import ink.tuanzi.kelpLib.api.text.Display;
import org.jetbrains.annotations.ApiStatus;

/**
 * KelpLib 服务门面（平台无关）。
 *
 * <p>业务插件通过本类的静态方法获取各模块入口：{@link #scheduler()}、{@link #commands()}、
 * {@link #messenger()}、{@link #display()}、{@link #configs()}、{@link #storage()}。
 * 平台专属视图（Bukkit 侧 {@code KelpBukkit}、Velocity 侧 {@code KelpVelocity}）由对应平台模块导出。</p>
 *
 * <p>门面由平台模块在加载期（Bukkit {@code onLoad()} / Velocity 插件构造）初始化；
 * 使用前可用 {@link #isAvailable()} 判断。API 稳定性约定见 {@link ApiStatus} 标注。</p>
 */
public final class Kelp {

    private static volatile KelpPlatform platform;

    private Kelp() {
    }

    /**
     * 由平台模块在加载期调用，注册平台服务实现。
     *
     * @param platform 平台服务实现
     */
    @ApiStatus.Internal
    public static void init(KelpPlatform platform) {
        Kelp.platform = platform;
    }

    /**
     * 门面是否已初始化（KelpLib 是否已安装并完成加载）。
     *
     * @return 已初始化返回 {@code true}
     */
    public static boolean isAvailable() {
        return platform != null;
    }

    /**
     * 取平台服务实现（内部使用，静态工厂类经由它分发）。
     *
     * @return 平台服务实现
     * @throws IllegalStateException KelpLib 未安装或未完成加载
     */
    @ApiStatus.Internal
    public static KelpPlatform platform() {
        KelpPlatform p = platform;
        if (p == null) {
            throw new IllegalStateException(
                    "KelpLib is not available. Declare KelpLib as a plugin dependency (Bukkit: depend: [KelpLib], Velocity: @Dependency(id = \"kelplib\")).");
        }
        return p;
    }

    /** 调度抽象入口（GLOBAL/ASYNC 作用域，双端同 API）。 */
    public static KelpScheduler scheduler() {
        return platform().scheduler();
    }

    /** 命令注册门面（注解式单入口，底层 LiteCommands）。 */
    public static Commands commands() {
        return platform().commands();
    }

    /** 跨服消息通道门面（三通道分层，传输可插拔）。 */
    public static Messenger messenger() {
        return platform().messenger();
    }

    /** 展示 API（ActionBar / Title / BossBar；计分板为 Bukkit 侧专属）。 */
    public static Display display() {
        return platform().display();
    }

    /** 配置引擎入口（注解映射、序列化器注册表、热重载）。 */
    public static ConfigEngine configs() {
        return platform().configEngine();
    }

    /** 存储引擎入口（文件 / SQL / Redis 仓库工厂）。 */
    public static StorageEngine storage() {
        return platform().storageEngine();
    }
}
