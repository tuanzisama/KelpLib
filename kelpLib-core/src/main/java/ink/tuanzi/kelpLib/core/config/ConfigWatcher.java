package ink.tuanzi.kelpLib.core.config;

import ink.tuanzi.kelpLib.api.scheduler.KelpScheduler;
import ink.tuanzi.kelpLib.api.terminable.Terminable;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 配置文件变更监视（watch service）：变更 → 异步重读 → 经平台同步执行器安全回调（§6.6）。
 * 实现幂等 {@link Terminable}，随插件 disable 自动关闭。
 */
final class ConfigWatcher implements Terminable {

    private static final long DEBOUNCE_MILLIS = 300;

    private final ConfigEngineImpl engine;
    private final Object plugin;
    private final Class<?> type;
    private final Consumer<Object> onChange;
    private final KelpScheduler scheduler;
    private final Path file;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private volatile Thread worker;
    private volatile WatchService watchService;

    ConfigWatcher(ConfigEngineImpl engine, Object plugin, Class<?> type,
                  Consumer<?> onChange, KelpScheduler scheduler) {
        this.engine = engine;
        this.plugin = plugin;
        this.type = type;
        @SuppressWarnings("unchecked")
        Consumer<Object> consumer = (Consumer<Object>) onChange;
        this.onChange = consumer;
        this.scheduler = scheduler;
        this.file = engineDataDirectory(plugin).resolve(ConfigScanner.pathOf(type));
    }

    private Path engineDataDirectory(Object pluginInstance) {
        // 经由引擎的公共 load/save 路径即可推导文件；这里直接复用 load 的文件定位逻辑
        return engine.dataDirectoryFor(pluginInstance);
    }

    /** 启动监视线程。 */
    void start() {
        try {
            Path parent = file.getParent();
            if (parent == null || !Files.isDirectory(parent)) {
                Files.createDirectories(parent);
            }
            watchService = parent.getFileSystem().newWatchService();
            parent.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE);
        } catch (IOException e) {
            ConfigEngineImpl.logFailure(e);
            return;
        }
        worker = new Thread(this::pollLoop, "KelpLib-ConfigWatch-" + file.getFileName());
        worker.setDaemon(true);
        worker.start();
    }

    private void pollLoop() {
        long lastFire = 0;
        try {
            while (!closed.get()) {
                WatchKey key = watchService.poll(500, TimeUnit.MILLISECONDS);
                if (key == null) {
                    continue;
                }
                boolean relevant = false;
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.context() != null && event.context().toString().equals(file.getFileName().toString())) {
                        relevant = true;
                    }
                }
                key.reset();
                long now = System.currentTimeMillis();
                if (relevant && now - lastFire > DEBOUNCE_MILLIS) {
                    lastFire = now;
                    fire();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ClosedWatchServiceException ignored) {
            // 正常关闭
        }
    }

    private void fire() {
        CompletableFuture
                .supplyAsync(() -> engine.load(plugin, type), scheduler.asyncExecutor())
                .whenComplete((fresh, error) -> {
                    if (error != null) {
                        ConfigEngineImpl.logFailure(error);
                        return;
                    }
                    // 同步安全回调：回到平台 GLOBAL 作用域执行器
                    scheduler.syncExecutor().execute(() -> {
                        try {
                            onChange.accept(fresh);
                        } catch (Throwable t) {
                            ConfigEngineImpl.logFailure(t);
                        }
                    });
                });
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            if (watchService != null) {
                try {
                    watchService.close();
                } catch (IOException ignored) {
                    // 关闭失败无需处理
                }
            }
            if (worker != null) {
                worker.interrupt();
            }
        }
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }
}
