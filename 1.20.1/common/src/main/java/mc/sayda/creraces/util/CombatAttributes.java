package mc.sayda.creraces.util;

import mc.sayda.creraces.registry.ModAttributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Utility class for LoL-style combat attribute resolution.
 * Implements "Smart Getters" that prioritize Apothic Attributes (Forge) 
 * but fall back to CreRaces equivalents on Fabric or standalone Forge.
 */
public class CombatAttributes {

    public static final String APOTHIC_ID = "attributeslib";

    public static double getHealingReceived(LivingEntity entity) {
        return entity.getAttributeValue(ModAttributes.resolve(ModAttributes.HEALING_RECEIVED));
    }

    public static double getArmor(LivingEntity entity) {
        // We use vanilla armor as the base for Physical defense
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

    /**
     * Resolves an attribute, preferring Apothic Attributes if the mod is loaded.
     * @deprecated Use {@link ModAttributes#resolve(dev.architectury.registry.registries.RegistrySupplier)}
     */
    @Deprecated
    private static Attribute resolve(String name, Attribute fallback) {
        return fallback;
    }
}
