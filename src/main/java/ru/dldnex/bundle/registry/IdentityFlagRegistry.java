package ru.dldnex.bundle.registry;

import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Registry for Identity Hash Code ↔ Bundle Flush Flag relationship.
 */
public class IdentityFlagRegistry {
    private final Int2BooleanOpenHashMap store = new Int2BooleanOpenHashMap();

    /**
     * Pull a flag value from the registry.
     * @param holder Flag holder.
     * @return Flag value if an object is holding any, else null.
     * @see IdentityFlagRegistry#putFlag(Object, Boolean)
     */
    public @Nullable Boolean pullFlag(@NotNull Object holder) {
        int hashCode = System.identityHashCode(holder);
        if (store.containsKey(hashCode)) {
            return store.remove(hashCode);
        }
        return null;
    }

    /**
     * Put a flag value to the registry.
     * @param holder Flag holder.
     * @param flag   Flag value.
     * @see IdentityFlagRegistry#pullFlag(Object)
     */
    public void putFlag(@NotNull Object holder, @Nullable Boolean flag) {
        if (flag == null) {
            return;
        }
        store.put(System.identityHashCode(holder), flag.booleanValue());
    }
}
