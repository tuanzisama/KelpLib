package ink.tuanzi.kelpLib.api.command;

import java.util.List;

/**
 * 自定义参数解析器（动态参数值在注解模型内的表达方式，§6.5）：
 * 解析原始 token 为类型值，并提供运行时补全候选（可从配置/数据源读取）。
 *
 * @param <T> 目标类型
 */
public interface ArgumentResolver<T> {

    /**
     * 解析参数 token。
     *
     * @param raw 原始 token（非空）
     * @param ctx 命令上下文
     * @return 解析值；无法解析返回 {@code null}（报参数无效）
     */
    T parse(String raw, CommandContext ctx);

    /**
     * 运行时补全候选（默认空）。
     *
     * @param current 当前已输入内容
     * @param ctx     命令上下文
     * @return 候选列表
     */
    default List<String> suggest(String current, CommandContext ctx) {
        return List.of();
    }
}
