package mc.sayda.creraces.engine;

import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

/**
 * Determines which blocks are safe to place inside a MicroBlock.
 *
 * EntityBlocks are denied by default except the explicit whitelist in
 * isInteractive() below (chests, furnaces, crafting stations, and similar).
 * Non-EntityBlocks are filtered by category further down: multi-block
 * structures, liquids, redstone/active blocks, and portals are handled case
 * by case.
 */
public class MicroBlockWhitelist {

    public static boolean isInteractive(Block block) {
        return block instanceof CraftingTableBlock
                || block instanceof BarrelBlock
                || block instanceof AbstractFurnaceBlock
                || block instanceof BedBlock
                || block instanceof DoorBlock
                || block instanceof TrapDoorBlock
                || block instanceof FenceGateBlock
                || block instanceof ChestBlock
                || block instanceof EnderChestBlock
                || block instanceof JukeboxBlock
                || block instanceof AnvilBlock
                || block instanceof StonecutterBlock
                || block instanceof GrindstoneBlock
                || block instanceof EnchantmentTableBlock
                || block instanceof LoomBlock
                || block instanceof CartographyTableBlock
                || block instanceof BrewingStandBlock
                || block instanceof CampfireBlock
                || block instanceof SmithingTableBlock
                || block == net.minecraft.world.level.block.Blocks.LODESTONE
                || block instanceof LecternBlock
                || block instanceof ChiseledBookShelfBlock
                || block instanceof DecoratedPotBlock
                || block instanceof BellBlock
                || block instanceof NoteBlock
                || block instanceof RespawnAnchorBlock
                || block instanceof AbstractCauldronBlock;
    }

    public static boolean isAllowed(Block block) {
        // EntityBlocks are blocked by default, EXCEPT our explicitly supported
        // interactive types.
        if (block instanceof EntityBlock) {
            return isInteractive(block);
        }

        // Multi-block structures (Enabled as single-slot components)
        if (block instanceof BedBlock)
            return true;
        if (block instanceof DoorBlock)
            return true;
        if (block instanceof TrapDoorBlock)
            return true;
        if (block instanceof FenceGateBlock)
            return true;
        if (block instanceof TorchBlock)
            return true;
        if (block instanceof RedstoneTorchBlock)
            return true;

        // Piston family: pistons have a block entity so already blocked above,
        // but MovingPistonBlock does not - exclude it explicitly.
        // Note: class names vary by mapping; we use a property-based check instead.
        if (block.getDescriptionId().contains("piston"))
            return false;

        // Command / structure blocks
        if (block instanceof CommandBlock)
            return false;
        if (block instanceof StructureBlock)
            return false;
        if (block instanceof JigsawBlock)
            return false;

        // Liquids / fire / void
        if (block instanceof LiquidBlock) {
            // Allow water and lava source blocks (static only, no flow logic)
            return block == net.minecraft.world.level.block.Blocks.WATER
                    || block == net.minecraft.world.level.block.Blocks.LAVA;
        }
        if (block instanceof BaseFireBlock)
            return false;

        // Plants and tall structures (1-block variants are allowed)
        if (block instanceof DoublePlantBlock)
            return false;
        if (block instanceof TallFlowerBlock)
            return false;
        if (block instanceof SugarCaneBlock)
            return false;
        // BambooStalkBlock (old bamboo plant in 1.20.1)
        if (block instanceof BambooStalkBlock)
            return false;
        if (block instanceof ScaffoldingBlock)
            return false;

        // Redstone / active blocks
        if (block instanceof RedStoneWireBlock)
            return false;
        if (block instanceof RepeaterBlock)
            return false;
        if (block instanceof ComparatorBlock)
            return false;
        if (block instanceof ButtonBlock)
            return false;
        if (block instanceof LeverBlock)
            return false;
        if (block instanceof PressurePlateBlock)
            return false;
        if (block instanceof WeightedPressurePlateBlock)
            return false;
        if (block instanceof TripWireHookBlock)
            return false;
        if (block instanceof TripWireBlock)
            return false;

        // Portal blocks
        if (block instanceof NetherPortalBlock)
            return false;
        if (block instanceof EndPortalBlock)
            return false;
        if (block instanceof EndGatewayBlock)
            return false;

        // Air / void
        if (block instanceof AirBlock)
            return false;

        // Passed all checks
        return true;
    }
}
