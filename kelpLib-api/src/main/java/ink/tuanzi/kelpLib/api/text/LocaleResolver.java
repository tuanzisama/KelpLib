package ink.tuanzi.kelpLib.api.text;

import net.kyori.adventure.audience.Audience;

import java.util.Locale;

/**
 * per-audience locale 解析器（per-player locale）：Bukkit 经 {@code Player#locale()}，
 * Velocity 经 {@code PlayerSettings#getLocale()}，由平台实现注入。
 */
@FunctionalInterface
public interface LocaleResolver {

    /**
     * 解析 audience 的语言；无法解析返回 {@code null}（走默认语言回退链）。
     *
     * @param audience 目标受众
     * @return locale 或 {@code null}
     */
    Locale resolve(Audience audience);
}
