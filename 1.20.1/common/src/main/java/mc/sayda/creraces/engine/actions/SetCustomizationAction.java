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
    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;

    public SetCustomizationAction(String key, String value, String mode, int offsetX, int offsetY, int offsetZ) {
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
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            String valToSet = value;
            switch (mode.toUpperCase()) {
                case "POS_X" -> valToSet = String.valueOf(player.getX() + offsetX);
                case "POS_Y" -> valToSet = String.valueOf(player.getY() + offsetY);
                case "POS_Z" -> valToSet = String.valueOf(player.getZ() + offsetZ);
                case "BLOCK_X" -> valToSet = String.valueOf(player.blockPosition().getX() + offsetX);
                case "BLOCK_Y" -> valToSet = String.valueOf(player.blockPosition().getY() + offsetY);
                case "BLOCK_Z" -> valToSet = String.valueOf(player.blockPosition().getZ() + offsetZ);
                case "TARGET_X" -> {
                    if (target != null)
                        valToSet = String.valueOf(target.getX() + offsetX);
                }
                case "TARGET_Y" -> {
                    if (target != null)
                        valToSet = String.valueOf(target.getY() + offsetY);
                }
                case "TARGET_Z" -> {
                    if (target != null)
                        valToSet = String.valueOf(target.getZ() + offsetZ);
                }
                case "TARGET_BLOCK_X" -> {
                    net.minecraft.world.phys.HitResult hit = player.pick(5.0, 0.0F, false);
                    if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                        valToSet = String.valueOf(
                                ((net.minecraft.world.phys.BlockHitResult) hit).getBlockPos().getX() + offsetX);
                    }
                }
                case "TARGET_BLOCK_Y" -> {
                    net.minecraft.world.phys.HitResult hit = player.pick(5.0, 0.0F, false);
                    if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                        valToSet = String.valueOf(
                                ((net.minecraft.world.phys.BlockHitResult) hit).getBlockPos().getY() + offsetY);
                    }
                }
                case "TARGET_BLOCK_Z" -> {
                    net.minecraft.world.phys.HitResult hit = player.pick(5.0, 0.0F, false);
                    if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                        valToSet = String.valueOf(
                                ((net.minecraft.world.phys.BlockHitResult) hit).getBlockPos().getZ() + offsetZ);
                    }
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
            int offsetX = GsonHelper.getAsInt(json, "offset_x", 0);
            int offsetY = GsonHelper.getAsInt(json, "offset_y", 0);
            int offsetZ = GsonHelper.getAsInt(json, "offset_z", 0);
            return new SetCustomizationAction(key, value, mode, offsetX, offsetY, offsetZ);
        });
    }
}
