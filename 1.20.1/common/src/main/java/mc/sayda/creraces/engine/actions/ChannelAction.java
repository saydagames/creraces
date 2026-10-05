package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ChannelingManager;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Starts a channel handled by {@link ChannelingManager}: effects and actions run while it lasts, and
 * a branch runs when it completes or is interrupted. Casting again mid-channel either panic-casts
 * (finishing early) or runs on_already_channeling.
 */
public class ChannelAction implements ActionRegistry.RaceAction {

    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "channel");

    private final int duration;
    private final boolean cancelableByDamage;
    private final boolean cancelableByMovement;
    private final boolean allowPanicCast;
    private final List<ChannelingManager.DuringEffect> duringEffects;
    private final List<ActionRegistry.RaceAction> onComplete;
    private final List<ActionRegistry.RaceAction> onPanicComplete;
    private final List<ActionRegistry.RaceAction> onInterrupt;
    private final List<ActionRegistry.RaceAction> duringActions;
    private final List<ActionRegistry.RaceAction> onAlreadyChanneling;

    public ChannelAction(int duration, boolean cancelableByDamage, boolean cancelableByMovement,
            boolean allowPanicCast, List<ChannelingManager.DuringEffect> duringEffects,
            List<ActionRegistry.RaceAction> onComplete, List<ActionRegistry.RaceAction> onPanicComplete,
            List<ActionRegistry.RaceAction> onInterrupt, List<ActionRegistry.RaceAction> duringActions,
            List<ActionRegistry.RaceAction> onAlreadyChanneling) {
        this.duration = duration;
        this.cancelableByDamage = cancelableByDamage;
        this.cancelableByMovement = cancelableByMovement;
        this.allowPanicCast = allowPanicCast;
        this.duringEffects = duringEffects;
        this.onComplete = onComplete;
        this.onPanicComplete = onPanicComplete;
        this.onInterrupt = onInterrupt;
        this.duringActions = duringActions;
        this.onAlreadyChanneling = onAlreadyChanneling;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return true;
        }

        if (ChannelingManager.isChanneling(player.getUUID())) {
            if (allowPanicCast) {
                ChannelingManager.panicCast(serverPlayer);
            } else {
                ActionRegistry.runChain(onAlreadyChanneling, player, target, slot, interactPos);
            }
            return false;
        }

        ChannelingManager.start(player, duration, cancelableByDamage, cancelableByMovement, allowPanicCast,
                duringEffects, onComplete, onPanicComplete, onInterrupt, duringActions, target, slot, interactPos);
        return true;
    }

    public static void register() {
        ActionRegistry.register(ID, json -> new ChannelAction(
                GsonHelper.getAsInt(json, "duration", 60),
                GsonHelper.getAsBoolean(json, "cancelable_by_damage", false),
                GsonHelper.getAsBoolean(json, "cancelable_by_movement", false),
                GsonHelper.getAsBoolean(json, "panic_cast", false),
                parseDuringEffects(json),
                ActionRegistry.listFromJson(json, "on_complete"),
                ActionRegistry.listFromJson(json, "on_panic_complete"),
                ActionRegistry.listFromJson(json, "on_interrupt"),
                ActionRegistry.listFromJson(json, "during_actions"),
                ActionRegistry.listFromJson(json, "on_already_channeling")));
    }

    /** Unknown or malformed effect ids are skipped. */
    private static List<ChannelingManager.DuringEffect> parseDuringEffects(JsonObject json) {
        List<ChannelingManager.DuringEffect> effects = new ArrayList<>();
        if (!json.has("during_effects")) {
            return effects;
        }
        for (JsonElement element : json.getAsJsonArray("during_effects")) {
            JsonObject entry = element.getAsJsonObject();
            ResourceLocation effectId = ResourceLocation.tryParse(entry.get("id").getAsString());
            int amplifier = GsonHelper.getAsInt(entry, "amplifier", 0);
            int ticks = GsonHelper.getAsInt(entry, "duration", 30);
            if (effectId != null) {
                MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(effectId);
                if (effect != null) {
                    effects.add(new ChannelingManager.DuringEffect(effect, amplifier, ticks));
                }
            }
        }
        return effects;
    }
}
