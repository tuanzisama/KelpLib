package ink.tuanzi.kelpLib.core.messenger;

import ink.tuanzi.kelpLib.api.terminable.Terminable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

/**
 * 通道实现与 Messenger 门面之间的内部上下文（包私有）：发布/订阅/分发/编解码/定时器/生命周期。
 */
interface MessengerContext {

    String PREFIX = "kelplib:";

    /** 向所有传输广播载荷。 */
    void publish(String channel, byte[] payload);

    /** 订阅原始载荷（close 即退订）。 */
    Terminable subscribeRaw(String channel, Consumer<byte[]> listener);

    /** 异步分发执行器（回调统一切回调度器执行池）。 */
    java.util.concurrent.Executor dispatch();

    /** 共享定时器（RPC 超时等）。 */
    ScheduledExecutorService timer();

    /** 取消息类型编解码器（带缓存）。 */
    MessageCodec codec(Class<?> messageType);

    /** 解码载荷为消息对象。 */
    default <T> T decodePayload(String yaml, Class<T> type) {
        return codec(type).decode(yaml == null ? null : yaml.getBytes(StandardCharsets.UTF_8), type);
    }

    /** 编码消息为 YAML 字符串。 */
    default String encodePayload(Object message) {
        return new String(codec(message.getClass()).encode(message), StandardCharsets.UTF_8);
    }

    /** 绑定到 Messenger 自身生命周期（close 时统一关闭）。 */
    void bind(Terminable terminable);

    static String prefixed(String name) {
        return name.startsWith(PREFIX) ? name : PREFIX + name;
    }

    DumperOptions FRAME_OPTIONS = new DumperOptions();

    /** 编码会话帧：{c: 会话ID, r: 是否回复, d: 载荷(YAML 字符串)}。 */
    static byte[] encodeFrame(String conversationId, boolean reply, String payloadYaml) {
        Map<String, Object> frame = new LinkedHashMap<>();
        frame.put("c", conversationId);
        frame.put("r", Boolean.toString(reply));
        frame.put("d", payloadYaml == null ? "" : payloadYaml);
        return new Yaml(FRAME_OPTIONS).dump(frame).getBytes(StandardCharsets.UTF_8);
    }

    /** 解码会话帧；非法帧返回 null。 */
    @SuppressWarnings("unchecked")
    static Frame decodeFrame(byte[] payload) {
        try {
            Object loaded = new Yaml().load(new String(payload, StandardCharsets.UTF_8));
            if (!(loaded instanceof Map)) {
                return null;
            }
            Map<String, Object> frame = (Map<String, Object>) loaded;
            return new Frame(
                    String.valueOf(frame.get("c")),
                    Boolean.parseBoolean(String.valueOf(frame.get("r"))),
                    String.valueOf(frame.get("d")));
        } catch (Throwable t) {
            return null;
        }
    }

    /** 会话帧。 */
    record Frame(String conversationId, boolean reply, String data) {
    }
}
