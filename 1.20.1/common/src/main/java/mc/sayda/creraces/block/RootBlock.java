package mc.sayda.creraces.block;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.territory.TerritoryManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/**
 * Anchor for race-specific locations such as the Dryad's tree; the interaction logic lives in race
 * traits (JSON). Territory anchors are registered by ClaimTerritoryAction rather than on placement,
 * so a hand-placed block can't create a spurious anchor that later unclaims territory.
 */
@SuppressWarnings({"null", "deprecation"})
public class RootBlock extends Block {

    private static final ResourceLocation NODE_X = new ResourceLocation(CreRaces.MODID, "node_x");
    private static final ResourceLocation NODE_Y = new ResourceLocation(CreRaces.MODID, "node_y");
    private static final ResourceLocation NODE_Z = new ResourceLocation(CreRaces.MODID, "node_z");

    public RootBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide() && !state.is(newState.getBlock())) {
            TerritoryManager.get().removeRootBlock(pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    public static Properties getDefaultProperties() {
        return Properties.of()
                .mapColor(MapColor.DIRT)
                .strength(-1.0f, 3600000.0f) // Indestructible
                .sound(SoundType.GRAVEL)
                .noLootTable();
    }

    /** True if {@code pos} is the node recorded in the player's node_x/y/z persistent state. */
    public static boolean isOwner(Player player, BlockPos pos) {
        return DataUtils.getVariables(player).map(vars -> {
            double tx = vars.getPersistentState(NODE_X);
            double ty = vars.getPersistentState(NODE_Y);
            double tz = vars.getPersistentState(NODE_Z);
            if (tx == 0 && ty == 0 && tz == 0) return false;
            return pos.getX() == (int) Math.floor(tx)
                && pos.getY() == (int) Math.floor(ty)
                && pos.getZ() == (int) Math.floor(tz);
        }).orElse(false);
    }
}
