package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.EntitySelector;
import mc.sayda.creraces.engine.ManagedModifier;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.condition.ConditionRegistry;
import mc.sayda.creraces.engine.traits.AttributeModifierTrait;
import mc.sayda.creraces.registry.ModAttributes;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Adds an attribute modifier named "id" to an entity (the caster by default): "attribute" changed
 * by "value" with "operation" (add_value, add_multiplied_base, add_multiplied_total). Applying the
 * same id again replaces it; remove_modifier takes it off. With "keep_while" (a condition, players
 * only) the value is re-evaluated every second and the modifier removes itself once the condition fails.
 */
@SuppressWarnings("null")
public class AttributeModifierAction implements ActionRegistry.RaceAction {
    /** Interval in ticks at which a keep_while modifier is re-evaluated. */
    private static final int KEEP_WHILE_INTERVAL = 20;

    private final ResourceLocation attributeId;
    private final String id;
    private final ScalingValue value;
    private final JsonElement rawValue;
    private final AttributeModifier.Operation operation;
    @Nullable
    private final JsonObject keepWhileJson;
    private final EntitySelector entity;

    private AttributeModifierAction(ResourceLocation attributeId, String id, ScalingValue value, JsonElement rawValue,
            AttributeModifier.Operation operation, @Nullable JsonObject keepWhileJson, EntitySelector entity) {
        this.attributeId = attributeId;
        this.id = id;
        this.value = value;
        this.rawValue = rawValue;
        this.operation = operation;
        this.keepWhileJson = keepWhileJson;
        this.entity = entity;
    }

    /** Derived the same way as trait modifier UUIDs, so it stays stable across sessions. */
    static UUID modifierUuid(String id) {
        return UUID.nameUUIDFromBytes(modifierName(id).getBytes(StandardCharsets.UTF_8));
    }

    static String modifierName(String id) {
        return CreRaces.MODID + ":" + id;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        LivingEntity subject = entity.resolve(player, target);
        Attribute attribute = ModAttributes.getAttribute(attributeId);
        AttributeInstance instance = subject != null && attribute != null ? subject.getAttribute(attribute) : null;
        if (instance == null) {
            return true;
        }

        double amount = value.evaluate(player, target, slot, interactPos);
        if (ModAttributes.isPercentAttribute(attribute)) {
            amount /= 100.0;
        }
        UUID uuid = modifierUuid(id);
        instance.removeModifier(uuid);
        instance.addPermanentModifier(new AttributeModifier(uuid, modifierName(id), amount, operation));

        if (subject instanceof Player subjectPlayer) {
            DataUtils.getVariables(subjectPlayer).ifPresent(vars -> {
                if (keepWhileJson != null) {
                    vars.addManagedModifier(new ManagedModifier(uuid, attributeId, rawValue, operation, modifierName(id),
                            keepWhileJson, true, KEEP_WHILE_INTERVAL, subjectPlayer.tickCount + KEEP_WHILE_INTERVAL));
                } else {
                    // A plain application must not leave an earlier keep_while entry re-applying it.
                    vars.removeManagedModifier(uuid);
                }
            });
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(CreRaces.id("attribute_modifier"), json -> {
            ResourceLocation attributeId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "attribute"));
            if (attributeId == null) {
                throw new IllegalArgumentException("malformed attribute id");
            }
            JsonObject keepWhile = json.has("keep_while") ? json.getAsJsonObject("keep_while") : null;
            if (keepWhile != null) {
                // Parsed here as well so a broken condition is reported at load time, not on first use.
                ConditionRegistry.fromJson(keepWhile);
            }
            return new AttributeModifierAction(attributeId,
                    GsonHelper.getAsString(json, "id"),
                    ScalingValue.fromJson(json, "value", 0.0),
                    json.has("value") ? json.get("value") : new JsonPrimitive(0),
                    AttributeModifierTrait.parseOperation(GsonHelper.getAsString(json, "operation", "add_value")),
                    keepWhile,
                    EntitySelector.fromJson(json, EntitySelector.SELF));
        });
    }
}
