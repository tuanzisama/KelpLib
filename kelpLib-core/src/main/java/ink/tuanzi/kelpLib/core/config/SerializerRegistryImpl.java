package ink.tuanzi.kelpLib.core.config;

import ink.tuanzi.kelpLib.api.config.ConfigSerializer;
import ink.tuanzi.kelpLib.api.config.SerializerRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 序列化器注册表默认实现。枚举类型无需显式注册（按名称内置处理）。
 */
public final class SerializerRegistryImpl implements SerializerRegistry {

    private static final Map<Class<?>, Class<?>> PRIMITIVES = BuiltinSerializers.primitives();

    private final Map<Class<?>, ConfigSerializer<?>> serializers = new ConcurrentHashMap<>();

    public SerializerRegistryImpl() {
        BuiltinSerializers.registerAll(this);
    }

    <T> void registerInternal(Class<T> type, ConfigSerializer<T> serializer) {
        serializers.put(type, serializer);
    }

    <T> void registerInternal(Class<T> type, java.util.function.Function<String, T> parser,
                              java.util.function.Function<T, String> writer) {
        serializers.put(type, new BuiltinSerializers.Simple<>(parser, writer));
    }

    @Override
    public <T> void register(Class<T> type, ConfigSerializer<T> serializer) {
        serializers.put(type, serializer);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> ConfigSerializer<T> serializer(Class<T> type) {
        Class<?> key = PRIMITIVES.getOrDefault(type, type);
        return (ConfigSerializer<T>) serializers.get(key);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object deserialize(Class<?> type, String raw) {
        ConfigSerializer serializer = serializer(type);
        if (serializer != null) {
            return serializer.deserialize(raw);
        }
        if (type.isEnum()) {
            return Enum.valueOf((Class<? extends Enum>) asEnum(type), raw);
        }
        if (type == String.class) {
            return raw;
        }
        if (type == Character.class || type == char.class) {
            return raw.isEmpty() ? '\0' : raw.charAt(0);
        }
        throw new IllegalArgumentException("No serializer registered for type " + type.getName());
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Enum<?>> asEnum(Class<?> type) {
        return (Class<? extends Enum<?>>) type;
    }

    @Override
    public String serialize(Object value) {
        if (value == null) {
            return "";
        }
        ConfigSerializer serializer = serializer(value.getClass());
        if (serializer != null) {
            return serializer.serialize(value);
        }
        if (value instanceof Enum<?> e) {
            return e.name();
        }
        return String.valueOf(value);
    }
}
