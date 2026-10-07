package ink.tuanzi.kelpLib.api.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个 record/POJO 为注解驱动配置（§6.6）：实例与 YAML 文件双向映射，保留注释与键顺序。
 *
 * <pre>{@code
 * @KelpConfig(path = "config.yml")
 * public record PluginConfig(
 *     @Key("prefix") @Default("<green>[Kelp]</green>") Component prefix,
 *     @Key("max-homes") @Default("3") int maxHomes
 * ) {}
 *
 * PluginConfig cfg = Configs.load(plugin, PluginConfig.class);
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface KelpConfig {

    /** 配置文件相对路径（相对插件数据目录 / Velocity dataDirectory）。 */
    String path() default "config.yml";
}
