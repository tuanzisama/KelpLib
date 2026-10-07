package ink.tuanzi.kelpLib.api.storage;

/**
 * 仓储实体主键约定：{@link Repository} 经由本接口定位实体主键。
 *
 * @param <ID> 主键类型
 */
public interface Identifiable<ID> {

    /** 实体主键。 */
    ID id();
}
