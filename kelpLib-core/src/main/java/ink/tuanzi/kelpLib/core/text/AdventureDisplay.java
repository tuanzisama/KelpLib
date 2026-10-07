package ink.tuanzi.kelpLib.core.text;

import ink.tuanzi.kelpLib.api.text.Display;
import ink.tuanzi.kelpLib.api.text.Mini;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * 展示 API 默认实现（纯 Adventure，双平台共用；§6.7）：ActionBar/Title/BossBar。
 * 平台无主线程概念的差异由实现平台自行保证调用线程安全（adventure 操作本身线程安全）。
 */
public final class AdventureDisplay implements Display {

    private static final Duration DEFAULT_FADE_IN = Duration.ofMillis(500);
    private static final Duration DEFAULT_STAY = Duration.ofSeconds(3);
    private static final Duration DEFAULT_FADE_OUT = Duration.ofMillis(500);

    private final Map<BossBar, List<Audience>> bossBarViewers = new ConcurrentHashMap<>();

    @Override
    public void actionBar(Audience audience, String miniMessage) {
        audience.sendActionBar(Mini.render(miniMessage));
    }

    @Override
    public void actionBar(Audience audience, String miniMessage, TagResolver... placeholders) {
        audience.sendActionBar(Mini.render(miniMessage, placeholders));
    }

    @Override
    public void title(Audience audience, String title, String subtitle) {
        title(audience, title, subtitle, DEFAULT_FADE_IN, DEFAULT_STAY, DEFAULT_FADE_OUT);
    }

    @Override
    public void title(Audience audience, String title, String subtitle,
                      Duration fadeIn, Duration stay, Duration fadeOut) {
        audience.showTitle(Title.title(
                Mini.render(title),
                Mini.render(subtitle),
                Title.Times.times(fadeIn, stay, fadeOut)));
    }

    @Override
    public BossBar bossBar(Audience audience, String name, float progress, BossBar.Color color) {
        BossBar bar = BossBar.bossBar(Mini.render(name), Math.max(0f, Math.min(1f, progress)),
                color, BossBar.Overlay.PROGRESS);
        audience.showBossBar(bar);
        bossBarViewers.computeIfAbsent(bar, key -> new CopyOnWriteArrayList<>()).add(audience);
        return bar;
    }

    @Override
    public void removeBossBar(BossBar bar) {
        List<Audience> viewers = bossBarViewers.remove(bar);
        if (viewers != null) {
            for (Audience viewer : viewers) {
                viewer.hideBossBar(bar);
            }
        }
    }
}
