package mc.sayda.creraces.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Utility methods for world manipulation and common gameplay mechanics.
 * Replaces legacy MCreator procedures.
 */
public class WorldUtils {

    /**
     * Removes a door from a wall. Matches legacy RemoveDoorProcedure behaviour.
     *
     * <p>
     * Scans an orientation-aware volume centered on the panel: {@code width}
     * blocks across the wall, {@code height} blocks tall, and {@code depth} blocks
     * deep into the wall (opposite to panel facing). Any block matching
     * {@code blockToMatch} is destroyed. The panel block at {@code origin} is
     * always destroyed at the end.
     *
     * <p>
     * Default Dryad values (3×3×2) reproduce the legacy 3×3 hole.
     */
    @SuppressWarnings("null")
    public static void removeDoor(LevelAccessor world, @javax.annotation.Nonnull BlockPos origin,
            @javax.annotation.Nonnull BlockState blockToMatch,
            @javax.annotation.Nonnull Direction panelFacing,
            int width, int height, int depth) {
        if (origin == null || panelFacing == null)
            return;

        int halfW = width / 2;
        int halfH = height / 2;

        // The door penetrates into the wall, opposite to the panel's outward face
        Direction intoWall = panelFacing.getOpposite();

        for (int d = 0; d < depth; d++) {
            BlockPos layerOrigin = origin.relative(intoWall, d);
            for (int w = -halfW; w <= halfW; w++) {
                for (int h = -halfH; h <= halfH; h++) {
                    BlockPos pos;
                    Direction.Axis axis = panelFacing.getAxis();
                    if (axis == Direction.Axis.X) {
                        // Panel faces X → width spreads on Z, height on Y
                        pos = layerOrigin.offset(0, h, w);
                    } else if (axis == Direction.Axis.Z) {
                        // Panel faces Z → width spreads on X, height on Y
                        pos = layerOrigin.offset(w, h, 0);
                    } else {
                        // Panel faces Y → width spreads on X, height on Z
                        pos = layerOrigin.offset(w, 0, h);
                    }
                    // Clear ALL blocks in the aperture unconditionally, the door volume
                    // must be fully open regardless of what material the wall is made of.
                    // The door_block itself is handled below so skip the origin here.
                    if (!pos.equals(origin) && !world.getBlockState(pos).isAir()) {
                        world.destroyBlock(pos, false);
                    }
                }
            }
        }
        // Always remove the panel itself (the door_block) individually
        world.destroyBlock(origin, false);
    }

    /**
     * Checks if an entity is exposed to rain, taking into account vanilla shelter
     * and custom CreRaces micro-blocks.
     */
    @SuppressWarnings("null")
    public static boolean isExposedToRain(net.minecraft.world.entity.LivingEntity entity) {
        net.minecraft.world.level.Level level = entity.level();
        BlockPos pos = entity.blockPosition();

        if (!level.isRainingAt(pos)) {
            return false;
        }

        // Check for micro-block shelter starting from the entity's position up to 16
        // blocks
        for (int dy = 0; dy <= 16; dy++) {
            BlockPos overheadPos = pos.above(dy);
            BlockState state = level.getBlockState(overheadPos);

            if (state.is(mc.sayda.creraces.registry.ModBlocks.MICRO_BLOCK.get())) {
                if (level.getBlockEntity(overheadPos) instanceof mc.sayda.creraces.block.entity.MicroBlockEntity micro) {
                    int playerSlotY = -1;
                    if (dy == 0) {
                        double yOffset = entity.getY() - pos.getY();
                        playerSlotY = (int) (yOffset * 4);
                    }

                    // Calculate entity's sub-grid column once
                    int sx = (int) (((entity.getX() - overheadPos.getX()) % 1.0 + 1.0) % 1.0 * 4);
                    int sz = (int) (((entity.getZ() - overheadPos.getZ()) % 1.0 + 1.0) % 1.0 * 4);
                    sx = Math.max(0, Math.min(3, sx));
                    sz = Math.max(0, Math.min(3, sz));

                    for (int sy = playerSlotY + 1; sy < 4; sy++) {
                        if (!micro.getSlot(sx, sy, sz).isAir()) {
                            return false; // Sheltered by mini-block roof in THIS column!
                        }
                    }
                }
            } else if (dy > 0 && state.isSolidRender(level, overheadPos)) {
                // Regular solid block shelter - redundant due to isRainingAt but kept for
                // safety
                return false;
            }
        }

        return true;
    }
}
