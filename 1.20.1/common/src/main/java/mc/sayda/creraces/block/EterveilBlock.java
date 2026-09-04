package mc.sayda.creraces.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import org.jetbrains.annotations.Nullable;

/** Blesses living entities on contact; burns undead/hostile mobs. Ported from CreRaces Classic's "Holy Water". */
public class EterveilBlock extends LiquidBlock {

    public EterveilBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    /** Prevents buckets from collecting eterveil. */
    public ItemStack pickupBlock(@Nullable Player player, LevelAccessor level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide()) return;
        if (!(entity instanceof LivingEntity living) || entity.tickCount % 40 != 0) return;

        boolean isUndeadOrHostile = (living instanceof Mob mob && mob.getMobType() == net.minecraft.world.entity.MobType.UNDEAD)
                || (living instanceof Player player
                        && mc.sayda.creraces.capability.DataUtils.getVariables(player)
                                .map(mc.sayda.creraces.capability.IPlayerVariables::isUndead).orElse(false));

        if (isUndeadOrHostile) {
            living.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
            living.hurt(level.damageSources().magic(), 4.0f);
        } else {
            living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, false, false));
        }
    }
}
