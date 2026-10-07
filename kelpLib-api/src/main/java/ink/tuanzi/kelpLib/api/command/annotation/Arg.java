package ink.tuanzi.kelpLib.api.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 命令参数（按声明顺序解析）。支持类型：String、int/long/double/boolean、UUID，
 * 以及经 {@code Kelp.commands().registerArgumentResolver(type, resolver)} 注册的自定义类型；
 * 字符串参数可设 {@link #greedy()} 吞并剩余全部参数。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Arg {

    /** 参数名（补全提示与错误消息展示）。 */
    String name() default "";

    /** 静态候选补全（空则按类型与已注册解析器补全）。 */
    String[] suggestions() default {};

    /** 是否吞并剩余全部参数（仅 String 类型有效）。 */
    boolean greedy() default false;

    /** 参数是否可选（不可选参数缺失时报用法错误）。 */
    boolean optional() default false;
}
