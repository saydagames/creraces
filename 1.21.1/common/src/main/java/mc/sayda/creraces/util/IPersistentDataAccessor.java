package mc.sayda.creraces.util;

import net.minecraft.nbt.CompoundTag;

/** Mixed into Entity so common code gets the same persistent data tag on every loader. */
public interface IPersistentDataAccessor {
    CompoundTag creraces$getPersistentData();
}
