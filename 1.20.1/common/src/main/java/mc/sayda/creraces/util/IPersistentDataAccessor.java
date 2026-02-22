package mc.sayda.creraces.util;

import net.minecraft.nbt.CompoundTag;

/**
 * Interface that will be mixed into Entity to provide cross-platform
 * persistent data access.
 */
public interface IPersistentDataAccessor {
    CompoundTag creraces$getPersistentData();
}
