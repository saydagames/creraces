package mc.sayda.creraces.effect;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.registry.ModMobEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleTypes;

import java.util.Objects;

/** Doubles the player's Pehkui flight scale outside the fairy realm. Washed off by water or Soggy. */
public class FairyDustEffect extends MobEffect {

    private static final ResourceLocation FAIRY_REALM = new ResourceLocation(CreRaces.MODID, "fairy_realm");

    public FairyDustEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xCD88D6);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (player.isInWater() || player.hasEffect(Objects.requireNonNull(ModMobEffects.SOGGY.get()))) {
            player.removeEffect(Objects.requireNonNull(ModMobEffects.FAIRY_DUST_EFFECT.get()));
            return;
        }
        if (player.level().dimension().location().equals(FAIRY_REALM)) return;
        setFlightScale(player, 2.0f);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);
        if (entity instanceof ServerPlayer player)
            setFlightScale(player, 1.0f);
    }

    private static void setFlightScale(ServerPlayer player, float scale) {
        try {
            ScaleData data = ScaleTypes.FLIGHT.getScaleData(player);
            data.setScale(scale);
            data.setTargetScale(scale);
        } catch (NoClassDefFoundError pehkuiMissing) {
            // Pehkui is optional; without it fairy dust simply has no flight boost.
        } catch (Exception e) {
            CreRaces.LOGGER.debug("Pehkui scale error: {}", e.getMessage());
        }
    }
}
