package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class ChangeSizeAction implements ActionRegistry.RaceAction {

    private final mc.sayda.creraces.race.RaceScale scale;
    private final boolean atTarget;

    public ChangeSizeAction(mc.sayda.creraces.race.RaceScale scale, boolean atTarget) {
        this.scale = scale;
        this.atTarget = atTarget;
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        LivingEntity entity = atTarget && target != null ? target : player;

        mc.sayda.creraces.race.RaceIncidents.applyScale(entity, scale);
        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "change_size"), json -> {
            mc.sayda.creraces.race.RaceScale scale = mc.sayda.creraces.race.RaceScale.fromJson(json.get("scale"));
            boolean atTarget = json.has("use_target") && json.get("use_target").getAsBoolean();
            return new ChangeSizeAction(scale, atTarget);
        });
    }
}
