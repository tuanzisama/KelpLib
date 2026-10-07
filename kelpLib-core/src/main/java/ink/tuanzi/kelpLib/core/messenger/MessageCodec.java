package ink.tuanzi.kelpLib.core.messenger;

import ink.tuanzi.kelpLib.core.config.ConfigScanner;
import ink.tuanzi.kelpLib.core.storage.EntityCodec;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * Messenger 消息编解码：复用配置序列化器（§6.9），record/POJO ↔ YAML 字符串 ↔ UTF-8 字节。
 */
final class MessageCodec {

    private final EntityCodec codec;

    MessageCodec(Class<?> messageType, ConfigScanner.Decoder decoder, Function<Object, String> encoder) {
        this.codec = new EntityCodec(ConfigScanner.of(messageType), decoder, encoder);
    }

    byte[] encode(Object message) {
        return codec.encode(message).getBytes(StandardCharsets.UTF_8);
    }

    <T> T decode(byte[] payload, Class<T> type) {
        if (payload == null || payload.length == 0) {
            return null;
        }
        return codec.decode(new String(payload, StandardCharsets.UTF_8), type);
    }
}
