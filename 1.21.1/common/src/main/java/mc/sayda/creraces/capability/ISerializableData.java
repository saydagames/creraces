package mc.sayda.creraces.capability;

import net.minecraft.nbt.CompoundTag;

/**
 * Shared interface for all data types that can be serialized to/from NBT.
 */
public interface ISerializableData {
    CompoundTag serialize();

    /** Serialize with an option to include resource values (mana, rage, etc.). */
    default CompoundTag serialize(boolean fullSync) {
        return serialize();
    }

    void deserialize(CompoundTag tag);
}
