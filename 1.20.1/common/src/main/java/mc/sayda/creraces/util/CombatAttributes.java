package mc.sayda.creraces.util;

import dev.architectury.platform.Platform;
import mc.sayda.creraces.registry.ModAttributes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.Optional;

/**
 * Utility class for LoL-style combat attribute resolution.
 * Implements "Smart Getters" that prioritize Apothic Attributes (Forge) 
 * but fall back to CreRaces equivalents on Fabric or standalone Forge.
 */
public class CombatAttributes {

    public static final String APOTHIC_ID = "attributeslib";

    public static double getHealingReceived(LivingEntity entity) {
        return entity.getAttributeValue(resolve("healing_received", ModAttributes.HEALING_RECEIVED.get()));
    }

    public static double getArmor(LivingEntity entity) {
        // We use vanilla armor as the base for Physical defense
        return entity.getAttributeValue(Attributes.ARMOR);
    }

    public static double getArmorPierce(LivingEntity entity) {
        return entity.getAttributeValue(resolve("armor_pierce", ModAttributes.ARMOR_PIERCE.get()));
    }

    public static double getArmorShred(LivingEntity entity) {
        return entity.getAttributeValue(resolve("armor_shred", ModAttributes.ARMOR_SHRED.get()));
    }

    public static double getMagicResist(LivingEntity entity) {
        return entity.getAttributeValue(resolve("magic_resist", ModAttributes.MAGIC_RESIST.get()));
    }

    public static double getMagicPierce(LivingEntity entity) {
        return entity.getAttributeValue(resolve("magic_pierce", ModAttributes.MAGIC_PIERCE.get()));
    }

    public static double getMagicShred(LivingEntity entity) {
        return entity.getAttributeValue(resolve("magic_shred", ModAttributes.MAGIC_SHRED.get()));
    }

    /**
     * Resolves an attribute, preferring Apothic Attributes if the mod is loaded.
     */
    private static Attribute resolve(String name, Attribute fallback) {
        if (Platform.isModLoaded(APOTHIC_ID)) {
            // Note: On Fabric this returns null because the mod isn't loaded anyway.
            // On Forge, we look up the Registry entry.
            return net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.get(new ResourceLocation(APOTHIC_ID, name));
        }
        return fallback;
    }
}
