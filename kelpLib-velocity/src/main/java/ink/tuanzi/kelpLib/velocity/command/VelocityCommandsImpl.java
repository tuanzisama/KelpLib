package ink.tuanzi.kelpLib.velocity.command;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import ink.tuanzi.kelpLib.api.command.ArgumentResolver;
import ink.tuanzi.kelpLib.api.command.Commands;
import ink.tuanzi.kelpLib.api.terminable.Terminable;
import ink.tuanzi.kelpLib.core.command.CommandModel;
import ink.tuanzi.kelpLib.core.command.CommandTreeScanner;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 命令门面 Velocity 实现（§6.5；LiteCommands 不可达时的自研回退，§11.4）：
 * 注解命令树 → {@link SimpleCommand} → 原生 {@link CommandManager} 注册；
 * 注册纳入 Terminable 生命周期（关服/卸载自动注销）。
 */
public final class VelocityCommandsImpl implements Commands {

    private final ProxyServer proxy;
    private final TerminableConsumerBridge lifecycle;
    private final Map<Class<?>, ArgumentResolver<?>> resolvers = new ConcurrentHashMap<>();
    private final CommandTreeScanner.PermissionChecker permissionChecker =
            (source, permission) -> source instanceof CommandSource commandSource
                    && (permission.isEmpty() || commandSource.hasPermission(permission));

    public VelocityCommandsImpl(ProxyServer proxy, TerminableConsumerBridge lifecycle) {
        this.proxy = proxy;
        this.lifecycle = lifecycle;
    }

    @Override
    public void register(Object platformPlugin, Class<?>... commandClasses) {
        CommandManager commandManager = proxy.getCommandManager();
        for (CommandModel.RootCommand root : CommandTreeScanner.scan(commandClasses)) {
            CommandMeta meta = CommandMeta.builder(root.name())
                    .plugin(platformPlugin)
                    .build();
            VelocityKelpCommand command = new VelocityKelpCommand(root);
            commandManager.register(meta, command);
            lifecycle.bind(new CommandHandle(commandManager, root.name()));
        }
    }

    @Override
    public <T> void registerArgumentResolver(Class<T> type, ArgumentResolver<T> resolver) {
        resolvers.put(type, resolver);
    }

    CommandTreeScanner.PermissionChecker permissionChecker() {
        return permissionChecker;
    }

    Map<Class<?>, ArgumentResolver<?>> resolvers() {
        return resolvers;
    }

    /** lifecycle 绑定桥（避免平台模块循环依赖）。 */
    public interface TerminableConsumerBridge {
        void bind(Terminable terminable);
    }

    private static final class CommandHandle implements Terminable {

        private final CommandManager commandManager;
        private final String name;
        private final AtomicBoolean closed = new AtomicBoolean(false);

        private CommandHandle(CommandManager commandManager, String name) {
            this.commandManager = commandManager;
            this.name = name;
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                commandManager.unregister(name);
            }
        }

        @Override
        public boolean isClosed() {
            return closed.get();
        }
    }

    private final class VelocityKelpCommand implements SimpleCommand {

        private final CommandModel.RootCommand root;

        private VelocityKelpCommand(CommandModel.RootCommand root) {
            this.root = root;
        }

        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            String[] args = invocation.arguments();
            if (!root.permission().isEmpty() && !source.hasPermission(root.permission())) {
                source.sendMessage(net.kyori.adventure.text.Component.text("无权限执行该命令。"));
                return;
            }
            CommandModel.ExecNode exec;
            String[] restArgs;
            if (args.length > 0) {
                CommandModel.ExecNode child = root.child(args[0]);
                if (child != null) {
                    exec = child;
                    restArgs = Arrays.copyOfRange(args, 1, args.length);
                } else {
                    exec = root.rootExec();
                    restArgs = args;
                }
            } else {
                exec = root.rootExec();
                restArgs = args;
            }
            if (exec == null) {
                source.sendMessage(net.kyori.adventure.text.Component.text(
                        "未知子命令。可用: " + listChildren()));
                return;
            }
            String error = CommandTreeScanner.invoke(root, exec, source, restArgs, permissionChecker, resolvers);
            if (error != null) {
                source.sendMessage(net.kyori.adventure.text.Component.text(error));
            }
        }

        @Override
        public List<String> suggest(Invocation invocation) {
            return CommandTreeScanner.suggestions(root, invocation.source(), invocation.arguments(),
                    permissionChecker, resolvers);
        }

        private String listChildren() {
            return root.executes().stream()
                    .map(CommandModel.ExecNode::name)
                    .filter(name -> !name.isEmpty())
                    .map(name -> "/" + root.name() + " " + name)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.joining(", "));
        }
    }
}
