package ink.tuanzi.kelpLib.velocity.command;

import ink.tuanzi.kelpLib.api.command.annotation.Arg;
import ink.tuanzi.kelpLib.api.command.annotation.Command;
import ink.tuanzi.kelpLib.api.command.annotation.Execute;
import ink.tuanzi.kelpLib.api.command.annotation.Sender;
import com.velocitypowered.api.command.CommandSource;
import ink.tuanzi.kelpLib.velocity.demos.VelocityDemos;
import net.kyori.adventure.text.Component;

import java.util.Locale;

/**
 * 内置 {@code /kelp} 命令（§6.10，Velocity 侧子集）。
 */
@Command(name = "kelp", description = "KelpLib 演示与诊断", permission = "kelplib.command")
public final class VelocityCommand {

    private VelocityCommand() {
    }

    /** 运行模块演示：/kelp demo &lt;module&gt;。 */
    @Execute(name = "demo", description = "运行模块演示")
    public void demo(@Sender CommandSource sender,
                     @Arg(name = "module", suggestions = {
                             "scheduler", "promise", "config", "storage", "messenger"
                     }) String module) {
        sender.sendMessage(Component.text(VelocityDemos.run(module.toLowerCase(Locale.ROOT))));
    }

    /** 平台信息：/kelp info。 */
    @Execute(name = "info", description = "KelpLib 平台信息")
    public void info(@Sender CommandSource sender) {
        sender.sendMessage(Component.text("KelpLib — shared utility & extension API library"));
        sender.sendMessage(Component.text("platform: velocity (proxy)"));
        sender.sendMessage(Component.text("modules: scheduler/promise/config/storage/messenger"));
    }
}
