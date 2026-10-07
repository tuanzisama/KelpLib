package ink.tuanzi.kelpLib.api.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注命令执行方法（无 {@link #name} 时为根命令本身的执行体，有 name 时为子命令）。
 * 方法参数可含：{@code @Sender} 发送者、若干 {@code @Arg} 参数与 {@code @Flag} 布尔标记。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Execute {

    /** 子命令名；缺省表示根命令自身的执行体。 */
    String name() default "";

    /** 子命令权限。 */
    String permission() default "";

    /** 描述。 */
    String description() default "";
}
