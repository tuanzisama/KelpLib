package ink.tuanzi.kelpLib.core.config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.comments.CommentLine;
import org.yaml.snakeyaml.comments.CommentType;
import org.yaml.snakeyaml.emitter.Emitter;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.resolver.Resolver;
import org.yaml.snakeyaml.serializer.Serializer;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SnakeYAML 节点树操作工具：基于 compose/dump-as-node 保序、保注释地读写 YAML（§6.6）。
 */
final class YamlTree {

    private static final DumperOptions DUMPER_OPTIONS = new DumperOptions();

    static {
        DUMPER_OPTIONS.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        DUMPER_OPTIONS.setIndent(2);
        DUMPER_OPTIONS.setAllowUnicode(true);
    }

    private YamlTree() {
    }

    /** compose 文件为节点树；空文件返回 null。 */
    static Node compose(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return new Yaml().compose(reader);
        }
    }

    /** 节点安全转 Mapping；非映射返回空 Mapping。 */
    static MappingNode asMapping(Node node) {
        if (node instanceof MappingNode mapping) {
            return mapping;
        }
        return new MappingNode(Tag.MAP, new ArrayList<>(), DumperOptions.FlowStyle.BLOCK);
    }

    /** 创建标量节点（emitter 会在必要时自动加引号）。 */
    static ScalarNode scalar(String value) {
        return new ScalarNode(Tag.STR, value == null ? "" : value, null, null, DumperOptions.ScalarStyle.PLAIN);
    }

    static NodeTuple tuple(String key, Node value) {
        return new NodeTuple(scalar(key), value);
    }

    /** 按点分路径查找节点；缺失返回 null。 */
    static Node find(MappingNode root, String[] path) {
        MappingNode current = root;
        for (int i = 0; i < path.length; i++) {
            NodeTuple tuple = tupleOf(current, path[i]);
            if (tuple == null) {
                return null;
            }
            if (i == path.length - 1) {
                return tuple.getValueNode();
            }
            Node value = tuple.getValueNode();
            if (!(value instanceof MappingNode)) {
                return null;
            }
            current = (MappingNode) value;
        }
        return null;
    }

    /** 标量节点的原始字符串值。 */
    static String rawScalar(Node node) {
        if (node instanceof ScalarNode scalar) {
            return scalar.getValue();
        }
        return null;
    }

    /** 序列节点的元素列表。 */
    static List<Node> sequenceItems(Node node) {
        if (node instanceof SequenceNode sequence) {
            return sequence.getValue();
        }
        return List.of();
    }

    static NodeTuple tupleOf(MappingNode mapping, String key) {
        for (NodeTuple tuple : mapping.getValue()) {
            if (tuple.getKeyNode() instanceof ScalarNode scalar && key.equals(scalar.getValue())) {
                return tuple;
            }
        }
        return null;
    }

    /** 把 fresh 树中每个键的注释替换为 old 树中对应键的注释（注释保留的关键步骤）。 */
    static void mergeComments(MappingNode fresh, MappingNode old) {
        if (old == null) {
            return;
        }
        if (!old.getBlockComments().isEmpty()) {
            fresh.setBlockComments(new ArrayList<>(old.getBlockComments()));
        }
        if (!old.getInLineComments().isEmpty()) {
            fresh.setInLineComments(new ArrayList<>(old.getInLineComments()));
        }
        if (!old.getEndComments().isEmpty()) {
            fresh.setEndComments(new ArrayList<>(old.getEndComments()));
        }
        for (NodeTuple newTuple : fresh.getValue()) {
            if (!(newTuple.getKeyNode() instanceof ScalarNode keyNode)) {
                continue;
            }
            NodeTuple oldTuple = tupleOf(old, keyNode.getValue());
            if (oldTuple == null) {
                continue;
            }
            if (!oldTuple.getKeyNode().getBlockComments().isEmpty()) {
                keyNode.setBlockComments(new ArrayList<>(oldTuple.getKeyNode().getBlockComments()));
            }
            if (oldTuple.getValueNode() instanceof MappingNode oldMapping
                    && newTuple.getValueNode() instanceof MappingNode newMapping) {
                mergeComments(newMapping, oldMapping);
            }
        }
    }

    /** 以根注释（文件头）生成注释行。 */
    static List<CommentLine> header(String... lines) {
        List<CommentLine> comments = new ArrayList<>(lines.length);
        for (String line : lines) {
            comments.add(new CommentLine(null, null, " " + line, CommentType.BLOCK));
        }
        return comments;
    }

    /** 将节点树写出到文件。 */
    static void emit(Node root, Path file) throws IOException {
        String text = emitToString(root);
        Files.createDirectories(file.getParent());
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }

    private static String emitToString(Node root) {
        try {
            StringWriter writer = new StringWriter();
            Emitter emitter = new Emitter(writer, DUMPER_OPTIONS);
            Serializer serializer = new Serializer(emitter, new Resolver(), DUMPER_OPTIONS, Tag.MAP);
            serializer.open();
            serializer.serialize(root);
            serializer.close();
            return writer.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (YAMLException e) {
            throw e;
        }
    }

    /** 把节点树解析为嵌套 Map（标量为字符串），用于消息 bundle 等宽松场景。 */
    static Map<String, Object> toPlainMap(Node node) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (node instanceof MappingNode mapping) {
            for (NodeTuple tuple : mapping.getValue()) {
                if (tuple.getKeyNode() instanceof ScalarNode key) {
                    out.put(key.getValue(), plainValue(tuple.getValueNode()));
                }
            }
        }
        return out;
    }

    private static Object plainValue(Node node) {
        if (node instanceof MappingNode mapping) {
            Map<String, Object> nested = new LinkedHashMap<>();
            for (NodeTuple tuple : mapping.getValue()) {
                if (tuple.getKeyNode() instanceof ScalarNode key) {
                    nested.put(key.getValue(), plainValue(tuple.getValueNode()));
                }
            }
            return nested;
        }
        if (node instanceof SequenceNode sequence) {
            List<Object> items = new ArrayList<>();
            for (Node item : sequence.getValue()) {
                items.add(plainValue(item));
            }
            return items;
        }
        String raw = rawScalar(node);
        return raw == null ? "" : raw;
    }

    /** 从字符串 compose（测试/工具用）。 */
    static Node compose(String yaml) {
        return new Yaml().compose(new StringReader(yaml));
    }
}
