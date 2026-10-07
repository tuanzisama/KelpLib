package ink.tuanzi.kelpLib.core.text;

import ink.tuanzi.kelpLib.api.text.LocaleResolver;
import ink.tuanzi.kelpLib.api.text.Messages;
import ink.tuanzi.kelpLib.api.text.Mini;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Messages 默认实现：YAML 语言文件（键即消息 id、值支持 MiniMessage）、
 * per-player locale、三级回退（目标语言 → 默认语言 → 键本身）、开发模式缺失告警（§6.7）。
 */
public final class MessagesImpl implements Messages {

    private static final Logger LOGGER = Logger.getLogger("KelpLib");

    private final Path dataDirectory;
    private final List<BundleRef> bundleRefs;
    private final Locale defaultLocale;
    private final LocaleResolver resolver;
    private final boolean fallbackToKey;
    private final boolean devMode;
    private final Map<Locale, Map<String, String>> bundles = new ConcurrentHashMap<>();

    record BundleRef(Locale locale, Path file) {
    }

    MessagesImpl(Path dataDirectory, List<BundleRef> bundleRefs, Locale defaultLocale,
                 LocaleResolver resolver, boolean fallbackToKey, boolean devMode) {
        this.dataDirectory = dataDirectory;
        this.bundleRefs = List.copyOf(bundleRefs);
        this.defaultLocale = defaultLocale;
        this.resolver = resolver;
        this.fallbackToKey = fallbackToKey;
        this.devMode = devMode;
        reload();
    }

    @Override
    public void send(Audience audience, String key, TagResolver... placeholders) {
        audience.sendMessage(render(key, localeOf(audience), placeholders));
    }

    @Override
    public Component render(String key, Locale locale, TagResolver... placeholders) {
        return Mini.render(raw(key, locale), placeholders);
    }

    @Override
    public String raw(String key, Locale locale) {
        Locale target = locale == null ? defaultLocale : locale;
        String value = bundles.getOrDefault(target, Map.of()).get(key);
        if (value != null) {
            return value;
        }
        value = bundles.getOrDefault(defaultLocale, Map.of()).get(key);
        if (value != null) {
            return value;
        }
        if (devMode) {
            LOGGER.warning("[KelpLib][i18n] Missing message key: " + key + " (locale=" + target + ")");
        }
        return fallbackToKey ? key : key;
    }

    @Override
    public Locale localeOf(Audience audience) {
        Locale resolved = resolver.resolve(audience);
        return resolved == null ? defaultLocale : resolved;
    }

    @Override
    public void reload() {
        bundles.clear();
        for (BundleRef ref : bundleRefs) {
            Map<String, String> bundle = bundles.computeIfAbsent(ref.locale(), l -> new ConcurrentHashMap<>());
            if (!Files.isRegularFile(ref.file())) {
                continue;
            }
            try (Reader reader = Files.newBufferedReader(ref.file(), StandardCharsets.UTF_8)) {
                Object loaded = new Yaml().load(reader);
                if (loaded instanceof Map<?, ?> map) {
                    flatten("", map, bundle);
                }
            } catch (IOException | ClassCastException e) {
                LOGGER.log(Level.WARNING, "[KelpLib][i18n] Failed to load bundle " + ref.file(), e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void flatten(String prefix, Map<?, ?> source, Map<String, String> target) {
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            String key = prefix.isEmpty() ? String.valueOf(entry.getKey()) : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                flatten(key, nested, target);
            } else {
                target.put(key, value == null ? "" : String.valueOf(value));
            }
        }
    }

    /** 构建器实现（经平台注入数据目录与默认 locale 解析器）。 */
    public static final class Builder implements Messages.Builder {

        private final Path dataDirectory;
        private final LocaleResolver platformResolver;
        private final List<BundleRef> bundleRefs = new ArrayList<>();
        private Locale defaultLocale = Locale.forLanguageTag("en");
        private LocaleResolver resolver;
        private boolean fallbackToKey = true;
        private boolean devMode = false;

        public Builder(Path dataDirectory, LocaleResolver platformResolver) {
            this.dataDirectory = dataDirectory;
            this.platformResolver = platformResolver;
        }

        @Override
        public Messages.Builder defaultLocale(String localeTag) {
            this.defaultLocale = Locale.forLanguageTag(localeTag.replace('_', '-'));
            return this;
        }

        @Override
        public Messages.Builder localeResolver(LocaleResolver resolver) {
            this.resolver = resolver;
            return this;
        }

        @Override
        public Messages.Builder bundle(String fileName) {
            Locale locale = localeFromFileName(fileName, defaultLocale);
            bundleRefs.add(new BundleRef(locale, dataDirectory.resolve(fileName)));
            return this;
        }

        @Override
        public Messages.Builder fallbackToKey(boolean fallbackToKey) {
            this.fallbackToKey = fallbackToKey;
            return this;
        }

        @Override
        public Messages.Builder devMode(boolean devMode) {
            this.devMode = devMode;
            return this;
        }

        @Override
        public Messages build() {
            List<BundleRef> refs = new ArrayList<>(bundleRefs);
            // 默认尝试加载 messages_<locale>.yml（默认语言）与 messages.yml
            Path defaultFile = dataDirectory.resolve("messages_" + defaultLocale.toString().toLowerCase(Locale.ROOT).replace('-', '_') + ".yml");
            Path plainFile = dataDirectory.resolve("messages.yml");
            if (!refs.stream().anyMatch(r -> r.file().equals(defaultFile)) && Files.isRegularFile(defaultFile)) {
                refs.add(new BundleRef(defaultLocale, defaultFile));
            }
            if (!refs.stream().anyMatch(r -> r.file().equals(plainFile)) && Files.isRegularFile(plainFile)) {
                refs.add(new BundleRef(defaultLocale, plainFile));
            }
            return new MessagesImpl(dataDirectory, refs, defaultLocale,
                    resolver != null ? resolver : platformResolver, fallbackToKey, devMode);
        }

        /** 从文件名尾段解析语言（messages_zh_cn.yml → zh_cn）。 */
        static Locale localeFromFileName(String fileName, Locale fallback) {
            String base = fileName.endsWith(".yml") || fileName.endsWith(".yaml")
                    ? fileName.substring(0, fileName.lastIndexOf('.'))
                    : fileName;
            int idx = base.lastIndexOf('_');
            if (idx <= 0) {
                return fallback;
            }
            String tag = base.substring(idx + 1);
            return tag.matches("[a-zA-Z]{2}(_[a-zA-Z0-9]+)*") ? Locale.forLanguageTag(tag.replace('_', '-')) : fallback;
        }
    }
}
