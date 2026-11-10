package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.Position;
import mc.sayda.creraces.registry.ModGameRules;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Changes the block already at "position": its "state" properties and block entity "data" (see
 * BlockDataEdit). An unloaded chunk is loaded for it unless "load_chunk" is false.
 */
@SuppressWarnings("null")
public class UpdateBlockAction implements ActionRegistry.RaceAction {
    private final Position position;
    private final boolean loadChunk;
    private final Map<String, String> stateChanges;
    private final List<BlockDataEdit> dataEdits;

    private UpdateBlockAction(Position position, boolean loadChunk, Map<String, String> stateChanges,
            List<BlockDataEdit> dataEdits) {
        this.position = position;
        this.loadChunk = loadChunk;
        this.stateChanges = stateChanges;
        this.dataEdits = dataEdits;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (!(player.level() instanceof ServerLevel level)
                || !level.getGameRules().getBoolean(ModGameRules.RULE_RACEGRIEFING)) {
            return false;
        }
        BlockPos pos = position.resolveBlock(player, target, slot, interactPos);
        if (pos == null) {
            return false;
        }
        ChunkPos chunkPos = new ChunkPos(pos);
        if (!level.getChunkSource().hasChunk(chunkPos.x, chunkPos.z)) {
            if (!loadChunk) {
                return false;
            }
            level.getChunk(chunkPos.x, chunkPos.z);
        }

        BlockState oldState = level.getBlockState(pos);
        BlockState newState = BlockDataEdit.withProperties(oldState, stateChanges);
        if (newState != oldState) {
            level.setBlock(pos, newState, Block.UPDATE_ALL);
        }
        if (!dataEdits.isEmpty()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null && BlockDataEdit.applyAll(dataEdits, blockEntity, player, target, slot, interactPos)) {
                level.sendBlockUpdated(pos, oldState, newState, Block.UPDATE_ALL);
            }
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(CreRaces.id("update_block"), json -> new UpdateBlockAction(
                Position.fromJson(json, "position", Position.of(Position.Source.SELF)),
                GsonHelper.getAsBoolean(json, "load_chunk", true),
                BlockDataEdit.parseState(json),
                BlockDataEdit.parseList(json)));
    }
}
