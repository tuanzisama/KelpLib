package ink.tuanzi.kelpLib.api.command;

/**
 * 命令执行上下文（供 ArgumentResolver 等使用）。
 */
public interface CommandContext {

    /** 平台发送者（Bukkit CommandSender / Velocity CommandSource）。 */
    Object source();

    /** 发送者权限检查（代理到平台实现）。 */
    boolean hasPermission(String permission);

    /** 取已解析参数值；缺失返回 {@code null}。 */
    <T> T arg(String name);

    /** 原始参数（已剥离 flag）。 */
    String[] rawArgs();
}
