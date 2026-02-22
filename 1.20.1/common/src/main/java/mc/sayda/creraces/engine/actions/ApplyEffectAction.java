package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Action that applies a status effect (potion effect) to the player or target.
 */
public class ApplyEffectAction implements ActionRegistry.RaceAction {
    public static final ResourceLocation ID = new ResourceLocation("creraces", "apply_effect");

    private final ResourceLocation effectId;
    private final int duration;
    private final int amplifier;
    private final boolean ambient;
    private final boolean visible;
    private final boolean useTarget;
    @Nullable
    private final String scalingResource;
    private final double scalingMultiplier;

    public ApplyEffectAction(ResourceLocation effectId, int duration, int amplifier, boolean ambient, boolean visible,
            boolean useTarget, @Nullable String scalingResource, double scalingMultiplier) {
        this.effectId = effectId;
        this.duration = duration;
        this.amplifier = amplifier;
        this.ambient = ambient;
        this.visible = visible;
        this.useTarget = useTarget;
        this.scalingResource = scalingResource;
        this.scalingMultiplier = scalingMultiplier;
    }

    public static void register() {
        ActionRegistry.register(ID, json -> {
            ResourceLocation effectId = new ResourceLocation(GsonHelper.getAsString(json, "effect"));
            int duration = getIntOrBase(json, "duration", 200);
            int amplifier = getIntOrBase(json, "amplifier", 0);
            boolean ambient = GsonHelper.getAsBoolean(json, "ambient", false);
            boolean visible = GsonHelper.getAsBoolean(json, "visible", true);
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
            String scalingResource = GsonHelper.getAsString(json, "scaling_resource", null);
            double scalingMultiplier = GsonHelper.getAsDouble(json, "scaling_multiplier", 1.0);
            return new ApplyEffectAction(effectId, duration, amplifier, ambient, visible, useTarget, scalingResource,
                    scalingMultiplier);
        });
    }

    /**
     * Reads an int field that may be either a plain number or a {"base": N} scaling
     * object.
     * Only the "base" value is used — scaling is applied at runtime by the engine.
     */
    private static int getIntOrBase(com.google.gson.JsonObject json, String key, int defaultValue) {
        if (!json.has(key))
            return defaultValue;
        com.google.gson.JsonElement el = json.get(key);
        if (el.isJsonObject()) {
            // {"base": N, ...} format — take the base value
            com.google.gson.JsonObject obj = el.getAsJsonObject();
            return obj.has("base") ? obj.get("base").getAsInt() : defaultValue;
        }
        return el.getAsInt();
    }

    @Override
    public void execute(@Nonnull Player p, @Nullable LivingEntity target,
            @Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
        LivingEntity entity = useTarget ? target : p;
        if (entity == null)
            return;

        @Nonnull
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(effectId);
        if (effect != null) {
            int finalAmplifier = amplifier;
            if (scalingResource != null) {
                finalAmplifier += mc.sayda.creraces.capability.DataUtils.getVariables(p).map(vars -> {
                    double val = switch (scalingResource.toLowerCase()) {
                        case "stacks" -> vars.getStacks();
                        case "mana" -> vars.getMana();
                        case "energy" -> vars.getEnergy();
                        case "rage" -> vars.getRage();
                        case "souls" -> vars.getSouls();
                        default -> 0.0;
                    };
                    return (int) (val * scalingMultiplier);
                }).orElse(0);
            }
            entity.addEffect(new MobEffectInstance(effect, duration, finalAmplifier, ambient, visible));
        }
    }
}
