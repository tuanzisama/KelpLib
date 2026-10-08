# Display

展示 API，入口 `Kelp.display()`（`ink.tuanzi.kelpLib.api.text.Display`）。全部经 Adventure `Audience`，平台无关；计分板为 Bukkit 侧专属。

## ActionBar / Title / BossBar

```java
// ActionBar（MiniMessage 直出）
Kelp.display().actionBar(player, "<gold>能量已满</gold>");

// 带占位符
Kelp.display().actionBar(player, "<gold>余量 <seconds>s</gold>",
        Placeholder.unparsed("seconds", "30"));

// Title + Subtitle（默认淡入 500ms / 停留 3s / 淡出 500ms）
Kelp.display().title(player, "<gold>欢迎</gold>", "<gray>utoverse</gray>");

// 自定义时长
Kelp.display().title(player, "<gold>欢迎</gold>", "<gray>utoverse</gray>",
        Duration.ofMillis(200), Duration.ofSeconds(2), Duration.ofMillis(200));

// BossBar（Adventure BossBar）
BossBar bar = Kelp.display().bossBar(player, "<red>Boss 战</red>", 0.75f, BossBar.Color.RED);
bar.progress(0.5f);                          // 进度 0.0~1.0
Kelp.display().removeBossBar(bar);           // 移除
```

Velocity 支持同一 ActionBar / Title / BossBar 子集。

## ScoreboardDisplay（Bukkit 专属）

每玩家侧边栏（`ink.tuanzi.kelpLib.bukkit.text.ScoreboardDisplay`）：Objective + 逐行 Team prefix。全部更新经 `KelpBukkit.regions().forEntity` 路由到实体线程（Folia 硬约束）；实现幂等 `Terminable`，close 恢复主计分板。

```java
ScoreboardDisplay board = ScoreboardDisplay.create(player);   // 建议在玩家加入等同步回调中调用
board.update("<gold>KelpLib</gold>", List.of(                 // 最多 16 行，多余忽略；任意线程可调
        "<gray>玩家:</gray> " + player.getName(),
        "<gray>模块:</gray> scoreboard"));

KelpBukkit.lifecycle(plugin).bind(board);                     // 插件 disable 自动恢复
board.close();                                                // 或手动恢复
```

标题与行均为 MiniMessage。
