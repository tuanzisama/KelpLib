package ink.tuanzi.kelpLib.bukkit.listener;

import ink.tuanzi.kelpLib.api.terminable.Terminable;
import ink.tuanzi.kelpLib.api.terminable.TerminableConsumer;
import ink.tuanzi.kelpLib.KelpLib;
import ink.tuanzi.kelpLib.bukkit.lifecycle.LifecycleBinding;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 函数式事件订阅（§6.2，仅 Bukkit 侧）：subscribe/merge/bindEvent、EventFilters、expireAfter，
 * 订阅即流——过滤 → 过期 → 处理 → 绑定生命周期，一行收口。回调线程不做假设（Folia 区域线程）。
 *
 * <p>实现基于 {@link HandlerList} + {@link org.bukkit.plugin.RegisteredListener} 直接注册，
 * 免去动态类生成；{@code bindWith(consumer)} 时以 consumer 所属插件完成注册。</p>
 *
 * <pre>{@code
 * Events.subscribe(PlayerJoinEvent.class)
 *     .filter(EventFilters.ignoreCancelled())
 *     .handler(e -> messages.send(e.getPlayer(), "welcome"))
 *     .bindWith(pluginLifecycle);
 * }</pre>
 */
public final class Events {

    private static final Logger LOGGER = Logger.getLogger("KelpLib");

    private Events() {
    }

    /** 订阅单个事件类型。 */
    public static <E extends Event> SubscriptionBuilder<E> subscribe(Class<E> type) {
        return new SubscriptionBuilderImpl<>(type);
    }

    /** 多事件归一（共享父类型）。 */
    @SafeVarargs
    public static <B extends Event> MergeBuilder<B> merge(Class<B> base, Class<? extends B>... types) {
        MergeBuilderImpl<B> builder = new MergeBuilderImpl<>();
        for (Class<? extends B> type : types) {
            builder.bindEvent(type, event -> event);
        }
        return builder;
    }

    /** 订阅构建器。 */
    public interface SubscriptionBuilder<E extends Event> {

        SubscriptionBuilder<E> filter(Predicate<E> filter);

        SubscriptionBuilder<E> ignoreCancelled(boolean ignore);

        SubscriptionBuilder<E> priority(EventPriority priority);

        SubscriptionBuilder<E> expireAfter(int executions);

        SubscriptionBuilder<E> expireAfter(Duration duration);

        /** 设置处理器并返回未注册的订阅句柄（经 {@code bindWith}/{@link EventSubscription#register} 注册）。 */
        EventSubscription<E> handler(Consumer<E> handler);
    }

    /** 多事件归一构建器。 */
    public interface MergeBuilder<B extends Event> {

        /** 绑定另一事件类型并提取公共类型。 */
        <E extends B> MergeBuilder<B> bindEvent(Class<E> type, Function<E, B> extractor);

        MergeBuilder<B> filter(Predicate<B> filter);

        MergeBuilder<B> expireAfter(int executions);

        MergeBuilder<B> expireAfter(Duration duration);

        EventSubscription<B> handler(Consumer<B> handler);
    }

    /** 事件订阅句柄（Terminable：close/unsubscribe 即注销）。 */
    public interface EventSubscription<E extends Event> extends Terminable {

        Class<E> eventType();

        /** 立即以指定插件注册。 */
        EventSubscription<E> register(Plugin plugin);

        /** 绑定生命周期：consumer 所属插件即为注册主体；插件 disable 自动注销。 */
        @Override
        @SuppressWarnings("unchecked")
        default <T extends Terminable> T bindWith(TerminableConsumer consumer) {
            Plugin plugin = consumer instanceof LifecycleBinding binding
                    ? binding.plugin()
                    : KelpLib.getInstance();
            register(plugin);
            consumer.bind(this);
            return (T) this;
        }

        /** 注销（等价 {@link #close()}）。 */
        void unsubscribe();
    }

    static HandlerList handlerListOf(Class<? extends Event> type) {
        for (Class<?> current = type; current != null && Event.class.isAssignableFrom(current); current = current.getSuperclass()) {
            try {
                Method method = current.getDeclaredMethod("getHandlerList");
                return (HandlerList) method.invoke(null);
            } catch (NoSuchMethodException ignored) {
                // 继续向父类查找
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to access HandlerList of " + type.getName(), e);
            }
        }
        throw new IllegalStateException("No static getHandlerList() found for " + type.getName());
    }

