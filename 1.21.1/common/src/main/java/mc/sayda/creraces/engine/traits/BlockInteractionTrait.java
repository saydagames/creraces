package mc.sayda.creraces.engine.traits;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Runs actions when the player right-clicks a matching block. Returns true, which stops the vanilla
 * handling of the event, only when the block matches and every action succeeded.
 */
public class BlockInteractionTrait implements TraitRegistry.RaceTrait {
    private final String blockDefinition; // Block ID or #tag
    private final List<ActionRegistry.RaceAction> actions;
    @Nullable
    private final Condition condition;

    public BlockInteractionTrait(String blockDefinition, List<ActionRegistry.RaceAction> actions,
            @Nullable Condition condition) {
        this.blockDefinition = blockDefinition;
        this.actions = actions;
        this.condition = condition;
    }

    public static void register() {
        TraitRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "block_interaction"), data -> {
            String blockStr = GsonHelper.getAsString(data, "block", "minecraft:air");
            Condition condition = data.has("condition") ? Condition.fromJson(data.getAsJsonObject("condition")) : null;
            return new BlockInteractionTrait(blockStr, ActionRegistry.listFromJson(data, "actions"), condition);
        });
    }

    @Override
    public boolean onBlockInteraction(Player player, BlockPos pos, BlockState state) {
        if (!BlockDefinitionMatcher.matches(state, blockDefinition))
            return false;
        if (condition != null && !condition.evaluate(player, null, null, pos))
            return false;
        return !actions.isEmpty() && ActionRegistry.runChain(actions, player, null, null, pos);
    }
}
