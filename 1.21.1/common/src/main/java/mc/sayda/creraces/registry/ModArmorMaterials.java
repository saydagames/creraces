package mc.sayda.creraces.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Mermaid armor materials.
 *
 * ArmorMaterial stopped being an interface in 1.21 and became a registered record, so the old
 * MermaidArmorMaterial enum is now three registry entries instead. Durability is no longer part
 * of the material either - it comes from Item.Properties.durability() at the item, using the
 * same per-slot base values vanilla uses times the old per-material multiplier.
 */
public class ModArmorMaterials {

    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(CreRaces.MODID, Registries.ARMOR_MATERIAL);

    /** Vanilla's per-slot durability base, matching the old HEALTH_PER_SLOT constant. */
    private static final Map<ArmorItem.Type, Integer> HEALTH_PER_SLOT = new EnumMap<>(ArmorItem.Type.class);
    static {
        HEALTH_PER_SLOT.put(ArmorItem.Type.BOOTS, 13);
        HEALTH_PER_SLOT.put(ArmorItem.Type.LEGGINGS, 15);
        HEALTH_PER_SLOT.put(ArmorItem.Type.CHESTPLATE, 16);
        HEALTH_PER_SLOT.put(ArmorItem.Type.HELMET, 11);
        HEALTH_PER_SLOT.put(ArmorItem.Type.BODY, 16);
    }

    public static final int BLUE_DURABILITY_MULTIPLIER = 33;
    public static final int GREEN_DURABILITY_MULTIPLIER = 15;
    public static final int YELLOW_DURABILITY_MULTIPLIER = 7;

    public static final RegistrySupplier<ArmorMaterial> BLUE = ARMOR_MATERIALS.register("blue_mermaidarmor",
            () -> build(3, 6, 8, 3, 10, SoundEvents.ARMOR_EQUIP_DIAMOND, 2.0F, 0.0F, "blue_mermaidarmor"));

    public static final RegistrySupplier<ArmorMaterial> GREEN = ARMOR_MATERIALS.register("green_mermaidarmor",
            () -> build(2, 5, 6, 2, 9, SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.0F, "green_mermaidarmor"));

    public static final RegistrySupplier<ArmorMaterial> YELLOW = ARMOR_MATERIALS.register("yellow_mermaidarmor",
            () -> build(1, 3, 5, 2, 25, SoundEvents.ARMOR_EQUIP_GOLD, 0.0F, 0.0F, "yellow_mermaidarmor"));

    /** Defense values are given boots/leggings/chestplate/helmet, matching the old slotProtections order. */
    private static ArmorMaterial build(int boots, int leggings, int chestplate, int helmet, int enchantmentValue,
            net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> equipSound,
            float toughness, float knockbackResistance, String layerName) {
        Map<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.BOOTS, boots);
        defense.put(ArmorItem.Type.LEGGINGS, leggings);
        defense.put(ArmorItem.Type.CHESTPLATE, chestplate);
        defense.put(ArmorItem.Type.HELMET, helmet);
        defense.put(ArmorItem.Type.BODY, chestplate);

        return new ArmorMaterial(
                defense,
                enchantmentValue,
                equipSound,
                () -> Ingredient.of(Items.PRISMARINE_SHARD),
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, layerName))),
                toughness,
                knockbackResistance);
    }

    /** Total durability for a slot, replacing the old ArmorMaterial.getDurabilityForType. */
    public static int durabilityFor(ArmorItem.Type type, int multiplier) {
        return HEALTH_PER_SLOT.getOrDefault(type, 13) * multiplier;
    }

    public static void register() {
        ARMOR_MATERIALS.register();
    }
}
