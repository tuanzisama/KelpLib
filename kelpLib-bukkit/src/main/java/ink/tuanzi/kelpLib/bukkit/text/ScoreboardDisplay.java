package ink.tuanzi.kelpLib.bukkit.text;

import ink.tuanzi.kelpLib.api.terminable.Terminable;
import ink.tuanzi.kelpLib.api.text.Mini;
import ink.tuanzi.kelpLib.bukkit.KelpBukkit;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 计分板展示（§6.7，仅 Bukkit 侧；Velocity 不做计分板，§1.3）：
 * 每玩家侧边栏（Objective + 逐行 Team prefix），全部更新经
 * {@code KelpBukkit.regions().forEntity} 路由到实体线程（Folia 硬约束，§5.3）。
 * 实现幂等 {@link Terminable}：close 恢复主计分板。
 */
public interface ScoreboardDisplay extends Terminable {

    /** 更新标题与行（MiniMessage；最多 16 行，多余忽略）。可在任意线程调用。 */
    void update(String title, List<String> lines);

    /** 为玩家创建侧边栏（建议在玩家加入等同步回调中调用）。 */
    static ScoreboardDisplay create(Player player) {
        return new Sidebar(player);
    }
}

final class Sidebar implements ScoreboardDisplay {

    private static final int MAX_LINES = 16;

    private final Player player;
    private final Scoreboard scoreboard;
    private final Objective objective;
    private final Team[] teams = new Team[MAX_LINES];
    private final String[] entries = new String[MAX_LINES];
    private final AtomicBoolean closed = new AtomicBoolean(false);

    Sidebar(Player player) {
        this.player = player;
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.objective = scoreboard.registerNewObjective("kelp", "dummy", net.kyori.adventure.text.Component.empty());
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        ChatColor[] colors = ChatColor.values();
        for (int i = 0; i < MAX_LINES; i++) {
            entries[i] = colors[i % colors.length].toString(); // 唯一且不可见条目
            teams[i] = scoreboard.registerNewTeam("kelp-" + i);
            teams[i].addEntry(entries[i]);
        }
        player.setScoreboard(scoreboard);
    }

    @Override
    public void update(String title, List<String> lines) {
        if (closed.get()) {
            return;
        }
        KelpBukkit.regions().forEntity(player, task -> apply(title, lines));
    }

    private void apply(String title, List<String> lines) {
        if (closed.get()) {
            return;
        }
        objective.displayName(Mini.render(title));
        for (int i = 0; i < MAX_LINES; i++) {
            if (i < lines.size()) {
                teams[i].prefix(Mini.render(lines.get(i)));
                objective.getScore(entries[i]).setScore(MAX_LINES - i);
            } else {
                scoreboard.resetScores(entries[i]);
            }
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            KelpBukkit.regions().forEntity(player, task -> {
                try {
                    objective.unregister();
                } catch (Throwable ignored) {
                    // 已注销则忽略
                }
                if (player.isOnline()) {
                    player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                }
            });
        }
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }
}
