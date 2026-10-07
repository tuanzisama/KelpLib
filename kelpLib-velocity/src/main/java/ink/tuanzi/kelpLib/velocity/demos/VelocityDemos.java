package ink.tuanzi.kelpLib.velocity.demos;

import ink.tuanzi.kelpLib.api.Kelp;
import ink.tuanzi.kelpLib.api.config.Default;
import ink.tuanzi.kelpLib.api.config.KelpConfig;
import ink.tuanzi.kelpLib.api.config.Key;
import ink.tuanzi.kelpLib.api.promise.Promise;
import ink.tuanzi.kelpLib.api.storage.Identifiable;
import ink.tuanzi.kelpLib.api.storage.Storage;
import ink.tuanzi.kelpLib.velocity.KelpLibVelocity;

import java.time.Duration;
import java.util.UUID;

/**
 * {@code /kelp demo} Velocity 侧子集（§6.10）：scheduler/promise/config/message/storage/messenger。
 */
public final class VelocityDemos {

    private VelocityDemos() {
    }

    /** 运行指定模块演示，返回说明文本。 */
    public static String run(String module) {
        return switch (module) {
            case "scheduler" -> demoScheduler();
            case "promise" -> demoPromise();
            case "config" -> demoConfig();
            case "storage" -> {
                demoStorage();
                yield "存储演示已启动（文件仓库 存-读-删 闭环），结果将异步输出至日志。";
            }
            case "messenger" -> demoMessenger();
            default -> "Velocity 侧可用模块: scheduler/promise/config/storage/messenger";
        };
    }

    private static String demoScheduler() {
        Kelp.scheduler().runLater(Duration.ofSeconds(1), task ->
                KelpLibVelocity.getInstance().logger().info("[demo/scheduler] runLater OK（调度器执行池）"));
        Kelp.scheduler().supply(() -> "async-value").thenAccept(value ->
                KelpLibVelocity.getInstance().logger().info("[demo/scheduler] supply OK: " + value));
        return "调度演示已启动（1 秒后与异步池各输出一条日志）。";
    }

    private static String demoPromise() {
        Promise.start()
                .thenApplyAsync(value -> "from-pool")
                .thenDelay(Duration.ofMillis(500))
                .thenApplySync(value -> value + "-synced")
                .timeout(Duration.ofSeconds(5))
                .thenAcceptAsync(value ->
                        KelpLibVelocity.getInstance().logger().info("[demo/promise] OK: " + value))
                .exceptionally(error -> {
                    KelpLibVelocity.getInstance().logger().warn("[demo/promise] failed: " + error);
                    return null;
                });
        return "Promise 演示已启动（async → delay 500ms → sync，超时 5s）。";
    }

    @KelpConfig(path = "demo-config.yml")
    private record DemoConfig(
            @Key("greeting") @Default("<green>[Kelp]</green> 你好") String greeting,
            @Key("max-players") @Default("100") int maxPlayers
    ) {
    }

    private static String demoConfig() {
        DemoConfig config = Kelp.configs().load(KelpLibVelocity.getInstance(), DemoConfig.class);
        Kelp.configs().save(KelpLibVelocity.getInstance(), config);
        return "配置演示：greeting=" + config.greeting() + "，max-players=" + config.maxPlayers()
                + "（已写回 dataDirectory/demo-config.yml）。";
    }

    private record DemoEntity(UUID id, String name) implements Identifiable<UUID> {
    }

    private static void demoStorage() {
        var repository = Storage.file(KelpLibVelocity.getInstance(), "demo", DemoEntity.class, UUID.class);
        DemoEntity entity = new DemoEntity(UUID.nameUUIDFromBytes("kelp-demo".getBytes()), "kelp-demo-entity");
        repository.save(entity)
                .thenCompose(unused -> repository.load(entity.id()))
                .thenAccept(loaded -> KelpLibVelocity.getInstance().logger().info(
                        "[demo/storage] load OK: " + loaded.map(DemoEntity::name).orElse("<missing>")))
                .thenCompose(unused -> repository.delete(entity.id()))
                .thenRun(() -> KelpLibVelocity.getInstance().logger().info("[demo/storage] delete OK（闭环完成）"))
                .exceptionally(error -> {
                    KelpLibVelocity.getInstance().logger().warn("[demo/storage] failed: " + error);
                    return null;
                });
    }

    private static String demoMessenger() {
        var transports = Kelp.messenger().transports();
        if (transports.isEmpty()) {
            return "Messenger：当前无已注册传输。";
        }
        var channel = Kelp.messenger().getChannel("demo", String.class);
        var agent = channel.newAgent();
        agent.addListener((a, message) ->
                KelpLibVelocity.getInstance().logger().info("[demo/messenger] received: " + message));
        channel.sendMessage("ping");
        agent.close();
        return "Messenger 演示已执行（传输: " + transports.size() + " 个；Channel 收发闭环）。";
    }
}
