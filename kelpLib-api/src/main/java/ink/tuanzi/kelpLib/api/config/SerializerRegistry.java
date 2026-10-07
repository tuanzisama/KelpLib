package ink.tuanzi.kelpLib.api.config;

/**
 * 配置序列化器注册表。内置序列化器覆盖平台无关类型：
 * {@code String/基本类型及包装}、{@code Component}（MiniMessage 字符串）、{@code Duration}、
 * 枚举、{@code UUID}、{@code Locale}、{@code InetSocketAddress}、{@code List<T>}、{@code Map<String,T>}；
 * {@code ItemStack}/{@code Location} 序列化器由 bukkit 模块追加注册。
 */
public interface SerializerRegistry {

    /**
     * 注册（或覆盖）某类型的序列化器。
     *
     * @param type       目标类型
     * @param serializer 序列化器
     * @param <T>        目标类型
     */
    <T> void register(Class<T> type, ConfigSerializer<T> serializer);

    /** 查询某类型的序列化器；未注册返回 {@code null}。 */
    <T> ConfigSerializer<T> serializer(Class<T> type);

    /**
     * 将 YAML 标量字符串解码为目标类型的值（无注册序列化器时按内置规则兜底）。
     *
     * @param type 目标类型
     * @param raw  YAML 标量字符串
     * @return 解码后的值
     * @throws IllegalArgumentException 无法解码
     */
    Object deserialize(Class<?> type, String raw);

    /**
     * 将值编码为 YAML 标量字符串（无注册序列化器时按内置规则兜底）。
     *
     * @param value 待编码值
     * @return YAML 标量字符串
     * @throws IllegalArgumentException 无法编码
     */
    String serialize(Object value);
}
