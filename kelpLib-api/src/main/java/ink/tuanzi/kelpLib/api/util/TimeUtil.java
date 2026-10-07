package ink.tuanzi.kelpLib.api.util;

import java.time.Duration;
import java.util.Locale;

/**
 * 时间工具（双平台）：紧凑时长格式化与解析。
 */
public final class TimeUtil {

    private TimeUtil() {
    }

    /**
     * 解析紧凑时长字符串，如 {@code "5m30s"}、{@code "90s"}、{@code "1h"}、{@code "2d12h"}。
     * 支持单位：d/h/m/s/ms。
     *
     * @param input 时长字符串
     * @return 时长
     * @throws IllegalArgumentException 格式非法
     */
    public static Duration parseDuration(String input) {
        String s = input.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Empty duration: " + input);
        }
        Duration result = Duration.ZERO;
        int i = 0;
        while (i < s.length()) {
            int start = i;
            while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) {
                i++;
            }
            if (start == i) {
                throw new IllegalArgumentException("Invalid duration: " + input);
            }
            double number = Double.parseDouble(s.substring(start, i));
            int uStart = i;
            while (i < s.length() && !Character.isDigit(s.charAt(i))) {
                i++;
            }
            String unit = s.substring(uStart, i);
            result = result.plus(switch (unit) {
                case "d" -> Duration.ofDays((long) number);
                case "h" -> Duration.ofHours((long) number);
                case "m" -> Duration.ofMinutes((long) number);
                case "s" -> Duration.ofSeconds((long) number);
                case "ms" -> Duration.ofMillis((long) number);
                default -> throw new IllegalArgumentException("Unknown duration unit '" + unit + "' in: " + input);
            });
        }
        return result;
    }

    /**
     * 格式化为紧凑可读形式（{@code 2d 3h 4m 5s}；零段省略，全零返回 {@code 0s}）。
     */
    public static String formatDuration(Duration duration) {
        long seconds = Math.max(0, duration.getSeconds());
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0) {
            sb.append(minutes).append("m ");
        }
        if (secs > 0 || sb.isEmpty()) {
            sb.append(secs).append("s");
        }
        return sb.toString().trim();
    }

    /** 格式化为时钟形式 {@code mm:ss}（超过 1 小时为 {@code h:mm:ss}）。 */
    public static String formatClock(Duration duration) {
        long seconds = Math.max(0, duration.getSeconds());
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) {
            return String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s);
        }
        return String.format(Locale.ROOT, "%02d:%02d", m, s);
    }
}
