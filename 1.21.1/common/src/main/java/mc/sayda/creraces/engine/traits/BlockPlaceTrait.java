package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonElement;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Trait that triggers actions when the player places a block.
 */
public class BlockPlaceTrait implements TraitRegistry.RaceTrait {
    private final String blockDefinition; // Block ID or #tag
    private final List<ActionRegistry.RaceAction> actions;
    @Nullable
    private final Condition condition;

    public BlockPlaceTrait(String blockDefinition, List<ActionRegistry.RaceAction> actions,
            @Nullable Condition condition) {
        this.blockDefinition = blockDefinition;
        this.actions = actions;
        this.condition = condition;
    }

    public static void register() {
        TraitRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "block_place"), data -> {
            String blockStr = GsonHelper.getAsString(data, "block", "minecraft:air");
            Condition condition = data.has("condition") ? Condition.fromJson(data.getAsJsonObject("condition")) : null;
            List<ActionRegistry.RaceAction> actions = new ArrayList<>();
            if (data.has("actions")) {
                for (JsonElement e : data.getAsJsonArray("actions")) {
                    actions.add(ActionRegistry.fromJson(e.getAsJsonObject()));
                }
            }
            return new BlockPlaceTrait(blockStr, actions, condition);
        });
    }

    @Override
    public boolean onBlockPlace(Player player, BlockPos pos, BlockState state) {
        if (BlockDefinitionMatcher.matches(state, blockDefinition)) {
            if (condition != null && !condition.evaluate(player, null, null, pos)) {
                return false;
            }
            if (actions.isEmpty()) return false;
            for (ActionRegistry.RaceAction action : actions) {
                if (!action.execute(player, null, null, pos)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
