package ink.tuanzi.kelpLib.bukkit.transport;

import ink.tuanzi.kelpLib.api.messenger.Transport;
import ink.tuanzi.kelpLib.api.terminable.Terminable;
import ink.tuanzi.kelpLib.KelpLib;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 插件消息传输（§6.9，M5；Bukkit↔Velocity 直连通道）：
 * 单一 Minecraft 通道 {@code kelplib:main} 复用承载全部 KelpLib 频道，
 * 帧格式：{@code [频道名 UTF-8][0x00][载荷]}。
 *
 * <p>限制：Bukkit→Velocity 方向需至少一名在线玩家作为连接载体（无玩家时投递静默丢弃，
 * 平台插件消息机制的固有约束）。</p>
 */
public final class PluginMessageTransport implements Transport, PluginMessageListener {

    /** Minecraft 插件消息通道名。 */
    public static final String CHANNEL = "kelplib:main";

    private final CopyOnWriteArrayList<ChannelListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean registered = new AtomicBoolean(false);

    @Override
    public String id() {
        return "plugin-messaging";
    }

    @Override
    public void publish(String channel, byte[] payload) {
        byte[] framed = frame(channel, payload);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.sendPluginMessage(KelpLib.getInstance(), CHANNEL, framed)) {
                return; // 经任一在线玩家的连接送达代理端即可
            }
        }
        // 无在线玩家：静默丢弃（文档约束）
    }

    @Override
    public Terminable subscribe(String channel, Consumer<byte[]> listener) {
        ChannelListener handle = new ChannelListener(channel, listener);
        listeners.add(handle);
        register();
        return new Terminable() {
            private final AtomicBoolean closed = new AtomicBoolean(false);

            @Override
            public void close() {
                if (closed.compareAndSet(false, true)) {
                    listeners.remove(handle);
                }
            }

            @Override
            public boolean isClosed() {
                return closed.get();
            }
        };
    }

    @Override
    public void start() {
        register();
    }

    @Override
    public void stop() {
        if (registered.compareAndSet(true, false)) {
            KelpLib kelp = KelpLib.getInstance();
            Bukkit.getMessenger().unregisterIncomingPluginChannel(kelp, CHANNEL, this);
            Bukkit.getMessenger().unregisterOutgoingPluginChannel(kelp, CHANNEL);
        }
        listeners.clear();
    }

    private void register() {
        if (registered.compareAndSet(false, true)) {
            KelpLib kelp = KelpLib.getInstance();
            Bukkit.getMessenger().registerOutgoingPluginChannel(kelp, CHANNEL);
            Bukkit.getMessenger().registerIncomingPluginChannel(kelp, CHANNEL, this);
        }
    }

    @Override
    public void pluginMessageReceived(String channel, Player player, byte[] message) {
        if (!CHANNEL.equals(channel)) {
            return;
        }
        Frame frame = unframe(message);
        if (frame == null) {
            return;
        }
        for (ChannelListener listener : List.copyOf(listeners)) {
            if (listener.channel.equals(frame.channel)) {
                try {
                    listener.listener.accept(frame.payload);
                } catch (Throwable ignored) {
                    // 单个订阅者异常不影响其余订阅者
                }
            }
        }
    }

    private record ChannelListener(String channel, Consumer<byte[]> listener) {
    }

    private record Frame(String channel, byte[] payload) {
    }

    private static byte[] frame(String channel, byte[] payload) {
        byte[] name = channel.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream(name.length + 1 + payload.length);
        out.writeBytes(name);
        out.write(0x00);
        out.writeBytes(payload);
        return out.toByteArray();
    }

    private static Frame unframe(byte[] data) {
        int separator = -1;
        for (int i = 0; i < data.length; i++) {
            if (data[i] == 0x00) {
                separator = i;
                break;
            }
        }
        if (separator <= 0) {
            return null;
        }
        String channel = new String(data, 0, separator, StandardCharsets.UTF_8);
        byte[] payload = new byte[data.length - separator - 1];
        System.arraycopy(data, separator + 1, payload, 0, payload.length);
        return new Frame(channel, payload);
    }
}
