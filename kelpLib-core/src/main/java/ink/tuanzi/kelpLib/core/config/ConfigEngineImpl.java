package ink.tuanzi.kelpLib.core.config;

import ink.tuanzi.kelpLib.api.config.ConfigEngine;
import ink.tuanzi.kelpLib.api.config.KelpConfig;
import ink.tuanzi.kelpLib.api.config.SerializerRegistry;
import ink.tuanzi.kelpLib.api.scheduler.KelpScheduler;
import ink.tuanzi.kelpLib.api.terminable.Terminable;
import org.yaml.snakeyaml.comments.CommentType;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 配置引擎默认实现：注解 record/POJO ↔ YAML 双向映射（节点树操作保注释保序）、
 * 首次生成默认文件、热重载（异步读、同步安全回调，§6.6）。
 */
public final class ConfigEngineImpl implements ConfigEngine {

    private static final Logger LOGGER = Logger.getLogger("KelpLib");

    private final Function<Object, Path> dataDirectories;
    private final KelpScheduler scheduler;
    private final SerializerRegistryImpl registry;
    private final ConfigScanner.Decoder decoder;

    /** @param dataDirectories 业务插件实例 → 数据目录 @param registry 平台共享的序列化器注册表 */
    public ConfigEngineImpl(Function<Object, Path> dataDirectories, KelpScheduler scheduler,
                            SerializerRegistryImpl registry) {
        this.dataDirectories = dataDirectories;
        this.scheduler = scheduler;
        this.registry = registry;
        this.decoder = registry::deserialize;
    }

    @Override
    public <T> T load(Object plugin, Class<T> type) {
        Path file = fileOf(plugin, type);
        ConfigScanner scanner = ConfigScanner.of(type);
        Node root = composeOrNull(file);
        T instance = scanner.build(root, decoder);
        if (root == null && type.isAnnotationPresent(KelpConfig.class)) {
            save(plugin, instance); // 首次生成默认文件
        }
        return instance;
    }

    @Override
    public <T> void save(Object plugin, T instance) {
        @SuppressWarnings("unchecked")
        Class<T> type = (Class<T>) instance.getClass();
        Path file = fileOf(plugin, type);
        ConfigScanner scanner = ConfigScanner.of(type);
        try {
            Node existing = composeOrNull(file);
            MappingNode fresh = scanner.toNodeTree(scanner.extract(instance));
            if (!fresh.getValue().isEmpty() && existing instanceof MappingNode oldMapping) {
                YamlTree.mergeComments(fresh, oldMapping);
            } else if (!fresh.getValue().isEmpty()) {
                fresh.setBlockComments(YamlTree.header(
                        "KelpLib generated config (" + type.getSimpleName() + ")",
                        "Comments and key order are preserved on rewrite."));
            }
            YamlTree.emit(fresh, file);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save config " + file, e);
        }
    }

    @Override
    public <T> Terminable watch(Object plugin, Class<T> type, Consumer<T> onChange) {
        ConfigWatcher watcher = new ConfigWatcher(this, plugin, type, onChange, scheduler);
        watcher.start();
        return watcher;
    }

    @Override
    public SerializerRegistry serializers() {
        return registry;
    }

    private <T> Path fileOf(Object plugin, Class<T> type) {
        return dataDirectories.apply(plugin).resolve(ConfigScanner.pathOf(type));
    }

    /** 业务插件数据目录（供 watcher 等包内协作者使用）。 */
    Path dataDirectoryFor(Object plugin) {
        return dataDirectories.apply(plugin);
    }

    private static Node composeOrNull(Path file) {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            return YamlTree.compose(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read config " + file, e);
        }
    }

    static void logFailure(Throwable error) {
        LOGGER.log(Level.WARNING, "KelpLib config operation failed", error);
    }
}
