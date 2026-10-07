package ink.tuanzi.kelpLib.core.config;

import ink.tuanzi.kelpLib.api.config.Default;
import ink.tuanzi.kelpLib.api.config.Key;
import ink.tuanzi.kelpLib.api.config.KelpConfig;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 注解配置扫描器（record/POJO）：解析 {@link KelpConfig}/{@link Key}/{@link Default}，
 * 支持 YAML 节点树 → 实例构建、实例 → 键值树映射（嵌套 record 递归、点分键映射为嵌套节）。
 */
public final class ConfigScanner {

    /** 标量解码器（由引擎注入注册表能力）。 */
    public interface Decoder {
        Object decode(Class<?> type, String raw);
    }

    private record PropSpec(
            String key,
            Class<?> type,
            boolean nested,
            ConfigScanner nestedScanner,
            String defaultValue,
            java.lang.reflect.Method accessor,
            Field field
    ) {
    }

    private static final ConcurrentHashMap<Class<?>, ConfigScanner> CACHE = new ConcurrentHashMap<>();

    private final Class<?> type;
    private final boolean record;
    private final List<PropSpec> props;

    private ConfigScanner(Class<?> type) {
        this.type = type;
        this.record = type.isRecord();
        this.props = scan(type);
    }

    /** 扫描（带缓存）。 */
    public static ConfigScanner of(Class<?> type) {
        return CACHE.computeIfAbsent(type, ConfigScanner::new);
    }

