package ink.tuanzi.kelpLib.api.config;

import ink.tuanzi.kelpLib.api.terminable.Terminable;

import java.util.function.Consumer;

/**
 * 配置引擎（§6.6，双平台）：注解驱动的 record/POJO ↔ YAML 双向映射，
 * 基于 SnakeYAML 节点树操作以保留注释与键顺序；Velocity 侧根目录为 dataDirectory，
 * Bukkit 侧为插件数据目录，由平台适配。
 */
public interface ConfigEngine {

    /**
     * 加载（或首次生成）注解配置。
     *
     * @param plugin 业务插件实例
     * @param type   配置类型（标注 {@link KelpConfig} 的 record/POJO）
     * @param <T>    配置类型
     * @return 配置实例
     */
    <T> T load(Object plugin, Class<T> type);

    /**
     * 将实例写回文件（保留原文件注释；新建文件时按注解顺序与默认值生成）。
     *
     * @param plugin   业务插件实例
     * @param instance 配置实例
     * @param <T>      配置类型
     */
    <T> void save(Object plugin, T instance);

    /**
     * 热重载：监听配置文件变更（watch service），变更时异步重读、经平台同步执行器安全回调。
     *
     * @param plugin   业务插件实例
     * @param type     配置类型
     * @param onChange 收到新配置实例的回调（线程：平台 GLOBAL 作用域执行器）
     * @param <T>      配置类型
     * @return 监听句柄（Terminable，随插件 disable 自动关闭）
     */
    <T> Terminable watch(Object plugin, Class<T> type, Consumer<T> onChange);

    /** 序列化器注册表（内置 Component/Duration/枚举/UUID 等，可扩展）。 */
    SerializerRegistry serializers();
}
