package mc.sayda.creraces.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * DryadRootBlock — the anchor block placed beneath a Dryad's claimed sapling.
 * <p>
 * All gameplay interactions (sneak-right-click to reclaim, coordinate clearing,
 * sapling drop) are driven by the JSON {@code block_interaction} trait in
 * {@code dryad.json}. This class only needs to exist as a concrete Block type
 * so that {@link mc.sayda.creraces.registry.ModBlocks} can register it.
 */
public class DryadRootBlock extends Block {

    public DryadRootBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
}
