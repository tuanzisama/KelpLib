package ink.tuanzi.kelpLib.bukkit.demos;

import ink.tuanzi.kelpLib.api.Kelp;
import ink.tuanzi.kelpLib.api.config.Default;
import ink.tuanzi.kelpLib.api.config.KelpConfig;
import ink.tuanzi.kelpLib.api.config.Key;
import ink.tuanzi.kelpLib.api.promise.Promise;
import ink.tuanzi.kelpLib.api.storage.Identifiable;
import ink.tuanzi.kelpLib.api.storage.Storage;
import ink.tuanzi.kelpLib.api.text.Mini;
import ink.tuanzi.kelpLib.api.text.Messages;
import ink.tuanzi.kelpLib.bukkit.KelpBukkit;
import ink.tuanzi.kelpLib.KelpLib;
import ink.tuanzi.kelpLib.bukkit.gui.KelpGui;
import ink.tuanzi.kelpLib.bukkit.item.ItemBuilder;
import ink.tuanzi.kelpLib.bukkit.listener.Events;
import ink.tuanzi.kelpLib.bukkit.nbt.Nbt;
import ink.tuanzi.kelpLib.bukkit.text.ScoreboardDisplay;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * {@code /kelp demo} 各模块最小演示（§6.10）：替代正式测试基建的手动验收工具。
 */
public final class BukkitDemos {

    private BukkitDemos() {
    }

    /** 运行指定模块演示，返回说明文本。 */
    public static String run(CommandSender sender, String module) {
        return switch (module) {
            case "scheduler" -> demoScheduler(sender);
            case "promise" -> demoPromise(sender);
            case "event" -> demoEvent();
            case "listener" -> demoListener();
            case "item" -> demoItem(sender);
            case "nbt" -> demoNbt(sender);
            case "command" -> "命令层：本命令即自研命令层注册（注解树 → CommandMap）。"
                    + "自定义参数解析器：Kelp.commands().registerArgumentResolver(type, resolver)。";
            case "config" -> demoConfig();
            case "message" -> demoMessage(sender);
            case "scoreboard" -> demoScoreboard(sender);
            case "gui" -> "GUI 模块可用性: " + KelpGui.available()
                    + "（当前构建未捆绑 InvUI，见 README 环境受限说明）";
            case "storage" -> {
                demoStorage();
                yield "存储演示已启动（文件仓库 存-读-删 闭环），结果将异步输出。";
            }
            case "messenger" -> demoMessenger();
            default -> "未知模块。可用: scheduler/promise/event/listener/item/nbt/command/config/"
                    + "message/scoreboard/gui/storage/messenger";
        };
    }

    private static String demoScheduler(CommandSender sender) {
        Kelp.scheduler().runLater(Duration.ofSeconds(1), task ->
                sender.sendMessage("[demo/scheduler] runLater OK（GLOBAL 作用域）"));
        Kelp.scheduler().supply(() -> "async-value").thenAccept(value ->
                sender.sendMessage("[demo/scheduler] supply OK: " + value));
        return "调度演示已启动（1 秒后与异步池各输出一条）。";
    }

    private static String demoPromise(CommandSender sender) {
        Promise.start()
                .thenApplyAsync(value -> "from-async-pool")
                .thenDelay(Duration.ofMillis(500))
                .thenApplySync(value -> value + "-on-global")
                .timeout(Duration.ofSeconds(5))
                .thenAcceptAsync(value -> sender.sendMessage("[demo/promise] OK: " + value))
                .exceptionally(error -> {
                    sender.sendMessage("[demo/promise] failed: " + error);
                    return null;
                });
        return "Promise 演示已启动（async → delay 500ms → sync，超时 5s）。";
    }

    private static String demoEvent() {
        Events.subscribe(PlayerJoinEvent.class)
                .expireAfter(1)
                .handler(event -> event.getPlayer().sendMessage(
                        Mini.render("<green>[KelpLib]</green> 一次性订阅欢迎！随插件 disable 或触发一次后自动注销。")))
                .bindWith(KelpBukkit.lifecycle(KelpLib.getInstance()));
        return "事件演示已注册：下一名加入的玩家会收到一次性欢迎（expireAfter=1）。";
    }

