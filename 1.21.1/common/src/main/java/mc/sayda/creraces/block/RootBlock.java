package mc.sayda.creraces.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/**
 * Generic Root Block.
 * Acts as an anchor for race-specific locations (e.g. Dryad's tree).
 * Indestructible by default. Interaction logic is handled via race traits
 * (JSON).
 */
@SuppressWarnings({"null", "deprecation"})
public class RootBlock extends Block {

    public RootBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        // Territory anchoring is handled by ClaimTerritoryAction, not here, to prevent
        // manually placed blocks from registering spurious anchors and later unclaiming territory.
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide() && !state.is(newState.getBlock())) {
            mc.sayda.creraces.territory.TerritoryManager.get().removeRootBlock(pos);
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

    private static final net.minecraft.resources.ResourceLocation NODE_X =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(mc.sayda.creraces.CreRaces.MODID, "node_x");
    private static final net.minecraft.resources.ResourceLocation NODE_Y =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(mc.sayda.creraces.CreRaces.MODID, "node_y");
    private static final net.minecraft.resources.ResourceLocation NODE_Z =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(mc.sayda.creraces.CreRaces.MODID, "node_z");

    public static boolean isOwner(net.minecraft.world.entity.player.Player player,
            net.minecraft.core.BlockPos pos) {
        return mc.sayda.creraces.capability.DataUtils.getVariables(player).map(vars -> {
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
