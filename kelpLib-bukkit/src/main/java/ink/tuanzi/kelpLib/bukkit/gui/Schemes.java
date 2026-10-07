package ink.tuanzi.kelpLib.bukkit.gui;

import java.util.ArrayList;
import java.util.List;

/**
 * 掩码字符串 → 槽位集合（helper MenuScheme 思路，§6.8 低成本 sugar）。
 * 每行 9 列；{@code '1'}~{@code '9'} 为按顺序填充的槽位标记（同一字符属同组），
 * {@code '0'} 或空格为空槽。
 *
 * <pre>{@code
 * List<Integer> slots = Schemes.mask(
 *     "111000011",
 *     "100000001",
 *     "111000011");
 * }</pre>
 */
public final class Schemes {

    private Schemes() {
    }

    /** 解析掩码行；返回槽位序号列表（0~53，按行序）。 */
    public static List<Integer> mask(String... rows) {
        List<Integer> slots = new ArrayList<>();
        for (int row = 0; row < rows.length; row++) {
            String line = rows[row];
            for (int col = 0; col < Math.min(9, line.length()); col++) {
                char c = line.charAt(col);
                if (c >= '1' && c <= '9') {
                    slots.add(row * 9 + col);
                }
            }
        }
        return slots;
    }

    /** 解析掩码行并按标记字符分组（键为 '1'~'9'）。 */
    public static List<List<Integer>> maskGrouped(String... rows) {
        List<List<Integer>> groups = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            groups.add(new ArrayList<>());
        }
        for (int row = 0; row < rows.length; row++) {
            String line = rows[row];
            for (int col = 0; col < Math.min(9, line.length()); col++) {
                char c = line.charAt(col);
                if (c >= '1' && c <= '9') {
                    groups.get(c - '1').add(row * 9 + col);
                }
            }
        }
        groups.removeIf(List::isEmpty);
        return groups;
    }
}
