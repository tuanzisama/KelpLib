package ink.tuanzi.kelpLib.api.command;

import ink.tuanzi.kelpLib.api.terminable.Terminable;

/**
 * 命令注册门面（注解式单入口，§6.5）。
 *
 * <p>首选实现为 LiteCommands（Paper 端落到原生 Brigadier，Velocity 端注册到原生 CommandManager）；
 * 当依赖不可达时以自研命令层回退（§11.4 记载的回退路径）：注解模型（
 * {@code @Command}/{@code @Execute}/{@code @Arg}/{@code @Sender}/{@code @Flag}）语义一致，
 * 底层注册到平台命令表（Paper CommandMap / Velocity CommandManager）。</p>
 *
 * <p>命令注册自动纳入 Terminable 生命周期，插件 disable 自动注销（见 {@link Terminable}）。</p>
 */
public interface Commands {

    /**
     * 注册注解命令类（一个类一棵命令树）。可多次调用。
     *
     * @param platformPlugin 业务插件实例（Bukkit 为 {@code JavaPlugin}，Velocity 为插件主类实例）
     * @param commandClasses 注解命令类（需可无参构造）
     */
    void register(Object platformPlugin, Class<?>... commandClasses);

    /**
     * 注册自定义参数类型解析器（动态参数值；重复注册覆盖）。
     *
     * @param type     参数类型
     * @param resolver 解析器
     * @param <T>      参数类型
     */
    <T> void registerArgumentResolver(Class<T> type, ArgumentResolver<T> resolver);
}
