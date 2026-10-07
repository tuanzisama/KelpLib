package ink.tuanzi.kelpLib.api.storage;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 仓储抽象（§6.9）：文件 / SQL / Redis 三种实现共享同一 API，全异步（P3）。
 * 实体需实现 {@link Identifiable}。
 *
 * <p>连接操作在独立线程池执行，回调经调度器切换回异步执行器；调用方需要主线程语义时
 * 用 {@code Promise} 的 sync 系列方法衔接。</p>
 *
 * @param <T>  实体类型
 * @param <ID> 主键类型（UUID/Integer/Long/String）
 */
public interface Repository<T, ID> {

    /** 按主键加载；不存在时返回空 Optional。 */
    CompletableFuture<Optional<T>> load(ID id);

    /** 保存（upsert 语义）。 */
    CompletableFuture<Void> save(T entity);

    /** 按主键删除。 */
    CompletableFuture<Void> delete(ID id);

    /** 加载全部实体（数据量大时慎用）。 */
    CompletableFuture<List<T>> loadAll();
}
