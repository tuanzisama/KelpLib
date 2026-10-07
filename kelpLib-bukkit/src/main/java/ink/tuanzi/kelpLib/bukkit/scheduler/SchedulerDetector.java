package ink.tuanzi.kelpLib.bukkit.scheduler;

/**
 * 平台探测（§5.3）：类存在性检测 {@code io.papermc.paper.threadedregions.RegionizedServer}。
 */
public final class SchedulerDetector {

    private static final boolean FOLIA = detect();

    private SchedulerDetector() {
    }

    private static boolean detect() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    /** 当前服务端是否为 Folia（区域化线程模型）。 */
    public static boolean isFolia() {
        return FOLIA;
    }
}
