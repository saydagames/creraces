package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Sets a custom variable (customization) on the player.
 * Can save current position, target position, or static values.
 */
public class SetCustomizationAction implements ActionRegistry.RaceAction {
    private final String key;
    private final String value;
    private final String mode; // "STATIC", "POS_X", "POS_Y", "POS_Z", "TARGET_X", etc.
    private final mc.sayda.creraces.engine.ScalingValue offsetX;
    private final mc.sayda.creraces.engine.ScalingValue offsetY;
    private final mc.sayda.creraces.engine.ScalingValue offsetZ;

    public SetCustomizationAction(String key, String value, String mode, mc.sayda.creraces.engine.ScalingValue offsetX,
            mc.sayda.creraces.engine.ScalingValue offsetY, mc.sayda.creraces.engine.ScalingValue offsetZ) {
        this.key = key;
        this.value = value;
        this.mode = mode;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            String valToSet = value;
            double ox = offsetX.evaluate(player, target, slot);
            double oy = offsetY.evaluate(player, target, slot);
            double oz = offsetZ.evaluate(player, target, slot);

            switch (mode.toUpperCase()) {
                case "POS_X" -> valToSet = String.valueOf(player.getX() + ox);
                case "POS_Y" -> valToSet = String.valueOf(player.getY() + oy);
                case "POS_Z" -> valToSet = String.valueOf(player.getZ() + oz);
                case "POS_DIM" -> valToSet = player.level().dimension().location().toString();
                case "BLOCK_X" -> valToSet = String.valueOf(player.blockPosition().getX() + (int) ox);
                case "BLOCK_Y" -> valToSet = String.valueOf(player.blockPosition().getY() + (int) oy);
                case "BLOCK_Z" -> valToSet = String.valueOf(player.blockPosition().getZ() + (int) oz);
                case "TARGET_X" -> {
                    if (target != null)
                        valToSet = String.valueOf(target.getX() + ox);
                }
                case "TARGET_Y" -> {
                    if (target != null)
                        valToSet = String.valueOf(target.getY() + oy);
                }
                case "TARGET_Z" -> {
                    if (target != null)
                        valToSet = String.valueOf(target.getZ() + oz);
                }
                case "TARGET_BLOCK_X" -> {
                    if (interact_pos != null) {
                        valToSet = String.valueOf(interact_pos.getX() + (int) ox);
                    } else {
                        net.minecraft.world.phys.HitResult hit = player.pick(5.0, 0.0F, false);
                        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                            valToSet = String.valueOf(
                                    ((net.minecraft.world.phys.BlockHitResult) hit).getBlockPos().getX() + (int) ox);
                        }
                    }
                }
                case "TARGET_BLOCK_Y" -> {
                    if (interact_pos != null) {
                        valToSet = String.valueOf(interact_pos.getY() + (int) oy);
                    } else {
                        net.minecraft.world.phys.HitResult hit = player.pick(5.0, 0.0F, false);
                        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                            valToSet = String.valueOf(
                                    ((net.minecraft.world.phys.BlockHitResult) hit).getBlockPos().getY() + (int) oy);
                        }
                    }
                }
                case "TARGET_BLOCK_Z" -> {
                    if (interact_pos != null) {
                        valToSet = String.valueOf(interact_pos.getZ() + (int) oz);
                    } else {
                        net.minecraft.world.phys.HitResult hit = player.pick(5.0, 0.0F, false);
                        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                            valToSet = String.valueOf(
                                    ((net.minecraft.world.phys.BlockHitResult) hit).getBlockPos().getZ() + (int) oz);
                        }
                    }
                }
                case "TARGET_BLOCK_DIM" -> {
                    valToSet = player.level().dimension().location().toString();
                }
                case "REMOVE" -> {
                    valToSet = null;
                }
            }
            vars.setCustomization(key, valToSet);
        });
        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "set_customization"), json -> {
            String key = GsonHelper.getAsString(json, "key");
            String value = GsonHelper.getAsString(json, "value", "");
            String mode = GsonHelper.getAsString(json, "mode", "STATIC");
            mc.sayda.creraces.engine.ScalingValue offsetX = mc.sayda.creraces.engine.ScalingValue.fromJson(json,
                    "offset_x", 0.0);
            mc.sayda.creraces.engine.ScalingValue offsetY = mc.sayda.creraces.engine.ScalingValue.fromJson(json,
                    "offset_y", 0.0);
            mc.sayda.creraces.engine.ScalingValue offsetZ = mc.sayda.creraces.engine.ScalingValue.fromJson(json,
                    "offset_z", 0.0);
            return new SetCustomizationAction(key, value, mode, offsetX, offsetY, offsetZ);
        });
    }
}
