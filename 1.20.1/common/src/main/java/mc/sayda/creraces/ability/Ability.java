package mc.sayda.creraces.ability;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

/**
 * Represents a race ability.
 * Defined via JSON in data/creraces/abilities/
 */
public class Ability {
    private final ResourceLocation id;
    private final net.minecraft.network.chat.Component name;
    private final net.minecraft.network.chat.Component description;
    private final AbilityType type;
    private final ResourceLocation icon;
    private final int cooldown;
    private final int cost;
    private final boolean persistent;
    private final List<ResourceLocation> allowedRaces;
    private final List<mc.sayda.creraces.engine.ActionRegistry.RaceAction> onActivate;
    private final List<mc.sayda.creraces.engine.ActionRegistry.RaceAction> onDeactivate;
    private final mc.sayda.creraces.engine.condition.Condition condition;
    private final List<OverlayBar> overlayBars;

    public Ability(ResourceLocation id, net.minecraft.network.chat.Component name,
            net.minecraft.network.chat.Component description, AbilityType type, ResourceLocation icon, int cooldown,
            int cost, boolean persistent, List<ResourceLocation> allowedRaces,
            List<mc.sayda.creraces.engine.ActionRegistry.RaceAction> onActivate,
            List<mc.sayda.creraces.engine.ActionRegistry.RaceAction> onDeactivate,
            mc.sayda.creraces.engine.condition.Condition condition,
            List<OverlayBar> overlayBars) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.icon = icon;
        this.cooldown = cooldown;
        this.cost = cost;
        this.persistent = persistent;
        this.allowedRaces = allowedRaces;
        this.onActivate = onActivate;
        this.onDeactivate = onDeactivate;
        this.condition = condition;
        this.overlayBars = overlayBars;
    }

    public mc.sayda.creraces.engine.condition.Condition condition() {
        return condition;
    }

    public ResourceLocation id() {
        return id;
    }

    public net.minecraft.network.chat.Component name() {
        return name;
    }

    public net.minecraft.network.chat.Component description() {
        return description;
    }

    public AbilityType type() {
        return type;
    }

    public ResourceLocation icon() {
        return icon;
    }

    public int cooldown() {
        return cooldown;
    }

    public int cost() {
        return cost;
    }

    public boolean persistent() {
        return persistent;
    }

    public List<ResourceLocation> allowedRaces() {
        return allowedRaces;
    }

    public List<mc.sayda.creraces.engine.ActionRegistry.RaceAction> onActivate() {
        return onActivate;
    }

    public List<mc.sayda.creraces.engine.ActionRegistry.RaceAction> onDeactivate() {
        return onDeactivate;
    }

    public List<OverlayBar> overlayBars() {
        return overlayBars;
    }

    public String getTranslationKey() {
        return "ability." + id.getNamespace() + "." + id.getPath();
    }
}
