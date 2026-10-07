package ink.tuanzi.kelpLib.api.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/**
 * MiniMessage 渲染便捷入口。
 */
public final class Mini {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private Mini() {
    }

    /** 渲染 MiniMessage 字符串为组件。 */
    public static Component render(String miniMessage, TagResolver... placeholders) {
        return MINI.deserialize(miniMessage, placeholders);
    }

    /** 组件序列化回 MiniMessage 字符串。 */
    public static String serialize(Component component) {
        return MINI.serialize(component);
    }
}
