package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.AreaSearch;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * The engine's only area action: finds the entities around "position" (see AreaSearch for every key)
 * and runs "actions" once per entity with that entity as the target. Each entity's chain runs on its
 * own, so one failing doesn't stop the others. With "fail_if_empty", finding nobody fails the action.
 */
public class AOEAction implements ActionRegistry.RaceAction {
    public static final ResourceLocation ID = CreRaces.id("aoe");

    private final AreaSearch search;
    private final boolean failIfEmpty;
    private final List<ActionRegistry.RaceAction> actions;

    public AOEAction(AreaSearch search, boolean failIfEmpty, List<ActionRegistry.RaceAction> actions) {
        this.search = search;
        this.failIfEmpty = failIfEmpty;
        this.actions = actions;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        List<LivingEntity> found = search.find(player, target, slot, interactPos);
        if (found == null) {
            return false;
        }
        if (found.isEmpty()) {
            return !failIfEmpty;
        }
        for (LivingEntity entity : found) {
            ActionRegistry.runChain(actions, player, entity, slot, interactPos);
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(ID, json -> new AOEAction(
                AreaSearch.fromJson(json, 5.0, Set.of("enemies")),
                GsonHelper.getAsBoolean(json, "fail_if_empty", false),
                ActionRegistry.listFromJson(json, "actions")));
    }
}
