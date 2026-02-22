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

public class AttributeModifierTrait implements TraitRegistry.RaceTrait {

    private final Attribute attribute;
    private final double value;
    private final AttributeModifier.Operation operation;
    // We don't tick attributes, we just hold the data.
    // The AttributeIncidents class will extract this data.

    public AttributeModifierTrait(Attribute attribute, double value, AttributeModifier.Operation operation) {
        this.attribute = attribute;
        this.value = value;
        this.operation = operation;
    }

    @Override
    public void tick(Player player) {
        // No-op for attributes, they are applied statically
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public double getValue() {
        return value;
    }

    public AttributeModifier.Operation getOperation() {
        return operation;
    }

    public static void register() {
        TraitRegistry.register(new ResourceLocation(CreRaces.MODID, "attribute_modifier"), json -> {
            String attrId = GsonHelper.getAsString(json, "attribute");

            // Handle aliases again here? Or assume full ID + ScalingValue aliases
            // Let's implement basic alias support here too for consistency
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

            double value = GsonHelper.getAsDouble(json, "value", 0.0);
            String opStr = GsonHelper.getAsString(json, "operation", "addition").toUpperCase();
            AttributeModifier.Operation op = AttributeModifier.Operation.valueOf(opStr);

            return new AttributeModifierTrait(attribute, value, op);
        });
    }
}
