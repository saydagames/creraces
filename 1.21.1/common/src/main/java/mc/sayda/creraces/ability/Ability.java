package mc.sayda.creraces.ability;

import mc.sayda.creraces.engine.ActionRegistry.RaceAction;
import mc.sayda.creraces.engine.condition.Condition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Represents a race ability.
 * Defined via JSON in data/creraces/abilities/
 */
public class Ability {
    private final ResourceLocation id;
    private final Component name;
    private final Component description;
    private final AbilityType type;
    private final ResourceLocation icon;
    private final int cooldown;
    private final int cost;
    private final boolean persistent;
    private final List<ResourceLocation> allowedRaces;
    private final List<RaceAction> onActivate;
    private final List<RaceAction> onDeactivate;
    private final Condition condition;
    @Nullable
    private final String conditionFailMessage;
    private final List<OverlayBar> overlayBars;

    public Ability(ResourceLocation id, Component name,
            Component description, AbilityType type, ResourceLocation icon, int cooldown,
            int cost, boolean persistent, List<ResourceLocation> allowedRaces,
            List<RaceAction> onActivate,
            List<RaceAction> onDeactivate,
            Condition condition,
            @Nullable String conditionFailMessage,
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
        this.conditionFailMessage = conditionFailMessage;
        this.overlayBars = overlayBars;
    }

    public Condition condition() {
        return condition;
    }

    @Nullable
    public String conditionFailMessage() {
        return conditionFailMessage;
    }

    public ResourceLocation id() {
        return id;
    }

    public Component name() {
        return name;
    }

    public Component description() {
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

    public List<RaceAction> onActivate() {
        return onActivate;
    }

    public List<RaceAction> onDeactivate() {
        return onDeactivate;
    }

    public List<OverlayBar> overlayBars() {
        return overlayBars;
    }

    public String getTranslationKey() {
        return "ability." + id.getNamespace() + "." + id.getPath();
    }
}
