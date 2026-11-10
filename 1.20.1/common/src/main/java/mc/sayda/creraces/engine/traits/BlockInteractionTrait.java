package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.EventContext;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Runs when the player right-clicks a "block" (an id or #tag), with that block as the "block"
 * position. A completed run takes over the click.
 */
public class BlockInteractionTrait extends EventTrait {
    private final String block;

    BlockInteractionTrait(JsonObject json) {
        super(json, null);
        this.block = GsonHelper.getAsString(json, "block");
    }

    @Override
    public boolean onBlockInteraction(Player player, BlockPos pos, BlockState state) {
        return BlockDefinitionMatcher.matches(state, block) && hasActions()
                && fire(player, null, pos, EventContext.NONE);
    }

    public static void register() {
        TraitRegistry.register(CreRaces.id("block_interaction"), BlockInteractionTrait::new);
    }
}
