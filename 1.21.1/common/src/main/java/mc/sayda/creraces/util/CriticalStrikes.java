package mc.sayda.creraces.util;

import mc.sayda.creraces.registry.ModAttributes;
import mc.sayda.creraces.registry.ModDamageTags;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;

/**
 * Critical strikes from creraces:crit_rate and creraces:crit_damage. Any hit from an attacker with
 * these attributes can crit, unless its damage type is in #creraces:cannot_critically_strike. A crit
 * rate above 100% rolls again, and each extra crit adds 85% of the previous bonus. Vanilla jump crits
 * deal at least the crit damage.
 *
 * <p>Adapted from the critical strikes of Apothic Attributes by Shadows_of_Fire,
 * https://github.com/Shadows-of-Fire/Apothic-Attributes, under the MIT License (notice in
 * META-INF/licenses/apothic_attributes.txt). When that mod is installed it rolls the crits itself on
 * the same attributes, which ModAttributes.resolve hands over, so these methods leave hits alone.
 */
public final class CriticalStrikes {
    private CriticalStrikes() {
    }

    /** The hit's damage after rolling for crits. */
    public static float roll(LivingEntity victim, DamageSource source, float amount) {
        Holder<Attribute> critRate = ModAttributes.CRIT_RATE;
        if (ModAttributes.isApothicLoaded() || critRate == null || victim.level().isClientSide()
                || !(source.getEntity() instanceof LivingEntity attacker)
                || !attacker.getAttributes().hasAttribute(critRate)
                || source.is(ModDamageTags.CANNOT_CRITICALLY_STRIKE))
            return amount;

        double chance = attacker.getAttributeValue(critRate);
        float critDamage = critDamage(attacker);
        RandomSource random = victim.getRandom();
        float damage = amount;
        while (random.nextFloat() < chance && critDamage > 1.0F) {
            chance--;
            damage += amount * (critDamage - 1.0F);
            critDamage *= 0.85F;
        }

        return damage;
    }

    /** Shows crit particles on the victim of a crit that landed. */
    public static void showCrit(LivingEntity victim) {
        if (victim.level() instanceof ServerLevel level) {
            level.getChunkSource().broadcastAndSend(victim,
                    new ClientboundAnimatePacket(victim, ClientboundAnimatePacket.CRITICAL_HIT));
        }
    }

    /** The damage multiplier of a vanilla jump crit: vanilla's, or the player's crit damage if higher. */
    public static float jumpCritMultiplier(Player player, float vanilla) {
        return ModAttributes.isApothicLoaded() ? vanilla : Math.max(vanilla, critDamage(player));
    }

    private static float critDamage(LivingEntity entity) {
        Holder<Attribute> critDamage = ModAttributes.CRIT_DAMAGE;
        return critDamage != null && entity.getAttributes().hasAttribute(critDamage)
                ? (float) entity.getAttributeValue(critDamage)
                : 1.0F;
    }
}
