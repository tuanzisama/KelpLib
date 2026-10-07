package ink.tuanzi.kelpLib.velocity.transport;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import ink.tuanzi.kelpLib.api.messenger.Transport;
import ink.tuanzi.kelpLib.api.terminable.Terminable;
import ink.tuanzi.kelpLib.velocity.KelpLibVelocity;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 插件消息传输（§6.9，M5；Velocity 侧）：
 * 与 Bukkit 侧共用通道 {@code kelplib:main} 与帧格式 {@code [频道名 UTF-8][0x00][载荷]}。
 * Bukkit→Velocity 方向经后端连接到达（source 为 ServerConnection 时处理）；
 * Velocity→Bukkit 方向经在线玩家的 ServerConnection 转发（无玩家时静默丢弃）。
 */
public final class PluginMessageTransport implements Transport {

    /** Minecraft 插件消息通道标识。 */
    public static final MinecraftChannelIdentifier IDENTIFIER =
            MinecraftChannelIdentifier.create("kelplib", "main");

    private final ProxyServer proxy;
    private final CopyOnWriteArrayList<ChannelListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean registered = new AtomicBoolean(false);

    public PluginMessageTransport(ProxyServer proxy) {
        this.proxy = proxy;
        proxy.getEventManager().register(KelpLibVelocity.getInstance(), new MessageListener());
    }

    @Override
    public String id() {
        return "plugin-messaging";
    }

    @Override
    public void publish(String channel, byte[] payload) {
        byte[] framed = frame(channel, payload);
        for (Player player : proxy.getAllPlayers()) {
            ServerConnection connection = player.getCurrentServer().orElse(null);
            if (connection != null && connection.sendPluginMessage(IDENTIFIER, framed)) {
                return; // 经任一后端连接送达即可
            }
        }
        // 无在线玩家：静默丢弃（平台插件消息机制的固有约束）
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
            proxy.getChannelRegistrar().unregister(IDENTIFIER);
        }
        listeners.clear();
    }

    private void register() {
        if (registered.compareAndSet(false, true)) {
            proxy.getChannelRegistrar().register(IDENTIFIER);
        }
    }

    private final class MessageListener {

        @Subscribe
        public void onPluginMessage(PluginMessageEvent event) {
            if (!IDENTIFIER.getId().equals(event.getIdentifier().getId())) {
                return;
            }
            // 仅处理后端（Bukkit）→ 代理方向；客户端伪造消息（source 为 Player）忽略
            if (!(event.getSource() instanceof ServerConnection)) {
                return;
            }
            Frame frame = unframe(event.getData());
            if (frame == null) {
                return;
            }
            for (ChannelListener listener : List.copyOf(listeners)) {
                if (listener.channel().equals(frame.channel())) {
                    try {
                        listener.listener().accept(frame.payload());
                    } catch (Throwable ignored) {
                        // 单个订阅者异常不影响其余订阅者
                    }
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
