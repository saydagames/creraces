package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import mc.sayda.creraces.CreRaces;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.level.ServerLevel;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class UpdateBlockAction implements ActionRegistry.RaceAction {
    private final boolean useTarget;
    private final boolean useTargetBlock;
    private final ScalingValue offsetX;
    private final ScalingValue offsetY;
    private final ScalingValue offsetZ;
    private final boolean absolute;
    private final ScalingValue.MathOp coordinateMath;
    private final boolean loadChunk;
    private final List<DataModification> dataModifications;
    private final Map<String, String> stateModifications;

    private static class DataModification {
        final String key;
        final ScalingValue value;
        final String mode;

        DataModification(String key, ScalingValue value, String mode) {
            this.key = key;
            this.value = value;
            this.mode = mode != null ? mode.toUpperCase() : "SET";
        }
    }

    public UpdateBlockAction(boolean useTarget, boolean useTargetBlock, ScalingValue offsetX,
            ScalingValue offsetY, ScalingValue offsetZ, boolean absolute,
            ScalingValue.MathOp coordinateMath, boolean loadChunk,
            List<DataModification> dataModifications, Map<String, String> stateModifications) {
        this.useTarget = useTarget;
        this.useTargetBlock = useTargetBlock;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.absolute = absolute;
        this.coordinateMath = coordinateMath != null ? coordinateMath : ScalingValue.MathOp.FLOOR;
        this.loadChunk = loadChunk;
        this.dataModifications = dataModifications;
        this.stateModifications = stateModifications;
    }

    @Override
    public boolean execute(@javax.annotation.Nonnull Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {

        if (player.level().isClientSide())
            return false;
        ServerLevel level = (ServerLevel) player.level();

        BlockPos targetPos;
        if (absolute) {
            targetPos = BlockPos.ZERO;
        } else if (useTarget && target != null) {
            targetPos = target.blockPosition();
        } else if (useTargetBlock && interact_pos != null) {
            targetPos = interact_pos;
        } else {
            double tx = player.getX();
            double ty = player.getY();
            double tz = player.getZ();

            int x = (int) Math.floor(tx);
            int y = (int) Math.floor(ty);
            int z = (int) Math.floor(tz);

            if (coordinateMath == ScalingValue.MathOp.ROUND) {
                x = (int) Math.round(tx);
                y = (int) Math.round(ty);
                z = (int) Math.round(tz);
            } else if (coordinateMath == ScalingValue.MathOp.CEIL) {
                x = (int) Math.ceil(tx);
                y = (int) Math.ceil(ty);
                z = (int) Math.ceil(tz);
            }
            targetPos = new BlockPos(x, y, z);
        }

        @javax.annotation.Nonnull
        BlockPos finalPos = targetPos.offset((int) offsetX.evaluate(player, target, slot),
                (int) offsetY.evaluate(player, target, slot), (int) offsetZ.evaluate(player, target, slot));

        // Chunk Loading Support
        ChunkPos chunkPos = new ChunkPos(finalPos);
        if (!level.getChunkSource().hasChunk(chunkPos.x, chunkPos.z)) {
            if (loadChunk) {
                level.getChunk(chunkPos.x, chunkPos.z);
            } else {
                return false;
            }
        }

        @javax.annotation.Nonnull
        BlockState oldState = level.getBlockState(finalPos);
        @javax.annotation.Nonnull
        BlockState newState = oldState;

        // Apply State Modifications
        if (!stateModifications.isEmpty()) {
            for (Map.Entry<String, String> entry : stateModifications.entrySet()) {
                String key = entry.getKey();
                String val = entry.getValue();
                if (key != null && val != null) {
                    newState = applyProperty(newState, key, val);
                }
            }
        }

        if (newState != oldState) {
            level.setBlock(finalPos, newState, 3);
        }

        // Apply Data Modifications
        if (!dataModifications.isEmpty()) {
            BlockEntity be = level.getBlockEntity(finalPos);
            if (be != null) {
                CompoundTag tag = be.saveWithFullMetadata();
                boolean changed = false;
                for (DataModification mod : dataModifications) {
                    if ("owner".equalsIgnoreCase(mod.key)) {
                        tag.putUUID("owner", player.getUUID());
                        changed = true;
                    } else if ("REMOVE".equals(mod.mode)) {
                        if (tag.contains(mod.key)) {
                            tag.remove(mod.key);
                            changed = true;
                        }
                    } else {
                        double val = mod.value.evaluate(player, target, slot, interact_pos);
                        if (val == (long) val) {
                            tag.putInt(mod.key, (int) val);
                        } else {
                            tag.putDouble(mod.key, val);
                        }
                        changed = true;
                    }
                }
                if (changed) {
                    be.load(tag);
                    be.setChanged();
                    level.sendBlockUpdated(finalPos, oldState, newState, 3);
                }
            }
        }

        return true;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private @javax.annotation.Nonnull BlockState applyProperty(@javax.annotation.Nonnull BlockState state,
            @javax.annotation.Nonnull String name, @javax.annotation.Nonnull String value) {
        for (Property<?> prop : state.getProperties()) {
            if (prop.getName().equals(name)) {
                java.util.Optional<? extends Comparable<?>> optValue = ((Property) prop).getValue(value);
                if (optValue.isPresent()) {
                    return state.setValue((Property) prop, (Comparable) optValue.get());
                }
            }
        }
        return state;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "update_block"), json -> {
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
            boolean useTargetBlock = GsonHelper.getAsBoolean(json, "use_target_block", false);
            ScalingValue offsetX = ScalingValue.fromJson(json, "offset_x", 0.0);
            ScalingValue offsetY = ScalingValue.fromJson(json, "offset_y", 0.0);
            ScalingValue offsetZ = ScalingValue.fromJson(json, "offset_z", 0.0);
            boolean absolute = GsonHelper.getAsBoolean(json, "absolute", false);
            boolean loadChunk = GsonHelper.getAsBoolean(json, "load_chunk", true);

            ScalingValue.MathOp coordinateMath = ScalingValue.MathOp.FLOOR;
            if (json.has("math")) {
                try {
                    coordinateMath = ScalingValue.MathOp.valueOf(json.get("math").getAsString().toUpperCase());
                } catch (Exception e) {
                }
            }

            List<DataModification> dataMods = new ArrayList<>();
            if (json.has("data") && json.get("data").isJsonArray()) {
                JsonArray arr = json.getAsJsonArray("data");
                for (JsonElement el : arr) {
                    if (el.isJsonObject()) {
                        JsonObject obj = el.getAsJsonObject();
                        String key = GsonHelper.getAsString(obj, "key", "");
                        ScalingValue val = ScalingValue.fromJson(obj, "value", 0.0);
                        String mode = GsonHelper.getAsString(obj, "mode", "SET");
                        if (!key.isEmpty())
                            dataMods.add(new DataModification(key, val, mode));
                    }
                }
            }

            Map<String, String> stateMods = new HashMap<>();
            if (json.has("state") && json.get("state").isJsonObject()) {
                JsonObject obj = json.getAsJsonObject("state");
                for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                    stateMods.put(entry.getKey(), entry.getValue().getAsString());
                }
            }

            return new UpdateBlockAction(useTarget, useTargetBlock, offsetX, offsetY, offsetZ, absolute,
                    coordinateMath, loadChunk, dataMods, stateMods);
        });
    }
}
