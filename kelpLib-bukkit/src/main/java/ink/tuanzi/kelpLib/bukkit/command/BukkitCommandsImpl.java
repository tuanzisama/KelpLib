package ink.tuanzi.kelpLib.bukkit.command;

import ink.tuanzi.kelpLib.api.command.ArgumentResolver;
import ink.tuanzi.kelpLib.api.command.Commands;
import ink.tuanzi.kelpLib.api.terminable.Terminable;
import ink.tuanzi.kelpLib.KelpLib;
import ink.tuanzi.kelpLib.bukkit.KelpPlatformBukkit;
import ink.tuanzi.kelpLib.core.command.CommandModel;
import ink.tuanzi.kelpLib.core.command.CommandTreeScanner;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 命令门面 Bukkit 实现（§6.5；LiteCommands 不可达时的自研回退，§11.4）：
 * 注解命令树 → {@link Command} → Paper {@link CommandMap} 注册；注册纳入 Terminable 生命周期
 * （插件 disable 自动注销）；节点权限经 {@link CommandTreeScanner} 校验并在补全侧隐藏。
 */
public final class BukkitCommandsImpl implements Commands {

    private final KelpPlatformBukkit platform;
    private final Map<Class<?>, ArgumentResolver<?>> resolvers = new ConcurrentHashMap<>();
    private final CommandTreeScanner.PermissionChecker permissionChecker =
            (source, permission) -> source instanceof CommandSender sender
                    && (permission.isEmpty() || sender.hasPermission(permission));

    public BukkitCommandsImpl(KelpPlatformBukkit platform) {
        this.platform = platform;
    }

    @Override
    public void register(Object platformPlugin, Class<?>... commandClasses) {
        Plugin owner = platformPlugin instanceof Plugin bukkitPlugin ? bukkitPlugin : KelpLib.getInstance();
        CommandMap commandMap = Bukkit.getCommandMap();
        for (CommandModel.RootCommand root : CommandTreeScanner.scan(commandClasses)) {
            KelpBukkitCommand command = new KelpBukkitCommand(root);
            if (!root.description().isEmpty()) {
                command.setDescription(root.description());
            }
            if (!root.permission().isEmpty()) {
                command.setPermission(root.permission());
            }
            commandMap.register("kelp", command);
            platform.lifecycle(owner).bind(new CommandHandle(command, commandMap));
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

    private static final class CommandHandle implements Terminable {

        private final Command command;
        private final CommandMap commandMap;
        private final AtomicBoolean closed = new AtomicBoolean(false);

        private CommandHandle(Command command, CommandMap commandMap) {
            this.command = command;
            this.commandMap = commandMap;
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                command.unregister(commandMap);
            }
        }

        @Override
        public boolean isClosed() {
            return closed.get();
        }
    }

    private final class KelpBukkitCommand extends Command {

        private final CommandModel.RootCommand root;

        private KelpBukkitCommand(CommandModel.RootCommand root) {
            super(root.name());
            this.root = root;
        }

        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            if (!root.permission().isEmpty() && !sender.hasPermission(root.permission())) {
                sender.sendMessage("无权限执行该命令。");
                return true;
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
                sender.sendMessage("未知子命令。可用: " + listChildren());
                return true;
            }
            String error = CommandTreeScanner.invoke(root, exec, sender, restArgs, permissionChecker, resolvers);
            if (error != null) {
                sender.sendMessage(error);
            }
            return true;
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
            return CommandTreeScanner.suggestions(root, sender, args, permissionChecker, resolvers);
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
