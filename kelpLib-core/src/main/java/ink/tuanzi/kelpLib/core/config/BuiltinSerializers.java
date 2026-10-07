package ink.tuanzi.kelpLib.core.config;

import ink.tuanzi.kelpLib.api.config.ConfigSerializer;
import ink.tuanzi.kelpLib.api.text.Mini;
import ink.tuanzi.kelpLib.api.util.TimeUtil;
import net.kyori.adventure.text.Component;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * 内置配置序列化器：平台无关类型（String/数值/布尔、Component、Duration、枚举、UUID、Locale、InetSocketAddress）。
 */
final class BuiltinSerializers {

    private BuiltinSerializers() {
    }

    /** 注册全部内置序列化器。 */
    static void registerAll(SerializerRegistryImpl registry) {
        put(registry, String.class, raw -> raw);
        put(registry, Integer.class, Integer::parseInt);
        put(registry, Long.class, Long::parseLong);
        put(registry, Double.class, Double::parseDouble);
        put(registry, Float.class, Float::parseFloat);
        put(registry, Boolean.class, Boolean::parseBoolean);
        put(registry, Short.class, Short::parseShort);
        put(registry, Byte.class, Byte::parseByte);
        put(registry, UUID.class, UUID::fromString);
        put(registry, Duration.class, TimeUtil::parseDuration);
        put(registry, Locale.class, raw -> Locale.forLanguageTag(raw.replace('_', '-')));
        put(registry, InetSocketAddress.class, raw -> {
            int idx = raw.lastIndexOf(':');
            if (idx < 0) {
                throw new IllegalArgumentException("Invalid address (host:port expected): " + raw);
            }
            return new InetSocketAddress(raw.substring(0, idx), Integer.parseInt(raw.substring(idx + 1)));
        });
        put(registry, Component.class, Mini::render, Mini::serialize);
    }

    private static <T> void put(SerializerRegistryImpl registry, Class<T> type, Function<String, T> parser) {
        registry.registerInternal(type, parser::apply, Object::toString);
    }

    private static <T> void put(SerializerRegistryImpl registry, Class<T> type,
                                Function<String, T> parser, Function<T, String> writer) {
        registry.registerInternal(type, parser::apply, writer::apply);
    }

    /** Component 用：以 MiniMessage 字符串存取（供注册表内部使用）。 */
    static final class Simple<T> implements ConfigSerializer<T> {

        private final Function<String, T> parser;
        private final Function<T, String> writer;

        Simple(Function<String, T> parser, Function<T, String> writer) {
            this.parser = parser;
            this.writer = writer;
        }

        @Override
        public T deserialize(String raw) {
            return parser.apply(raw);
        }

        @Override
        public String serialize(T value) {
            return writer.apply(value);
        }
    }

    /** 类型映射工具（供引擎解析 List/Map 元素时使用）。 */
    static Map<Class<?>, Class<?>> primitives() {
        Map<Class<?>, Class<?>> map = new HashMap<>();
        map.put(int.class, Integer.class);
        map.put(long.class, Long.class);
        map.put(double.class, Double.class);
        map.put(float.class, Float.class);
        map.put(boolean.class, Boolean.class);
        map.put(short.class, Short.class);
        map.put(byte.class, Byte.class);
        map.put(char.class, Character.class);
        return map;
    }
}
