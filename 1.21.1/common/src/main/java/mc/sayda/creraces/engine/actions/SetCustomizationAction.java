package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Sets a customization (string) variable on the caster: the static "value", or a captured coordinate
 * or dimension selected by "mode" (POS_*, BLOCK_*, TARGET_*, TARGET_BLOCK_*, POS_DIM, TARGET_BLOCK_DIM).
 * REMOVE deletes the variable. A capture that has nothing to read (no target, no block in view)
 * falls back to the static value.
 */
public class SetCustomizationAction implements ActionRegistry.RaceAction {
    private final String key;
    private final String value;
    private final String mode;
    private final ScalingValue offsetX;
    private final ScalingValue offsetY;
    private final ScalingValue offsetZ;

    public SetCustomizationAction(String key, String value, String mode, ScalingValue offsetX,
            ScalingValue offsetY, ScalingValue offsetZ) {
        this.key = key;
        this.value = value;
        this.mode = mode;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            double ox = offsetX.evaluate(player, target, slot);
            double oy = offsetY.evaluate(player, target, slot);
            double oz = offsetZ.evaluate(player, target, slot);
            String toSet = switch (mode.toUpperCase()) {
                case "POS_X" -> String.valueOf(player.getX() + ox);
                case "POS_Y" -> String.valueOf(player.getY() + oy);
                case "POS_Z" -> String.valueOf(player.getZ() + oz);
                case "POS_DIM", "TARGET_BLOCK_DIM" -> player.level().dimension().location().toString();
                case "BLOCK_X" -> String.valueOf(player.blockPosition().getX() + (int) ox);
                case "BLOCK_Y" -> String.valueOf(player.blockPosition().getY() + (int) oy);
                case "BLOCK_Z" -> String.valueOf(player.blockPosition().getZ() + (int) oz);
                case "TARGET_X" -> target != null ? String.valueOf(target.getX() + ox) : value;
                case "TARGET_Y" -> target != null ? String.valueOf(target.getY() + oy) : value;
                case "TARGET_Z" -> target != null ? String.valueOf(target.getZ() + oz) : value;
                case "TARGET_BLOCK_X" -> targetBlockCoordinate(player, interactPos, Direction.Axis.X, ox);
                case "TARGET_BLOCK_Y" -> targetBlockCoordinate(player, interactPos, Direction.Axis.Y, oy);
                case "TARGET_BLOCK_Z" -> targetBlockCoordinate(player, interactPos, Direction.Axis.Z, oz);
                case "REMOVE" -> null;
                default -> value;
            };
            vars.setCustomization(key, toSet);
        });
        return true;
    }

    private String targetBlockCoordinate(Player player, @Nullable BlockPos interactPos, Direction.Axis axis,
            double offset) {
        BlockPos block = BlockTargeting.interactedOrLookedAt(player, interactPos);
        return block != null ? String.valueOf(block.get(axis) + (int) offset) : value;
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "set_customization"),
                json -> new SetCustomizationAction(
                        GsonHelper.getAsString(json, "key"),
                        GsonHelper.getAsString(json, "value", ""),
                        GsonHelper.getAsString(json, "mode", "STATIC"),
                        ScalingValue.fromJson(json, "offset_x", 0.0),
                        ScalingValue.fromJson(json, "offset_y", 0.0),
                        ScalingValue.fromJson(json, "offset_z", 0.0)));
    }
}
