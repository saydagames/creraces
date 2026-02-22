package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class ChangeSizeAction implements ActionRegistry.RaceAction {

    private final float scale;
    private final boolean atTarget;

    public ChangeSizeAction(float scale, boolean atTarget) {
        this.scale = scale;
        this.atTarget = atTarget;
    }

    @Override
    public void execute(Player player, LivingEntity target, mc.sayda.creraces.ability.AbilitySlot slot) {
        LivingEntity entity = atTarget && target != null ? target : player;

        try {
            virtuoel.pehkui.api.ScaleData data = virtuoel.pehkui.api.ScaleTypes.BASE.getScaleData(entity);
            data.setScale(scale);
            data.setTargetScale(scale);
        } catch (Throwable ignored) {
            CreRaces.LOGGER.error("Failed to change size: Pehkui not found or error occurred.");
        }
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "change_size"), json -> {
            float scale = json.has("scale") ? json.get("scale").getAsFloat() : 1.0f;
            boolean atTarget = json.has("use_target") && json.get("use_target").getAsBoolean();
            return new ChangeSizeAction(scale, atTarget);
        });
    }
}
