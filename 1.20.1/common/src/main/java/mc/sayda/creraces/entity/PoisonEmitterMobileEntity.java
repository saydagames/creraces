package mc.sayda.creraces.entity;

import mc.sayda.creraces.registry.ModEntities;
import mc.sayda.creraces.registry.ModMobEffects;
import mc.sayda.creraces.util.IPersistentDataAccessor;
import mc.sayda.creraces.config.CreRacesConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Poison Emitter Mobile — a mobile version of the emitter that follows the
 * owner.
 * Pulshes Ratvenom to enemies within 5.5 blocks.
 * Stacks increase every 120 ticks (0.2 of lifetime).
 */
@SuppressWarnings("null")
public class PoisonEmitterMobileEntity extends TamableAnimal {

    private int ticksAlive = 0;

    public PoisonEmitterMobileEntity(EntityType<? extends PoisonEmitterMobileEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(0.6f);
        this.xpReward = 0;
        this.setNoAi(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.ARMOR, 0.0)
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new FollowOwnerGoal(this, 1.0, 10.0f, 2.0f, false));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0));
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEFINED;
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (this.level().isClientSide())
            return;

        ticksAlive++;

        // Stacks logic
        int amplifier = (int) (ticksAlive / CreRacesConfig.POISON_EMITTER_PULSE_INTERVAL.get());

        // Pulse Ratvenom to nearby entities
        var venomEffect = ModMobEffects.RAT_VENOM.get();
        if (venomEffect != null) {
            Vec3 center = this.position();
            List<LivingEntity> nearby = this.level().getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(center, center).inflate(CreRacesConfig.POISON_EMITTER_RADIUS.get()),
                    e -> e != this && (getOwner() == null
                            || !mc.sayda.creraces.team.RaceTeamManager.canHurt(e, (Player) getOwner())));

            for (LivingEntity target : nearby) {
                // Apply source tag for attribution if owner is present
                if (getOwner() != null && target instanceof IPersistentDataAccessor accessor) {
                    CompoundTag data = accessor.creraces$getPersistentData();
                    data.putString("creraces:venom_source", getOwnerUUID().toString());
                }

                target.addEffect(new MobEffectInstance(venomEffect, 102, amplifier, true, true));
            }
        }

        // Self-destruct after lifetime expires
        if (ticksAlive >= CreRacesConfig.POISON_EMITTER_LIFETIME_TICKS.get()) {
            this.level().playSound(null, this.blockPosition(),
                    net.minecraft.sounds.SoundEvents.STONE_BREAK,
                    net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 1.3f);
            this.discard();
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide() && this.isOwnedBy(player)) {
            player.startRiding(this);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return Ingredient.of().test(stack);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return ModEntities.POISON_EMITTER_MOBILE.get().create(level);
    }
}
