# Utilities

`ink.tuanzi.kelpLib.api.util` 下的平台无关小工具。

## Cooldown

并发安全冷却器，按 UUID 键控：

```java
Cooldown cd = Cooldown.of(Duration.ofSeconds(30));

if (cd.tryAcquire(player.getUniqueId())) {
    doAction();                                        // 放行并开始冷却
} else {
    long seconds = cd.remaining(player.getUniqueId()).toSeconds();
    player.sendMessage("冷却中，剩余 " + seconds + "s");
}
```

| 方法 | 说明 |
|------|------|
| `of(Duration)` | 创建固定时长冷却器 |
| `tryAcquire(UUID)` | 尝试获取一次配额；允许返回 true 并开始冷却 |
| `isCooling(UUID)` | 是否处于冷却中 |
| `remaining(UUID)` | 剩余时长（不在冷却返回 `Duration.ZERO`）|
| `reset(UUID)` / `clear()` | 清除某键 / 全部冷却 |
| `clearExpired()` | 清理已过期键（低频调用，避免长驻内存）|

## Bucket

按 tick 分区分散大批量元素处理，避免单 tick 卡顿（Folia 下把大批量负载摊到多个 tick）：

```java
Bucket<UUID> bucket = Bucket.of(20);   // 分散到 20 个分区
pendingPlayers.forEach(bucket::add);

Kelp.scheduler().runTimer(Duration.ZERO, Duration.ofMillis(50), task -> {
    for (UUID id : bucket.poll()) {    // 每 tick 取当前分区
        process(id);
    }
});
```

| 方法 | 说明 |
|------|------|
| `of(bucketCount)` | 创建指定分区数的桶（≥ 1）|
| `add(T)` | 轮转写入下一分区 |
| `poll()` | 取出当前分区内容并推进游标；返回快照，勿长期持有 |
| `size()` / `clear()` | 未消费总数 / 清空 |

## WeightedRandom

加权随机：

```java
WeightedRandom<String> loot = WeightedRandom.<String>builder()
        .add("common", 70)
        .add("rare", 25)
        .add("legendary", 5)
        .build();

String item = loot.next();          // ThreadLocalRandom
String seeded = loot.next(random);  // 指定随机源；空集抛 IllegalStateException
```

权重 ≤ 0 的条目被忽略；`Map<T, Double>` 可经 `WeightedRandom.of(map)` 构建。

## TimeUtil

紧凑时长格式化与解析：

| 方法 | 示例 |
|------|------|
| `parseDuration(String)` | `"5m30s"`、`"90s"`、`"1h"`、`"2d12h"` → `Duration`；单位 d/h/m/s/ms，非法抛 `IllegalArgumentException` |
| `formatDuration(Duration)` | → `"2d 3h 4m 5s"`（零段省略，全零 `0s`）|
| `formatClock(Duration)` | → `"04:35"`；超 1 小时 `"1:04:35"` |

`parseDuration` 同时是 `Duration` 类型配置字段的序列化格式，见 [Config](config.md)。
