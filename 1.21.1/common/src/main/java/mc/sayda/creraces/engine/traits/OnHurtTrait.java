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

/** Runs actions when the player takes damage; the attacker, if any, is the actions' target. */
public class OnHurtTrait implements TraitRegistry.RaceTrait {
    private final List<ActionRegistry.RaceAction> actions;
    @Nullable
    private final Condition condition;

    public OnHurtTrait(List<ActionRegistry.RaceAction> actions, @Nullable Condition condition) {
        this.actions = actions;
        this.condition = condition;
    }

    @Override
    public void onHurt(Player player, DamageSource source, float amount) {
        LivingEntity attacker = source.getEntity() instanceof LivingEntity le ? le : null;
        if (condition == null || condition.evaluate(player, attacker, null, null)) {
            ActionRegistry.runChain(actions, player, attacker, null, null);
        }
    }

    public static void register() {
        TraitRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "on_hurt"), json -> {
            Condition condition = json.has("condition") ? Condition.fromJson(json.getAsJsonObject("condition")) : null;
            return new OnHurtTrait(ActionRegistry.listFromJson(json, "actions"), condition);
        });
    }
}
