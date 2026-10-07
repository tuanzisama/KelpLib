package org.bukkit.enchantments;

/**
 * Minimal compile-time stub of org.bukkit.enchantments.Enchantment.
 */
public abstract class Enchantment {

    public static final Enchantment SHARPNESS = stub("sharpness");
    public static final Enchantment EFFICIENCY = stub("efficiency");
    public static final Enchantment UNBREAKING = stub("unbreaking");
    public static final Enchantment PROTECTION = stub("protection");
    public static final Enchantment FIRE_ASPECT = stub("fire_aspect");
    public static final Enchantment LOOT_BONUS_BLOCKS = stub("fortune");

    private final String name;

    private Enchantment(String name) {
        this.name = name;
    }

    private static Enchantment stub(String name) {
        return null; // 编译期桩：仅用于引用类型与字段名，运行时取真实注册表值
    }

    public String getName() {
        return name;
    }
}
