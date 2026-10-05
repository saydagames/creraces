package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonElement;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.List;

/** Runs if_true or if_false depending on a condition; fails if the chosen branch fails. */
public class ConditionalAction implements ActionRegistry.RaceAction {

    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "conditional");

    private final Condition condition;
    private final List<ActionRegistry.RaceAction> ifTrue;
    private final List<ActionRegistry.RaceAction> ifFalse;

    public ConditionalAction(Condition condition, List<ActionRegistry.RaceAction> ifTrue,
            List<ActionRegistry.RaceAction> ifFalse) {
        this.condition = condition;
        this.ifTrue = ifTrue;
        this.ifFalse = ifFalse;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        List<ActionRegistry.RaceAction> branch = condition.evaluate(player, target, slot, interactPos)
                ? ifTrue
                : ifFalse;
        return ActionRegistry.runChain(branch, player, target, slot, interactPos);
    }

    public static void register() {
        ActionRegistry.register(ID, json -> {
            JsonElement conditionJson = json.get("condition");
            if (conditionJson == null) {
                CreRaces.LOGGER.error("ConditionalAction missing 'condition' - the action will do nothing.");
                return (player, target, slot, interactPos) -> true;
            }
            return new ConditionalAction(Condition.fromJson(conditionJson.getAsJsonObject()),
                    ActionRegistry.listFromJson(json, "if_true"),
                    ActionRegistry.listFromJson(json, "if_false"));
        });
    }
}
