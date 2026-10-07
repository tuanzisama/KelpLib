package org.bukkit.persistence;

/**
 * Minimal compile-time stub of org.bukkit.persistence.PersistentDataType.
 */
public interface PersistentDataType<T, Z> {

    PersistentDataType<Byte, Byte> BYTE = new Simple<>("byte");
    PersistentDataType<Short, Short> SHORT = new Simple<>("short");
    PersistentDataType<Integer, Integer> INTEGER = new Simple<>("integer");
    PersistentDataType<Long, Long> LONG = new Simple<>("long");
    PersistentDataType<Float, Float> FLOAT = new Simple<>("float");
    PersistentDataType<Double, Double> DOUBLE = new Simple<>("double");
    PersistentDataType<Boolean, Boolean> BOOLEAN = new Simple<>("boolean");
    PersistentDataType<String, String> STRING = new Simple<>("string");

    @SuppressWarnings("ClassNamingConvention")
    final class Simple<Z> implements PersistentDataType<Z, Z> {
        private final String name;

        private Simple(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "PersistentDataType." + name;
        }
    }
}
