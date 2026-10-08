# Items（Bukkit 专属）

## ItemBuilder

`ink.tuanzi.kelpLib.bukkit.item.ItemBuilder`：基于现代 ItemMeta API（Adventure 组件，非 legacy MaterialData），名称 / lore 直出 MiniMessage。

```java
ItemStack blade = ItemBuilder.of(Material.NETHERITE_SWORD)
        .name("<gold>烈焰之刃</gold>")
        .lore("<gray>攻击时点燃目标</gray>", "<dark_gray>KelpLib 演示</dark_gray>")
        .enchant(Enchantment.SHARPNESS, 5)
        .unbreakable(true)
        .pdc("utoverse", "item-id", "flame_blade")   // 字符串 PDC 便捷重载
        .skull(owner)                                  // 仅玩家头颅类物品生效
        .amount(1)
        .build();
```

| 方法 | 说明 |
|------|------|
| `of(Material)` / `of(Material, amount)` / `of(ItemStack)` | 起点；`of(ItemStack)` 克隆原物品 |
| `name(String)` / `name(Component)` | 显示名（MiniMessage / 组件）|
| `lore(String...)` | lore，每行一条 MiniMessage |
| `enchant(Enchantment, level)` | 附魔 |
| `unbreakable(boolean)` | 不可破坏 |
| `pdc(NamespacedKey, PersistentDataType, value)` | 写入 PDC（任意类型）|
| `pdc(namespace, key, value)` | 字符串 PDC 便捷重载 |
| `skull(OfflinePlayer)` | 头颅所有者 |
| `amount(int)` | 数量 |

全类型 PDC 写法：

```java
.pdc(new NamespacedKey(plugin, "item-id"), PersistentDataType.STRING, "flame_blade")
```

## Nbt

`ink.tuanzi.kelpLib.bukkit.nbt.Nbt`：封装 tr7zw/ItemNBTAPI 为**可选**集成。启动时自动检测（优先本插件 relocate 捆绑副本 `ink.tuanzi.kelpLib.libs.nbtapi`，其次服务器上的原始坐标 `de.tr7zw.changeme.nbtapi`）；不可用时 `available()` 返回 false，各方法抛带引导的 `IllegalStateException`，降级路径为 PersistentDataContainer（见上）。

```java
if (Nbt.available()) {
    ItemStack out = Nbt.edit(item, nbt -> {
        nbt.setString("utoverse:quest", "q_07");
        return nbt.toItem();
    });
}
```

| 方法 | 说明 |
|------|------|
| `available()` | ItemNBTAPI 是否可用 |
| `edit(ItemStack, Function<Handle, ItemStack>)` | 编辑 NBT，返回新物品（原物品不变）|
| `getString(item, key)` / `setString(item, key, value)` | 字符串 tag 读写 |
| `has(item, key)` | tag 是否存在 |
| `copy(from, to)` | 将 from 全部 NBT 合并到 to（返回新物品）|

`Handle` 提供 `setString` / `getString` / `hasTag` / `removeTag` / `toItem`。

NBT API 为反射调用，仅在需要 PDC 表达不了的原始 NBT 结构时使用。
