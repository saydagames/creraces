package mc.sayda.creraces.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

public class VolcanicRockHardenedBlock extends Block {

    public VolcanicRockHardenedBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(2.0f, 6.0f)
                .requiresCorrectToolForDrops()
                .instrument(NoteBlockInstrument.BASEDRUM));
    }
}
