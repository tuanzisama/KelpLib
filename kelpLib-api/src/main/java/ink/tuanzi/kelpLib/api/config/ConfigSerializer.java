package ink.tuanzi.kelpLib.api.config;

/**
 * 单值配置序列化器：YAML 标量字符串 ↔ 目标类型。
 * 复杂结构（List/Map/嵌套配置）由配置引擎按元素类型递归处理，无需自定义。
 *
 * @param <T> 目标类型
 */
public interface ConfigSerializer<T> {

    /**
     * 将 YAML 标量字符串解码为目标类型。
     *
     * @param raw YAML 标量字符串（永不为 null；空串表示空值）
     * @return 解码后的值
     * @throws IllegalArgumentException 解析失败
     */
    T deserialize(String raw);

    /**
     * 将值编码为 YAML 标量字符串。
     *
     * @param value 值（永不为 null）
     * @return YAML 标量字符串
     */
    String serialize(T value);
}
