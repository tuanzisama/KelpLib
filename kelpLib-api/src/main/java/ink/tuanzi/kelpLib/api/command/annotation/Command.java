package ink.tuanzi.kelpLib.api.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个类为命令树根（§6.5 注解式单入口）。一个类一棵命令树；
 * 子命令经 {@link Execute} 声明。
 *
 * <p>注：LiteCommands 集成为首选实现；本注解为自研命令层（回退方案，§11.4）的注解模型，
 * 语义与 LiteCommands 对应注解保持一致。</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Command {

    /** 根命令名（如 {@code "kelp"}）。 */
    String name();

    /** 根命令权限（客户端侧隐藏无权限分支；缺省无权限要求）。 */
    String permission() default "";

    /** 命令描述（可经 Messages 消息键渲染）。 */
    String description() default "";
}
