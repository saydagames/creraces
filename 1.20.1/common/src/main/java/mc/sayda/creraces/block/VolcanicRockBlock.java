package mc.sayda.creraces.block;

import mc.sayda.creraces.registry.ModBlocks;
import mc.sayda.creraces.registry.ModGameRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

import javax.annotation.Nonnull;

public class VolcanicRockBlock extends Block {

    public VolcanicRockBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.NETHER)
                .strength(-1.0f, 3600000.0f)
                .requiresCorrectToolForDrops()
                .hasPostProcess((state, level, pos) -> true)
                .emissiveRendering((state, level, pos) -> true)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .lightLevel(state -> 15));
    }

    @Override
    public void onPlace(@Nonnull BlockState blockstate, @Nonnull Level world, @Nonnull BlockPos pos,
            @Nonnull BlockState oldState, boolean moving) {
        super.onPlace(blockstate, world, pos, oldState, moving);
        world.scheduleTick(pos, this, 200); // Cools after 10 seconds
    }

    @Override
    public void tick(@Nonnull BlockState blockstate, @Nonnull ServerLevel world, @Nonnull BlockPos pos,
            @Nonnull RandomSource random) {
        super.tick(blockstate, world, pos, random);
        if (world.getGameRules().getBoolean(ModGameRules.RULE_RACEGRIEFING)) {
            world.setBlock(pos, ModBlocks.VOLCANIC_ROCK_HARDENED.get().defaultBlockState(), Block.UPDATE_ALL);
        } else {
            world.destroyBlock(pos, false);
        }
    }

    @Override
    public void stepOn(@Nonnull Level world, @Nonnull BlockPos pos, @Nonnull BlockState blockstate,
            @Nonnull Entity entity) {
        super.stepOn(world, pos, blockstate, entity);
        if (!(entity instanceof LivingEntity livingEntity && livingEntity.hasEffect(MobEffects.FIRE_RESISTANCE))) {
            entity.setSecondsOnFire(3);
        }
    }
}
