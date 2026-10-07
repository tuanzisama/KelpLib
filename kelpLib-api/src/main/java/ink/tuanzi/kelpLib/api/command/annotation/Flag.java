package ink.tuanzi.kelpLib.api.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 布尔命令标记：执行时以 {@code --名称} 形式出现在参数中（位置无关），解析后从参数列表移除。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Flag {

    /** 标记名（{@code --名称}）。 */
    String name();

    /** 标记权限。 */
    String permission() default "";
}
