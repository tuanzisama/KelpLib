package ink.tuanzi.kelpLib.api.terminable;

/**
 * 按功能分组注册的 Terminable 模块：库内各模块（调度、监听器、GUI 会话、存储……）
 * 可持有独立模块实例，便于分类注销与诊断。
 */
public interface TerminableModule extends TerminableConsumer {

    /** 模块名。 */
    String name();

    /** 创建具名模块。 */
    static TerminableModule create(String name) {
        return new Impl(name);
    }

    /** 默认实现。 */
    final class Impl implements TerminableModule {

        private final String name;
        private final CompositeTerminable.Impl composite = new CompositeTerminable.Impl();

        private Impl(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
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
}
