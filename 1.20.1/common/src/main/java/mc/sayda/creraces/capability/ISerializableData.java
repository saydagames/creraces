package mc.sayda.creraces.capability;

import net.minecraft.nbt.CompoundTag;

/**
 * Shared interface for all data types that can be serialized to/from NBT.
 */
public interface ISerializableData {
    CompoundTag serialize();

    void deserialize(CompoundTag tag);
}
