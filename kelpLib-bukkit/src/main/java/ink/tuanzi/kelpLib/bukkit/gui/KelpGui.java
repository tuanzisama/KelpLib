package ink.tuanzi.kelpLib.bukkit.gui;

import org.bukkit.entity.Player;

/**
 * GUI 集成胶水门面（§6.8，仅 Bukkit 侧）。
 *
 * <p>选型为 InvUI（捆绑但不 relocate，导出型依赖）。本构建环境无法访问 InvUI 仓库
 * （repo.xenondevs.xyz，§8 依赖风险项），故本版本为<b>能力门控的脚手架</b>：
 * {@link #available()} 返回 {@code false}，调用入口抛出带引导的错误；
 * {@link Schemes} 掩码 sugar 与会话生命周期约定（菜单会话实现 Terminable、
 * 经 {@code KelpBukkit.regions().forEntity(viewer, ...)} 驱动刷新、跨玩家零共享）
 * 已就位，待 InvUI 依赖恢复后按 §6.8 约束接入。</p>
 */
public final class KelpGui {

    private KelpGui() {
    }

    /**
     * GUI 模块是否可用（26.x + InvUI 捆绑齐备）。
     * 当前构建未捆绑 InvUI：恒为 {@code false}（§6.8 Capabilities 门控）。
     */
    public static boolean available() {
        return false;
    }

    /**
     * 打开菜单（预留入口：InvUI 接入后实现 viewer EntityScheduler 线程路由 + 会话 Terminable 注册）。
     *
     * @throws IllegalStateException 当前构建未捆绑 InvUI
     */
    public static void open(Player viewer, Object gui) {
        throw new IllegalStateException(
                "GUI 模块不可用：本构建未捆绑 InvUI（repo.xenondevs.xyz 不可达）。"
                        + "请安装 InvUI（xyz.xenondevs.invui）或使用 1.21.x 门控语义（GUI 仅 26.x 生效）。");
    }
}
