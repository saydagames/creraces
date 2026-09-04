package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ChannelingManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class ChannelAction implements ActionRegistry.RaceAction {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("creraces", "channel");

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
    public boolean execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable BlockPos interact_pos) {
        if (!(player instanceof ServerPlayer sp)) return true;

        if (ChannelingManager.isChanneling(player.getUUID())) {
            if (allowPanicCast) {
                ChannelingManager.panicCast(sp);
            } else {
                for (ActionRegistry.RaceAction action : onAlreadyChanneling) {
                    if (!action.execute(player, target, slot, interact_pos)) break;
                }
            }
            return false;
        }

        ChannelingManager.start(player, duration, cancelableByDamage, cancelableByMovement, allowPanicCast,
                duringEffects, onComplete, onPanicComplete, onInterrupt, duringActions, target, slot, interact_pos);
        return true;
    }

    public static void register() {
        ActionRegistry.register(ID, json -> {
            int duration = json.has("duration") ? json.get("duration").getAsInt() : 60;
            boolean cancelableByDamage = json.has("cancelable_by_damage")
                    && json.get("cancelable_by_damage").getAsBoolean();
            boolean cancelableByMovement = json.has("cancelable_by_movement")
                    && json.get("cancelable_by_movement").getAsBoolean();
            boolean allowPanicCast = json.has("panic_cast") && json.get("panic_cast").getAsBoolean();

            List<ChannelingManager.DuringEffect> effects = new ArrayList<>();
            if (json.has("during_effects")) {
                for (JsonElement e : json.getAsJsonArray("during_effects")) {
                    JsonObject obj = e.getAsJsonObject();
                    String id = obj.get("id").getAsString();
                    int amp = obj.has("amplifier") ? obj.get("amplifier").getAsInt() : 0;
                    int dur = obj.has("duration") ? obj.get("duration").getAsInt() : 30;
                    ResourceLocation loc = ResourceLocation.tryParse(id);
                    if (loc != null) {
                        net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(loc)
                                .ifPresent(eff -> effects.add(new ChannelingManager.DuringEffect(eff, amp, dur)));
                    }
                }
            }

            return new ChannelAction(duration, cancelableByDamage, cancelableByMovement, allowPanicCast,
                    effects, parseList(json, "on_complete"), parseList(json, "on_panic_complete"),
                    parseList(json, "on_interrupt"), parseList(json, "during_actions"),
                    parseList(json, "on_already_channeling"));
        });
    }

    private static List<ActionRegistry.RaceAction> parseList(JsonObject json, String key) {
        List<ActionRegistry.RaceAction> list = new ArrayList<>();
        if (!json.has(key)) return list;
        for (JsonElement e : json.getAsJsonArray(key)) {
            list.add(ActionRegistry.fromJson(e.getAsJsonObject()));
        }
        return list;
    }
}
