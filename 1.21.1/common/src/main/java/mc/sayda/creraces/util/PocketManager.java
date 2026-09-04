package mc.sayda.creraces.util;

import mc.sayda.creraces.CreRaces;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages the global registry of pockets.
 */
public class PocketManager {
    private static final String DATA_ID = "creraces_pockets";

    private static final AtomicInteger NEXT_POCKET_INDEX = new AtomicInteger(1);
    private static volatile MinecraftServer currentServer = null;
    /** Holds no state of its own, NEXT_POCKET_INDEX above is the source of truth; this is just the SavedData handle to mark dirty. */
    private static Data currentData;

    private static class Data extends SavedData {
        @Override
        public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
            tag.putInt("next_pocket_index", NEXT_POCKET_INDEX.get());
            return tag;
        }
    }

    public static int getNextIndex() {
        int index = NEXT_POCKET_INDEX.getAndIncrement();
        if (currentData != null) currentData.setDirty();
        MinecraftServer srv = currentServer;
        if (srv != null) {
            save(srv);
        }
        return index;
    }

    public static void onServerStop() {
        currentServer = null;
    }

    /** Forces an immediate flush (in addition to vanilla's own periodic autosave, since getNextIndex() marks this dirty). */
    public static void save(MinecraftServer server) {
        if (currentData != null) {
            currentData.setDirty();
            server.overworld().getDataStorage().save();
        }
    }

    public static void load(MinecraftServer server) {
        currentServer = server;
        ServerLevel overworld = server.overworld();
        SavedData.Factory<Data> factory = new SavedData.Factory<>(
                Data::new,
                (tag, registries) -> {
                    NEXT_POCKET_INDEX.set(tag.getInt("next_pocket_index"));
                    return new Data();
                },
                net.minecraft.util.datafix.DataFixTypes.LEVEL);
        currentData = overworld.getDataStorage().computeIfAbsent(factory, DATA_ID);
        CreRaces.LOGGER.info("Loaded pocket registry (next index: {}).", NEXT_POCKET_INDEX.get());
    }
}
