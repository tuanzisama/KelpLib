package ink.tuanzi.kelpLib.bukkit.item;

import ink.tuanzi.kelpLib.api.text.Mini;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemMeta;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * 物品构建器（§6.3，仅 Bukkit 侧）：基于现代 ItemMeta API（Adventure 组件，非 legacy MaterialData），
 * 名称/lore 直出 MiniMessage，支持附魔、PDC、头颅所有者。
 *
 * <pre>{@code
 * ItemStack blade = ItemBuilder.of(Material.NETHERITE_SWORD)
 *     .name("<gold>烈焰之刃</gold>")
 *     .lore("<gray>攻击时点燃目标</gray>")
 *     .enchant(Enchantment.SHARPNESS, 5)
 *     .pdc(new NamespacedKey(plugin, "item-id"), PersistentDataType.STRING, "flame_blade")
 *     .skull(owner)
 *     .build();
 * }</pre>
 */
public final class ItemBuilder {

    private final ItemStack item;

    private ItemBuilder(ItemStack item) {
        this.item = item;
    }

    /** 以材料开始（数量 1）。 */
    public static ItemBuilder of(Material material) {
        return new ItemBuilder(new ItemStack(material));
    }

    /** 以材料与数量开始。 */
    public static ItemBuilder of(Material material, int amount) {
        return new ItemBuilder(new ItemStack(material, Math.max(1, amount)));
    }

    /** 以既有物品开始（副本，不影响原物品）。 */
    public static ItemBuilder of(ItemStack item) {
        return new ItemBuilder(item.clone());
    }

    /** 显示名（MiniMessage 直出）。 */
    public ItemBuilder name(String miniMessage) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Mini.render(miniMessage));
            item.setItemMeta(meta);
        }
        return this;
    }

    /** 显示名（组件）。 */
    public ItemBuilder name(Component component) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(component);
            item.setItemMeta(meta);
        }
        return this;
    }

    /** lore（MiniMessage，每行一条）。 */
    public ItemBuilder lore(String... miniMessages) {
        List<Component> lore = new ArrayList<>(miniMessages.length);
        for (String line : miniMessages) {
            lore.add(Mini.render(line));
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return this;
    }

    /** 附魔。 */
    public ItemBuilder enchant(Enchantment enchantment, int level) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(enchantment, level, true);
            item.setItemMeta(meta);
        }
        return this;
    }

    /** 不可破坏。 */
    public ItemBuilder unbreakable(boolean unbreakable) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(unbreakable);
            item.setItemMeta(meta);
        }
        return this;
    }

    /** 写入 PersistentDataContainer（任意类型）。 */
    public <T, Z> ItemBuilder pdc(NamespacedKey key, PersistentDataType<T, Z> type, Z value) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(key, type, value);
            item.setItemMeta(meta);
        }
        return this;
    }

    /** 写入字符串 PDC（便捷重载）。 */
    public ItemBuilder pdc(String namespace, String key, String value) {
        return pdc(new NamespacedKey(namespace, key), PersistentDataType.STRING, value);
    }

    /** 头颅所有者（仅玩家头颅类物品生效）。 */
    public ItemBuilder skull(OfflinePlayer owner) {
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(owner);
            item.setItemMeta(skullMeta);
        }
        return this;
    }

    /** 数量。 */
    public ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, amount));
        return this;
    }

    /** 构建物品。 */
    public ItemStack build() {
        return item;
    }
}
