package mc.sayda.creraces.engine.traits;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.List;

/** Runs actions when the player dies; the killer, if any, is the actions' target. */
public class OnDeathTrait implements TraitRegistry.RaceTrait {
    private final List<ActionRegistry.RaceAction> actions;
    @Nullable
    private final Condition condition;

    public OnDeathTrait(List<ActionRegistry.RaceAction> actions, @Nullable Condition condition) {
        this.actions = actions;
        this.condition = condition;
    }

    @Override
    public void onDeath(Player player, DamageSource source) {
        LivingEntity attacker = source.getEntity() instanceof LivingEntity le ? le : null;
        if (condition == null || condition.evaluate(player, attacker, null, null)) {
            ActionRegistry.runChain(actions, player, attacker, null, null);
        }
    }

    public static void register() {
        TraitRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "on_death"), json -> {
            Condition condition = json.has("condition") ? Condition.fromJson(json.getAsJsonObject("condition")) : null;
            return new OnDeathTrait(ActionRegistry.listFromJson(json, "actions"), condition);
        });
    }
}
