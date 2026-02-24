package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class PlaceBlockAction implements ActionRegistry.RaceAction {

    private final String blockId;
    private final boolean atTarget;
    private final boolean useTargetBlock;
    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;

    public PlaceBlockAction(String blockId, boolean atTarget, boolean useTargetBlock, int offsetX, int offsetY,
            int offsetZ) {
        this.blockId = blockId;
        this.atTarget = atTarget;
        this.useTargetBlock = useTargetBlock;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        LivingEntity entity = atTarget && target != null ? target : player;
        BlockPos pos = entity.blockPosition();

        if (this.useTargetBlock) {
            if (interactionPos != null) {
                pos = interactionPos;
            } else {
                net.minecraft.world.phys.HitResult hit = player.pick(5.0, 0.0F, false);
                if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                    pos = ((net.minecraft.world.phys.BlockHitResult) hit).getBlockPos();
                }
            }
        }

        pos = pos.offset(offsetX, offsetY, offsetZ);

        Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(new ResourceLocation(blockId));
        if (block != Blocks.AIR) {
            player.level().setBlockAndUpdate(pos, block.defaultBlockState());
        } else {
            player.level().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "place_block"), json -> {
            String block = json.has("block") ? json.get("block").getAsString() : "minecraft:stone";
            boolean atTarget = GsonHelper.getAsBoolean(json, "use_target", false);
            boolean useTargetBlock = GsonHelper.getAsBoolean(json, "use_target_block", false);
            int offsetX = GsonHelper.getAsInt(json, "offset_x", 0);
            int offsetY = GsonHelper.getAsInt(json, "offset_y", 0);
            int offsetZ = GsonHelper.getAsInt(json, "offset_z", 0);
            return new PlaceBlockAction(block, atTarget, useTargetBlock, offsetX, offsetY, offsetZ);
        });
    }
}
