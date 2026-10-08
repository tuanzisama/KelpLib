# Messenger

跨服消息通道，入口 `Kelp.messenger()`（`ink.tuanzi.kelpLib.api.messenger.Messenger`）。三层通道分层，传输可插拔，后端服与代理端同一套 API。

信任边界：Messenger 假定运行于可信内网（服务器家族自控网络），不提供端到端加密与鉴权。

## 通道三层

| 层 | 工厂方法 | 语义 |
|----|----------|------|
| Channel | `getChannel(name, type)` | fire-and-forget 广播，无回执 |
| ConversationChannel | `getConversationChannel(name, type, replyType)` | 消息携带会话 ID，回复可路由回发起方 |
| ReqRespChannel | `getReqRespChannel(name, reqType, replyType)` | Promise 化 RPC，超时自动异常完成 |

频道名自动加 `kelplib:` 前缀，同名重复获取返回同一实例。消息为 record / POJO，编解码复用配置序列化器（见 [Config](config.md)）。

## Channel（广播）

```java
Channel<HomeUpdate> channel = Kelp.messenger().getChannel("home-update", HomeUpdate.class);

channel.sendMessage(new HomeUpdate(playerId, "spawn"));   // 广播到全部订阅端

MessageAgent<HomeUpdate> agent = channel.newAgent();      // Terminable：close 即退订
agent.addListener((a, message) -> applyUpdate(message));
agent.bindWith(lifecycle);
```

## ConversationChannel（会话）

```java
ConversationChannel<TeleportAsk, TeleportAnswer, String> channel =
        Kelp.messenger().getConversationChannel("tp-ask", TeleportAsk.class, TeleportAnswer.class);

// 应答方
ConversationChannel.ConversationAgent<TeleportAsk, TeleportAnswer, String> agent = channel.newAgent();
agent.addListener((a, message) -> a.reply(message, doTeleport(message.message())));
agent.bindWith(lifecycle);

// 发起方：同一频道开 agent 监听回复，sendMessage 时带上会话 ID
ConversationChannel.ConversationAgent<TeleportAsk, TeleportAnswer, String> requester = channel.newAgent();
requester.addReplyListener((a, reply) -> handleAnswer(reply.message()));
requester.bindWith(lifecycle);
requester... // channel.sendMessage(msg, UUID.randomUUID().toString())
```

`ConversationMessage<M, R, C>` 含 `message`（请求正文）、`reply`（回复正文，非回复帧为 null）、`conversationId`、`isReply`。

## ReqRespChannel（RPC）

```java
ReqRespChannel<TeleportReq, Boolean> rpc =
        Kelp.messenger().getReqRespChannel("tp-req", TeleportReq.class, Boolean.class);

rpc.responseHandler(req -> doTeleport(req));                          // 服务端注册处理器

CompletableFuture<Boolean> ok = rpc.request(req, Duration.ofSeconds(5));  // 客户端发起
ok.thenAccept(success -> ...);                                        // 超时后 future 以 TimeoutException 异常完成
```

处理器应为同步快速逻辑；耗时逻辑内部转异步。

## 传输

传输 SPI（`Transport`）只负责「频道名 → 字节载荷」的广播与订阅；编解码与 agent 生命周期由通道层负责。

| id | 实现 | 说明 |
|----|------|------|
| `plugin-messaging` | 内置，启动时自动注册 | Bukkit↔Velocity 直连通道，Minecraft 插件消息 `kelplib:main` 复用承载全部 KelpLib 频道 |
| `redis` | `ink.tuanzi.kelpLib.core.messenger.RedisTransport` | Lettuce Pub/Sub，双端可用，后端↔代理端共享数据的主载体；需显式注册 |

注册 Redis 传输（依赖 `kelpLib-core`，双端均可）：

```java
Kelp.messenger().registerTransport(new RedisTransport(RedisSpec.of("redis://localhost:6379/0")));
```

同 id 重复注册覆盖旧实例。**同时注册多个传输会导致消息重复投递，生产环境只注册一种。**

## 线程

传输回调发生在传输线程（Lettuce 线程 / 网络线程），通道层统一切回平台异步执行器后再回调 agent 监听器；监听器内不要做阻塞操作。
