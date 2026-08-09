package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonElement;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Trait that triggers actions when the player right-clicks a block.
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
        TraitRegistry.register(new ResourceLocation(CreRaces.MODID, "block_interaction"), data -> {
            String blockStr = GsonHelper.getAsString(data, "block", "minecraft:air");
            Condition condition = data.has("condition") ? Condition.fromJson(data.getAsJsonObject("condition")) : null;
            List<ActionRegistry.RaceAction> actions = new ArrayList<>();
            if (data.has("actions")) {
                for (JsonElement e : data.getAsJsonArray("actions")) {
                    actions.add(ActionRegistry.fromJson(e.getAsJsonObject()));
                }
            }
            return new BlockInteractionTrait(blockStr, actions, condition);
        });
    }

    @Override
    public boolean onBlockInteraction(Player player, BlockPos pos, BlockState state) {
        if (matches(state, blockDefinition)) {
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

    private boolean matches(BlockState state, String definition) {
        if (definition.startsWith("#")) {
            ResourceLocation tagLoc = new ResourceLocation(definition.substring(1));
            return state.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, tagLoc));
        } else {
            return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals(definition);
        }
    }
}
