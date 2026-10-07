package ink.tuanzi.kelpLib.core.command;

import ink.tuanzi.kelpLib.api.command.ArgumentResolver;
import ink.tuanzi.kelpLib.api.command.CommandContext;
import ink.tuanzi.kelpLib.api.command.annotation.Arg;
import ink.tuanzi.kelpLib.api.command.annotation.Command;
import ink.tuanzi.kelpLib.api.command.annotation.Execute;
import ink.tuanzi.kelpLib.api.command.annotation.Flag;
import ink.tuanzi.kelpLib.api.command.annotation.Sender;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiPredicate;

/**
 * 注解命令树扫描与执行引擎（自研命令层，§11.4 回退方案）：一个类一棵命令树，
 * 支持子命令、@Sender、@Arg（内置类型 + 自定义解析器 + 静态补全 + 贪婪串）、@Flag 布尔标记、
 * 节点权限（补全侧隐藏无权限分支）。
 */
public final class CommandTreeScanner {

    /** 平台权限检查（source, permission）。 */
    public interface PermissionChecker extends BiPredicate<Object, String> {
    }

    private CommandTreeScanner() {
    }

    /** 扫描注解命令类（需可无参构造）。 */
    public static List<CommandModel.RootCommand> scan(Class<?>... classes) {
        List<CommandModel.RootCommand> roots = new ArrayList<>();
        for (Class<?> type : classes) {
            Command annotation = type.getAnnotation(Command.class);
            if (annotation == null) {
                throw new IllegalArgumentException(type.getName() + " is not annotated with @Command");
            }
            Object instance;
            try {
                instance = type.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Command class " + type.getName() + " requires a no-arg constructor", e);
            }
            List<CommandModel.ExecNode> execs = new ArrayList<>();
            for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
                for (Method method : current.getDeclaredMethods()) {
                    Execute execute = method.getAnnotation(Execute.class);
                    if (execute == null) {
                        continue;
                    }
                    if (!Modifier.isPublic(method.getModifiers())) {
                        method.setAccessible(true);
                    }
                    execs.add(new CommandModel.ExecNode(execute.name(), execute.permission(),
                            execute.description(), method, instance, scanParams(method)));
                }
            }
            if (execs.isEmpty()) {
                throw new IllegalArgumentException("Command class " + type.getName() + " has no @Execute methods");
            }
            roots.add(new CommandModel.RootCommand(annotation.name(), annotation.permission(),
                    annotation.description(), instance, List.copyOf(execs)));
        }
        return roots;
    }

    private static List<CommandModel.ParamSpec> scanParams(Method method) {
        List<CommandModel.ParamSpec> params = new ArrayList<>();
        for (Parameter parameter : method.getParameters()) {
            Sender sender = parameter.getAnnotation(Sender.class);
            Flag flag = parameter.getAnnotation(Flag.class);
            Arg arg = parameter.getAnnotation(Arg.class);
            if (sender != null) {
                params.add(new CommandModel.ParamSpec(parameter.getName(), true, false, null, null,
                        parameter.getType(), false, false));
            } else if (flag != null) {
                params.add(new CommandModel.ParamSpec(flag.name(), false, true, flag.name(), null,
                        boolean.class, false, true));
            } else if (arg != null) {
                params.add(new CommandModel.ParamSpec(
                        arg.name().isEmpty() ? parameter.getName() : arg.name(),
                        false, false, null, arg, parameter.getType(),
                        arg.greedy(), arg.optional()));
            } else {
                throw new IllegalArgumentException("Command method parameter '" + parameter.getName()
                        + "' in " + method.getDeclaringClass().getName()
                        + " must be annotated with @Sender, @Arg or @Flag");
            }
        }
        return params;
    }

    /** 剥离 flag 并解析、校验参数，随后反射调用执行方法。 */
    public static String invoke(CommandModel.RootCommand root, CommandModel.ExecNode exec,
                                Object source, String[] rawArgs, PermissionChecker permission,
                                Map<Class<?>, ArgumentResolver<?>> resolvers) {
        // 权限
        if (!exec.permission().isEmpty() && !permission.test(source, exec.permission())) {
            return "无权限执行该命令。";
        }
        // 剥离布尔 flag
        Map<String, Boolean> flags = new LinkedHashMap<>();
        List<String> args = new ArrayList<>(Arrays.asList(rawArgs));
        for (CommandModel.ParamSpec param : exec.params()) {
            if (param.flag()) {
                String token = "--" + param.flagName();
                if (args.remove(token)) {
                    if (!param.flagName().isEmpty()
                            && !permissionOfFlag(permission, source, exec, param)) {
                        return "无权限使用 --" + param.flagName() + " 标记。";
                    }
                    flags.put(param.flagName(), true);
                }
            }
        }
        // 解析位置参数
        Map<String, Object> values = new LinkedHashMap<>();
        int index = 0;
        for (CommandModel.ParamSpec param : exec.params()) {
            if (param.sender()) {
                values.put(param.displayName(), source);
                continue;
            }
            if (param.flag()) {
                values.put(param.flagName(), flags.getOrDefault(param.flagName(), false));
                continue;
            }
            if (param.greedy()) {
                String rest = index < args.size() ? String.join(" ", args.subList(index, args.size())) : null;
                if (rest == null || rest.isEmpty()) {
                    if (param.optional()) {
                        values.put(param.displayName(), null);
                        continue;
                    }
                    return usage(exec);
                }
                values.put(param.displayName(), rest);
                index = args.size();
                continue;
            }
            String token = index < args.size() ? args.get(index) : null;
            if (token == null) {
                if (param.optional()) {
                    values.put(param.displayName(), null);
                    continue;
                }
                return usage(exec);
            }
            index++;
            Object value = coerce(param, token, resolvers, context(source, values, permission, args));
            if (value == null) {
                return "参数 <" + param.displayName() + "> 无效: " + token;
            }
            values.put(param.displayName(), value);
        }
        if (index < args.size()) {
            return usage(exec);
        }
        Object[] callArgs = new Object[exec.params().size()];
        for (int i = 0; i < exec.params().size(); i++) {
            CommandModel.ParamSpec param = exec.params().get(i);
            callArgs[i] = param.flag() ? values.get(param.flagName()) : values.get(param.displayName());
        }
        try {
            exec.method().invoke(exec.instance(), callArgs);
            return null;
        } catch (ReflectiveOperationException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            return "命令执行出错: " + cause.getMessage();
        }
    }

    private static boolean permissionOfFlag(PermissionChecker permission, Object source,
                                            CommandModel.ExecNode exec, CommandModel.ParamSpec param) {
        Flag annotation = null;
        for (Parameter parameter : exec.method().getParameters()) {
            Flag flag = parameter.getAnnotation(Flag.class);
            if (flag != null && flag.name().equals(param.flagName())) {
                annotation = flag;
                break;
            }
        }
        String flagPermission = annotation == null ? "" : annotation.permission();
        return flagPermission.isEmpty() || permission.test(source, flagPermission);
    }

    private static CommandContext context(Object source, Map<String, Object> values,
                                          PermissionChecker permission, List<String> args) {
        return new CommandContext() {
            @Override
            public Object source() {
                return source;
            }

            @Override
            public boolean hasPermission(String perm) {
                return perm.isEmpty() || permission.test(source, perm);
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> T arg(String name) {
                return (T) values.get(name);
            }

            @Override
            public String[] rawArgs() {
                return args.toArray(String[]::new);
            }
        };
    }

    /** 参数类型转换（内置类型 + 自定义解析器）。 */
    @SuppressWarnings("unchecked")
    private static Object coerce(CommandModel.ParamSpec param, String token,
                                 Map<Class<?>, ArgumentResolver<?>> resolvers, CommandContext ctx) {
        ArgumentResolver<Object> resolver = (ArgumentResolver<Object>) resolvers.get(param.type());
        if (resolver != null) {
            return resolver.parse(token, ctx);
        }
        Class<?> type = param.type();
        try {
            if (type == String.class) {
                return token;
            }
            if (type == int.class || type == Integer.class) {
                return Integer.parseInt(token);
            }
            if (type == long.class || type == Long.class) {
                return Long.parseLong(token);
            }
            if (type == double.class || type == Double.class) {
                return Double.parseDouble(token);
            }
            if (type == float.class || type == Float.class) {
                return Float.parseFloat(token);
            }
            if (type == boolean.class || type == Boolean.class) {
                return Boolean.parseBoolean(token);
            }
            if (type == UUID.class) {
                return UUID.fromString(token);
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
        return null;
    }

    /** 补全候选：子命令名（带权限过滤）或当前参数候选（静态 → 自定义解析器）。 */
    @SuppressWarnings("unchecked")
    public static List<String> suggestions(CommandModel.RootCommand root, Object source, String[] rawArgs,
                                           PermissionChecker permission,
                                           Map<Class<?>, ArgumentResolver<?>> resolvers) {
        // 根权限过滤
        if (!root.permission().isEmpty() && !permission.test(source, root.permission())) {
            return List.of();
        }
        // 子命令补全
        if (rawArgs.length <= 1) {
            String current = rawArgs.length == 1 ? rawArgs[0] : "";
            List<String> candidates = new ArrayList<>();
            for (CommandModel.ExecNode exec : root.executes()) {
                if (exec.name().isEmpty()) {
                    continue;
                }
                if (!exec.permission().isEmpty() && !permission.test(source, exec.permission())) {
                    continue; // 客户端侧隐藏无权限分支
                }
                if (exec.name().toLowerCase(Locale.ROOT).startsWith(current.toLowerCase(Locale.ROOT))) {
                    candidates.add(exec.name());
                }
            }
            return candidates;
        }
        // 参数补全：定位匹配的子命令
        CommandModel.ExecNode exec = root.child(rawArgs[0]);
        if (exec == null) {
            exec = root.rootExec();
        }
        if (exec == null) {
            return List.of();
        }
        // 当前正在输入的 token 位置（跳过子命令名）
        int argIndex = rawArgs.length - 1;
        int position = argIndex - 1; // 位置参数序号（不含 sender）
        int seen = 0;
        CommandModel.ParamSpec target = null;
        for (CommandModel.ParamSpec param : exec.params()) {
            if (param.sender() || param.flag()) {
                continue;
            }
            if (seen == position) {
                target = param;
                break;
            }
            seen++;
        }
        if (target == null) {
            return List.of();
        }
        String current = rawArgs[argIndex];
        for (String candidate : target.arg() != null ? target.arg().suggestions() : new String[0]) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(current.toLowerCase(Locale.ROOT))) {
                return List.of(candidate);
            }
        }
        ArgumentResolver<Object> resolver = (ArgumentResolver<Object>) resolvers.get(target.type());
        if (resolver != null) {
            return resolver.suggest(current, context(source, Map.of(), permission, Arrays.asList(rawArgs)));
        }
        return List.of();
    }

    private static String usage(CommandModel.ExecNode exec) {
        StringBuilder sb = new StringBuilder("用法: ");
        for (CommandModel.ParamSpec param : exec.params()) {
            if (param.sender()) {
                continue;
            }
            if (param.flag()) {
                sb.append(" --").append(param.flagName());
            } else {
                sb.append(param.optional() ? " [" : " <").append(param.displayName())
                        .append(param.optional() ? ']' : '>');
            }
        }
        return sb.toString();
    }
}
