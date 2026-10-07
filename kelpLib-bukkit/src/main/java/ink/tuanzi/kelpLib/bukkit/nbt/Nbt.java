package ink.tuanzi.kelpLib.bukkit.nbt;

import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * NBT 服务（§6.4，仅 Bukkit 侧）：封装 tr7zw/ItemNBTAPI 为可选依赖。
 *
 * <p>启动时自动检测（优先检测本插件 relocate 捆绑副本，其次检测服务器上的原始坐标）；
 * 不可用时 {@link #available()} 返回 {@code false}，各方法抛出带说明的 {@link IllegalStateException}
 * 并引导降级到 PersistentDataContainer（PDC）。</p>
 *
 * <pre>{@code
 * ItemStack out = Nbt.edit(item, nbt -> {
 *     nbt.setString("utoverse:quest", "q_07");
 *     return nbt.toItem();
 * });
 * }</pre>
 */
public final class Nbt {

    private static final Logger LOGGER = Logger.getLogger("KelpLib");
    private static final String[] NBT_ITEM_CLASSES = {
            "ink.tuanzi.kelpLib.libs.nbtapi.NBTItem",      // 本插件 relocate 捆绑副本（§8）
            "de.tr7zw.changeme.nbtapi.NBTItem"             // 服务器上独立安装
    };

    private static volatile Resolver resolver;
    private static volatile boolean resolved;

    private Nbt() {
    }

    /** ItemNBTAPI 是否可用。 */
    public static boolean available() {
        return resolve() != null;
    }

    /** 编辑物品 NBT：函数内经 {@link Handle} 读写，返回新物品（原物品不变）。 */
    public static ItemStack edit(ItemStack item, Function<Handle, ItemStack> editor) {
        Resolver r = requireResolver();
        try {
            Object nbt = r.constructor.newInstance(item, false);
            return editor.apply(new Handle(nbt, r));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("NBT edit failed: " + e.getMessage(), e);
        }
    }

    /** 读取字符串 tag；缺失返回 null。 */
    public static String getString(ItemStack item, String key) {
        Resolver r = requireResolver();
        try {
            Object nbt = r.readConstructor.newInstance(item, true);
            Object value = r.getString.invoke(nbt, key);
            return value == null ? null : value.toString();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("NBT read failed: " + e.getMessage(), e);
        }
    }

    /** 写入字符串 tag。 */
    public static ItemStack setString(ItemStack item, String key, String value) {
        return edit(item, handle -> {
            handle.setString(key, value);
            return handle.toItem();
        });
    }

    /** 是否存在指定 tag。 */
    public static boolean has(ItemStack item, String key) {
        Resolver r = requireResolver();
        try {
            Object nbt = r.readConstructor.newInstance(item, true);
            return (Boolean) r.hasTag.invoke(nbt, key);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("NBT check failed: " + e.getMessage(), e);
        }
    }

    /** 将 from 的全部 NBT 合并到 to（返回新物品）。 */
    public static ItemStack copy(ItemStack from, ItemStack to) {
        Resolver r = requireResolver();
        try {
            Object source = r.readConstructor.newInstance(from, true);
            Object target = r.constructor.newInstance(to, false);
            r.mergeCompound.invoke(target, source);
            return (ItemStack) r.getItem.invoke(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("NBT copy failed: " + e.getMessage(), e);
        }
    }

    /** 编辑句柄（包装 NBTItem 实例）。 */
    public static final class Handle {

        private final Object nbtItem;
        private final Resolver resolver;

        private Handle(Object nbtItem, Resolver resolver) {
            this.nbtItem = nbtItem;
            this.resolver = resolver;
        }

        /** 写入字符串 tag。 */
        public void setString(String key, String value) {
            try {
                resolver.setString.invoke(nbtItem, key, value);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("NBT setString failed", e);
            }
        }

        /** 读取字符串 tag。 */
        public String getString(String key) {
            try {
                Object value = resolver.getString.invoke(nbtItem, key);
                return value == null ? null : value.toString();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("NBT getString failed", e);
            }
        }

        /** 是否存在 tag。 */
        public boolean hasTag(String key) {
            try {
                return (Boolean) resolver.hasTag.invoke(nbtItem, key);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("NBT hasTag failed", e);
            }
        }

        /** 移除 tag。 */
        public void removeTag(String key) {
            try {
                resolver.removeKey.invoke(nbtItem, key);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("NBT removeTag failed", e);
            }
        }

        /** 产出物品。 */
        public ItemStack toItem() {
            try {
                return (ItemStack) resolver.getItem.invoke(nbtItem);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("NBT toItem failed", e);
            }
        }
    }

    private static Resolver requireResolver() {
        Resolver r = resolve();
        if (r == null) {
            throw new IllegalStateException(
                    "ItemNBTAPI 不可用（Nbt.available() == false）。请降级使用 PersistentDataContainer（PDC）"
                            + "或安装 ItemNBTAPI（de.tr7zw:item-nbt-api）后重启。");
        }
        return r;
    }

    private static Resolver resolve() {
        if (resolved) {
            return resolver;
        }
        synchronized (Nbt.class) {
            if (resolved) {
                return resolver;
            }
            try {
                resolver = Resolver.create();
            } catch (ReflectiveOperationException | LinkageError e) {
                resolver = null;
                LOGGER.fine("ItemNBTAPI not available: " + e.getMessage());
            }
            resolved = true;
            return resolver;
        }
    }

    /** 反射方法集（延迟解析并缓存）。 */
    private static final class Resolver {

        final Constructor<?> constructor;      // NBTItem(ItemStack, boolean)
        final Constructor<?> readConstructor;  // 同上（只读）
        final Method setString;                // setString(String, String)
        final Method getString;                // getString(String)
        final Method hasTag;                   // hasTag(String)
        final Method removeKey;                // removeKey(String)
        final Method mergeCompound;            // mergeCompound(NBTCompound)
        final Method getItem;                  // getItem()

        private Resolver(Constructor<?> constructor, Constructor<?> readConstructor, Method setString,
                         Method getString, Method hasTag, Method removeKey, Method mergeCompound, Method getItem) {
            this.constructor = constructor;
            this.readConstructor = readConstructor;
            this.setString = setString;
            this.getString = getString;
            this.hasTag = hasTag;
            this.removeKey = removeKey;
            this.mergeCompound = mergeCompound;
            this.getItem = getItem;
        }

        static Resolver create() throws ReflectiveOperationException {
            ClassNotFoundException firstFailure = null;
            for (String className : NBT_ITEM_CLASSES) {
                try {
                    Class<?> nbtItem = Class.forName(className);
                    Class<?> nbtCompound = Class.forName(
                            className.substring(0, className.lastIndexOf('.') + 1) + "NBTCompound");
                    Constructor<?> constructor = nbtItem.getConstructor(ItemStack.class, boolean.class);
                    return new Resolver(
                            constructor,
                            constructor,
                            nbtItem.getMethod("setString", String.class, String.class),
                            nbtItem.getMethod("getString", String.class),
                            nbtItem.getMethod("hasTag", String.class),
                            nbtItem.getMethod("removeKey", String.class),
                            nbtItem.getMethod("mergeCompound", nbtCompound),
                            nbtItem.getMethod("getItem"));
                } catch (ClassNotFoundException e) {
                    if (firstFailure == null) {
                        firstFailure = e;
                    }
                }
            }
            throw firstFailure != null ? firstFailure
                    : new ClassNotFoundException("No ItemNBTAPI candidate found");
        }
    }
}
