package ink.tuanzi.kelpLib.bukkit.command;

import ink.tuanzi.kelpLib.api.command.annotation.Arg;
import ink.tuanzi.kelpLib.api.command.annotation.Command;
import ink.tuanzi.kelpLib.api.command.annotation.Execute;
import ink.tuanzi.kelpLib.api.command.annotation.Sender;
import ink.tuanzi.kelpLib.bukkit.KelpBukkit;
import ink.tuanzi.kelpLib.bukkit.demos.BukkitDemos;
import org.bukkit.command.CommandSender;

import java.util.Locale;

/**
 * 内置 {@code /kelp} 命令（§6.10）：各模块最小演示与诊断，作为里程碑的手动验收工具。
 */
@Command(name = "kelp", description = "KelpLib 演示与诊断", permission = "kelplib.command")
public final class KelpCommand {

    private KelpCommand() {
    }

    /** 运行模块演示：/kelp demo &lt;module&gt;。 */
    @Execute(name = "demo", description = "运行模块演示")
    public void demo(@Sender CommandSender sender,
                     @Arg(name = "module", suggestions = {
                             "scheduler", "promise", "event", "listener", "item", "nbt",
                             "command", "config", "message", "scoreboard", "gui", "storage", "messenger"
                     }) String module) {
        sender.sendMessage(BukkitDemos.run(sender, module.toLowerCase(Locale.ROOT)));
    }

    /** 平台信息：/kelp info。 */
    @Execute(name = "info", description = "KelpLib 平台信息")
    public void info(@Sender CommandSender sender) {
        sender.sendMessage("KelpLib — shared utility & extension API library");
        sender.sendMessage("platform: " + KelpBukkit.platform().describe());
        sender.sendMessage("modules: scheduler/promise/terminable/event/item/nbt/command/config/message/storage/messenger");
    }
}
