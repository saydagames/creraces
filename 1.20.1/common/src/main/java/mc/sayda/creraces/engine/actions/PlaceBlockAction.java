package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class PlaceBlockAction implements ActionRegistry.RaceAction {
    private final ResourceLocation block;
    private final boolean useTarget;
    private final boolean useTargetBlock;
    private final ScalingValue offsetX;
    private final ScalingValue offsetY;
    private final ScalingValue offsetZ;

    public PlaceBlockAction(ResourceLocation block, boolean useTarget, boolean useTargetBlock, ScalingValue offsetX,
            ScalingValue offsetY, ScalingValue offsetZ) {
        this.block = block;
        this.useTarget = useTarget;
        this.useTargetBlock = useTargetBlock;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    @SuppressWarnings("null")
    @Override
    public boolean execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        BlockPos targetPos = player.blockPosition();

        if (useTarget && target != null) {
            targetPos = target.blockPosition();
        } else if (useTargetBlock && interactionPos != null) {
            targetPos = interactionPos;
        }

        int ox = (int) offsetX.evaluate(player, target);
        int oy = (int) offsetY.evaluate(player, target);
        int oz = (int) offsetZ.evaluate(player, target);
        BlockPos finalPos = targetPos.offset(ox, oy, oz);

        net.minecraft.world.level.block.Block resolvedBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .get(block);

        // Respect the micro-block whitelist — prevents race JSONs from placing
        // bedrock, command blocks, or other restricted blocks via this action
        if (!mc.sayda.creraces.engine.MicroBlockWhitelist.isAllowed(resolvedBlock)) {
            mc.sayda.creraces.CreRaces.LOGGER.warn("[CreRaces] PlaceBlockAction blocked: {} is not whitelisted", block);
            return false;
        }

        if (player.level().getBlockState(finalPos).isAir() || player.level().getBlockState(finalPos).canBeReplaced()) {
            player.level().setBlockAndUpdate(finalPos, resolvedBlock.defaultBlockState());
            return true;
        }

        return false;
    }

    @SuppressWarnings("null")
    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "place_block"), json -> {
            @SuppressWarnings("null")
            String blockId = GsonHelper.getAsString(json, "block", "minecraft:air");
            ResourceLocation block = new ResourceLocation(blockId);
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
            boolean useTargetBlock = GsonHelper.getAsBoolean(json, "use_target_block", false);
            ScalingValue offsetX = ScalingValue.fromJson(json, "offset_x", 0.0);
            ScalingValue offsetY = ScalingValue.fromJson(json, "offset_y", 0.0);
            ScalingValue offsetZ = ScalingValue.fromJson(json, "offset_z", 0.0);
            return new PlaceBlockAction(block, useTarget, useTargetBlock, offsetX, offsetY, offsetZ);
        });
    }
}
