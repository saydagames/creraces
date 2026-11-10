package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.DataValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The "data" and "state" keys shared by place_block and update_block. "data" is a list of
 * { "key": ..., "value": data value } (or "operation": "remove") written to the block entity's
 * saved data; "state" sets block state properties, e.g. { "facing": "north" }.
 */
@SuppressWarnings("null")
record BlockDataEdit(String key, @Nullable DataValue value) {

    static List<BlockDataEdit> parseList(JsonObject json) {
        List<BlockDataEdit> edits = new ArrayList<>();
        if (!json.has("data")) {
            return edits;
        }
        for (JsonElement element : json.getAsJsonArray("data")) {
            JsonObject entry = element.getAsJsonObject();
            String key = GsonHelper.getAsString(entry, "key");
            String operation = GsonHelper.getAsString(entry, "operation", "set");
            if (operation.equals("remove")) {
                edits.add(new BlockDataEdit(key, null));
            } else if (operation.equals("set")) {
                DataValue value = DataValue.fromJson(entry, "value", DataValue.DataType.DOUBLE);
                if (value == null) {
                    throw new IllegalArgumentException("block data '" + key + "' needs a \"value\"");
                }
                edits.add(new BlockDataEdit(key, value));
            } else {
                throw new IllegalArgumentException("unknown operation '" + operation + "' (set, remove)");
            }
        }
        return edits;
    }

    static Map<String, String> parseState(JsonObject json) {
        Map<String, String> changes = new LinkedHashMap<>();
        if (json.has("state") && json.get("state").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("state").entrySet()) {
                changes.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
        return changes;
    }

    /** Sets the named properties from their string form; unknown properties or values are skipped. */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    static BlockState withProperties(BlockState state, Map<String, String> changes) {
        for (Map.Entry<String, String> change : changes.entrySet()) {
            for (Property<?> property : state.getProperties()) {
                if (property.getName().equals(change.getKey())) {
                    Optional<? extends Comparable<?>> parsed = ((Property) property).getValue(change.getValue());
                    if (parsed.isPresent()) {
                        state = state.setValue((Property) property, (Comparable) parsed.get());
                    }
                }
            }
        }
        return state;
    }

    /**
     * Applies the edits to the block entity's saved data and reloads it if anything changed.
     * Returns whether it changed, so the caller knows to send a block update.
     */
    static boolean applyAll(List<BlockDataEdit> edits, BlockEntity blockEntity, Player player,
            @Nullable LivingEntity target, @Nullable AbilitySlot slot, @Nullable BlockPos interactPos) {
        HolderLookup.Provider registries = player.level().registryAccess();
        CompoundTag tag = blockEntity.saveWithFullMetadata(registries);
        boolean changed = false;
        for (BlockDataEdit edit : edits) {
            if (edit.value() == null) {
                changed |= tag.contains(edit.key());
                tag.remove(edit.key());
            } else {
                changed |= edit.value().writeInto(tag, edit.key(), player, target, slot, interactPos);
            }
        }
        if (changed) {
            blockEntity.loadWithComponents(tag, registries);
            blockEntity.setChanged();
        }
        return changed;
    }
}
