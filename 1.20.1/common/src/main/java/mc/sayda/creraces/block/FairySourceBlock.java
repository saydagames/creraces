package mc.sayda.creraces.block;

import mc.sayda.creraces.registry.ModItems;
import mc.sayda.creraces.registry.ModMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import org.jetbrains.annotations.Nullable;

public class FairySourceBlock extends LiquidBlock {

    public FairySourceBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    /** Prevents buckets from collecting fairy source. */
    public ItemStack pickupBlock(@Nullable Player player, LevelAccessor level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide()) return;

        // Sugar → fairy dust conversion
        // isRemoved() guard: entityInside fires once per overlapping fluid block per tick,
        // so a single item entity can be seen by source + flowing neighbours simultaneously.
        if (entity instanceof ItemEntity item && !item.isRemoved() && item.getItem().is(Items.SUGAR)) {
            int count = item.getItem().getCount();
            ItemEntity dust = new ItemEntity(level, item.getX(), item.getY(), item.getZ(),
                    new ItemStack(ModItems.FAIRY_DUST.get(), count));
            dust.setPickUpDelay(20);
            level.addFreshEntity(dust);
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.END_ROD,
                        item.getX(), item.getY() + 0.2, item.getZ(),
                        12, 0.25, 0.25, 0.25, 0.05);
            }
            item.discard();
            return;
        }

        // Living entity effects — throttled to once every 2 seconds per entity
        if (entity instanceof LivingEntity living && entity.tickCount % 40 == 0) {
            // Weak short regeneration
            living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, false, false));

            // Remove soggy — fairy source is pure water, wings don't get wet here
            // var soggy = ModMobEffects.SOGGY.get();
            // if (soggy != null) living.removeEffect(soggy);

            // Remove broken wings — the source heals damaged wings and clears the state
            var brokenWings = ModMobEffects.BROKEN_WINGS.get();
            if (brokenWings != null) {
                living.removeEffect(brokenWings);
            }
        }
    }
}
