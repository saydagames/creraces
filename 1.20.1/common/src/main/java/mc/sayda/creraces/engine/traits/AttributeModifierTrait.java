package mc.sayda.creraces.engine.traits;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.registry.ModAttributes;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.condition.Condition;
import javax.annotation.Nullable;

public class AttributeModifierTrait implements TraitRegistry.RaceTrait {

    /**
     * The raw attribute ID as specified in JSON.
     * We store this rather than the resolved Attribute object to avoid
     * registration-order issues: third-party mods (e.g. TwilightLib) may
     * register their attributes after CreRaces loads its race JSONs.
     * Resolution is deferred to {@link #getAttribute()} at runtime.
     */
    private final ResourceLocation attributeId;
    private final ScalingValue value;
    private final AttributeModifier.Operation operation;
    @Nullable
    private final Condition condition;
    private String traitId = "";

    public AttributeModifierTrait(ResourceLocation attributeId, ScalingValue value,
            AttributeModifier.Operation operation, @Nullable Condition condition) {
        this.attributeId = attributeId;
        this.value = value;
        this.operation = operation;
        this.condition = condition;
    }

    @Override
    public void tick(Player player) {
        // No-op for attributes, they are applied statically or via AttributeIncidents
    }

    /**
     * Resolves the attribute lazily at runtime.
     * First applies known aliases, then checks the Vanilla/Apothic registry.
     * Returns null if the attribute is not registered (e.g. the mod is absent).
     */
    @Nullable
    public Attribute getAttribute() {
        if (attributeId == null) return null;

        String attrIdStr = attributeId.toString();

        // 1. Known Aliases (short names without namespace)
        String path = attributeId.getPath();
        if (attributeId.getNamespace().equals("creraces") || !attrIdStr.contains(":")) {
            Attribute aliased = resolveAlias(path.isEmpty() ? attrIdStr : path);
            if (aliased != null) return aliased;
        }

        // 2. Direct registry lookup (works for vanilla + any mod that's loaded)
        Attribute attr = BuiltInRegistries.ATTRIBUTE.getOptional(attributeId).orElse(null);

        // 3. Apothic/attributeslib resolution passthrough
        if (attr != null) {
            attr = ModAttributes.resolve(attr);
        }

        return attr;
    }

    @Nullable
    private static Attribute resolveAlias(String statKey) {
        return switch (statKey.toLowerCase()) {
            case "max_health", "hp" -> net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH;
            case "attack_damage", "ad" -> net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE;
            case "movement_speed", "speed" -> net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED;
            case "armor" -> net.minecraft.world.entity.ai.attributes.Attributes.ARMOR;
            case "ap" -> ModAttributes.ABILITY_POWER.get();
            default -> {
                if (statKey.contains("life_steal") || statKey.contains("lifesteal")) {
                    Attribute ls = BuiltInRegistries.ATTRIBUTE.get(new ResourceLocation("attributeslib", "life_steal"));
                    if (ls == null)
                        ls = BuiltInRegistries.ATTRIBUTE.get(new ResourceLocation("attributeslib", "lifesteal"));
                    yield ls;
                }
                yield null;
            }
        };
    }

    /** The raw attribute ID as specified in the race JSON. */
    public ResourceLocation getAttributeId() {
        return attributeId;
    }

    public ScalingValue getValue() {
        return value;
    }

    public AttributeModifier.Operation getOperation() {
        return operation;
    }

    @Nullable
    public Condition getCondition() {
        return condition;
    }

    @Override
    public void setTraitId(String id) {
        this.traitId = id;
    }

    @Override
    public String getTraitId() {
        return traitId;
    }

    public static void register() {
        TraitRegistry.register(new ResourceLocation(CreRaces.MODID, "attribute_modifier"), json -> {
            String attrIdStr = GsonHelper.getAsString(json, "attribute");

            // Store the raw ResourceLocation — do NOT resolve the Attribute here.
            // Resolution is deferred to getAttribute() to handle mods that register
            // attributes after race data is loaded (registration order issues).
            ResourceLocation attrId = ResourceLocation.tryParse(attrIdStr);
            if (attrId == null) {
                // Try treating it as a plain alias (no namespace)
                attrId = new ResourceLocation("creraces", attrIdStr.toLowerCase());
            }

            ScalingValue value = ScalingValue.fromJson(json, "value", 0.0);
            String opStr = GsonHelper.getAsString(json, "operation", "addition").toUpperCase();
            AttributeModifier.Operation op = AttributeModifier.Operation.valueOf(opStr);

            Condition condition = null;
            if (json.has("condition")) {
                condition = Condition.fromJson(json.getAsJsonObject("condition"));
            }

            return new AttributeModifierTrait(attrId, value, op, condition);
        });
    }
}
