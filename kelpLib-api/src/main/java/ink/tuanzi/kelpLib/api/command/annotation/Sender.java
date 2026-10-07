package ink.tuanzi.kelpLib.api.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注命令方法的发送者参数（平台原生发送者类型，Bukkit 为 CommandSender/Player，
 * Velocity 为 CommandSource/Player）。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Sender {
}
