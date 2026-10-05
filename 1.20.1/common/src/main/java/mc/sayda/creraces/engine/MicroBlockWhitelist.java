package mc.sayda.creraces.engine;

import net.minecraft.world.level.block.*;

/**
 * Decides which blocks may be placed inside a MicroBlock.
 *
 * EntityBlocks are refused unless isInteractive() lists them (chests, furnaces, crafting stations
 * and similar). Other blocks are allowed unless isAllowed() refuses their category: pistons,
 * fluids other than water and lava, fire, tall plants, redstone components and nether portals.
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
                || block == Blocks.LODESTONE
                || block instanceof LecternBlock
                || block instanceof ChiseledBookShelfBlock
                || block instanceof DecoratedPotBlock
                || block instanceof BellBlock
                || block instanceof NoteBlock
                || block instanceof RespawnAnchorBlock
                || block instanceof AbstractCauldronBlock;
    }

    public static boolean isAllowed(Block block) {
        // Also refuses command, structure and jigsaw blocks and end portals, which all carry block entities.
        if (block instanceof EntityBlock) {
            return isInteractive(block);
        }

        // Always allowed, including two-block doors, which fit as single-slot components.
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

        // Piston bases and heads have no block entity, so the EntityBlock check above misses them.
        // Matching on the description id also catches modded pistons.
        if (block.getDescriptionId().contains("piston"))
            return false;

        // Only vanilla water and lava; other fluid blocks are refused.
        if (block instanceof LiquidBlock) {
            return block == Blocks.WATER || block == Blocks.LAVA;
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

        if (block instanceof NetherPortalBlock)
            return false;

        return !(block instanceof AirBlock);
    }
}
