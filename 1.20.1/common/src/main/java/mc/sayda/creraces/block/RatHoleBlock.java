package mc.sayda.creraces.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import mc.sayda.creraces.capability.DataUtils;
import net.minecraft.resources.ResourceLocation;

/**
 * Rat Hole - a flat, nearly invisible block placed by Ratkin's Rat Tunnels
 * ability.
 * <ul>
 * <li>No collision (entities walk through it).</li>
 * <li>No occlusion (transparent to light).</li>
 * <li>Gravel-like sound and feel.</li>
 * <li>Indestructible by normal means (unbreakable, explosion-immune).</li>
 * </ul>
 *
 * Teleportation logic is handled on block use (click).
 */
public class RatHoleBlock extends Block {

    // 15×1×15 pixel-thin floor-level hitbox, same as legacy
    private static final VoxelShape SHAPE = Block.box(0.5, 0, 0.5, 15.5, 1, 15.5);

    public RatHoleBlock() {
        super(BlockBehaviour.Properties.of()
                .sound(SoundType.GRAVEL)
                .strength(-1.0f, 3600000.0f) // indestructible
                .noCollission()
                .noOcclusion()
                .pushReaction(PushReaction.BLOCK)
                .isRedstoneConductor((bs, bl, bp) -> false));
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (level.isClientSide())
            return InteractionResult.SUCCESS;

        DataUtils.getVariables(player).ifPresent(vars -> {
            if (vars.getPersistentState(new ResourceLocation("creraces:rat_tunnels")) == 2) {
                // Check if we are clicking Hole A or Hole B
                String axS = vars.getCustomization("tunnel_ax");
                String ayS = vars.getCustomization("tunnel_ay");
                String azS = vars.getCustomization("tunnel_az");
                String bxS = vars.getCustomization("tunnel_bx");
                String byS = vars.getCustomization("tunnel_by");
                String bzS = vars.getCustomization("tunnel_bz");

                if (axS != null && ayS != null && azS != null && bxS != null && byS != null && bzS != null) {
                    try {
                        double ax = Double.parseDouble(axS);
                        double ay = Double.parseDouble(ayS);
                        double az = Double.parseDouble(azS);
                        double bx = Double.parseDouble(bxS);
                        double by = Double.parseDouble(byS);
                        double bz = Double.parseDouble(bzS);

                        // If at A, go to B. If at B, go to A.
                        // We use a small epsilon for coordinate matching
                        boolean isAtA = Math.abs(pos.getX() - ax) < 1.1 && Math.abs(pos.getY() - ay) < 1.1
                                && Math.abs(pos.getZ() - az) < 1.1;
                        boolean isAtB = Math.abs(pos.getX() - bx) < 1.1 && Math.abs(pos.getY() - by) < 1.1
                                && Math.abs(pos.getZ() - bz) < 1.1;

                        if (isAtA) {
                            player.teleportTo(bx + 0.5, by + 0.1, bz + 0.5);
                            level.playSound(player, pos, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                                    net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.5f);
                        } else if (isAtB) {
                            player.teleportTo(ax + 0.5, ay + 0.1, az + 0.5);
                            level.playSound(player, pos, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                                    net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.5f);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        });

        return InteractionResult.SUCCESS;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter world, BlockPos pos) {
        return 0;
    }
}