    private record Binding(Class<? extends Event> type, Function<Event, ? extends Event> extractor) {
    }

    static final class SubscriptionImpl<E extends Event> implements EventSubscription<E> {

        private final Class<E> type;
        private final List<Predicate<E>> filters;
        private final Consumer<E> handler;
        private final EventPriority priority;
        private final boolean ignoreCancelled;
        private final long executionLimit;   // 0 = 不限
        private final long deadlineMillis;   // 0 = 不限
        private final AtomicInteger executions = new AtomicInteger();
        private final AtomicBoolean closed = new AtomicBoolean(false);
        private volatile RegisteredListener registered;
        private volatile HandlerList handlerList;
        private volatile Plugin owner;

        SubscriptionImpl(Class<E> type, List<Predicate<E>> filters, Consumer<E> handler,
                         EventPriority priority, boolean ignoreCancelled,
                         long executionLimit, long deadlineMillis) {
            this.type = type;
            this.filters = List.copyOf(filters);
            this.handler = handler;
            this.priority = priority;
            this.ignoreCancelled = ignoreCancelled;
            this.executionLimit = executionLimit;
            this.deadlineMillis = deadlineMillis;
        }

        @Override
        public Class<E> eventType() {
            return type;
        }

        @Override
        public EventSubscription<E> register(Plugin plugin) {
            if (closed.get()) {
                return this;
            }
            synchronized (this) {
                if (closed.get() || registered != null) {
                    return this;
                }
                HandlerList list = handlerListOf(type);
                Listener marker = new Listener() {
                };
                RegisteredListener listener = new RegisteredListener(marker,
                        (l, event) -> dispatch(type.cast(event)),
                        priority, plugin, ignoreCancelled);
                list.register(listener);
                this.handlerList = list;
                this.registered = listener;
                this.owner = plugin;
                return this;
            }
        }

        private void dispatch(E event) {
            if (closed.get()) {
                return;
            }
            if (executionLimit > 0 && executions.incrementAndGet() > executionLimit) {
                close();
                return;
            }
            if (deadlineMillis > 0 && System.currentTimeMillis() > deadlineMillis) {
                close();
                return;
            }
            try {
                for (Predicate<E> filter : filters) {
                    if (!filter.test(event)) {
                        return;
                    }
                }
                handler.accept(event);
            } catch (Throwable throwable) {
                LOGGER.log(Level.WARNING, "KelpLib event handler failed for " + type.getSimpleName(), throwable);
            }
        }

        @Override
        public void unsubscribe() {
            close();
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                RegisteredListener listener = registered;
                HandlerList list = handlerList;
                if (listener != null && list != null) {
                    list.unregister(listener);
                }
                registered = null;
                handlerList = null;
            }
        }

