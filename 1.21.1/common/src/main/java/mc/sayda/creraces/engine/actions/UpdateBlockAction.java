package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonElement;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Changes block state properties and/or block entity data of an existing block, loading its chunk if asked. */
@SuppressWarnings("null")
public class UpdateBlockAction implements ActionRegistry.RaceAction {
    private final boolean useTarget;
    private final boolean useTargetBlock;
    private final ScalingValue offsetX;
    private final ScalingValue offsetY;
    private final ScalingValue offsetZ;
    private final boolean absolute;
    private final ScalingValue.MathOp coordinateMath;
    private final boolean loadChunk;
    private final List<BlockDataEdit> dataEdits;
    private final Map<String, String> stateChanges;

    private UpdateBlockAction(boolean useTarget, boolean useTargetBlock, ScalingValue offsetX, ScalingValue offsetY,
            ScalingValue offsetZ, boolean absolute, ScalingValue.MathOp coordinateMath, boolean loadChunk,
            List<BlockDataEdit> dataEdits, Map<String, String> stateChanges) {
        this.useTarget = useTarget;
        this.useTargetBlock = useTargetBlock;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.absolute = absolute;
        this.coordinateMath = coordinateMath;
        this.loadChunk = loadChunk;
        this.dataEdits = dataEdits;
        this.stateChanges = stateChanges;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }

        BlockPos pos = BlockTargeting.resolveAnchor(player, target, interactPos, absolute, useTarget, useTargetBlock,
                coordinateMath).offset(
                        (int) offsetX.evaluate(player, target, slot),
                        (int) offsetY.evaluate(player, target, slot),
                        (int) offsetZ.evaluate(player, target, slot));

        ChunkPos chunkPos = new ChunkPos(pos);
        if (!level.getChunkSource().hasChunk(chunkPos.x, chunkPos.z)) {
            if (!loadChunk) {
                return false;
            }
            level.getChunk(chunkPos.x, chunkPos.z);
        }

        BlockState oldState = level.getBlockState(pos);
        BlockState newState = oldState;
        for (Map.Entry<String, String> change : stateChanges.entrySet()) {
            newState = withProperty(newState, change.getKey(), change.getValue());
        }
        if (newState != oldState) {
            level.setBlock(pos, newState, Block.UPDATE_ALL);
        }

        if (!dataEdits.isEmpty()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null
                    && BlockDataEdit.applyAll(dataEdits, blockEntity, player, target, slot, interactPos)) {
                level.sendBlockUpdated(pos, oldState, newState, Block.UPDATE_ALL);
            }
        }
        return true;
    }

    /** Sets the named property from its string form; unknown properties or unparsable values leave the state as is. */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static BlockState withProperty(BlockState state, String name, String value) {
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals(name)) {
                Optional<? extends Comparable<?>> parsed = ((Property) property).getValue(value);
                if (parsed.isPresent()) {
                    return state.setValue((Property) property, (Comparable) parsed.get());
                }
            }
        }
        return state;
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "update_block"), json -> {
            Map<String, String> stateChanges = new HashMap<>();
            if (json.has("state") && json.get("state").isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("state").entrySet()) {
                    stateChanges.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
            return new UpdateBlockAction(
                    GsonHelper.getAsBoolean(json, "use_target", false),
                    GsonHelper.getAsBoolean(json, "use_target_block", false),
                    ScalingValue.fromJson(json, "offset_x", 0.0),
                    ScalingValue.fromJson(json, "offset_y", 0.0),
                    ScalingValue.fromJson(json, "offset_z", 0.0),
                    GsonHelper.getAsBoolean(json, "absolute", false),
                    BlockTargeting.parseMathOp(json, "UpdateBlockAction"),
                    GsonHelper.getAsBoolean(json, "load_chunk", true),
                    BlockDataEdit.parseList(json),
                    stateChanges);
        });
    }
}