    private static List<PropSpec> scan(Class<?> type) {
        List<PropSpec> props = new ArrayList<>();
        if (type.isRecord()) {
            for (RecordComponent component : type.getRecordComponents()) {
                Key key = component.getAnnotation(Key.class);
                Default def = component.getAnnotation(Default.class);
                Class<?> fieldType = component.getType();
                boolean nested = isNested(fieldType);
                props.add(new PropSpec(
                        key != null ? key.value() : kebab(component.getName()),
                        fieldType,
                        nested,
                        nested ? of(fieldType) : null,
                        def != null ? def.value() : null,
                        component.getAccessor(),
                        null));
            }
        } else {
            List<Field> fields = new ArrayList<>();
            Class<?> current = type;
            while (current != null && current != Object.class) {
                fields.addAll(Arrays.asList(current.getDeclaredFields()));
                current = current.getSuperclass();
            }
            for (Field field : fields) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
                    continue;
                }
                Key key = field.getAnnotation(Key.class);
                Default def = field.getAnnotation(Default.class);
                if (key == null && def == null) {
                    continue; // POJO 中仅处理显式标注的字段
                }
                Class<?> fieldType = field.getType();
                boolean nested = isNested(fieldType);
                field.setAccessible(true);
                props.add(new PropSpec(
                        key != null ? key.value() : kebab(field.getName()),
                        fieldType,
                        nested,
                        nested ? of(fieldType) : null,
                        def != null ? def.value() : null,
                        null,
                        field));
            }
        }
        if (props.isEmpty()) {
            throw new IllegalArgumentException("Config type " + type.getName()
                    + " has no annotated properties (use @Key/@Default on record components or fields)");
        }
        return props;
    }

    private static boolean isNested(Class<?> type) {
        return (type.isRecord() || type.isAnnotationPresent(KelpConfig.class)) && !type.isEnum();
    }

    private static String kebab(String name) {
        StringBuilder sb = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (Character.isUpperCase(c)) {
                if (!sb.isEmpty()) {
                    sb.append('-');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 从节点树构建实例（root 可为 null——全部走默认值）。
     */
    public <T> T build(Node root, Decoder decoder) {
        MappingNode mapping = YamlTree.asMapping(root);
        Map<String, Object> values = new LinkedHashMap<>();
        for (PropSpec prop : props) {
            String[] path = prop.key().split("\\.");
            Node node = YamlTree.find(mapping, path);
            values.put(prop.key(), resolveValue(prop, node, decoder));
        }
        return instantiate(values);
    }

    private Object resolveValue(PropSpec prop, Node node, Decoder decoder) {
        if (prop.nested()) {
            return prop.nestedScanner().build(node, decoder);
        }
        if (node instanceof SequenceNode sequence) {
            List<Object> items = new ArrayList<>();
            for (Node item : sequence.getValue()) {
                String element = YamlTree.rawScalar(item);
                items.add(decoder.decode(String.class, element == null ? "" : element));
            }
            return items;
        }
        String raw = YamlTree.rawScalar(node);
        if (raw != null) {
            return decoder.decode(prop.type(), raw);
        }
        if (prop.defaultValue() != null) {
            return decoder.decode(prop.type(), prop.defaultValue());
        }
        if (prop.type() == List.class) {
            return new ArrayList<>();
        }
        if (prop.type().isPrimitive()) {
            return prop.type() == boolean.class ? Boolean.FALSE : (Object) 0;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> T instantiate(Map<String, Object> values) {
        try {
            if (record) {
                RecordComponent[] components = type.getRecordComponents();
                Class<?>[] paramTypes = new Class<?>[components.length];
                Object[] args = new Object[components.length];
                for (int i = 0; i < components.length; i++) {
                    paramTypes[i] = components[i].getType();
                    PropSpec spec = findProp(components[i].getName());
                    args[i] = spec == null ? null : values.get(spec.key());
                }
                Constructor<?> ctor = type.getDeclaredConstructor(paramTypes);
                ctor.setAccessible(true);
                return (T) ctor.newInstance(args);
            }
            Object instance = type.getDeclaredConstructor().newInstance();
            for (PropSpec prop : props) {
                Object value = values.get(prop.key());
                if (value != null) {
                    prop.field().set(instance, value);
                }
            }
            return (T) instance;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to instantiate config type " + type.getName()
                    + (record ? "" : " (POJO configs require a no-arg constructor)"), e);
        }
    }

    private PropSpec findProp(String componentName) {
        String kebab = kebab(componentName);
        for (PropSpec prop : props) {
            if (prop.key().equals(kebab) || prop.key().equals(componentName)) {
                return prop;
            }
        }
        return null;
    }

    /**
     * 提取实例值为 键 → 值树（标量为字符串、嵌套为 Map、列表为 List），供写出 YAML / 编码消息。
     */
    public Map<String, Object> extract(Object instance) {
        try {
            Map<String, Object> out = new LinkedHashMap<>();
            for (PropSpec prop : props) {
                Object value = prop.accessor() != null
                        ? prop.accessor().invoke(instance)
                        : prop.field().get(instance);
                if (prop.nested()) {
                    out.put(prop.key(), value == null ? Map.of() : prop.nestedScanner().extract(value));
                } else {
                    out.put(prop.key(), value == null ? "" : value);
                }
            }
            return out;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to extract values from " + type.getName(), e);
        }
    }

    /** 值树 → YAML 节点树（嵌套 Map → 嵌套节、List → 序列、标量 → 字符串）。 */
    public MappingNode toNodeTree(Map<String, Object> values) {
        MappingNode root = new MappingNode(Tag.MAP, new ArrayList<>(), DumperOptions.FlowStyle.BLOCK);
        for (PropSpec prop : props) {
            String[] path = prop.key().split("\\.");
            Object value = values.get(prop.key());
            setNode(root, path, 0, prop, value);
        }
        return root;
    }

    private void setNode(MappingNode current, String[] path, int depth, PropSpec prop, Object value) {
        String key = path[depth];
        NodeTuple existing = YamlTree.tupleOf(current, key);
        if (depth < path.length - 1) {
            MappingNode child = existing != null && existing.getValueNode() instanceof MappingNode mapping
                    ? mapping
                    : new MappingNode(Tag.MAP, new ArrayList<>(), DumperOptions.FlowStyle.BLOCK);
            setNode(child, path, depth + 1, prop, value);
            upsert(current, key, child, existing);
            return;
        }
        Node leaf;
        if (prop.nested() && value instanceof Map<?, ?> nestedMap) {
            leaf = prop.nestedScanner().toNodeTree(castMap(nestedMap));
        } else if (value instanceof List<?> list) {
            List<Node> items = new ArrayList<>();
            for (Object item : list) {
                items.add(YamlTree.scalar(item == null ? "" : String.valueOf(item)));
            }
            leaf = new SequenceNode(Tag.SEQ, items, DumperOptions.FlowStyle.BLOCK);
        } else {
            leaf = YamlTree.scalar(value == null ? "" : String.valueOf(value));
        }
        upsert(current, key, leaf, existing);
    }

    private static void upsert(MappingNode mapping, String key, Node value, NodeTuple existing) {
        NodeTuple tuple = YamlTree.tuple(key, value);
        if (existing != null) {
            // 保留原键节点（含其注释）
            mapping.getValue().set(mapping.getValue().indexOf(existing),
                    new NodeTuple(existing.getKeyNode(), value));
        } else {
            mapping.getValue().add(tuple);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }

    /** 解析该类型的配置文件相对路径。 */
    public static String pathOf(Class<?> type) {
        KelpConfig annotation = type.getAnnotation(KelpConfig.class);
        return annotation != null ? annotation.path() : "config.yml";
    }
}
