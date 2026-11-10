package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.block.RootBlock;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.Position;
import mc.sayda.creraces.registry.ModGameRules;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Places "block" at "position" (the caster's feet by default), with optional "state" properties and
 * block entity "data" (see BlockDataEdit). Without "overwrite" the spot must be free, as in vanilla,
 * and placing the same block again fails so an ability doesn't charge for doing nothing.
 */
@SuppressWarnings("null")
public class PlaceBlockAction implements ActionRegistry.RaceAction {
    private final Block block;
    private final Position position;
    private final boolean overwrite;
    private final Map<String, String> stateChanges;
    private final List<BlockDataEdit> dataEdits;

    private PlaceBlockAction(Block block, Position position, boolean overwrite, Map<String, String> stateChanges,
            List<BlockDataEdit> dataEdits) {
        this.block = block;
        this.position = position;
        this.overwrite = overwrite;
        this.stateChanges = stateChanges;
        this.dataEdits = dataEdits;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        Level level = player.level();
        if (!level.getGameRules().getBoolean(ModGameRules.RULE_RACEGRIEFING)) {
            player.displayClientMessage(Component.translatable("msg.creraces.race_griefing_disabled"), true);
            return false;
        }
        BlockPos pos = position.resolveBlock(player, target, slot, interactPos);
        if (pos == null) {
            return false;
        }

        BlockState existing = level.getBlockState(pos);
        if (!overwrite && existing.is(block)) {
            return false;
        }
        if (!canPlaceAt(level, pos, existing, player)) {
            player.displayClientMessage(Component.translatable("msg.creraces.place_block_failed"), true);
            return false;
        }
        // A territory root block can only be replaced by its owner (or in creative).
        if (existing.getBlock() instanceof RootBlock && !player.isCreative() && !RootBlock.isOwner(player, pos)) {
            return false;
        }

        level.setBlockAndUpdate(pos, BlockDataEdit.withProperties(block.defaultBlockState(), stateChanges));
        if (!dataEdits.isEmpty()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null && BlockDataEdit.applyAll(dataEdits, blockEntity, player, target, slot, interactPos)) {
                BlockState placed = level.getBlockState(pos);
                level.sendBlockUpdated(pos, placed, placed, Block.UPDATE_ALL);
            }
        }
        return true;
    }

    /** Overwrite ignores everything; otherwise the spot must be replaceable and, like vanilla, free of entities. */
    private boolean canPlaceAt(Level level, BlockPos pos, BlockState existing, Player player) {
        if (overwrite) {
            return true;
        }
        if (!existing.isAir() && !existing.canBeReplaced()) {
            return false;
        }
        return level.getEntitiesOfClass(Entity.class, new AABB(pos), e -> e != player).isEmpty();
    }

    /** The block named by "block"; the registry would quietly fall back to air for an unknown id. */
    static Block parseBlock(String text) {
        ResourceLocation id = ResourceLocation.tryParse(text);
        if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) {
            throw new IllegalArgumentException("unknown block '" + text + "'");
        }
        return BuiltInRegistries.BLOCK.get(id);
    }

    public static void register() {
        ActionRegistry.register(CreRaces.id("place_block"), json -> new PlaceBlockAction(
                parseBlock(GsonHelper.getAsString(json, "block")),
                Position.fromJson(json, "position", Position.of(Position.Source.SELF)),
                GsonHelper.getAsBoolean(json, "overwrite", false),
                BlockDataEdit.parseState(json),
                BlockDataEdit.parseList(json)));
    }
}