    private static String demoListener() {
        org.bukkit.event.Listener legacy = new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onQuit(PlayerQuitEvent event) {
                KelpLib.getInstance().getLogger().info(
                        "[demo/listener] 传统监听器捕获退出: " + event.getPlayer().getName());
            }
        };
        KelpBukkit.listeners().register(KelpLib.getInstance(), legacy)
                .bindWith(KelpBukkit.lifecycle(KelpLib.getInstance()));
        return "传统监听器演示已注册（@EventHandler + Terminable 绑定，随插件 disable 注销）。";
    }

    private static String demoItem(CommandSender sender) {
        ItemBuilder builder = ItemBuilder.of(Material.NETHERITE_SWORD)
                .name("<gold>烈焰之刃</gold>")
                .lore("<gray>攻击时点燃目标</gray>", "<dark_gray>KelpLib ItemBuilder 演示</dark_gray>")
                .enchant(Enchantment.SHARPNESS, 5)
                .pdc("utoverse", "item-id", "flame_blade");
        if (sender instanceof Player player) {
            player.getInventory().addItem(builder.build());
            return "已将演示物品放入你的背包（Data Component 物品模型 + PDC 标记）。";
        }
        return "物品演示已构建（仅玩家执行时发放）：烈焰之刃（附魔 + PDC）。";
    }

    private static String demoNbt(CommandSender sender) {
        if (!Nbt.available()) {
            return "NBT: ItemNBTAPI 不可用（Nbt.available()==false）——已按 §6.4 引导降级到 PDC。";
        }
        if (sender instanceof Player player) {
            ItemStack out = Nbt.setString(new ItemStack(Material.STONE), "utoverse:quest", "q_07");
            player.getInventory().addItem(out);
            return "NBT 演示：石头已写入 utoverse:quest=q_07 并发放。";
        }
        return "NBT 可用（玩家执行时发放带 tag 物品）。";
    }

    @KelpConfig(path = "demo-config.yml")
    private record DemoConfig(
            @Key("greeting") @Default("<green>[Kelp]</green> 你好") String greeting,
            @Key("max-homes") @Default("3") int maxHomes
    ) {
    }

    private static String demoConfig() {
        DemoConfig config = Kelp.configs().load(KelpLib.getInstance(), DemoConfig.class);
        Kelp.configs().save(KelpLib.getInstance(), config);
        return "配置演示：greeting=" + config.greeting() + "，max-homes=" + config.maxHomes()
                + "（已写回 demo-config.yml，注释与键顺序保留）。";
    }

    private static String demoMessage(CommandSender sender) {
        Messages messages = Messages.create(KelpLib.getInstance())
                .defaultLocale("zh_cn")
                .devMode(true)
                .build();
        if (sender instanceof Player player) {
            messages.send(player, "kelp.demo.hello");
        }
        return "消息演示：render('kelp.demo.hello') = "
                + messages.raw("kelp.demo.hello", null) + "（缺失键回退键本身）。";
    }

    private static String demoScoreboard(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return "计分板演示需要玩家执行。";
        }
        ScoreboardDisplay board = ScoreboardDisplay.create(player);
        board.update("<gold>KelpLib</gold>", List.of(
                "<gray>玩家:</gray> " + player.getName(),
                "<gray>模块:</gray> scoreboard",
                "<gray>线程:</gray> 实体作用域"));
        KelpBukkit.lifecycle(KelpLib.getInstance()).bind(board);
        Kelp.scheduler().runLater(Duration.ofSeconds(10), task -> board.close());
        return "计分板演示已显示（10 秒后自动移除）。";
    }

    private record DemoEntity(UUID id, String name) implements Identifiable<UUID> {
    }

    private static void demoStorage() {
        var repository = Storage.file(KelpLib.getInstance(), "demo", DemoEntity.class, UUID.class);
        DemoEntity entity = new DemoEntity(UUID.nameUUIDFromBytes("kelp-demo".getBytes()), "kelp-demo-entity");
        repository.save(entity)
                .thenCompose(unused -> repository.load(entity.id()))
                .thenAccept(loaded -> KelpLib.getInstance().getLogger().info(
                        "[demo/storage] load OK: " + loaded.map(DemoEntity::name).orElse("<missing>")))
                .thenCompose(unused -> repository.delete(entity.id()))
                .thenRun(() -> KelpLib.getInstance().getLogger().info("[demo/storage] delete OK（闭环完成）"))
                .exceptionally(error -> {
                    KelpLib.getInstance().getLogger().warning("[demo/storage] failed: " + error);
                    return null;
                });
    }

    private static String demoMessenger() {
        var transports = Kelp.messenger().transports();
        if (transports.isEmpty()) {
            return "Messenger：当前无已注册传输。Redis 传输经 new RedisTransport(spec) 注册（双端可用）。";
        }
        var channel = Kelp.messenger().getChannel("demo", String.class);
        var agent = channel.newAgent();
        agent.addListener((a, message) ->
                KelpLib.getInstance().getLogger().info("[demo/messenger] received: " + message));
        channel.sendMessage("ping");
        agent.close();
        return "Messenger 演示已执行（传输: " + transports.size() + " 个；Channel 收发闭环，agent 已退订）。";
    }
}
