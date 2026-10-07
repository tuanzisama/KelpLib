package ink.tuanzi.kelpLib.api.text;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.time.Duration;

/**
 * 展示 API（§6.7）：ActionBar / Title / BossBar，全部经 Adventure {@link Audience}，
 * 平台无关（Velocity 支持同一子集）；计分板为 Bukkit 侧专属（kelpLib-bukkit 的
 * {@code ScoreboardDisplay}），代理端不做计分板（需后端转发，见 §1.3）。
 *
 * <p>全部实现线程安全（内部经平台调度收口，见 P2）。</p>
 */
public interface Display {

    /** 发送 ActionBar（MiniMessage）。 */
    void actionBar(Audience audience, String miniMessage);

    /** 发送 ActionBar（MiniMessage + 占位符）。 */
    void actionBar(Audience audience, String miniMessage, TagResolver... placeholders);

    /** 发送 Title + Subtitle（默认淡入淡出时长）。 */
    void title(Audience audience, String title, String subtitle);

    /** 发送 Title + Subtitle（自定义时长）。 */
    void title(Audience audience, String title, String subtitle,
               Duration fadeIn, Duration stay, Duration fadeOut);

    /**
     * 创建并向受众展示 BossBar（Adventure BossBar，平台无关）。
     *
     * @param audience 受众
     * @param name     BossBar 标题（MiniMessage）
     * @param progress 进度 0.0~1.0
     * @param color    颜色
     * @return BossBar 句柄（可用 {@link #removeBossBar(BossBar)} 移除）
     */
    BossBar bossBar(Audience audience, String name, float progress, BossBar.Color color);

    /** 从受众移除 BossBar。 */
    void removeBossBar(BossBar bar);
}
