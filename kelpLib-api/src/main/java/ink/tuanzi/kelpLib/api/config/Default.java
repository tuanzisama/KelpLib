package ink.tuanzi.kelpLib.api.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 配置字段默认值（字符串形式，经 {@link ConfigSerializer} 解析为目标类型）。
 * 文件缺失键或新建文件时使用。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
public @interface Default {

    /** 默认值（字符串形式，如 {@code "3"}、{@code "<green>[Kelp]</green>"}、{@code "5m30s"}）。 */
    String value();
}
