package ink.tuanzi.kelpLib.api.storage;

/**
 * Redis 连接规格（Redis 仓库与 Messenger Redis 传输共用）。
 *
 * @param uri      连接串（如 {@code redis://localhost:6379/0}，支持密码 {@code redis://:pass@host:port}）
 * @param keyPrefix 键前缀（默认 {@code kelplib:}，避免与第三方冲突）
 */
public record RedisSpec(String uri, String keyPrefix) {

    /** 默认键前缀。 */
    public static final String DEFAULT_PREFIX = "kelplib:";

    /** 以 URI 创建规格。 */
    public static RedisSpec of(String uri) {
        return new RedisSpec(uri, DEFAULT_PREFIX);
    }

    /** 以主机/端口/密码创建规格。 */
    public static RedisSpec of(String host, int port, String password) {
        String uri = password == null || password.isEmpty()
                ? "redis://" + host + ":" + port + "/0"
                : "redis://:" + password + "@" + host + ":" + port + "/0";
        return new RedisSpec(uri, DEFAULT_PREFIX);
    }

    /** 自定义键前缀。 */
    public RedisSpec keyPrefix(String prefix) {
        return new RedisSpec(uri, prefix);
    }
}
