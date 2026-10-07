package ink.tuanzi.kelpLib.bukkit.lifecycle;

import ink.tuanzi.kelpLib.api.terminable.CompositeTerminable;
import ink.tuanzi.kelpLib.api.terminable.Terminable;
import ink.tuanzi.kelpLib.api.terminable.TerminableConsumer;
import org.bukkit.plugin.Plugin;

/**
 * 与业务插件生命周期绑定的 TerminableConsumer：插件 disable（PluginDisableEvent）时统一关闭。
 */
public final class BukkitLifecycleConsumer implements TerminableConsumer, LifecycleBinding {

    private final Plugin plugin;
    private final CompositeTerminable.Impl composite = new CompositeTerminable.Impl();

    public BukkitLifecycleConsumer(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Plugin plugin() {
        return plugin;
    }

    @Override
    public <T extends Terminable> T bind(T terminable) {
        return composite.bind(terminable);
    }

    @Override
    public void close() {
        composite.close();
    }

    @Override
    public boolean isClosed() {
        return composite.isClosed();
    }
}
