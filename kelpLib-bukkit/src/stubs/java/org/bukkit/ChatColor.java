package org.bukkit;

/**
 * Minimal compile-time stub of org.bukkit.ChatColor（计分板行条目去重前缀用）。
 */
public enum ChatColor {

    BLACK('0'),
    DARK_BLUE('1'),
    DARK_GREEN('2'),
    DARK_AQUA('3'),
    DARK_RED('4'),
    DARK_PURPLE('5'),
    GOLD('6'),
    GRAY('7'),
    DARK_GRAY('8'),
    BLUE('9'),
    GREEN('a'),
    AQUA('b'),
    RED('c'),
    LIGHT_PURPLE('d'),
    YELLOW('e'),
    WHITE('f');

    private final char code;

    ChatColor(char code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "§" + code;
    }
}
