package ink.tuanzi.kelpLib.api.terminable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 组合注册表（分组注册）：将多个 {@link Terminable} 聚合为一个，统一绑定与关闭。
 * 线程安全；关闭幂等。
 */
public interface CompositeTerminable extends TerminableConsumer {

    /** 创建空的组合注册表。 */
    static CompositeTerminable create() {
        return new Impl();
    }

    /** 默认实现（api 内置的纯 Java 实现，无平台依赖）。 */
    final class Impl implements CompositeTerminable {

        private final List<Terminable> terminables = new CopyOnWriteArrayList<>();
        private final AtomicBoolean closed = new AtomicBoolean(false);

        @Override
        public <T extends Terminable> T bind(T terminable) {
            if (closed.get()) {
                terminable.close();
                return terminable;
            }
            terminables.add(terminable);
            if (closed.get()) {
                // 关闭与绑定并发：已关闭则补偿关闭
                terminable.close();
            }
            return terminable;
        }

        @Override
        public void close() {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            for (Terminable terminable : terminables) {
                try {
                    terminable.close();
                } catch (Throwable throwable) {
                    // 关闭单个失败不影响其余资源回收
                }
            }
            terminables.clear();
        }

        @Override
        public boolean isClosed() {
            return closed.get();
        }
    }
}
