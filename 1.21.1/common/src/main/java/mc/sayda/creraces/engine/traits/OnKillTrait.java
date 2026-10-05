package mc.sayda.creraces.engine.traits;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import mc.sayda.creraces.team.RaceTeamManager;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Runs actions, targeting the victim, when the player kills an entity. Allies are skipped
 * unless bypass_safety is set.
 */
public class OnKillTrait implements TraitRegistry.RaceTrait {
    private final List<ActionRegistry.RaceAction> actions;
    @Nullable
    private final Condition condition;
    private final boolean bypassSafety;

    public OnKillTrait(List<ActionRegistry.RaceAction> actions, @Nullable Condition condition, boolean bypassSafety) {
        this.actions = actions;
        this.condition = condition;
        this.bypassSafety = bypassSafety;
    }

    public static void register() {
        TraitRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "on_kill"), json -> {
            Condition condition = json.has("condition") ? Condition.fromJson(json.getAsJsonObject("condition")) : null;
            boolean bypassSafety = GsonHelper.getAsBoolean(json, "bypass_safety", false);
            return new OnKillTrait(ActionRegistry.listFromJson(json, "actions"), condition, bypassSafety);
        });
    }

    @Override
    public void onKill(Player player, LivingEntity target) {
        if (!bypassSafety && !RaceTeamManager.canHurt(target, player)) {
            return;
        }
        if (condition == null || condition.evaluate(player, target, null, null)) {
            ActionRegistry.runChain(actions, player, target, null, null);
        }
    }
}
