package ink.tuanzi.kelpLib.api.text;

import ink.tuanzi.kelpLib.api.Kelp;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.Locale;

/**
 * 消息与多语言（§6.7，双平台）：语言文件沿用 YAML，键即消息 id，值支持 MiniMessage；
 * per-player locale；缺失键回退默认语言，再回退键本身；开发模式打印缺失键告警。
 *
 * <pre>{@code
 * Messages messages = Messages.create(plugin)
 *     .defaultLocale("zh_cn")
 *     .build();
 *
 * messages.send(player, "welcome", Placeholder.unparsed("player", player.getName()));
 * }</pre>
 */
public interface Messages {

    /** 创建构建器（经平台注入数据目录与默认 locale 解析器）。 */
    static Builder create(Object plugin) {
        return Kelp.platform().messages(plugin);
    }

    /**
     * 向受众发送渲染后的消息。
     *
     * @param audience     目标受众（玩家/控制台）
     * @param key          消息键
     * @param placeholders MiniMessage 占位符
     */
    void send(Audience audience, String key, TagResolver... placeholders);

    /**
     * 渲染消息为组件（不发送）。
     *
     * @param key          消息键
     * @param locale       目标语言（null 走默认语言）
     * @param placeholders MiniMessage 占位符
     * @return 渲染后的组件
     */
    Component render(String key, Locale locale, TagResolver... placeholders);

    /**
     * 取消息原文（MiniMessage 字符串；缺失时按回退链取键本身）。
     *
     * @param key    消息键
     * @param locale 目标语言
     * @return 原文
     */
    String raw(String key, Locale locale);

    /** 解析受众语言（用构建时注入的解析器；未解析出返回默认语言）。 */
    Locale localeOf(Audience audience);

    /** 重载全部语言文件。 */
    void reload();

    /** 构建器。 */
    interface Builder {

        /** 默认语言标签（如 {@code "zh_cn"}），默认 {@code "en"}。 */
        Builder defaultLocale(String localeTag);

        /** 覆盖 locale 解析器（缺省用平台默认）。 */
        Builder localeResolver(LocaleResolver resolver);

        /**
         * 追加语言文件（相对数据目录；默认尝试 {@code messages_<locale>.yml}）。
         * 同一语言可多次调用合并（后者优先）。
         */
        Builder bundle(String fileName);

        /** 缺失键是否回退到键本身（默认 true）。 */
        Builder fallbackToKey(boolean fallbackToKey);

        /** 开发模式：缺失键打印告警（默认关闭）。 */
        Builder devMode(boolean devMode);

        /** 构建 Messages 实例。 */
        Messages build();
    }
}
