package mc.sayda.creraces.util;

import mc.sayda.creraces.CreRaces;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.concurrent.atomic.AtomicInteger;

/** Hands out pocket indices and persists the next free one in the overworld's saved data. */
public class PocketManager {
    private static final String DATA_ID = "creraces_pockets";

    private static final AtomicInteger NEXT_POCKET_INDEX = new AtomicInteger(1);
    private static volatile MinecraftServer currentServer = null;
    /** Only the SavedData handle to mark dirty; NEXT_POCKET_INDEX is the real state. */
    private static Data currentData;

    private static class Data extends SavedData {
        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
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
        // No datafixer: this is mod data, not level.dat. NeoForge and Fabric API both accept null here.
        SavedData.Factory<Data> factory = new SavedData.Factory<>(
                Data::new,
                (tag, registries) -> {
                    NEXT_POCKET_INDEX.set(tag.getInt("next_pocket_index"));
                    return new Data();
                },
                null);
        currentData = overworld.getDataStorage().computeIfAbsent(factory, DATA_ID);
        CreRaces.LOGGER.info("Loaded pocket registry (next index: {}).", NEXT_POCKET_INDEX.get());
    }
}
