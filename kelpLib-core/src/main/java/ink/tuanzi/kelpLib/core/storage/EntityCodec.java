package ink.tuanzi.kelpLib.core.storage;

import ink.tuanzi.kelpLib.core.config.ConfigScanner;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;

import java.util.Map;

/**
 * 实体编解码：record/POJO ↔ YAML 字符串（复用配置扫描器与序列化器注册表）。
 * 文件仓库、SQL 文档表、Redis hash 与 Messenger 消息编码共用此机制。
 */
public final class EntityCodec {

    private static final DumperOptions DUMPER_OPTIONS = new DumperOptions();

    static {
        DUMPER_OPTIONS.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        DUMPER_OPTIONS.setIndent(2);
        DUMPER_OPTIONS.setAllowUnicode(true);
    }

    private final ConfigScanner scanner;
    private final ConfigScanner.Decoder decoder;
    private final java.util.function.Function<Object, String> encoder;

    public EntityCodec(ConfigScanner scanner, ConfigScanner.Decoder decoder, java.util.function.Function<Object, String> encoder) {
        this.scanner = scanner;
        this.decoder = decoder;
        this.encoder = encoder;
    }

    /** 实例 → YAML 字符串。 */
    public String encode(Object instance) {
        Map<String, Object> values = scanner.extract(instance);
        // 标量统一字符串化（与解码路径对称）
        MappingNode root = scanner.toNodeTree(stringify(values));
        Yaml yaml = new Yaml(DUMPER_OPTIONS);
        return yaml.dump(root);
    }

    /** YAML 字符串 → 实例。 */
    public <T> T decode(String yamlText, Class<T> type) {
        if (yamlText == null || yamlText.isBlank()) {
            return null;
        }
        Node node = new Yaml().compose(new java.io.StringReader(yamlText));
        return scanner.build(node, decoder);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> stringify(Map<String, Object> values) {
        Map<String, Object> out = new java.util.LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (value instanceof Map<?, ?> nested) {
                out.put(key, stringify((Map<String, Object>) nested));
            } else if (value instanceof java.util.List<?> list) {
                out.put(key, list.stream().map(v -> v == null ? "" : String.valueOf(v)).toList());
            } else {
                out.put(key, value == null ? "" : String.valueOf(value));
            }
        });
        return out;
    }

    ConfigScanner scanner() {
        return scanner;
    }

    ConfigScanner.Decoder decoder() {
        return decoder;
    }

    java.util.function.Function<Object, String> encoder() {
        return encoder;
    }
}