        @Override
        public boolean isClosed() {
            return closed.get();
        }
    }

    private static final class SubscriptionBuilderImpl<E extends Event> implements SubscriptionBuilder<E> {

        private final Class<E> type;
        private final List<Predicate<E>> filters = new ArrayList<>();
        private EventPriority priority = EventPriority.NORMAL;
        private boolean ignoreCancelled = false;
        private long executionLimit = 0;
        private long deadlineMillis = 0;

        SubscriptionBuilderImpl(Class<E> type) {
            this.type = type;
        }

        @Override
        public SubscriptionBuilder<E> filter(Predicate<E> filter) {
            filters.add(filter);
            return this;
        }

        @Override
        public SubscriptionBuilder<E> ignoreCancelled(boolean ignore) {
            this.ignoreCancelled = ignore;
            return this;
        }

        @Override
        public SubscriptionBuilder<E> priority(EventPriority priority) {
            this.priority = priority;
            return this;
        }

        @Override
        public SubscriptionBuilder<E> expireAfter(int executions) {
            this.executionLimit = executions;
            return this;
        }

        @Override
        public SubscriptionBuilder<E> expireAfter(Duration duration) {
            this.deadlineMillis = System.currentTimeMillis() + duration.toMillis();
            return this;
        }

        @Override
        public EventSubscription<E> handler(Consumer<E> handler) {
            return new SubscriptionImpl<>(type, filters, handler, priority, ignoreCancelled,
                    executionLimit, deadlineMillis);
        }
    }

    private static final class MergeBuilderImpl<B extends Event> implements MergeBuilder<B> {

        private final List<Binding> bindings = new ArrayList<>();
        private final List<Predicate<B>> filters = new ArrayList<>();
        private long executionLimit = 0;
        private long deadlineMillis = 0;

        @Override
        public <E extends B> MergeBuilder<B> bindEvent(Class<E> type, Function<E, B> extractor) {
            bindings.add(new Binding(type, event -> extractor.apply(type.cast(event))));
            return this;
        }

        @Override
        public MergeBuilder<B> filter(Predicate<B> filter) {
            filters.add(filter);
            return this;
        }

        @Override
        public MergeBuilder<B> expireAfter(int executions) {
            this.executionLimit = executions;
            return this;
        }

        @Override
        public MergeBuilder<B> expireAfter(Duration duration) {
            this.deadlineMillis = System.currentTimeMillis() + duration.toMillis();
            return this;
        }

        @Override
        public EventSubscription<B> handler(Consumer<B> handler) {
            MergedSubscription<B> subscription = new MergedSubscription<>(filters, handler, executionLimit, deadlineMillis);
            for (Binding binding : bindings) {
                subscription.bind(binding);
            }
            return subscription;
        }
    }

    private static final class MergedSubscription<B extends Event> implements EventSubscription<B> {

        private final List<Predicate<B>> filters;
        private final Consumer<B> handler;
        private final long executionLimit;
        private final long deadlineMillis;
        private final AtomicInteger executions = new AtomicInteger();
        private final AtomicBoolean closed = new AtomicBoolean(false);
        private final List<HandlerList> handlerLists = new ArrayList<>();
        private final List<RegisteredListener> registered = new ArrayList<>();
        private volatile Plugin owner;
        private final Class<B> baseType;

        @SuppressWarnings("unchecked")
        MergedSubscription(List<Predicate<B>> filters, Consumer<B> handler, long executionLimit, long deadlineMillis) {
            this.filters = List.copyOf(filters);
            this.handler = handler;
            this.executionLimit = executionLimit;
            this.deadlineMillis = deadlineMillis;
            this.baseType = (Class<B>) Event.class;
        }

        void bind(Binding binding) {
            HandlerList list = handlerListOf(binding.type());
            RegisteredListener listener = new RegisteredListener(new Listener() {
            }, (l, event) -> dispatch(binding.extractor().apply(event)), EventPriority.NORMAL,
                    owner != null ? owner : KelpLib.getInstance(), false);
            list.register(listener);
            handlerLists.add(list);
            registered.add(listener);
            if (closed.get()) {
                list.unregister(listener);
            }
        }

        private void dispatch(Event extracted) {
            @SuppressWarnings("unchecked")
            B typed = (B) extracted;
            if (closed.get()) {
                return;
            }
            if (executionLimit > 0 && executions.incrementAndGet() > executionLimit) {
                close();
                return;
            }
            if (deadlineMillis > 0 && System.currentTimeMillis() > deadlineMillis) {
                close();
                return;
            }
            try {
                for (Predicate<B> filter : filters) {
                    if (!filter.test(typed)) {
                        return;
                    }
                }
                handler.accept(typed);
            } catch (Throwable throwable) {
                LOGGER.log(Level.WARNING, "KelpLib merged event handler failed", throwable);
            }
        }

        @Override
        public Class<B> eventType() {
            return baseType;
        }

        @Override
        public EventSubscription<B> register(Plugin plugin) {
            this.owner = plugin; // 已注册监听以 KelpLib 为主体；此字段供后续 bind 使用
            return this;
        }

        @Override
        public void unsubscribe() {
            close();
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                for (int i = 0; i < handlerLists.size(); i++) {
                    handlerLists.get(i).unregister(registered.get(i));
                }
                handlerLists.clear();
                registered.clear();
            }
        }

        @Override
        public boolean isClosed() {
            return closed.get();
        }
    }
}
