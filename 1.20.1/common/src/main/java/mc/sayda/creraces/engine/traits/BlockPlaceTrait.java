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
 * Runs when the player places a "block" (an id or #tag), with it as the "block" position. The block
 * stays placed either way; a completed run only stops other place handlers from running.
 */
public class BlockPlaceTrait extends EventTrait {
    private final String block;

    BlockPlaceTrait(JsonObject json) {
        super(json, null);
        this.block = GsonHelper.getAsString(json, "block");
    }

    @Override
    public boolean onBlockPlace(Player player, BlockPos pos, BlockState state) {
        return BlockDefinitionMatcher.matches(state, block) && hasActions()
                && fire(player, null, pos, EventContext.NONE);
    }

    public static void register() {
        TraitRegistry.register(CreRaces.id("block_place"), BlockPlaceTrait::new);
    }
}
