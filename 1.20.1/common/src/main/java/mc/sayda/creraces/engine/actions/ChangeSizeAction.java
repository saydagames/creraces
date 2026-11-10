package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.EntitySelector;
import mc.sayda.creraces.race.RaceIncidents;
import mc.sayda.creraces.race.RaceScale;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/** Applies a "scale" (the same shape as a race's creraces:scale) to a player. */
public class ChangeSizeAction implements ActionRegistry.RaceAction {
    private final RaceScale scale;
    private final EntitySelector entity;

    public ChangeSizeAction(RaceScale scale, EntitySelector entity) {
        this.scale = scale;
        this.entity = entity;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        LivingEntity subject = entity.resolve(player, target);
        if (subject != null) {
            RaceIncidents.applyScale(subject, scale);
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(CreRaces.id("change_size"), json -> new ChangeSizeAction(
                RaceScale.fromJson(json.get("scale")),
                EntitySelector.fromJson(json, EntitySelector.AUTO)));
    }
}
