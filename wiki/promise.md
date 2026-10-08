# Promise

`ink.tuanzi.kelpLib.api.promise.Promise` 是跨线程链式异步抽象，负责「一个值在哪个线程、何时产生」。`sync` 语义三平台统一映射到 GLOBAL 作用域执行器，`async` 语义为各平台异步池。

## 起点

| 静态方法 | 说明 |
|----------|------|
| `Promise.start()` | 空起点（值为 null 的已完成链头），便于书写链式调用 |
| `Promise.completed(value)` | 以已完成值开始 |
| `Promise.failed(throwable)` | 以异常完成态开始（测试与错误传播）|
| `Promise.of(CompletableFuture)` | 包装既有 future（关闭 Promise 会取消底层 future）|
| `Promise.supply(Supplier)` | 在 ASYNC 作用域执行 supplier 并开启链 |

## 链式方法

转换方法均有 sync / async 两个变体，区别仅在回调执行的执行器：

| 方法 | 说明 |
|------|------|
| `thenApplySync` / `thenApplyAsync` | 转换值 |
| `thenRunSync` / `thenRunAsync` | 执行动作，链式值原样传递 |
| `thenAcceptSync` / `thenAcceptAsync` | 消费值（如回到主线程更新玩家展示）|
| `thenComposeSync` / `thenComposeAsync` | 衔接下一级 Promise（扁平化组合）|
| `thenDelay(Duration)` | 链式值在指定延迟后向后续阶段传递 |
| `timeout(Duration)` | 超时约束，未在时限内完成以 `TimeoutException` 异常完成 |
| `combine(Promise, BiFunction)` | 与另一 Promise 汇合，两者都完成后合并（异步执行）|
| `exceptionally(Function<Throwable, T>)` | 异常恢复，返回新 Promise，不影响原 Promise |
| `join()` / `join(Duration)` | 阻塞等待结果（勿在主线程使用）|
| `future()` | 底层 `CompletableFuture`，与标准库互操作 |

## 生命周期

Promise 实现 `Terminable`。KelpLib 卸载时未完成的 Promise 以 `CancellationException` 异常完成；可经 `bindWith(consumer)` 绑定到更细的分组提前取消，见 [Terminables](terminables.md)。

## 示例

```java
Promise.start()
        .thenApplyAsync(value -> "from-async-pool")
        .thenDelay(Duration.ofMillis(500))
        .thenApplySync(value -> value + "-on-global")
        .timeout(Duration.ofSeconds(5))
        .thenAcceptAsync(value -> sender.sendMessage(value))
        .exceptionally(error -> {
            sender.sendMessage("failed: " + error);
            return null;
        });
```
