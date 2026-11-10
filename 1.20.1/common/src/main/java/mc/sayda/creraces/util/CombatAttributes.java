package mc.sayda.creraces.util;

import mc.sayda.creraces.registry.ModAttributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * LoL-style combat stat getters. All but healing go through ModAttributes.resolve, so Apothic
 * Attributes' version of a stat is used when that mod is loaded and CreRaces' own attribute otherwise.
 */
public class CombatAttributes {

    // Always our own: Apothic Attributes applies its healing_received itself, and effects such as
    // Grievous Wounds modify ours.
    public static double getHealingReceived(LivingEntity entity) {
        return entity.getAttributeValue(ModAttributes.HEALING_RECEIVED.get());
    }

    public static double getArmor(LivingEntity entity) {
        // Physical defense is plain vanilla armor.
        return entity.getAttributeValue(Attributes.ARMOR);
    }

    public static double getArmorPierce(LivingEntity entity) {
        return entity.getAttributeValue(ModAttributes.resolve(ModAttributes.ARMOR_PIERCE));
    }

    public static double getArmorShred(LivingEntity entity) {
        return entity.getAttributeValue(ModAttributes.resolve(ModAttributes.ARMOR_SHRED));
    }

    public static double getMagicResist(LivingEntity entity) {
        return entity.getAttributeValue(ModAttributes.resolve(ModAttributes.MAGIC_RESIST));
    }

    public static double getMagicPierce(LivingEntity entity) {
        return entity.getAttributeValue(ModAttributes.resolve(ModAttributes.MAGIC_PIERCE));
    }

    public static double getMagicShred(LivingEntity entity) {
        return entity.getAttributeValue(ModAttributes.resolve(ModAttributes.MAGIC_SHRED));
    }
}
