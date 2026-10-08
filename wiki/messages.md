# Messages

多语言消息，`ink.tuanzi.kelpLib.api.text.Messages`。语言文件沿用 YAML，键即消息 id，值支持 MiniMessage；per-player locale；缺失键回退默认语言，再回退键本身；开发模式打印缺失键告警。

## 构建

```java
Messages messages = Messages.create(plugin)
        .defaultLocale("zh_cn")
        .bundle("messages.yml")
        .devMode(true)
        .build();
```

| 构建器方法 | 说明 |
|------|------|
| `defaultLocale(String)` | 默认语言标签，默认 `en` |
| `localeResolver(LocaleResolver)` | 覆盖 locale 解析器；缺省用平台默认（Bukkit `Player#locale()`，Velocity `PlayerSettings#getLocale()`）|
| `bundle(String)` | 追加语言文件（相对数据目录）；缺省尝试 `messages_<locale>.yml`；同一语言多次调用合并，后者优先 |
| `fallbackToKey(boolean)` | 缺失键是否回退到键本身，默认 true |
| `devMode(boolean)` | 缺失键打印告警，默认关闭 |

## 发送与渲染

```java
messages.send(player, "welcome", Placeholder.unparsed("player", player.getName()));
```

| 方法 | 说明 |
|------|------|
| `send(Audience, key, placeholders...)` | 按受众语言渲染并发送 |
| `render(key, Locale, placeholders...)` | 渲染为组件（不发送）；locale 为 null 走默认语言 |
| `raw(key, Locale)` | 取 MiniMessage 原文，缺失时按回退链取键本身 |
| `localeOf(Audience)` | 解析受众语言，未解析出返回默认语言 |
| `reload()` | 重载全部语言文件 |

占位符为标准 MiniMessage `TagResolver`（`net.kyori.adventure.text.minimessage.tag.resolver`）。

## 语言文件示例

`messages_zh_cn.yml`：

```yaml
welcome: "<green>欢迎回来，<player>！</green>"
home-set: "家 <name> 已设置（上限 <max>）。"
```

`messages_en.yml`：

```yaml
welcome: "<green>Welcome back, <player>!</green>"
home-set: "Home <name> set (limit <max>)."
```

## Mini

`ink.tuanzi.kelpLib.api.text.Mini` 为 MiniMessage 渲染便捷入口：

```java
Component c = Mini.render("<gold>标题</gold>", Placeholder.unparsed("name", value));
String s = Mini.serialize(component);
```
