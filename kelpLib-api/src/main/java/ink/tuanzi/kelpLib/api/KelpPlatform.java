package ink.tuanzi.kelpLib.api;

import ink.tuanzi.kelpLib.api.command.Commands;
import ink.tuanzi.kelpLib.api.config.ConfigEngine;
import ink.tuanzi.kelpLib.api.messenger.Messenger;
import ink.tuanzi.kelpLib.api.promise.PromiseFactory;
import ink.tuanzi.kelpLib.api.scheduler.KelpScheduler;
import ink.tuanzi.kelpLib.api.storage.StorageEngine;
import ink.tuanzi.kelpLib.api.text.Display;
import ink.tuanzi.kelpLib.api.text.LocaleResolver;
import ink.tuanzi.kelpLib.api.text.Messages;
import ink.tuanzi.kelpLib.api.terminable.TerminableConsumer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * 平台服务 SPI（内部）。由 kelpLib-bukkit / kelpLib-velocity 实现，经 {@link Kelp#init(KelpPlatform)} 注册。
 *
 * <p>该接口是共享层（api/core）触达平台能力的唯一收口，业务插件不应使用。</p>
 */
@ApiStatus.Internal
public interface KelpPlatform {

    /** 平台标识（{@code "bukkit"} / {@code "velocity"}）。 */
    String id();

    /** 平台调度实现。 */
    KelpScheduler scheduler();

    /** 命令注册实现。 */
    Commands commands();

    /** Messenger 实现。 */
    Messenger messenger();

    /** 展示 API 实现。 */
    Display display();

    /** 配置引擎实现。 */
    ConfigEngine configEngine();

    /** 存储引擎实现。 */
    StorageEngine storageEngine();

    /** Promise 工厂实现（注入平台同步/异步执行器语义）。 */
    PromiseFactory promiseFactory();

    /**
     * 业务插件的数据目录（Bukkit 为插件数据目录，Velocity 为 dataDirectory）。
     *
     * @param plugin 业务插件实例（平台原生插件对象）
     * @return 数据目录
     */
    Path dataDirectory(Object plugin);

    /**
     * 取业务插件绑定生命周期的 {@link TerminableConsumer}：插件 disable（代理端为关服/卸载）时统一关闭其绑定物。
     *
     * @param plugin 业务插件实例
     * @return 生命周期 consumer（同插件重复获取返回同一实例）
     */
    TerminableConsumer lifecycle(Object plugin);

    /**
     * 平台默认的 locale 解析器（Bukkit 经 {@code Player#locale()}，Velocity 经 {@code PlayerSettings#getLocale()}）。
     *
     * @return 默认解析器（不可为 null，但可能对未知 audience 返回 {@code null}）
     */
    LocaleResolver defaultLocaleResolver();

    /**
     * 为业务插件构造 {@link Messages} 构建器（注入数据目录与默认 locale 解析）。
     *
     * @param plugin 业务插件实例
     * @return Messages 构建器
     */
    Messages.Builder messages(Object plugin);

    /** 平台是否支持区域调度视图（仅 Bukkit 侧返回 true；供平台模块内部探测）。 */
    default boolean supportsRegionScheduler() {
        return false;
    }

    /**
     * 平台附加上下文信息，用于诊断输出（可选）。
     *
     * @return 描述字符串，无则 {@code null}
     */
    default @Nullable String describe() {
        return null;
    }
}
