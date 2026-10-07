package ink.tuanzi.kelpLib.api.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 配置字段到 YAML 键的显式映射。缺省时使用字段/记录组件名的 kebab-case 形式。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
public @interface Key {

    /** YAML 键（支持点分嵌套路径，如 {@code "database.host"}）。 */
    String value();
}
