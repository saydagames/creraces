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

    private final Attribute attribute;
    private final ScalingValue value;
    private final AttributeModifier.Operation operation;
    @Nullable
    private final Condition condition;
    private String traitId = "";

    public AttributeModifierTrait(Attribute attribute, ScalingValue value, AttributeModifier.Operation operation,
            @Nullable Condition condition) {
        this.attribute = attribute;
        this.value = value;
        this.operation = operation;
        this.condition = condition;
    }

    @Override
    public void tick(Player player) {
        // No-op for attributes, they are applied statically or via AttributeIncidents
    }

    public Attribute getAttribute() {
        return attribute;
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
            String attrId = GsonHelper.getAsString(json, "attribute");

            String statKey = attrId.toLowerCase();
            Attribute attribute = null;

            if (statKey.equals("max_health") || statKey.equals("hp"))
                attribute = net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH;
            else if (statKey.equals("attack_damage") || statKey.equals("ad"))
                attribute = net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE;
            else if (statKey.equals("movement_speed") || statKey.equals("speed"))
                attribute = net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED;
            else if (statKey.equals("armor"))
                attribute = net.minecraft.world.entity.ai.attributes.Attributes.ARMOR;
            else if (statKey.equals("ap"))
                attribute = ModAttributes.ABILITY_POWER.get();
            else
                attribute = BuiltInRegistries.ATTRIBUTE.get(new ResourceLocation(attrId));

            if (attribute == null) {
                CreRaces.LOGGER.error(
                        "[CreRaces] AttributeModifierTrait: unknown attribute '{}', trait will be skipped", attrId);
                return null;
            }

            ScalingValue value = ScalingValue.fromJson(json, "value", 0.0);
            String opStr = GsonHelper.getAsString(json, "operation", "addition").toUpperCase();
            AttributeModifier.Operation op = AttributeModifier.Operation.valueOf(opStr);

            Condition condition = null;
            if (json.has("condition")) {
                condition = Condition.fromJson(json.getAsJsonObject("condition"));
            }

            return new AttributeModifierTrait(attribute, value, op, condition);
        });
    }
}
