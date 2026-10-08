# GUI（Bukkit 专属）

GUI 集成门面为 `ink.tuanzi.kelpLib.bukkit.gui.KelpGui`。当前构建为**能力门控的脚手架**：

- `KelpGui.available()` 恒返回 `false`（本构建未捆绑 InvUI，`repo.xenondevs.xyz` 不可达）
- `KelpGui.open(viewer, gui)` 抛 `IllegalStateException`（带引导说明）

调用前以 `available()` 门控。会话生命周期约定已就位：菜单会话实现 `Terminable`、经 `KelpBukkit.regions().forEntity(viewer, ...)` 驱动刷新、跨玩家零共享，待 InvUI 依赖恢复后接入。

## Schemes

掩码字符串 → 槽位集合（布局 sugar），独立于 GUI 门控可用。每行 9 列；`'1'`~`'9'` 为按顺序填充的槽位标记（同一字符属同组），`'0'` 或空格为空槽：

```java
// 获取槽位序号列表（0~53，按行序）
List<Integer> border = Schemes.mask(
        "111000011",
        "100000001",
        "100000001",
        "111000011");

// 按标记字符分组（键为 '1'~'9'）
List<List<Integer>> groups = Schemes.maskGrouped(
        "111000011",
        "100000001",
        "111000011");
// groups.get(0) 为 '1' 标记的 16 个槽位
```

`mask` 适合单组布局（边框、通栏）；`maskGrouped` 适合多组布局（头部 / 内容 / 底部各自一组），空组剔除。
