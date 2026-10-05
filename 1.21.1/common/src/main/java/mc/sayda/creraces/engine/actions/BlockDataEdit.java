package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** One entry of the "data" array of place_block and update_block: sets or removes a key in a block entity's NBT. */
@SuppressWarnings("null")
record BlockDataEdit(String key, ScalingValue value, boolean remove) {
    /** Reserved key that always receives the caster's UUID instead of an evaluated value. */
    private static final String OWNER_KEY = "owner";

    static List<BlockDataEdit> parseList(JsonObject json) {
        List<BlockDataEdit> edits = new ArrayList<>();
        if (!json.has("data") || !json.get("data").isJsonArray()) {
            return edits;
        }
        for (JsonElement element : json.getAsJsonArray("data")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject entry = element.getAsJsonObject();
            String key = GsonHelper.getAsString(entry, "key", "");
            if (!key.isEmpty()) {
                boolean remove = "REMOVE".equalsIgnoreCase(GsonHelper.getAsString(entry, "mode", "SET"));
                edits.add(new BlockDataEdit(key, ScalingValue.fromJson(entry, "value", 0.0), remove));
            }
        }
        return edits;
    }

    /**
     * Applies the edits to the block entity's saved NBT and reloads it if anything changed.
     * Returns whether it changed, so the caller knows to send a block update.
     */
    static boolean applyAll(List<BlockDataEdit> edits, BlockEntity blockEntity, Player player,
            @Nullable LivingEntity target, @Nullable AbilitySlot slot, @Nullable BlockPos interactPos) {
        HolderLookup.Provider registries = player.level().registryAccess();
        CompoundTag tag = blockEntity.saveWithFullMetadata(registries);
        boolean changed = false;
        for (BlockDataEdit edit : edits) {
            changed |= edit.applyTo(tag, player, target, slot, interactPos);
        }
        if (changed) {
            blockEntity.loadWithComponents(tag, registries);
            blockEntity.setChanged();
        }
        return changed;
    }

    private boolean applyTo(CompoundTag tag, Player player, @Nullable LivingEntity target,
            @Nullable AbilitySlot slot, @Nullable BlockPos interactPos) {
        if (OWNER_KEY.equalsIgnoreCase(key)) {
            tag.putUUID(OWNER_KEY, player.getUUID());
            return true;
        }
        if (remove) {
            if (!tag.contains(key)) {
                return false;
            }
            tag.remove(key);
            return true;
        }
        double evaluated = value.evaluate(player, target, slot, interactPos);
        if (evaluated == (long) evaluated) {
            tag.putInt(key, (int) evaluated);
        } else {
            tag.putDouble(key, evaluated);
        }
        return true;
    }
}
