package ink.tuanzi.kelpLib.core.command;

import ink.tuanzi.kelpLib.api.command.annotation.Arg;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 命令树模型（注解扫描产物）。
 */
public final class CommandModel {

    private CommandModel() {
    }

    /** 根命令。 */
    public record RootCommand(
            String name,
            String permission,
            String description,
            Object instance,
            List<ExecNode> executes
    ) {
        /** 根命令自身的执行体（无 @Execute(name) 的方法）。 */
        public ExecNode rootExec() {
            for (ExecNode exec : executes) {
                if (exec.name().isEmpty()) {
                    return exec;
                }
            }
            return null;
        }

        /** 按子命令名查找。 */
        public ExecNode child(String name) {
            for (ExecNode exec : executes) {
                if (!exec.name().isEmpty() && exec.name().equalsIgnoreCase(name)) {
                    return exec;
                }
            }
            return null;
        }
    }

    /** 执行节点（根执行体或子命令）。 */
    public record ExecNode(
            String name,
            String permission,
            String description,
            Method method,
            Object instance,
            List<ParamSpec> params
    ) {
    }

    /** 参数规格。 */
    public record ParamSpec(
            String displayName,
            boolean sender,
            boolean flag,
            String flagName,
            Arg arg,
            Class<?> type,
            boolean greedy,
            boolean optional
    ) {
    }
}
